#!/usr/bin/env bash
#
# Verify that the example TeamCode module builds in an untouched stock FTC SDK checkout.
#
# Usage:
#   bash scripts/stockfork-compat.sh <ftc-sdk-tag> [work-dir]
#
# The CurioControl artifact is expected in the local Maven repository under the JitPack
# coordinates (publish with JITPACK=true VERSION=v<version> ./gradlew publishToMavenLocal).
# The script injects mavenLocal() next to JitPack in the copied TeamCode build file so CI does
# not depend on a remote JitPack build. The published TeamCode file stays unchanged in the repo.

set -euo pipefail

SDK_REF="${1:?Usage: stockfork-compat.sh <ftc-sdk-tag> [work-dir]}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK_ROOT="${2:-${RUNNER_TEMP:-$(mktemp -d)}}"
FORK="$WORK_ROOT/FtcRobotController-${SDK_REF//\//_}"
EXAMPLE="$REPO_ROOT/examples/robot/TeamCode"

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "ANDROID_HOME or ANDROID_SDK_ROOT must point to an Android SDK" >&2
    exit 1
fi

rm -rf "$FORK"
git clone --depth 1 --branch "$SDK_REF" \
    https://github.com/FIRST-Tech-Challenge/FtcRobotController.git "$FORK"

cp -f "$EXAMPLE/build.gradle" "$FORK/TeamCode/build.gradle"
cp -f "$EXAMPLE/src/main/AndroidManifest.xml" "$FORK/TeamCode/src/main/AndroidManifest.xml"
mkdir -p "$FORK/TeamCode/src/main/res/values"
cp -f "$EXAMPLE/src/main/res/values/strings.xml" "$FORK/TeamCode/src/main/res/values/strings.xml"
cp -R "$EXAMPLE/src/main/java/." "$FORK/TeamCode/src/main/java/"

python3 - "$FORK/TeamCode/build.gradle" <<'PY'
from pathlib import Path
import sys

build_file = Path(sys.argv[1])
text = build_file.read_text(encoding="utf-8")
jitpack = "    maven { url 'https://jitpack.io' }"
if jitpack not in text:
    raise SystemExit(f"JitPack repository line missing from {build_file}")
build_file.write_text(text.replace(jitpack, f"    mavenLocal()\n{jitpack}", 1), encoding="utf-8")
PY

ANDROID_SDK="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
printf 'sdk.dir=%s\n' "$ANDROID_SDK" > "$FORK/local.properties"

cd "$FORK"
./gradlew --no-daemon :TeamCode:assembleDebug
