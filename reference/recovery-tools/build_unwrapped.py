#!/usr/bin/env python3
"""Prepare an unsigned, unwrapped test APK using Apktool and the supplied DEX ZIP.

Requirements:
  * Python 3.9+
  * Apktool CLI available as `apktool`
  * This script, StubApp_compat.smali, the original APK, and the Epic DEX ZIP

The script writes only to a new output path. It does not sign, install, or alter
source APK/DEX files. Review the generated APK before signing or installing.
"""
from __future__ import annotations

import argparse
import hashlib
import re
import shutil
import struct
import subprocess
import tempfile
import zipfile
import zlib
from pathlib import Path

DEX_MAP = {
    "classes.dex": "com.hhm.verticalsearch/classes.dex",
    "classes2.dex": "com.hhm.verticalsearch/classes2.dex",
    "classes3.dex": "com.hhm.verticalsearch/classes4.dex",
    "classes4.dex": "com.hhm.verticalsearch/classes5.dex",
    "classes5.dex": "com.hhm.verticalsearch/classes6.dex",
    "classes6.dex": "com.hhm.verticalsearch/classes7.dex",
}


def checked_run(args: list[str]) -> None:
    print("+", " ".join(args))
    subprocess.run(args, check=True)


def recalc_dex_header(blob: bytes, label: str) -> bytes:
    data = bytearray(blob)
    if len(data) < 112 or not data.startswith(b"dex\n") or data[7] != 0:
        raise ValueError(f"Invalid DEX magic/header: {label}")
    declared_size = struct.unpack_from("<I", data, 32)[0]
    if declared_size != len(data):
        raise ValueError(
            f"DEX size mismatch for {label}: declared {declared_size}, actual {len(data)}"
        )
    # DEX signature is SHA-1 over the file from byte 32 through EOF.
    data[12:32] = hashlib.sha1(data[32:]).digest()
    # Adler-32 covers the file from byte 12 through EOF, including the new SHA-1.
    struct.pack_into("<I", data, 8, zlib.adler32(data[12:]) & 0xFFFFFFFF)
    return bytes(data)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path, help="original APK")
    parser.add_argument("--dex-zip", required=True, type=Path, help="Epic DEX ZIP")
    parser.add_argument("--out", required=True, type=Path, help="new unsigned APK output")
    parser.add_argument("--apktool", default="apktool", help="Apktool CLI command/path")
    args = parser.parse_args()

    apk = args.apk.resolve()
    dex_zip = args.dex_zip.resolve()
    out = args.out.resolve()
    if not apk.is_file() or not dex_zip.is_file():
        parser.error("--apk and --dex-zip must both point to existing files")
    if out in (apk, dex_zip):
        parser.error("Output path must not overwrite either input")
    template = Path(__file__).with_name("StubApp_compat.smali")
    if not template.is_file():
        parser.error(f"Missing compatibility smali template: {template}")

    with zipfile.ZipFile(dex_zip) as dz:
        missing = [p for p in DEX_MAP.values() if p not in dz.namelist()]
        if missing:
            parser.error("DEX ZIP is missing: " + ", ".join(missing))
        dex_payloads = {
            output_name: recalc_dex_header(dz.read(source_name), source_name)
            for output_name, source_name in DEX_MAP.items()
        }

    with tempfile.TemporaryDirectory(prefix="badcat_unwrapped_") as td:
        project = Path(td) / "project"
        rebuilt = Path(td) / "apktool_unsigned.apk"

        checked_run([args.apktool, "d", "-f", str(apk), "-o", str(project)])
        manifest = project / "AndroidManifest.xml"
        if not manifest.is_file():
            raise RuntimeError("Apktool output has no AndroidManifest.xml")
        xml = manifest.read_text(encoding="utf-8")
        old = 'android:name="com.stub.StubApp"'
        new = 'android:name="com.search.verticalsearch.common.HuaYueApplication"'
        count = xml.count(old)
        if count != 1:
            raise RuntimeError(
                f"Expected exactly one Manifest Application entry {old!r}; found {count}. "
                "No output APK was written."
            )
        manifest.write_text(xml.replace(old, new), encoding="utf-8")

        # Prevent Apktool's decoded shell StubApp from being assembled into its
        # primary DEX. The recovered business DEXes are overlaid below.
        for candidate in project.rglob("StubApp.smali"):
            text = candidate.read_text(encoding="utf-8", errors="replace")
            if "Lcom/stub/StubApp;" in text:
                candidate.unlink()

        shim_dir = project / "smali_classes7" / "com" / "stub"
        shim_dir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(template, shim_dir / "StubApp.smali")

        checked_run([args.apktool, "b", str(project), "-o", str(rebuilt)])
        with zipfile.ZipFile(rebuilt) as built:
            if "classes7.dex" not in built.namelist():
                raise RuntimeError(
                    "Apktool did not produce classes7.dex from smali_classes7; "
                    "check your Apktool version/build output."
                )
            shim_dex = built.read("classes7.dex")
            if not shim_dex.startswith(b"dex\n"):
                raise RuntimeError("Apktool classes7.dex is not a DEX file")

            # Replace all root multidex entries, retaining all other resources,
            # Manifest, assets, and native libraries from the Apktool build.
            dex_names = set(DEX_MAP) | {"classes7.dex"}
            out.parent.mkdir(parents=True, exist_ok=True)
            with zipfile.ZipFile(out, "w") as final:
                for info in built.infolist():
                    if info.filename in dex_names:
                        continue
                    final.writestr(info, built.read(info.filename))
                for name, payload in dex_payloads.items():
                    final.writestr(name, payload, compress_type=zipfile.ZIP_DEFLATED)
                final.writestr("classes7.dex", shim_dex, compress_type=zipfile.ZIP_DEFLATED)

    with zipfile.ZipFile(out) as result:
        bad = result.testzip()
        if bad:
            raise RuntimeError(f"Output ZIP integrity failure at {bad}")
        names = set(result.namelist())
        absent = sorted((set(DEX_MAP) | {"classes7.dex"}) - names)
        if absent:
            raise RuntimeError("Output is missing DEX entries: " + ", ".join(absent))
        manifest_bytes = result.read("AndroidManifest.xml")
        if b"HuaYueApplication" not in manifest_bytes:
            raise RuntimeError("Output Manifest does not show HuaYueApplication")

    print(f"Unsigned test APK created: {out}")
    print("Next: inspect the APK, zipalign it, sign it with a test key, then test on a spare device.")
    print("Do not uninstall or overwrite the existing production app/data.")


if __name__ == "__main__":
    main()