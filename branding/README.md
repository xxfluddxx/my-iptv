# Branding

## Boot animation
Put a file named `bootanimation.zip` here (`branding/bootanimation.zip`). It
replaces the Bliss OS boot animation. If there's no file, the Bliss one stays.

Format (standard Android boot animation):
```
bootanimation.zip   (stored, NOT compressed: zip -0)
├── desc.txt        e.g. "1920 1080 30" then "p 0 0 part0"
└── part0/
    ├── 00000.png
    ├── 00001.png
    └── ...
```
Build it with: `cd my_anim && zip -0 -r ../bootanimation.zip desc.txt part0`
