"""Synthetic 240px catalog photography stand-ins, distinct stable URLs per product."""
import struct
import sys
import zlib
from pathlib import Path


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


directory = Path(sys.argv[1])
directory.mkdir(exist_ok=True, parents=True)
for theme in range(12):
    raw = b"".join(b"\0" + bytes((x * 3 + y + theme * 17) % 256 for x in range(240)) for y in range(240))
    header = struct.pack(">IIBBBBB", 240, 240, 8, 0, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    for product in range(theme + 1, 1201, 12):
        (directory / f"00000000-0000-4000-8000-{product:012}.png").write_bytes(png)
