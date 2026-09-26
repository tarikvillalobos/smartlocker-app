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
        env=environment,
    )


def main():
    environment = os.environ.copy()
    environment["SMARTLOCKER_NATIVE_SECURE_TESTS"] = "1"
    if platform.system() != "Linux":
        return gradle(environment)
    if "--isolated-session" not in sys.argv:
        with tempfile.TemporaryDirectory(prefix="smartlocker-vault-ci-") as directory:
            for name in ("XDG_DATA_HOME", "XDG_CONFIG_HOME", "XDG_RUNTIME_DIR"):
                location = Path(directory, name.lower())
                location.mkdir(mode=0o700)
                environment[name] = str(location)
            environment.pop("GNOME_KEYRING_CONTROL", None)
            environment.pop("GNOME_KEYRING_PID", None)
            return subprocess.call(
                ["dbus-run-session", "--", sys.executable, __file__, "--isolated-session"],
                env=environment,
            )
    password = secrets.token_urlsafe(32).encode()
    started = subprocess.run(
        ["gnome-keyring-daemon", "--daemonize", "--unlock", "--components=secrets"],
        input=password, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=environment, timeout=20,
    )
    if started.returncode:
        raise RuntimeError("Could not start the isolated CI Secret Service")
    return gradle(environment)


if __name__ == "__main__":
    raise SystemExit(main())
