#!/usr/bin/env python3
"""Copies the recorded API responses from the Kotlin project (../src/test/resources/fixtures),
which records them with ../tools/record-fixtures.sh, into tests/fixtures. Both libraries are
tested against the same real responses."""

import shutil
from pathlib import Path

here = Path(__file__).resolve().parent.parent
source = here.parent / "src" / "test" / "resources" / "fixtures"
target = here / "tests" / "fixtures"
if target.exists():
    shutil.rmtree(target)
shutil.copytree(source, target)
print(f"copied {sum(1 for _ in target.rglob('*.json'))} fixtures to {target.relative_to(here)}")
