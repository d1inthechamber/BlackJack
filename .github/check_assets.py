from pathlib import Path
import struct

root = Path(__file__).resolve().parents[1] / "app/src/main/res"
for path in root.glob("drawable-nodpi/*.webp"):
    data = path.read_bytes()
    assert len(data) > 20 and data[:4] == b"RIFF" and data[8:12] == b"WEBP", f"Invalid artwork: {path.name}"
    assert struct.unpack("<I", data[4:8])[0] + 8 == len(data), f"Truncated artwork: {path.name}"
for path in root.glob("raw/music_*.ogg"):
    data = path.read_bytes()
    assert len(data) > 100 and data[:4] == b"OggS", f"Invalid music: {path.name}"
print("Artwork containers and music headers verified")
