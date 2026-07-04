#!/usr/bin/env python3
"""Regenerate the per-OS app icon containers from packaging/icons/source/.

Packs the hand-made source PNGs directly into a Windows .ico and a macOS
.icns (both formats can embed PNG data verbatim) and copies the x256 PNG for
Linux. Nothing is rescaled, so every size in the output is exactly the PNG the 
artist exported and nothing is upscaled past the master.

Run this whenever the logo changes, then commit the resulting
OpenTowns.ico / OpenTowns.icns / OpenTowns.png. jpackage bakes each one into
the Windows launcher, the macOS .app and the Linux app-image respectively
(see the jpackageImage task in build.gradle).

    python packaging/icons/generate.py
"""
import struct
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = HERE / "source"


def load(size):
    """Raw PNG bytes for a source size. master is logo.png"""
    name = "logo.png" if size == 512 else f"logo-{size}.png"
    return (SRC / name).read_bytes()


def build_ico(path):
    """Windows .ico: ICONDIR + one ICONDIRENTRY per image + PNG blobs.

    PNG-compressed entries are read by Windows Vista and later, which covers
    every jpackage target. A size of 256 is encoded as 0 in the width/height
    byte, per the ICO format.
    """
    sizes = [16, 32, 48, 64, 128, 256]
    imgs = [(s, load(s)) for s in sizes]
    header = struct.pack("<HHH", 0, 1, len(imgs))  # reserved, type=1, count
    offset = 6 + 16 * len(imgs)
    entries = b""
    blobs = b""
    for size, data in imgs:
        dim = 0 if size >= 256 else size
        # width, height, colors, reserved, planes, bpp, bytes, offset
        entries += struct.pack("<BBBBHHII", dim, dim, 0, 0, 1, 32, len(data), offset)
        blobs += data
        offset += len(data)
    path.write_bytes(header + entries + blobs)


def build_icns(path):
    """macOS .icns: 'icns' magic + total length, then TypedData chunks of
    4-byte OSType, 4-byte big-endian length (incl. the 8-byte header) and PNG
    bytes. Uses Apple's standard PNG OSTypes for a full 1x + Retina @2x ladder
    capped at 512 (no ic10/1024 slot, so nothing is upscaled).
    """
    entries = [
        (b"icp4", 16),   # 16x16
        (b"icp5", 32),   # 32x32
        (b"ic07", 128),  # 128x128
        (b"ic08", 256),  # 256x256
        (b"ic09", 512),  # 512x512
        (b"ic11", 32),   # 16x16@2x
        (b"ic12", 64),   # 32x32@2x
        (b"ic13", 256),  # 128x128@2x
        (b"ic14", 512),  # 256x256@2x
    ]
    body = b""
    for ostype, size in entries:
        data = load(size)
        body += ostype + struct.pack(">I", len(data) + 8) + data
    path.write_bytes(b"icns" + struct.pack(">I", len(body) + 8) + body)


def build_png(path):
    """Linux app-image: a single PNG. jpackage takes one file; 256 is the
    practical size."""
    path.write_bytes(load(256))


build_ico(HERE / "OpenTowns.ico")
build_icns(HERE / "OpenTowns.icns")
build_png(HERE / "OpenTowns.png")
print("Wrote OpenTowns.ico, OpenTowns.icns, OpenTowns.png")
