from pathlib import Path
import struct
import hashlib
import json

root = Path(__file__).resolve().parents[1] / "app/src/main/res"
manifest = json.loads((root.parents[3] / "art/casino-chaos/approved-v34-assets.json").read_text())
assert {item["packaged"] for item in manifest} == {
    "craps_hands.webp", "craps_banknote.webp", "craps_alley.webp", "slot_symbols.webp"
}, "All four approved v3.4 assets must be present"
for item in manifest:
    path = root / "drawable-nodpi" / item["packaged"]
    assert hashlib.sha256(path.read_bytes()).hexdigest() == item["sha256"], f"Wrong or truncated approved artwork: {path.name}"
for path in root.glob("drawable-nodpi/*.webp"):
    data = path.read_bytes()
    assert len(data) > 20 and data[:4] == b"RIFF" and data[8:12] == b"WEBP", f"Invalid artwork: {path.name}"
    assert struct.unpack("<I", data[4:8])[0] + 8 == len(data), f"Truncated artwork: {path.name}"
required_audio = {
    "dealer_laugh.ogg",
    "dealer_groan.ogg",
    "dealer_grunt.ogg",
    "music_vegas.ogg",
    "music_carnival.ogg",
    "music_egypt.ogg",
    "music_iron.ogg",
    "music_west.ogg",
    "music_punk.ogg",
    "music_green.ogg",
}
found_audio = {path.name for path in root.glob("raw/*.ogg")}
assert required_audio <= found_audio, f"Missing audio: {sorted(required_audio - found_audio)}"
for path in root.glob("raw/*.ogg"):
    data = path.read_bytes()
    assert len(data) > 1000 and data[:4] == b"OggS", f"Invalid audio: {path.name}"
print("Artwork containers and audio headers verified")
