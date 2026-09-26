"""Run the native iOS suite on an available iPhone simulator."""
import argparse
import json
from pathlib import Path
import subprocess
import uuid

parser = argparse.ArgumentParser()
parser.add_argument("--device", help="Use an existing simulator; default creates and removes an isolated iPhone")
parser.add_argument("--output", default="artifacts/ios-tests")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
output = root / args.output
output.parent.mkdir(parents=True, exist_ok=True)
if output.with_suffix(".xcresult").exists():
    raise SystemExit("Choose a new --output path to preserve the previous test result.")
result = subprocess.check_output(["xcrun", "simctl", "list", "devices", "available", "--json"], text=True)
available = json.loads(result)["devices"]
devices = [device for values in available.values() for device in values]
device = args.device
owned = device is None
if owned:
    template = next(((runtime, item) for runtime, values in available.items()
                     for item in values if "iPhone" in item["name"]), None)
    if template is None:
        raise SystemExit("An available iPhone simulator runtime is required.")
    runtime, item = template
    device = subprocess.check_output([
        "xcrun", "simctl", "create", f"SmartLocker Test {uuid.uuid4()}", item["deviceTypeIdentifier"], runtime,
    ], text=True).strip()
elif not any(item["udid"] == device for item in devices):
    raise SystemExit("The requested simulator is not available.")
try:
    subprocess.run(["python3", "scripts/prepare_resources.py"], cwd=root, check=True)
    subprocess.run(["python3", "scripts/prepare_icons.py"], cwd=root, check=True)
    subprocess.run(["xcodegen", "generate", "--spec", "iosApp/project.yml"], cwd=root, check=True)
    subprocess.run([
