package io.github.schumey.mydiet

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks for a newer APK among the GitHub releases of this repository and installs it on request.
 *
 * - [checkDaily] runs on start, at most once a day, and only speaks up when there is an update.
 * - [checkNow] is the button in the settings: it also says "up to date" or why the check failed.
 * Releases are tagged `v<versionName>` and carry one `.apk` asset; the APK must be signed with the same key.
 */
class Updater(private val activity: Activity) {
    private val prefs = activity.getSharedPreferences("updater", Context.MODE_PRIVATE)

    data class Release(val version: String, val notes: String, val apkUrl: String, val page: String)

    fun checkDaily() {
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("last_check", 0) < DAY_MS) return
        prefs.edit().putLong("last_check", now).apply()
        check(manual = false)
    }

    fun checkNow() = check(manual = true)

    private fun check(manual: Boolean) {
        Thread {
            val release = try { fetchLatest() } catch (_: Exception) { null }
            activity.runOnUiThread {
                if (activity.isFinishing) return@runOnUiThread
                val current = BuildConfig.VERSION_NAME
                when {
                    release == null -> if (manual) toast(activity.getString(R.string.update_failed))
                    !newer(release.version, current) -> if (manual) toast(activity.getString(R.string.update_none, current))
                    !manual && prefs.getString("skip", "") == release.version -> Unit
                    else -> offer(release, current)
                }
            }
        }.start()
    }

    private fun fetchLatest(): Release? {
        val con = URL(LATEST).openConnection() as HttpURLConnection
        con.connectTimeout = 10_000
        con.readTimeout = 10_000
        con.setRequestProperty("Accept", "application/vnd.github+json")
        con.setRequestProperty("User-Agent", "MyDietAssistant-Android")
        try {
            if (con.responseCode != 200) return null
            val o = JSONObject(con.inputStream.bufferedReader().use { it.readText() })
            val assets = o.optJSONArray("assets") ?: return null
            var apk = ""
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) { apk = a.optString("browser_download_url"); break }
            }
            if (apk.isEmpty()) return null
            return Release(o.optString("tag_name").removePrefix("v"), o.optString("body").trim(), apk, o.optString("html_url"))
        } finally {
            con.disconnect()
        }
    }

    private fun offer(r: Release, current: String) {
        val notes = r.notes.lines().filter { it.isNotBlank() }.take(12).joinToString("\n")
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.update_title, r.version))
            .setMessage(activity.getString(R.string.update_text, current) + if (notes.isNotEmpty()) "\n\n" + notes else "")
            .setPositiveButton(R.string.update_install) { _, _ -> install(r) }
            .setNegativeButton(R.string.update_later, null)
            .setNeutralButton(R.string.update_skip) { _, _ -> prefs.edit().putString("skip", r.version).apply() }
            .show()
    }

    private fun install(r: Release) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            // Android asks once per app whether it may install other apps; afterwards the user taps "Install" again
            toast(activity.getString(R.string.update_allow))
            try {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
            } catch (_: Exception) {
            }
            return
        }
        val dir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: run { openPage(r); return }
        dir.listFiles()?.filter { it.name.endsWith(".apk") }?.forEach { it.delete() }   // old downloads
        val file = File(dir, "MyDietAssistant-${r.version}.apk")
        val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(DownloadManager.Request(Uri.parse(r.apkUrl))
            .setTitle("MyDietAssistant ${r.version}")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file)))
        toast(activity.getString(R.string.update_downloading))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                try { activity.unregisterReceiver(this) } catch (_: Exception) { }
                val ok = dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
                    c.moveToFirst() && c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
                }
                if (!ok || !file.exists()) { toast(activity.getString(R.string.update_failed)); return }
                val uri = FileProvider.getUriForFile(activity, activity.packageName + ".files", file)
                activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") activity.registerReceiver(receiver, filter)
    }

    private fun openPage(r: Release) {
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.page))) } catch (_: Exception) { }
    }

    private fun toast(text: String) = Toast.makeText(activity, text, Toast.LENGTH_LONG).show()

    companion object {
        private const val LATEST = "https://api.github.com/repos/Schumey/MyDietAssistant-Android/releases/latest"
        private const val APK_MIME = "application/vnd.android.package-archive"
        private const val DAY_MS = 24L * 60 * 60 * 1000

        /** "0.10.0" > "0.9.1"; anything after a "-" (0.4.0-beta) is ignored. */
        fun newer(candidate: String, current: String): Boolean {
            fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
