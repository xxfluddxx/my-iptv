#
# Installed as /vendor/etc/init.sh. Bliss's /system/etc/init.sh sources this
# file on every run and calls post_bootcomplete() once Android has booted.
# It runs as root, so keep it to function definitions: no work at top level.
#

MYIPTV_CONF=/vendor/etc/myiptv.conf
MYIPTV_MARKER=/data/vendor/myiptv_setup_done

[ -f $MYIPTV_CONF ] && . $MYIPTV_CONF

function myiptv_log()
{
	log -t myiptv "$*"
}

function myiptv_set_launcher()
{
	pkg=$MYIPTV_DEFAULT_LAUNCHER
	[ -n "$pkg" ] || return 0

	if ! pm path "$pkg" > /dev/null 2>&1; then
		myiptv_log "launcher $pkg is not installed, skipping"
		return 0
	fi
	if cmd role add-role-holder --user 0 android.app.role.HOME "$pkg"; then
		myiptv_log "home launcher set to $pkg"
	else
		cmd package set-home-activity --user 0 "$pkg" && myiptv_log "home launcher set to $pkg (set-home-activity)"
	fi
}

# Runs once per new build (first boot, and again after you flash an update),
# so the user can change these later and keep the change.
function myiptv_first_boot()
{
	myiptv_log "applying first-boot settings"

	if [ -n "$MYIPTV_DEVICE_NAME" ]; then
		settings put global device_name "$MYIPTV_DEVICE_NAME"
		settings put secure bluetooth_name "$MYIPTV_DEVICE_NAME"
	fi

	if [ "$MYIPTV_SCREEN_NEVER_OFF" = "true" ]; then
		settings put system screen_off_timeout 2147483647
		settings put secure screensaver_enabled 0
		settings put global stay_on_while_plugged_in 7
	fi

	myiptv_set_launcher
}

function myiptv_every_boot()
{
	for pkg in $MYIPTV_DISABLE_PACKAGES; do
		pm disable-user --user 0 "$pkg" > /dev/null 2>&1 && myiptv_log "disabled $pkg"
	done
}

function post_bootcomplete()
{
	build_id=$(getprop ro.build.date.utc)
	if [ "$(cat $MYIPTV_MARKER 2> /dev/null)" != "$build_id" ]; then
		myiptv_first_boot
		echo "$build_id" > $MYIPTV_MARKER
	fi
	myiptv_every_boot
}
