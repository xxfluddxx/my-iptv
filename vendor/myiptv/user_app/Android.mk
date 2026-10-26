#
# Ships every APK in this folder to /system/etc/user_app. Bliss's init.sh
# runs "pm install" on them at the first boot of each new build, so they
# end up as normal, updatable apps.
# Same mechanism as device/generic/common/user_app/Android.mk.
#
LOCAL_PATH := $(call my-dir)
MYIPTV_APPS := $(subst $(LOCAL_PATH)/,,$(wildcard $(LOCAL_PATH)/*.apk))

define myiptv-include-app
include $$(CLEAR_VARS)

LOCAL_LIBS := $$(shell zipinfo -1 $$(LOCAL_PATH)/$(1) | grep ^lib/ | grep -v /$$$$)

LOCAL_MODULE := myiptv_$$(basename $(1))
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_SUFFIX := $$(suffix $(1))
LOCAL_BUILT_MODULE_STEM := package.apk
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_SRC_FILES := $(1)
LOCAL_DEX_PREOPT := false
LOCAL_MODULE_RELATIVE_PATH := user_app
LOCAL_MODULE_TARGET_ARCH := $$(call get-prebuilt-src-arch,$$(notdir $$(patsubst %/,%,$$(dir $$(LOCAL_LIBS)))))
LOCAL_PREBUILT_JNI_LIBS := $$(addprefix @,$$(filter lib/$$(LOCAL_MODULE_TARGET_ARCH)/%,$$(LOCAL_LIBS)))
include $$(BUILD_PREBUILT)

ALL_DEFAULT_INSTALLED_MODULES += $$(LOCAL_INSTALLED_MODULE)
endef

$(foreach a,$(MYIPTV_APPS),$(eval $(call myiptv-include-app,$(a))))
