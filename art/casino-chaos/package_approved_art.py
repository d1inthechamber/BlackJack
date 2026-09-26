"""Package the supplied v3.4 artwork; only resize and encode, preserving alpha."""
from pathlib import Path
import hashlib
import json
import sys
from PIL import Image

source = Path(sys.argv[1])
root = Path(__file__).resolve().parents[2]
destination = root / "app/src/main/res/drawable-nodpi"
assets = [
    ("Eight tattooed hand poses, transparent sprite atlas.png", "craps_hands.webp", (1152, 768), "libfile_d1964b279bc88191af453bdad83cc480"),
    ("Casino Chaos virtual dollar banknote.png", "craps_banknote.webp", (1128, 502), "libfile_881d68ed0b708191a4228373ab0e55ae"),
    ("Worn Neon Casino Alley.png", "craps_alley.webp", (768, 1152), "libfile_7d66012b6b5481919f2eef7ced89eb47"),
    ("Dark Vegas Slot Emblem Atlas.png", "slot_symbols.webp", (1060, 1484), "libfile_24879e87d210819183714bab3a76c8f2"),
]
manifest = []
for filename, output, dimensions, library_id in assets:
    path = source / filename
    original = Image.open(path)
    packaged = original.resize(dimensions, Image.Resampling.LANCZOS)
    packaged.save(destination / output, "WEBP", quality=94, method=6)
    manifest.append(dict(source=filename, library_id=library_id,
        source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
        packaged=output, size=list(dimensions),
        sha256=hashlib.sha256((destination / output).read_bytes()).hexdigest()))
    print(output, (destination / output).stat().st_size)
(Path(__file__).parent / "approved-v34-assets.json").write_text(json.dumps(manifest, indent=2) + "\n")
