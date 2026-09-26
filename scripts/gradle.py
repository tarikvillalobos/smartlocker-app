#!/usr/bin/env python3
"""Cross-platform, checksum-verified Gradle bootstrap without a binary wrapper jar."""
import hashlib
import os
import pathlib
import re
import subprocess
import sys
import urllib.request
import zipfile

root = pathlib.Path(__file__).resolve().parents[1]
os.chdir(root)
versions = (root / "gradle/libs.versions.toml").read_text()
version = re.search(r'^gradle = "([0-9.]+)"', versions, re.M).group(1)
tools = root / ".tools"
executable = tools / f"gradle-{version}/bin" / ("gradle.bat" if os.name == "nt" else "gradle")
if not executable.exists():
    tools.mkdir(exist_ok=True)
    archive = tools / f"gradle-{version}-bin.zip"
