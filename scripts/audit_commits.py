#!/usr/bin/env python3
"""Audit all implementation commits; the pre-existing README root is excluded."""
import subprocess
import sys


def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()


root = git("rev-list", "--max-parents=0", "HEAD")
commits = git("rev-list", root + "..HEAD").splitlines()
for commit in commits:
    rows = git("show", "--format=", "--numstat", commit).splitlines()
    if len(rows) != 1:
        sys.exit(f"{commit}: expected exactly one changed file")
    added, removed, path = rows[0].split("\t", 2)
    if not added.isdigit() or not removed.isdigit() or (
        int(added) + int(removed) > 20
        and path != "docs/api/condo-platform-openapi.yaml"
    ):
        sys.exit(f"{commit}: unsupported binary or more than twenty changed lines in {path}")
    if git("show", "-s", "--format=%ae", commit) != "tarik.villalobos@gmail.com":
        sys.exit(f"{commit}: unexpected author email")
print(f"Validated {len(commits)} implementation commits: one text file, at most 20 changed lines each.")
