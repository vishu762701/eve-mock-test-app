#!/bin/sh
# Portable launcher: use the standard wrapper when bundled, otherwise installed Gradle.
set -eu
ROOT=$(CDPATH= cd "$(dirname "$0")" && pwd)
if [ -f "$ROOT/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$ROOT/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "ERROR: Gradle not found. Install Gradle 8.7 and JDK 21, or use Build Eve APK via GitHub Actions." >&2
exit 127
