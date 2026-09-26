#!/bin/sh
# Source-only bootstrap: avoids committing an indivisible binary wrapper jar.
set -eu
cd "$(dirname "$0")"
version=8.14.3
base="$PWD/.tools"
archive="$base/gradle-$version-bin.zip"
mkdir -p "$base"
if [ ! -x "$base/gradle-$version/bin/gradle" ]; then
    curl -fL "https://services.gradle.org/distributions/gradle-$version-bin.zip" -o "$archive"
    curl -fL "https://services.gradle.org/distributions/gradle-$version-bin.zip.sha256" -o "$archive.sha256"
    expected=$(cat "$archive.sha256")
    actual=$(shasum -a 256 "$archive" | cut -d ' ' -f 1)
    [ "$actual" = "$expected" ] || exit 1
    unzip -q -o "$archive" -d "$base"
fi
exec "$base/gradle-$version/bin/gradle" "$@"
