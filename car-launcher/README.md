# Car Launcher

A touch home screen for Android car head units, made for the **ATOTO S8**
(landscape, 7" to 10" screens).

- **Clock** with the date. Tap it to open your alarms.
- **Speed** from GPS. Tap it to switch between mph and km/h.
- **Now playing** from any music app (Spotify, YouTube Music, Pandora, podcast
  apps...), with album art and previous / play-pause / next. Tap the card to open the app.
- **Dock** along the bottom with your favorite apps, plus **All apps**.
- **Hold** any app → add to/remove from dock, move it left/right, app info.

Package name: `com.myiptv.carlauncher`

## Get the APK

Every push that changes `car-launcher/` builds it on GitHub:
**Actions → Build car launcher APK → the latest run → Artifacts → car-launcher-apk**.
The download is a `.zip`; the APK is inside.

Or build locally (needs the Android SDK): `cd car-launcher && ./gradlew assembleRelease`
→ `app/build/outputs/apk/release/app-release.apk`

## Install on the ATOTO S8

1. Copy the APK to a USB stick, plug it into the head unit and open it with the
   file manager. Allow **Install unknown apps** when it asks.
2. Press **Home** and choose **Car Launcher → Always**. If it doesn't ask, go to
   **Settings → Apps → Default apps → Home app**.
3. On the home screen, set up the extras:
   - Tap **Show what's playing** and turn on **Car Launcher**. This is Android's
     notification access; the launcher only uses it to read and control the music.
   - Tap **Tap to show speed** and allow location.
4. If the ATOTO home screen comes back after the car's been off, go to
   **Settings → Apps → Car Launcher → Battery** and pick **Unrestricted**.

ATOTO's built-in radio and Bluetooth music apps may not report what they're playing
to other apps. If so, they won't show in the now-playing card; their own
screens still work as normal.

## Customize

- Default dock apps: `app/src/main/res/values/config.xml`
- Colors: `app/src/main/java/com/myiptv/carlauncher/Theme.kt`
- Text: `app/src/main/res/values/strings.xml`

Builds are signed with `keystore/car.keystore` so each new build installs over
the last one. It's in the repo on purpose and isn't secret: use your own
keystore before giving the launcher to other people.
