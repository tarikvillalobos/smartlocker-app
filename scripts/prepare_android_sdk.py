#!/usr/bin/env python3
"""Copy the tested Android SDK packages into an isolated, ignored directory."""
import argparse
import pathlib
import shutil
import sys

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--source", type=pathlib.Path, required=True,
                    help="Existing Android SDK root; it will only be read")
parser.add_argument("--destination", type=pathlib.Path,
                    default=pathlib.Path(".tools/android-sdk"))
arguments = parser.parse_args()
source = arguments.source.expanduser().resolve()
destination = arguments.destination.expanduser().resolve()
packages = ("platforms/android-35", "build-tools/35.0.0", "platform-tools", "licenses")
if source == destination or source in destination.parents:
    sys.exit("Choose an isolated destination outside the source SDK.")
for package in packages:
    if not (source / package).is_dir():
        sys.exit(f"Required installed SDK package is missing: {package}")
properties = (source / "platforms/android-35/source.properties").read_text()
if "AndroidVersion.ApiLevel=35" not in properties.splitlines():
    sys.exit("The selected source platform is not Android API 35.")
for group, allowed in (("platforms", "android-35"), ("build-tools", "35.0.0")):
    existing = destination / group
    if existing.exists() and any(path.name != allowed for path in existing.iterdir()):
        sys.exit(f"Destination contains additional {group}; choose a fresh destination.")
for package in packages:
    target = destination / package
    if target.is_symlink():
        sys.exit(f"Destination must contain copies, not symlinks: {target}")
    shutil.copytree(source / package, target, dirs_exist_ok=True)
print(f"Isolated SDK prepared: {destination}")
print("Set ANDROID_HOME and ANDROID_SDK_ROOT to this directory before Gradle.")
print("An existing local.properties sdk.dir overrides environment configuration.")
