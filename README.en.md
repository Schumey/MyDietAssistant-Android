# MyDietAssistant for Android

[Deutsch](README.md) · **English**

Android app for [MyDietAssistant](https://github.com/Schumey/MyDietAssistant), the food diary that runs as a Home Assistant app. It shows the familiar interface full screen and adds what only works on a phone:

- **Full screen** without the HA sidebar; login through the normal HA login page (the app never sees your password)
- **System barcode scanner** (Google Play services, no HTTPS and no camera permission needed)
- **Home screen widget**: today's calories, what is left of your goal, buttons “Scan” and “Add”
- **Health Connect:** take over weight, steps and active calories from the phone
- **Print the report** or save it as PDF
- **Updates** right inside the app

Your data stays in Home Assistant. The app only stores your server address and the numbers for the widget.

## Download

**[Latest APK](https://github.com/Schumey/MyDietAssistant-Android/releases/latest)** (Android 8 or newer). The app is not in the Play Store, so Android asks during installation whether to allow apps from this source.

You need MyDietAssistant in Home Assistant (1.29.0 or newer for scanner, widget and Health Connect). Step-by-step guide: [Android app](https://github.com/Schumey/MyDietAssistant/blob/main/docs/en/android.md).

## Updates

On start, at most once a day, the app looks here among the releases for a newer version and offers it with **Install**, **Later** or **Skip**. By hand: in the interface *Settings → Android app → Look for app updates*. On the first update, Android asks whether MyDietAssistant may install apps; afterwards tap **Install** once more.

Changes per version: [CHANGELOG.md](CHANGELOG.md) (German).

## Development

Building, publishing and testing are described in the German [README](README.md#bauen). In short: `./gradlew assembleRelease` with a `local.properties` pointing to the Android SDK; releases are tagged `v<versionName>` and carry one `.apk` asset, which the in-app update check picks up. APKs are signed with the debug key of the building machine, so updates must always be built on the same machine.

## License

MIT, see [LICENSE](LICENSE).
