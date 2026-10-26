#!/usr/bin/env bash
# One-time setup of an Ubuntu 22.04/24.04 build machine for Bliss OS.
# Follows the dependency list in the Bliss OS platform_manifest README.
set -euo pipefail
. "$(dirname "$0")/common.sh"
load_config

[ -f /etc/os-release ] && . /etc/os-release
[ "${ID:-}" = "ubuntu" ] || warn "This script is written for Ubuntu. Continuing anyway."

info "Checking machine resources"
mem_gb=$(( $(awk '/MemTotal/ {print $2}' /proc/meminfo) / 1024 / 1024 ))
mkdir -p "$SRC_DIR"
disk_gb=$(df -BG --output=avail "$SRC_DIR" | tail -1 | tr -dc '0-9')
echo "    RAM: ${mem_gb} GB    Free disk at $SRC_DIR: ${disk_gb} GB"
[ "$mem_gb" -ge 15 ] || warn "Less than 16 GB RAM: the build will probably fail or swap heavily. Add swap or use a bigger machine."
[ "$disk_gb" -ge 250 ] || warn "Less than 250 GB free at $SRC_DIR: the source plus build needs about 250 GB."

info "Installing build packages (needs sudo)"
sudo dpkg --add-architecture i386
sudo apt-get update
sudo apt-get install -y \
    git-core gnupg flex bison gperf build-essential zip curl zlib1g-dev gcc-multilib g++-multilib \
    libc6-dev-i386 x11proto-core-dev libx11-dev lib32z-dev ccache libgl1-mesa-dev libxml2-utils \
    xsltproc unzip squashfs-tools python3-mako libssl-dev ninja-build lunzip syslinux syslinux-utils \
    gettext genisoimage bc xorriso xmlstarlet meson glslang-tools git-lfs libelf-dev aapt zstd \
    rdfind nasm rsync python-is-python3

if [ "${BLISS_BRANCH#typhoon}" != "$BLISS_BRANCH" ]; then
    # Bliss OS 16's prebuilt toolchain still wants libncurses5.
    sudo apt-get install -y lib32ncurses-dev libncurses6 libncurses6:i386
    for lib in /lib/x86_64-linux-gnu/libncurses /lib/x86_64-linux-gnu/libncursesw /lib/x86_64-linux-gnu/libtinfo \
               /lib/i386-linux-gnu/libncurses /lib/i386-linux-gnu/libncursesw /lib/i386-linux-gnu/libtinfo; do
        if [ -e "$lib.so.6" ] && [ ! -e "$lib.so.5" ]; then
            sudo ln -s "$lib.so.6" "$lib.so.5"
        fi
    done
fi

git lfs install

info "Installing the 'repo' tool"
mkdir -p "$HOME/bin"
if ! command -v repo >/dev/null 2>&1; then
    curl -fsSL https://storage.googleapis.com/git-repo-downloads/repo -o "$HOME/bin/repo"
    chmod a+x "$HOME/bin/repo"
fi
grep -q 'HOME/bin' "$HOME/.profile" 2>/dev/null || echo 'export PATH="$HOME/bin:$PATH"' >> "$HOME/.profile"

info "Installing the Rust toolchain via rustup (Bliss needs it instead of Ubuntu's rustc)"
sudo apt-get remove -y rustc bindgen cargo 2>/dev/null || true
if ! command -v rustup >/dev/null 2>&1 && [ ! -x "$HOME/.cargo/bin/rustup" ]; then
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
fi
# shellcheck disable=SC1091
. "$HOME/.cargo/env"
rustup target add x86_64-linux-android i686-linux-android
cargo install cargo-ndk
cargo install --version 0.69.1 bindgen-cli
cargo install cbindgen

if [ -z "$(git config --global user.email || true)" ]; then
    warn "Git has no identity set. 'repo' needs one. Run:"
    echo '    git config --global user.name  "Your Name"'
    echo '    git config --global user.email "you@example.com"'
fi

info "Done. Log out and back in (or run: source ~/.profile), then run ./scripts/sync.sh"
