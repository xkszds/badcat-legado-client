#!/usr/bin/env python3
"""Recalculate standard DEX SHA-1 signature and Adler-32 checksum.

This script writes new output files and never modifies its input files.
It does not repair malformed DEX sections or validate bytecode semantics.
"""
from __future__ import annotations

import argparse
import hashlib
import struct
import zlib
from pathlib import Path


def recalc(source: Path, destination: Path) -> tuple[bool, bool]:
    data = bytearray(source.read_bytes())
    if len(data) < 112 or not data.startswith(b"dex\n") or data[7] != 0:
        raise ValueError(f"Not a standard DEX file: {source}")

    declared_size = struct.unpack_from("<I", data, 32)[0]
    if declared_size != len(data):
        raise ValueError(
            f"DEX file_size mismatch in {source}: header={declared_size}, actual={len(data)}"
        )

    old_sig = bytes(data[12:32])
    new_sig = hashlib.sha1(data[32:]).digest()
    data[12:32] = new_sig

    old_checksum = struct.unpack_from("<I", data, 8)[0]
    new_checksum = zlib.adler32(data[12:]) & 0xFFFFFFFF
    struct.pack_into("<I", data, 8, new_checksum)

    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    return old_sig == new_sig, old_checksum == new_checksum


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="input DEX file")
    parser.add_argument("destination", type=Path, help="output DEX file (separate from input)")
    args = parser.parse_args()
    if args.source.resolve() == args.destination.resolve():
        parser.error("Refusing to overwrite the source DEX; choose a separate destination.")
    sig_ok, checksum_ok = recalc(args.source, args.destination)
    print(f"Input : {args.source}")
    print(f"Output: {args.destination}")
    print(f"SHA-1 signature already valid: {sig_ok}")
    print(f"Adler-32 checksum already valid: {checksum_ok}")
    print("Recalculation complete. Structural/bytecode verification is still required.")


if __name__ == "__main__":
    main()
