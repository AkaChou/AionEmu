#!/usr/bin/env python3
"""Patch China-client Game.dll STS server IP (hardcoded string).

Aion 5.8 Game.dll stores STS host at file offset 0xE4DDF8 as a C string
(default: 172.20.53.100). hosts cannot remap IP→IP, so rewrite the string.

Usage:
  python3 patch_game_dll_sts_ip.py /path/to/Game.dll 192.168.1.18
  python3 patch_game_dll_sts_ip.py Game.dll 192.168.1.18 --backup
  python3 patch_game_dll_sts_ip.py Game.dll 192.168.1.18 --old-ip 172.20.53.100
  python3 patch_game_dll_sts_ip.py Game.dll --show
"""

from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

# China 5.8 Game.dll: STS host C-string (ASCII)
DEFAULT_OFFSET = 0xE4DDF8
DEFAULT_OLD_IP = "172.20.53.100"
# field is the old IP + trailing NULs in a small static buffer; keep writes <= this
DEFAULT_FIELD_SIZE = 16


def read_c_string(data: bytes, offset: int, max_len: int = 64) -> str:
    end = min(len(data), offset + max_len)
    chunk = data[offset:end]
    nul = chunk.find(b"\x00")
    if nul < 0:
        return chunk.decode("ascii", errors="replace")
    return chunk[:nul].decode("ascii", errors="replace")


def find_ip_offsets(data: bytes, ip: str) -> list[int]:
    needle = ip.encode("ascii") + b"\x00"
    offsets: list[int] = []
    start = 0
    while True:
        idx = data.find(needle, start)
        if idx < 0:
            break
        offsets.append(idx)
        start = idx + 1
    return offsets


def patch_ip(
    data: bytearray,
    new_ip: str,
    *,
    offset: int | None,
    old_ip: str,
    field_size: int,
) -> tuple[int, str]:
    new_bytes = new_ip.encode("ascii")
    if b"\x00" in new_bytes:
        raise SystemExit("IP must be ASCII without NUL")
    if len(new_bytes) >= field_size:
        raise SystemExit(
            f"IP too long for field ({len(new_bytes)}+1 > {field_size}): {new_ip!r}"
        )

    if offset is None:
        hits = find_ip_offsets(data, old_ip)
        if not hits:
            raise SystemExit(f"old IP not found: {old_ip!r}")
        if len(hits) > 1:
            raise SystemExit(
                f"old IP found at multiple offsets {', '.join(hex(h) for h in hits)}; "
                "pass --offset"
            )
        offset = hits[0]

    if offset < 0 or offset + field_size > len(data):
        raise SystemExit(f"offset out of range: {hex(offset)}")

    current = read_c_string(data, offset, field_size)
    if current not in (old_ip, new_ip) and old_ip:
        # still allow if user forced offset and current looks like an IPv4
        if current.count(".") != 3:
            raise SystemExit(
                f"offset {hex(offset)} has unexpected string {current!r}; "
                f"expected {old_ip!r} or {new_ip!r}"
            )

    if current == new_ip:
        return offset, current

    # write IP + NUL, zero the rest of the field so no old tail remains
    data[offset : offset + field_size] = b"\x00" * field_size
    data[offset : offset + len(new_bytes)] = new_bytes
    return offset, current


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Patch Game.dll STS IP string")
    parser.add_argument("game_dll", type=Path, help="path to Game.dll")
    parser.add_argument(
        "new_ip",
        nargs="?",
        help="new STS IPv4, e.g. 192.168.1.18 (omit with --show)",
    )
    parser.add_argument(
        "--old-ip",
        default=DEFAULT_OLD_IP,
        help=f"string to replace (default: {DEFAULT_OLD_IP})",
    )
    parser.add_argument(
        "--offset",
        type=lambda s: int(s, 0),
        default=DEFAULT_OFFSET,
        help=f"file offset of STS IP C-string (default: {hex(DEFAULT_OFFSET)})",
    )
    parser.add_argument(
        "--scan",
        action="store_true",
        help="ignore --offset and scan for --old-ip",
    )
    parser.add_argument(
        "--field-size",
        type=int,
        default=DEFAULT_FIELD_SIZE,
        help=f"writable field size including NUL (default: {DEFAULT_FIELD_SIZE})",
    )
    parser.add_argument(
        "--backup",
        action="store_true",
        help="write Game.dll.sts-ip-original once if missing",
    )
    parser.add_argument(
        "--out",
        type=Path,
        help="write patched file here instead of in-place",
    )
    parser.add_argument(
        "--show",
        action="store_true",
        help="print current IP at offset / scan hits and exit",
    )
    args = parser.parse_args(argv)

    path: Path = args.game_dll
    if not path.is_file():
        print(f"not found: {path}", file=sys.stderr)
        return 1

    data = bytearray(path.read_bytes())
    offset = None if args.scan else args.offset

    if args.show:
        if offset is not None:
            print(f"{hex(offset)}: {read_c_string(data, offset)!r}")
        hits = find_ip_offsets(data, args.old_ip)
        if hits:
            print(f"scan {args.old_ip!r}: " + ", ".join(hex(h) for h in hits))
        else:
            print(f"scan {args.old_ip!r}: not found")
        # also report whatever is at default offset
        if offset != DEFAULT_OFFSET:
            print(
                f"{hex(DEFAULT_OFFSET)}: {read_c_string(data, DEFAULT_OFFSET)!r}"
            )
        return 0

    if not args.new_ip:
        print("new_ip required (or use --show)", file=sys.stderr)
        return 2

    # basic IPv4 shape check (ponytail: no full validation)
    parts = args.new_ip.split(".")
    if len(parts) != 4 or any(not p.isdigit() or not 0 <= int(p) <= 255 for p in parts):
        print(f"invalid IPv4: {args.new_ip!r}", file=sys.stderr)
        return 2

    used_offset, previous = patch_ip(
        data,
        args.new_ip,
        offset=offset,
        old_ip=args.old_ip,
        field_size=args.field_size,
    )

    out = args.out or path
    if previous == args.new_ip and out == path:
        print(f"already {args.new_ip!r} at {hex(used_offset)}")
        return 0

    if args.backup and out == path:
        backup = path.with_name(path.name + ".sts-ip-original")
        if not backup.exists():
            shutil.copy2(path, backup)
            print(f"backup: {backup}")
        else:
            print(f"backup exists: {backup}")

    out.write_bytes(data)
    print(f"patched {out}")
    print(f"  offset: {hex(used_offset)}")
    print(f"  {previous!r} -> {args.new_ip!r}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
