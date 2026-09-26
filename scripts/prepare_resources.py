#!/usr/bin/env python3
"""Fetch pinned font sources and generate local static weights; no binary commits."""
import pathlib
import subprocess
import sys
import urllib.request

root = pathlib.Path(__file__).resolve().parents[1]
output = root / "app/src/commonMain/composeResources/font"
output.mkdir(parents=True, exist_ok=True)
if all((output / f"{font}_{weight}.ttf").exists()
       for font in ("jakarta", "sora") for weight in (400, 500, 600, 700)):
    sys.exit(0)
venv = root / ".tools/fonts"
python = venv / ("Scripts/python.exe" if sys.platform == "win32" else "bin/python")
if not python.exists():
    subprocess.run([sys.executable, "-m", "venv", str(venv)], check=True)
    subprocess.run([str(python), "-m", "pip", "install", "fonttools==4.59.2"], check=True)
if pathlib.Path(sys.prefix).resolve() != venv.resolve():
    subprocess.run([str(python), __file__], check=True)
    sys.exit(0)
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

fonts = {
    "jakarta": ("8b0a1d0f5983c89bc2b93f1b5fb55f9e252744b5", "plusjakartasans", "PlusJakartaSans[wght].ttf"),
    "sora": ("a926665019d3f7f25c8b1212cecbfa871e70de82", "sora", "Sora[wght].ttf"),
}
for name, (revision, folder, filename) in fonts.items():
    base = f"https://raw.githubusercontent.com/google/fonts/{revision}/ofl/{folder}/"
    source = venv / f"{name}.ttf"
    urllib.request.urlretrieve(base + urllib.parse.quote(filename), source)
    urllib.request.urlretrieve(base + "OFL.txt", venv / f"{name}-OFL.txt")
    for weight in (400, 500, 600, 700):
        font = instantiateVariableFont(TTFont(source), {"wght": weight}, inplace=False)
        font.save(output / f"{name}_{weight}.ttf")
print("Pinned Sora and Plus Jakarta Sans resources prepared.")
