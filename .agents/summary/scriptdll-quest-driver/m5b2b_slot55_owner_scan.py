#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""扫宿主 EXE 全部 RTTI 虚表，列出"槽 55 (+0x1b8)"的类与实现符号名。

用途：ScriptDLL64 的任务跳板形如
    (**(code **)(lVar1 + 0x1b8))(param_1, uVar2, <questId>, *puVar5, *puVar4, *puVar3)
要先确定 param_1 到底是哪个宿主类，才能判断槽 55 的语义（任务驱动 / 播片 / 其它）。

用法：
    python3 -B m5b2b_slot55_owner_scan.py <binary> [--min-slots N] [--name-substr Quest]
"""
from __future__ import annotations
import os

import argparse
import sys
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())

sys.path.insert(0, str(Path(__file__).resolve().parent))
import re_vtable_scan  # noqa: E402
from m5b2_pe_probe import Pe  # noqa: E402

# 真端反编译导出根目录 / Retail decompilation export root
EXPORT_ROOT = Path(f"{REPO.parent / '58Server'}/server58-source")
# 二进制名 → 符号表目录 / Binary name to symbol table directory
SYMBOL_DIRS = {
	"Server64.exe": "MainServer_Server64",
	"ScriptDLL64.dll": "MainServer_ScriptDLL64",
	"NPCSvr64.exe": "NPCServer_NPCSvr64",
}


def load_symbols(binary: Path) -> dict[int, str]:
	"""按二进制名加载对应 symbols.tsv。 / Load the matching symbols.tsv for the binary."""
	directory = SYMBOL_DIRS.get(binary.name)
	if directory is None:
		return {}
	path = EXPORT_ROOT / directory / "symbols.tsv"
	if not path.is_file():
		return {}
	symbols: dict[int, str] = {}
	for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
		if line.startswith("#") or not line.strip():
			continue
		parts = line.split("\t")
		if len(parts) < 3 or not parts[0].strip():
			continue
		try:
			symbols[int(parts[0], 16)] = parts[2]
		except ValueError:
			continue
	return symbols


def main(argv: list[str]) -> int:
	parser = argparse.ArgumentParser(description=__doc__)
	parser.add_argument("binary", type=Path)
	parser.add_argument("--min-slots", type=int, default=56)
	parser.add_argument("--name-substr", default="")
	parser.add_argument("--limit", type=int, default=0)
	args = parser.parse_args(argv)

	pe = Pe(str(args.binary))
	pe, vts, _ = re_vtable_scan.build(pe)
	symbols = load_symbols(args.binary)
	needle = args.name_substr.lower()
	rows = []
	for vt, (name, slots, colva) in vts.items():
		if slots < args.min_slots:
			continue
		target = pe.u64(vt + 0x1B8)
		symbol = symbols.get(target, "")
		if needle and needle not in name.lower() and needle not in symbol.lower():
			continue
		rows.append((name, slots, vt, target, symbol))
	rows.sort(key=lambda row: (-row[1], row[0]))
	print(f"{args.binary}: RTTI 虚表总 {len(vts)}，槽 55 可用（slots>={args.min_slots}）的类 {len(rows)}")
	print(f"{'class':52s} {'slots':>5s} {'vtable':>12s} {'slot55':>12s} symbol")
	for name, slots, vt, target, symbol in rows[: args.limit or None]:
		print(f"{name:52s} {slots:5d} {vt:#12x} {target:#12x} {symbol}")
	return 0


if __name__ == "__main__":
	raise SystemExit(main(sys.argv[1:]))
