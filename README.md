# my-iptv: custom Android TV box firmware

Build your own branded Android image for an x86 mini PC (made for the
**Beelink Mini S12, Intel N95**), based on **[Bliss OS](https://blissos.org)**.

You get a bootable `.iso` with:
- your apps preinstalled (`apps/user/`)
- your launcher set as the Home screen
- your boot animation (`branding/`)
- TV-box defaults: screen never sleeps, no desktop taskbar, no setup wizard, your device name

Everything is set in one file: **`box.conf`**.

## How it works

```
my-iptv/                         Bliss OS source ($SRC_DIR, ~200 GB)
├── box.conf      ─┐             ├── device/generic/common/device.mk  ← one line added that
├── apps/user/*.apk├─ build.sh ─▶├── vendor/myiptv/                       inherits vendor/myiptv
├── branding/      │             │   ├── myiptv.mk      product config
└── vendor/myiptv ─┘             │   ├── init.myiptv.sh → /vendor/etc/init.sh (first-boot setup)
                                 │   └── user_app/*.apk → installed on first boot
                                 └── out/target/product/x86_64/*.iso   ← your image
```

Bliss itself is never forked. `build.sh` copies your files in and adds one hook
line, and `sync.sh` undoes that before updating Bliss, so updates stay painless.

## What you need

A separate **Linux build machine**. The Beelink is far too small to build Android.
- Ubuntu 22.04 or 24.04
- 16 GB RAM minimum for Bliss OS 16 (32 GB recommended), 24-32 GB for Bliss OS 17
- 300 GB free on an SSD
- A fast internet connection (first download ≈ 170 GB)

No spare PC? Rent a cloud VM for a day, e.g. 8 vCPU / 32 GB RAM / 400 GB SSD,
and delete it when you're done.

## Build it

```bash
git clone https://github.com/xxfluddxx/my-iptv.git && cd my-iptv

nano box.conf                 # 1. name, Android version, launcher, options
cp ~/Downloads/*.apk apps/user/   # 2. (optional) your apps
cp bootanimation.zip branding/    # 3. (optional) your boot animation

./scripts/setup-host.sh       # 4. once per build machine: installs tools (sudo)
source ~/.profile
./scripts/sync.sh             # 5. downloads Bliss OS source (hours, first time)
./scripts/build.sh            # 6. builds the ISO (1-4 hours first time, faster after)
```

The script prints where the `.iso` landed.

Changed `box.conf` or swapped an APK? Just run `./scripts/build.sh` again.
Want the latest Bliss fixes? Run `./scripts/sync.sh`, then `./scripts/build.sh`.

To check your setup without compiling: `./scripts/build.sh --stage-only`.

## Install on the Beelink

1. Flash the `.iso` to a USB stick with **Rufus** (Windows) or **balenaEtcher**.
2. Plug it into the Beelink, power on, and press **F7** for the boot menu. Pick the USB stick.
3. Choose **live mode** to test, or the **install** option to install to a drive.
   - Tip: most Mini S12 units have a 2.5" SATA bay underneath. Put a cheap SSD there and install to it.
     Windows stays on the main drive as a fallback.
4. In the BIOS (**Del** at power-on), set **Restore on AC Power Loss → Power On**
   so it starts like a real TV box.

## Settings (`box.conf`)

| Setting | What it does |
|---|---|
| `BOX_NAME` | Device name in Settings, on the network, and over Bluetooth |
| `BLISS_BRANCH` | `typhoon-x86` = Android 13 (default), `universe-x86` = Android 14 |
| `BLISS_BUILD_VARIANT` | `gapps` (Play Store), `vanilla`, `foss`, `microg` |
| `DEFAULT_LAUNCHER` | Package name made the Home screen on first boot |
| `SCREEN_NEVER_OFF` | No sleep, no screensaver |
| `DISABLE_TASKBAR` | Hide Bliss's desktop taskbar |
| `ADVERTISE_LEANBACK` | Report the system as a TV so TV-only apps install |
| `REMOVE_BLISS_USER_APPS` | Drop F-Droid, Open Camera, etc. that Bliss bundles |
| `DISABLE_PACKAGES` | Apps to disable on every boot |
| `USE_WIDEVINE` | Add Widevine L3 DRM (SD-quality streaming apps) |
| `TIMEZONE` | Default timezone |

First-boot settings apply again after each new image you flash, so a user's own
changes survive normal reboots.

## Troubleshooting

- **Build dies with "Killed" or out-of-memory errors:** not enough RAM. Add swap
  (`sudo fallocate -l 32G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile`),
  or set `JOBS` in `box.conf` lower, e.g. `JOBS="4"`.
- **`repo sync` errors:** run `./scripts/sync.sh` again. It resumes.
- **Launcher wasn't set:** check `adb logcat -s myiptv` on the box, and that
  `DEFAULT_LAUNCHER` matches the package name exactly.
- **WiFi, audio or graphics problems on the box:** these come from Bliss itself, not
  this repo. Try the other `BLISS_BRANCH`, or ask the Bliss community (Telegram: t.me/blissx86).

## Notes

- Bliss OS images aren't Google-certified, and Netflix-style apps only get
  Widevine L3 on x86 (SD quality). IPTV players aren't affected.
- Only bundle apps and streams you have the rights to use and redistribute.
