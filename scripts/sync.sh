#!/usr/bin/env bash
# Download (first run) or update the Bliss OS source into $SRC_DIR.
# The first sync downloads about 170 GB and takes hours.
set -euo pipefail
. "$(dirname "$0")/common.sh"
load_config

export PATH="$HOME/bin:$PATH"
command -v repo >/dev/null 2>&1 || die "'repo' not found. Run ./scripts/setup-host.sh first."

mkdir -p "$SRC_DIR"
cd "$SRC_DIR"

if [ -d .repo ]; then
    # Undo our edits to Bliss's own files so repo can update them cleanly.
    # build.sh re-applies them every time.
    if [ -d device/generic/common/.git ]; then
        git -C device/generic/common checkout -- device.mk user_app 2>/dev/null || true
    fi
    current=$(git -C .repo/manifests rev-parse --abbrev-ref HEAD 2>/dev/null || true)
    info "Updating existing source in $SRC_DIR (branch $BLISS_BRANCH)"
    if [ -n "$current" ] && [ "$current" != "default" ] && [ "$current" != "$BLISS_BRANCH" ]; then
        warn "Switching manifest from '$current' to '$BLISS_BRANCH'. Run 'make clean' (or delete out/) before building."
    fi
fi

info "repo init -b $BLISS_BRANCH"
repo init -u https://github.com/BlissOS/platform_manifest.git -b "$BLISS_BRANCH" --git-lfs

info "repo sync (be patient)"
repo sync -c --force-sync --no-tags --no-clone-bundle -j"$JOBS" --optimized-fetch --prune

info "Source ready in $SRC_DIR. Next: ./scripts/build.sh"
