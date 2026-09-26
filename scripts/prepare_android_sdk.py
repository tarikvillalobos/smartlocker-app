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
