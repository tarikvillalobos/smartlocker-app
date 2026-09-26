"""Run the native iOS suite on an available iPhone simulator."""
import argparse
import json
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--device", help="Existing simulator UUID; defaults to an available iPhone")
parser.add_argument("--output", default="artifacts/ios-tests")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
output = root / args.output
output.parent.mkdir(parents=True, exist_ok=True)
if output.with_suffix(".xcresult").exists():
    raise SystemExit("Choose a new --output path to preserve the previous test result.")
result = subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "--json"], text=True)
devices = [device for values in json.loads(result)["devices"].values() for device in values]
device = args.device or next((item["udid"] for item in devices if "iPhone" in item["name"]), None)
if device is None or not any(item["udid"] == device for item in devices):
    raise SystemExit("An available iPhone simulator is required.")
subprocess.run(["python3", "scripts/prepare_resources.py"], cwd=root, check=True)
subprocess.run(["python3", "scripts/prepare_icons.py"], cwd=root, check=True)
subprocess.run(["xcodegen", "generate", "--spec", "iosApp/project.yml"], cwd=root, check=True)
subprocess.run([
    "xcodebuild", "-project", "iosApp/SmartLocker.xcodeproj", "-scheme", "SmartLocker",
    "-configuration", "Debug", "-destination", f"platform=iOS Simulator,id={device}",
    "-derivedDataPath", str(output), "-resultBundlePath", str(output.with_suffix(".xcresult")),
    "-parallel-testing-enabled", "NO", "CODE_SIGNING_ALLOWED=NO", "test",
], cwd=root, check=True)
