#!/usr/bin/env python3
"""Generate native app icons from the two vector marks; no binary source files."""
import argparse
import json
from pathlib import Path
import subprocess
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
root = parser.parse_args().root.resolve()
venv = root / ".tools/icons"
python = venv / ("Scripts/python.exe" if sys.platform == "win32" else "bin/python")
if Path(sys.prefix).resolve() != venv.resolve():
    if not python.exists():
        subprocess.run([sys.executable, "-m", "venv", str(venv)], check=True)
    installed = subprocess.run([str(python), "-c", "import PIL; assert PIL.__version__ == '11.3.0'"],
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if installed.returncode:
        subprocess.run([str(python), "-m", "pip", "install", "Pillow==11.3.0"], check=True)
    subprocess.run([str(python), __file__, "--root", str(root)], check=True)
    sys.exit(0)
from PIL import Image, ImageDraw

output = root / ".tools/packaging/icons"
output.mkdir(parents=True, exist_ok=True)
assets = root / "iosApp/GeneratedAssets.xcassets"
assets.mkdir(parents=True, exist_ok=True)
info = {"author": "xcode", "version": 1}
(assets / "Contents.json").write_text(json.dumps({"info": info}, indent=2) + "\n")


def mark(brand, rounded):
    scale = 2
    side = 1024 * scale
    color = "#007A5E" if brand == "smartlocker" else "#6652B8"
    image = Image.new("RGBA" if rounded else "RGB", (side, side), (0, 0, 0, 0) if rounded else color)
    draw = ImageDraw.Draw(image)
    if rounded:
        draw.rounded_rectangle((64 * scale, 64 * scale, 960 * scale, 960 * scale),
