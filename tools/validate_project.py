#!/usr/bin/env python3
"""Validate a Şirin Engine project package."""

from __future__ import annotations

import json
import sys
from pathlib import Path

def fail(message: str) -> int:
    print(f"INVALID PROJECT: {message}", file=sys.stderr)
    return 1

def main() -> int:
    root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()
    manifest = root / "project.sr"
    if not manifest.is_file():
        return fail("project.sr is required at the project root")
    try:
        data = json.loads(manifest.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return fail(f"cannot read project.sr: {exc}")
    if data.get("engine") != "Sirin Engine":
        return fail("project.sr does not identify Sirin Engine")
    if not isinstance(data.get("format"), int):
        return fail("project.sr must contain an integer format")
    entry = data.get("entry")
    if not isinstance(entry, str) or not entry:
        return fail("project.sr must declare an entry file")
    if not (root / entry).is_file():
        return fail(f"entry file does not exist: {entry}")
    print(f"VALID PROJECT: {data.get('project_name', root.name)}")
    print(f"Engine: {data['engine']} {data.get('engine_version', '')}".rstrip())
    print(f"Entry: {entry}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())