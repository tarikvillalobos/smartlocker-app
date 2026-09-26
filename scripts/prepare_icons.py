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
                               radius=208 * scale, fill=color)
    unit = 34 * scale
    offset = side / 2 - 12 * unit
    width = 48 * scale
    def line(points):
        transformed = [(round(offset + x * unit), round(offset + y * unit)) for x, y in points]
        draw.line(transformed, fill="white", width=width, joint="curve")
        for x, y in transformed:
            draw.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill="white")
    if brand == "smartlocker":
        line([(21, 8), (12, 3), (3, 8), (3, 16), (12, 21), (21, 16), (21, 8)])
        line([(3, 8), (12, 13), (21, 8)])
        line([(12, 13), (12, 21)])
    else:
        line([(4, 18), (8, 6), (12, 18)])
        line([(5.5, 14), (10.5, 14)])
        line([(15, 6), (15, 18), (21, 18)])
    return image.resize((1024, 1024), Image.Resampling.LANCZOS)


for brand, name in (("smartlocker", "AppIcon"), ("aurora", "AuroraIcon")):
    desktop = mark(brand, rounded=True)
    desktop.save(output / f"{brand}.png")
    desktop.save(output / f"{brand}.ico", sizes=[(x, x) for x in (16, 24, 32, 48, 64, 128, 256)])
    desktop.save(output / f"{brand}.icns")
    ios = mark(brand, rounded=False)
    target = assets / f"{name}.appiconset"
    target.mkdir(parents=True, exist_ok=True)
    images = []
    slots = [("iphone", x, s) for x in (20, 29, 40, 60) for s in (2, 3)]
    slots += [("ipad", x, s) for x in (20, 29, 40, 76) for s in (1, 2)]
    slots += [("ipad", 83.5, 2), ("ios-marketing", 1024, 1)]
    for idiom, size, scale in slots:
        filename = f"icon-{size}@{scale}x.png"
        pixels = round(size * scale)
        ios.resize((pixels, pixels), Image.Resampling.LANCZOS).save(target / filename)
        images.append({"idiom": idiom, "size": f"{size}x{size}", "scale": f"{scale}x", "filename": filename})
    (target / "Contents.json").write_text(json.dumps({"images": images, "info": info}, indent=2) + "\n")
print("Generated SmartLocker and Aurora desktop icons and opaque iOS icon catalogs.")
