#
# my-iptv product customizations for Bliss OS.
# Inherited at the end of device/generic/common/device.mk (scripts/build.sh adds the hook).
#

# Values generated from box.conf
-include vendor/myiptv/generated.mk

# First-boot / every-boot setup. Bliss's /system/etc/init.sh sources
# /vendor/etc/init.sh and calls its post_bootcomplete() after boot.
PRODUCT_COPY_FILES += \
    vendor/myiptv/init.myiptv.sh:$(TARGET_COPY_OUT_VENDOR)/etc/init.sh \
    vendor/myiptv/myiptv.conf:$(TARGET_COPY_OUT_VENDOR)/etc/myiptv.conf

# Custom boot animation. /product/media is checked before Bliss's /system/media one.
ifneq ($(wildcard vendor/myiptv/bootanimation.zip),)
PRODUCT_COPY_FILES += \
    vendor/myiptv/bootanimation.zip:$(TARGET_COPY_OUT_PRODUCT)/media/bootanimation.zip
endif

# Tell apps this is a TV
ifeq ($(MYIPTV_LEANBACK),true)
PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.software.leanback.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.leanback.xml
endif
