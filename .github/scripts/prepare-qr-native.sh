#!/usr/bin/env bash
set -euo pipefail
# Matches app/src/main/cpp/CMakeLists.txt; Telegram pins tlottie independently of Rust.
rustup toolchain install 1.99.1 --profile minimal
rustup target add --toolchain 1.99.1 \
  aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
"${ANDROID_HOME:?ANDROID_HOME is required}/cmdline-tools/latest/bin/sdkmanager" \
  'ndk;27.2.12479018' 'cmake;3.22.1'
