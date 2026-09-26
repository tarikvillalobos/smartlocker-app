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
