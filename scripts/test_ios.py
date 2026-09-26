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
