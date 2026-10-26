# Box Launcher

A minimal home screen for the TV box: big clock, a **Favorites** row, and an
**Apps** row. It's built for the remote: arrow keys to move, OK to open.

- **Hold OK** on any app → add/remove favorite, move it left/right, app info
- Shows each app's TV banner when it has one, otherwise its icon
- Top bar: box name (from `BOX_NAME` in `box.conf`), Wi-Fi and Settings shortcuts
- Back does nothing on the home screen, like a real TV box

Package name: `com.myiptv.launcher`

## Get the APK

Every push that changes `launcher/` builds it on GitHub:
**Actions → Build launcher APK → the latest run → Artifacts → box-launcher-apk**.

Or build locally (needs the Android SDK): `cd launcher && ./gradlew assembleRelease`
→ `app/build/outputs/apk/release/app-release.apk`

## Use it

- **Now, on the box:** install the APK, press Home, choose **Box Launcher → Always**.
- **In your firmware:** copy the APK to `apps/user/` and set
  `DEFAULT_LAUNCHER="com.myiptv.launcher"` in `box.conf`.

## Customize

- Default favorites: `app/src/main/res/values/config.xml`
- Colors: `app/src/main/java/com/myiptv/launcher/Theme.kt`
- Fallback box name: `app/src/main/res/values/strings.xml`

Builds are signed with `keystore/box.keystore` so each new build installs over
the last one. It's in the repo on purpose and isn't secret: use your own
keystore before giving the launcher to other people.
