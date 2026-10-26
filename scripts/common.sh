#!/usr/bin/env bash
# Shared helpers for the my-iptv build scripts. Sourced, not run directly.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG_FILE="${CONFIG_FILE:-$REPO_ROOT/box.conf}"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mWARNING:\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

load_config() {
    [ -f "$CONFIG_FILE" ] || die "Config file not found: $CONFIG_FILE"
    # shellcheck source=../box.conf
    . "$CONFIG_FILE"

    : "${BOX_NAME:=MyIPTV Box}"
    : "${BOX_VERSION:=1.0}"
    : "${BLISS_BRANCH:=typhoon-x86}"
    : "${BLISS_BUILD_VARIANT:=vanilla}"
    : "${BUILD_TYPE:=userdebug}"
    : "${USE_WIDEVINE:=false}"
    : "${SCREEN_NEVER_OFF:=true}"
    : "${DISABLE_TASKBAR:=true}"
    : "${DISABLE_SETUP_WIZARD:=true}"
    : "${ADVERTISE_LEANBACK:=false}"
    : "${REMOVE_BLISS_USER_APPS:=true}"
    : "${SRC_DIR:=$HOME/bliss-src}"
    : "${JOBS:=$(nproc --all)}"

    if [ -z "${LUNCH_TARGET:-}" ]; then
        case "$BLISS_BRANCH" in
            typhoon-x86*)  LUNCH_TARGET="bliss_x86_64-$BUILD_TYPE" ;;
            universe-x86*) LUNCH_TARGET="bliss_x86_64-ap2a-$BUILD_TYPE" ;;
            *) die "Unknown BLISS_BRANCH '$BLISS_BRANCH': set LUNCH_TARGET in box.conf (see the README of that branch)." ;;
        esac
    fi

    # Short label for file names, e.g. "MyIPTV Box" -> "MyIPTVBox"
    BOX_SLUG="$(printf '%s' "$BOX_NAME" | tr -cd '[:alnum:]')"
    [ -n "$BOX_SLUG" ] || BOX_SLUG="MyIPTV"
}

# Quote a value for a POSIX shell file: abc'd -> 'abc'\''d'
shell_quote() {
    printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}
