"""Run only isolated synthetic desktop vault tests; Linux uses its own D-Bus session."""
from pathlib import Path
import os
import platform
import secrets
import subprocess
import sys
import tempfile


def gradle(environment):
    return subprocess.call(
        [sys.executable, "scripts/gradle.py", ":app:desktopTest", "--tests",
         "app.smartlocker.NativeSecureStorageTest", "--no-daemon"],
        env=environment,
    )


def main():
    environment = os.environ.copy()
