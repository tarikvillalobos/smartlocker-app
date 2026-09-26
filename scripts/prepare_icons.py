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
