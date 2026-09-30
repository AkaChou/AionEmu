#!/usr/bin/env python3
"""Phase 1：从 ScriptDLL64 反编译产物提取「按任务 ID 的任务脚本注册表」。

原理
----
真端 ScriptDLL64 为每个任务注册若干"步骤"，注册点形如：

    /* 180d49a10 */
    void FUN_180d49a10(undefined8 param_1, undefined8 param_2)
    {
      FUN_180cb13b0(0x44e, param_2, 1, 3, 3, 1);   // 0x44e = 任务 1102
      return;
    }

其中 0x44e 是任务 ID，后面的字面量是该步骤的参数（可与真端模板表交叉验证）。
注册点既可能各自一个小函数（stub），也可能批量出现在同一个函数体内，因此分三步：

  1. 扫描全部语句级调用点，收集「首实参为字面量任务 ID」的调用；
  2. 出现 >= N 个不同任务 ID 的被调用函数 = 注册辅助函数（registrar helper）；
  3. 输出这些辅助函数的**全部**调用点：quest_id / helper / params / 所在函数地址 / 调用位置。

顺序：同一任务内按调用点偏移排序 = 步骤序假设（Phase 2 用客户端 HTML 与现有 XML 验证）。

用法
----
    python3 -B extract_quest_registry.py \
        --dump <真端根>/server58/MainServer_ScriptDLL64/ScriptDLL64.c \
        --out quest_registry.tsv --helpers registry_helpers.tsv
"""
from __future__ import annotations

import argparse
import bisect
import hashlib
import re
from collections import defaultdict
from pathlib import Path

FUNC_HEADER = re.compile(r"(?m)^/\* (?P<addr>[0-9A-Fa-f]{6,}) \*/$")
# 实参内不允许括号，避免贪婪匹配把函数签名/嵌套调用一起吃进来
CALL_RE = re.compile(r"\b(?P<callee>[A-Za-z_][A-Za-z0-9_]*)\s*\((?P<args>[^;()]*)\)\s*;")
INT_RE = re.compile(r"^(?:0x[0-9A-Fa-f]+|\d+)$")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def split_args(text: str) -> list[str]:
    return [a.strip() for a in text.split(",")]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dump", required=True)
    ap.add_argument("--out", default="quest_registry.tsv")
    ap.add_argument("--helpers", default="registry_helpers.tsv")
    ap.add_argument("--min-ids", type=int, default=20, help="判定注册辅助函数所需的最小不同任务 id 数")
    ap.add_argument("--min-quest-id", type=int, default=100)
    ap.add_argument("--max-quest-id", type=int, default=200000)
    args = ap.parse_args()

    dump_path = Path(args.dump)
    dump = dump_path.read_text(encoding="latin1", errors="replace")
    header_offsets = [m.start() for m in FUNC_HEADER.finditer(dump)]
    header_addrs = [int(m.group("addr"), 16) for m in FUNC_HEADER.finditer(dump)]

    sites: list[tuple[int, str, int, list[str]]] = []
    callee_ids: dict[str, set[int]] = defaultdict(set)
    for m in CALL_RE.finditer(dump):
        raw = split_args(m.group("args"))
        if not raw or not INT_RE.match(raw[0]):
            continue
        qid = int(raw[0], 0)
        if not (args.min_quest_id <= qid <= args.max_quest_id):
            continue
        callee = m.group("callee")
        sites.append((m.start(), callee, qid, raw[1:]))
        callee_ids[callee].add(qid)

    helpers = {c for c, ids in callee_ids.items() if len(ids) >= args.min_ids}
    rows = [s for s in sites if s[1] in helpers]
    rows.sort(key=lambda s: s[0])

    order: dict[int, int] = defaultdict(int)
    out = Path(args.out)
    with out.open("w", encoding="utf-8", newline="\n") as fh:
        fh.write(f"# source={dump_path}\n# sha256={sha256(dump_path)}\n")
        fh.write("quest_id\tstep_index\thelper\tparams\towner_func\tcall_offset\n")
        for offset, callee, qid, params in rows:
            idx = order[qid]
            order[qid] = idx + 1
            hi = bisect.bisect_right(header_offsets, offset) - 1
            owner = f"0x{header_addrs[hi]:x}" if hi >= 0 else "-"
            fh.write(f"{qid}\t{idx}\t{callee}\t{' '.join(params) or '-'}\t{owner}\t{offset}\n")

    per_helper: dict[str, set[int]] = defaultdict(set)
    stubs: dict[str, int] = defaultdict(int)
    for _offset, callee, qid, _params in rows:
        per_helper[callee].add(qid)
        stubs[callee] += 1
    hp = Path(args.helpers)
    with hp.open("w", encoding="utf-8", newline="\n") as fh:
        fh.write("helper\tquests\tstubs\tid_min\tid_max\n")
        for callee in sorted(per_helper, key=lambda c: -len(per_helper[c])):
            ids = per_helper[callee]
            fh.write(f"{callee}\t{len(ids)}\t{stubs[callee]}\t{min(ids)}\t{max(ids)}\n")

    print(f"call sites with literal quest id: {len(sites)}")
    print(f"registrar helpers (>= {args.min_ids} quest ids): {len(helpers)}")
    print(f"registration rows: {len(rows)}  quests: {len(order)}")
    print(f"-> {out}\n-> {hp}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
