#!/usr/bin/env python3
"""Reject commits touching multiple files, binaries, or more than 20 lines."""
import subprocess
import sys

rows = subprocess.check_output(
    ["git", "diff", "--cached", "--numstat"], text=True
).strip().splitlines()
valid = len(rows) == 1
if valid:
    added, removed, _ = rows[0].split("\t", 2)
    valid = added.isdigit() and removed.isdigit()
    valid = valid and int(added) + int(removed) <= 20
if not valid:
    sys.exit("Commit rejected: exactly one text file and at most 20 changed lines.")
subprocess.run(["git", "diff", "--cached", "--check"], check=True)
subprocess.run(["git", "diff", "--cached", "--no-ext-diff"], check=True)
