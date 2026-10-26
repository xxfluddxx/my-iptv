# Apps to bake into your image

Put `.apk` files in this folder (`apps/user/`). Every APK here gets installed
on the first boot of each new build as a **normal app**: it can be updated
from its store or by sideloading, and the user can uninstall it.

Tips:
- Use **x86_64** or universal APKs. ARM-only apps only work if Bliss's ARM
  translation is included (it usually is for the gapps variant).
- Use plain file names like `projectivy-launcher.apk`.
- To make one of them the Home screen, put its package name in
  `DEFAULT_LAUNCHER` in `box.conf`.
  (Find a package name with: `aapt dump badging app.apk | grep package`)
- Only bundle apps you're allowed to redistribute.
