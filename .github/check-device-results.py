"""Require executed device tests, zero failures, and nonempty PNG evidence."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

mode = sys.argv[1]
expected = {"FoldableSmokeTest"} if mode == "fold" else {"CasinoSmokeTest", "TableSmokeTest", "UpgradeSmokeTest"}
seen = set()
total = 0
for path in Path("app/build/outputs/androidTest-results/connected").rglob("*.xml"):
    root = ET.parse(path).getroot()
    for case in root.iter("testcase"):
        assert case.find("failure") is None and case.find("error") is None, f"Device test failed: {case.attrib}"
        if case.find("skipped") is None:
            seen.add(case.get("classname", root.get("name", "")).rsplit(".", 1)[-1])
            total += 1
assert expected <= seen, f"Device tests did not execute: {expected - seen}; found {seen}"
directory = Path("foldable-screenshots" if mode == "fold" else "screenshots")
shots = list(directory.glob("*.png"))
assert len(shots) >= (3 if mode == "fold" else 25), "Required screenshot set is incomplete"
for path in shots:
    data = path.read_bytes()
    assert len(data) > 10000 and data.startswith(b"\x89PNG\r\n\x1a\n"), f"Empty or invalid screenshot: {path}"
print(f"Verified {total} executed tests and {len(shots)} screenshots")
