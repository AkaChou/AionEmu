#!/usr/bin/env python3
"""Generate the Aion 5.8 VIP Game.dll in one step.

Usage:
  python3 scripts/patch_game_dll_vip.py --source /path/to/Game.dll --out /path/to/Game.vip-world.dll
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import struct
from pathlib import Path

SOURCE_SHA256 = "f928a5dbc3d4d54e71f1968f38a75745882ef29f459d81b7b0fe50ac7cf490ad"
OUTPUT_SHA256 = "7e17fdce6984f0a0af298f42ef97e3844659b9d008fabde53320ba69b32982dc"

PUBLIC_KEY_B64 = (
    "BAAAAAEAAQAAAQAAJ0c1jqcaK7IXH/cicgYe0G6tgli1zi4xeo+6o3arvfFJYffb"
    "JDjUG/S2pplElVQ0YBoieHm+rfJQw4sIb0GKk0lh02CwvbAevbZGStu41Ey6afHA"
    "RdhMe7Kib8ELJKAAESXT5fZ8WwqFCCtQ02MH+bk7um9KV5ula9LhORXcZ/BTtXSe"
    "WORll0YRVFNliZFm5PWRb/FlzjqzD8TNVOiORX4KJuTOQo8La8t+wLA02SbHrJEJ"
    "t663zfWbeq8R2ugTMPGo49K+ZOuFNoDeTK+yOF342nLu3vDk8+Nf6hpJYJ1aVCba"
    "4arXNBBE7VqK8MbFGgwvJwoWOGzmc08EQA3Oxg=="
)

PUBLIC_KEY_OFFSET = 0xE4D270
PUBLIC_KEY_FIELD_SIZE = 368
VERIFY_OFFSET = 0x876F13
VERIFY_ORIGINAL = bytes.fromhex("e818e9ffff85c0740c")
VERIFY_PATCH = bytes.fromhex("33c09090909090eb0c")
RSA_ENCRYPT_OFFSET = 0x875BC0
RSA_ENCRYPT_ORIGINAL = bytes.fromhex("4c8bdc4d894318535741574881ec4006")
RSA_ENCRYPT_PATCH = bytes.fromhex("c6020141c7000100000033c0c3909090")

BM_STASH_SITE = 0x6E2A76
BM_STASH_ORIGINAL = bytes.fromhex("4883c703488d5c2450")
BM_STASH_BACK = 0x6E2A7F
BM_CALL_SITE = 0x6E2BAF
BM_CALL_ORIGINAL = bytes.fromhex("e84c7be7ff")
BM_CALL_BACK = 0x6E2BB4
DEFAULT_SCORE_CALL = 0x55AAC3
DEFAULT_SCORE_ORIGINAL = bytes.fromhex("e878d10100")
VIP_ICON_GUARD = 0x7C0294
VIP_ICON_GUARD_ORIGINAL = bytes.fromhex("743b")
FUN_1055A700 = 0x55A700
FUN_10577C40 = 0x577C40
CAVE = 0xC20BCE
SCORE_STASH = CAVE + 0xC0
CURRENT_SCORE = 0x1406040
MARKER = b"AIONVP10"
MAX_VIP_SCORE = 3759


def u32(value: int) -> bytes:
    return struct.pack("<I", value & 0xFFFFFFFF)


def rel32(site: int, target: int) -> bytes:
    return u32(target - (site + 5))


def rip_rel(insn_end: int, target: int) -> bytes:
    return u32(target - insn_end)


def replace_exact(data: bytearray, offset: int, original: bytes, patched: bytes, label: str) -> None:
    current = bytes(data[offset : offset + len(original)])
    if current != original:
        raise SystemExit(f"{label} mismatch at {hex(offset)}: {current.hex()}")
    data[offset : offset + len(patched)] = patched


def patch_sts(data: bytearray) -> None:
    raw_key = base64.b64decode(PUBLIC_KEY_B64, validate=True)
    key_type, exponent, modulus_length = struct.unpack_from("<III", raw_key)
    if (len(PUBLIC_KEY_B64), len(raw_key), key_type, exponent, modulus_length) != (360, 268, 4, 65537, 256):
        raise SystemExit("embedded STS public key is invalid")
    data[PUBLIC_KEY_OFFSET : PUBLIC_KEY_OFFSET + PUBLIC_KEY_FIELD_SIZE] = bytes(PUBLIC_KEY_FIELD_SIZE)
    data[PUBLIC_KEY_OFFSET : PUBLIC_KEY_OFFSET + len(PUBLIC_KEY_B64)] = PUBLIC_KEY_B64.encode("ascii")
    replace_exact(data, VERIFY_OFFSET, VERIFY_ORIGINAL, VERIFY_PATCH, "STS verify")
    replace_exact(data, RSA_ENCRYPT_OFFSET, RSA_ENCRYPT_ORIGINAL, RSA_ENCRYPT_PATCH, "STS RSA")


def build_stash_stub(entry: int) -> bytes:
    body = bytearray.fromhex("4883c7034180fc03")
    not_vip = len(body)
    body += bytes.fromhex("7500")
    body += bytes.fromhex("664183fe00")
    empty = len(body)
    body += bytes.fromhex("7400")
    body += bytes.fromhex("8b47053d") + u32(MAX_VIP_SCORE)
    in_world = len(body)
    body += bytes.fromhex("7700")
    mov_site = entry + len(body)
    body += bytes.fromhex("8905") + rip_rel(mov_site + 6, SCORE_STASH)
    body += bytes.fromhex("4533f6")
    captured = len(body)
    body += bytes.fromhex("eb00")
    skip = len(body)
    skip_site = entry + skip
    body += bytes.fromhex("c705") + rip_rel(skip_site + 10, SCORE_STASH) + u32(0xFFFFFFFF)
    skipped = len(body)
    body += bytes.fromhex("eb00")
    clear = len(body)
    clear_site = entry + clear
    body += bytes.fromhex("c705") + rip_rel(clear_site + 10, SCORE_STASH) + u32(0)
    done = len(body)
    for branch in (not_vip, in_world):
        body[branch + 1] = skip - (branch + 2)
    body[empty + 1] = clear - (empty + 2)
    for branch in (captured, skipped):
        body[branch + 1] = done - (branch + 2)
    body += bytes.fromhex("488d5c2450")
    jump_site = entry + len(body)
    body += bytes.fromhex("e9") + rel32(jump_site, BM_STASH_BACK)
    return bytes(body)


def build_apply_stub(entry: int) -> bytes:
    body = bytearray()
    call_site = entry + len(body)
    body += bytes.fromhex("e8") + rel32(call_site, FUN_1055A700)
    body += bytes.fromhex("4180fc03")
    not_vip = len(body)
    body += bytes.fromhex("7500")
    mov_site = entry + len(body)
    body += bytes.fromhex("8b15") + rip_rel(mov_site + 6, SCORE_STASH)
    body += bytes.fromhex("81fa") + u32(MAX_VIP_SCORE)
    not_score = len(body)
    body += bytes.fromhex("7700")
    compare_site = entry + len(body)
    body += bytes.fromhex("3b15") + rip_rel(compare_site + 6, CURRENT_SCORE)
    unchanged = len(body)
    body += bytes.fromhex("7400")
    call_site = entry + len(body)
    body += bytes.fromhex("e8") + rel32(call_site, FUN_10577C40)
    clear = len(body)
    clear_site = entry + clear
    body += bytes.fromhex("c705") + rip_rel(clear_site + 10, SCORE_STASH) + u32(0)
    back = len(body)
    body[not_vip + 1] = back - (not_vip + 2)
    body[not_score + 1] = back - (not_score + 2)
    body[unchanged + 1] = clear - (unchanged + 2)
    jump_site = entry + len(body)
    body += bytes.fromhex("e9") + rel32(jump_site, BM_CALL_BACK)
    return bytes(body)


def patch_vip(data: bytearray) -> None:
    replace_exact(data, BM_STASH_SITE, BM_STASH_ORIGINAL, BM_STASH_ORIGINAL, "VIP stash")
    replace_exact(data, BM_CALL_SITE, BM_CALL_ORIGINAL, BM_CALL_ORIGINAL, "VIP call")
    replace_exact(data, DEFAULT_SCORE_CALL, DEFAULT_SCORE_ORIGINAL, DEFAULT_SCORE_ORIGINAL, "VIP default score")
    replace_exact(data, VIP_ICON_GUARD, VIP_ICON_GUARD_ORIGINAL, VIP_ICON_GUARD_ORIGINAL, "VIP icon guard")
    if data[CAVE : CAVE + 200] != bytes(200):
        raise SystemExit(f"VIP code cave is not empty at {hex(CAVE)}")

    stash_entry = CAVE + len(MARKER)
    stash = build_stash_stub(stash_entry)
    apply_entry = stash_entry + len(stash)
    payload = MARKER + stash + build_apply_stub(apply_entry)
    if len(payload) > SCORE_STASH - CAVE:
        raise SystemExit(f"VIP payload is too large: {len(payload)}")
    data[CAVE : CAVE + len(payload)] = payload
    data[BM_STASH_SITE : BM_STASH_SITE + 9] = bytes.fromhex("e9") + rel32(BM_STASH_SITE, stash_entry) + bytes(4)
    data[BM_STASH_SITE + 5 : BM_STASH_SITE + 9] = bytes.fromhex("90909090")
    data[BM_CALL_SITE : BM_CALL_SITE + 5] = bytes.fromhex("e9") + rel32(BM_CALL_SITE, apply_entry)
    data[DEFAULT_SCORE_CALL : DEFAULT_SCORE_CALL + 5] = bytes.fromhex("9090909090")
    data[VIP_ICON_GUARD : VIP_ICON_GUARD + 2] = bytes.fromhex("9090")
    data[SCORE_STASH : SCORE_STASH + 4] = u32(0)


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate Game.vip-world.dll")
    parser.add_argument("--source", type=Path, required=True, help="source Game.dll path")
    parser.add_argument("--out", type=Path, required=True, help="output DLL path")
    args = parser.parse_args()

    if not args.source.is_file():
        raise SystemExit(f"source DLL not found: {args.source}")
    data = bytearray(args.source.read_bytes())
    source_hash = hashlib.sha256(data).hexdigest()
    if source_hash != SOURCE_SHA256:
        raise SystemExit(f"unexpected source SHA-256: {source_hash}")

    patch_sts(data)
    patch_vip(data)
    output_hash = hashlib.sha256(data).hexdigest()
    if output_hash != OUTPUT_SHA256:
        raise SystemExit(f"unexpected output SHA-256: {output_hash}")

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(data)
    print(f"source: {args.source}")
    print(f"output: {args.out}")
    print(f"SHA-256: {output_hash}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
