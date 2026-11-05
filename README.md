# MyDietAssistant für Android

**Deutsch** · [English](README.en.md)

Android-App für [MyDietAssistant](https://github.com/Schumey/MyDietAssistant), das Ernährungstagebuch als Home-Assistant-App. Sie zeigt die gewohnte Oberfläche im Vollbild und ergänzt, was nur auf dem Handy geht:

- **Vollbild** ohne HA-Seitenleiste; Anmeldung über die normale HA-Anmeldeseite (die App sieht Dein Passwort nicht)
- **Barcode-Scanner des Systems** (Google-Play-Dienste, ohne HTTPS und ohne Kamera-Berechtigung)
- **Widget** für den Startbildschirm: Kalorien von heute, Rest bis zum Ziel, Knöpfe „Scannen“ und „Eintragen“
- **Health Connect:** Gewicht, Schritte und aktive Kalorien vom Handy übernehmen
- **Bericht drucken** oder als PDF speichern
- **Updates** direkt in der App

Deine Daten bleiben in Home Assistant. Die App speichert nur die Adresse Deines Servers und die Zahlen für das Widget.

## Herunterladen

**[Neueste APK](https://github.com/Schumey/MyDietAssistant-Android/releases/latest)** (Android 8 oder neuer). Die App gibt es nicht im Play Store; Android fragt deshalb bei der Installation, ob Du Apps aus dieser Quelle zulassen willst.

Voraussetzung ist MyDietAssistant in Home Assistant (für Scanner, Widget und Health Connect ab Version 1.29.0). Schritt-für-Schritt-Anleitung: [Android-App](https://github.com/Schumey/MyDietAssistant/blob/main/docs/de/android.md).

## Updates

Die App schaut beim Start höchstens einmal am Tag hier unter den Releases nach einer neueren Version und bietet sie mit **Installieren**, **Später** oder **Überspringen** an. Von Hand: in der Oberfläche *Einstellungen → Android-App → Nach App-Updates suchen*. Beim ersten Update fragt Android, ob MyDietAssistant Apps installieren darf; danach tippst Du noch einmal auf **Installieren**.

Änderungen je Version: [CHANGELOG.md](CHANGELOG.md).

## Aufbau

Die App ist eine Hülle (Kotlin, ohne Jetpack Compose) um die Weboberfläche. Sie lädt Home Assistant im WebView, liest nach der Anmeldung das HA-Token aus `localStorage` (`hassTokens`), holt per WebSocket (`supervisor/api`) eine Ingress-Sitzung und öffnet die App im Vollbild. Ohne Administratorrechte fällt sie auf das Panel in der HA-Oberfläche zurück.

| Datei | Aufgabe |
|---|---|
| `MainActivity.kt` | WebView, Ingress-Wechsel, Kamera/Datei/Download, `window.open` und Druck, Widget-Aktionen (`onNewIntent`, `launchMode="singleTask"`) |
| `Ingress.kt` | JavaScript, das mit dem HA-Token die Ingress-Sitzung holt und alle 40 Minuten erneuert |
| `NativeBridge.kt` | `window.MDAApp` für die Oberfläche: `scanBarcode()`, `summary(json)`, `pendingAction()`, `healthStatus()`, `healthRead(tage, fragen)`, `print(titel)`, `checkUpdate()`, `version()` |
| `Health.kt` | Health Connect lesen (Gewicht, Schritte, aktive Kalorien), Berechtigung anfragen |
| `PrivacyActivity.kt` | Datenschutz-Hinweis, den Health Connect beim Berechtigungsdialog verlangt |
| `Widget.kt` | `AppWidgetProvider`; Zahlen aus `SharedPreferences`, die die Oberfläche über `summary` setzt |
| `Updater.kt` | neuestes Release über die GitHub-API, Download per `DownloadManager`, Installation über `FileProvider` |
| `SetupActivity.kt`, `Prefs.kt` | Adresse und App-Kennung |

Das Gegenstück liegt im Haupt-Repo in `mydiet/app/static/app.js` (`nativeScan`, `healthRead`, `MDAApp.summary` in `renderDiary`, Karte „Android-App“ in `renderSettings`, `window.mdaAction`) und `mydiet/app/extras19.py` (`POST /api/health/import`). Die Oberfläche prüft jede Methode vor dem Aufruf, damit ältere APKs weiterlaufen. Änderungen an der Oberfläche brauchen keine neue APK, nur das Update der HA-App.

## Bauen

Voraussetzung: Android Studio (JDK und SDK 36 liegen dabei). Eine Datei `local.properties` mit `sdk.dir=<Pfad zum Android SDK>` anlegen (nicht im Repo), `JAVA_HOME` auf das JBR von Android Studio setzen, dann:

```bash
./gradlew assembleRelease
```

Die APK liegt unter `app/build/outputs/apk/release/app-release.apk`. Sie ist mit dem Debug-Schlüssel des bauenden Rechners signiert (kein Keystore nötig). **Updates lassen sich nur mit demselben Schlüssel installieren**, auch über die Update-Prüfung: Releases deshalb immer vom selben Rechner bauen (`~/.android/debug.keystore`), sonst muss die App vorher deinstalliert werden.

## Veröffentlichen

1. `versionCode` und `versionName` in `app/build.gradle.kts` erhöhen, Abschnitt in `CHANGELOG.md` ergänzen.
2. `./gradlew assembleRelease`, die APK nach `MyDietAssistant-android-<Version>.apk` kopieren.
3. Commit, `git push`, dann `gh release create v<Version> <apk> --title "MyDietAssistant für Android <Version>" --notes-file <notizen>`. Die Update-Prüfung nimmt das Release, das GitHub als „Latest“ führt, und dessen erste `.apk`-Datei; der Tag muss `v` + `versionName` sein.

## Testen

`tools/fakeha.py` ist ein Fake-HA (FastAPI) für den Emulator: Startseite mit `hassTokens`, WebSocket mit `auth` und `supervisor/api`, Ingress-Seite. Mit `MDA_UPSTREAM=http://127.0.0.1:8099` reicht er den Ingress-Pfad an einen lokal laufenden MyDietAssistant durch (echte Oberfläche aus dem Haupt-Repo):

```bash
cd ../MyDietAssistant/mydiet/app && MYDIET_DB=/tmp/emu.db python -m uvicorn main:app --port 8099
cd tools && MDA_UPSTREAM=http://127.0.0.1:8099 python -m uvicorn fakeha:app --host 0.0.0.0 --port 8123
```

In der App als Adresse `http://10.0.2.2:8123` eintragen. Scanner und Widget-Knöpfe lassen sich per `adb shell am start -n io.github.schumey.mydiet/.MainActivity --es mda_action scan` (oder `add`) auslösen.

## Geprüft / ungeprüft

- Im Emulator (API 37 mit Play-Diensten) gegen den Fake-HA mit echter Oberfläche geprüft: Vollbild, nativer Scanner (öffnen, abbrechen), Widget mit Stand und Knöpfen, Bericht öffnen und Druckdialog, Health-Connect-Berechtigung und Import (ohne echte Daten), Update-Prüfung mit Download und Installation.
- Am echten HA (Pixel 7): Anmeldung und Vollbild.
- Ungeprüft: echter Barcode, Health Connect mit echten Daten, Sitzungserneuerung nach 40 Minuten, Datei-Upload und Download am echten HA, Verhalten ohne Play-Dienste.

## Lizenz

MIT, siehe [LICENSE](LICENSE).
