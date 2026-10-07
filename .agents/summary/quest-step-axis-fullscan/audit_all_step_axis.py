#!/usr/bin/env python3
"""全量步号轴审计：生产 quest XML 所有 transition 目标节点 var0 vs 真端 SetProgress 常量集合。

背景（2026-10-07，itemUseArea 族审计 qe-useitem-step-axis 的全族推广）：
- itemUseArea 族 96 任务审计发现 5 例迁移期「末行索引当轴值」真错（QE-054 三源口径）。
- sensoryArea/killedByUser 等族在 XML 侧无一一对应事件词汇，按客户端 sourceType 分族不适配；
  故对本仓库全部生产 XML 的每个带事件 transition，直接以「目标节点 var0 ∈ 真端 SetProgress 值集合」判据扫描。
- 判据细节与已知坑同 audit_itemusearea_step_axis.py：
  - 真端 `0x100(quest,0,0)` 状态推进不写轴（轴保持 from 值，天然在集合内）；
  - `0xf0(obj, quest, 值[, 0])`（SetProgress）写轴；`0x110(quest, from, to)` 显式轴推进；
  - MISMATCH 只是候选，必须逐任务人工复核（真端函数体 + legacy handler + 客户端任务书行）。

用法：
    python3 .agents/summary/quest-step-axis-fullscan/audit_all_step_axis.py
输出（本目录）：
    fullscan-step-axis.tsv（逐任务一行，含各事件类型的目标值明细）
    fullscan-mismatch.tsv（候选差异清单）
"""

from __future__ import annotations

import csv
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS_DIR = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"


def resolve_external(candidates: list[Path], probe: str) -> Path:
    """按同宿主目录约定解析外部根；容错若干常见宿主布局。"""
    for base in candidates:
        if (base / probe).exists():
            return base
    raise SystemExit(f"cannot resolve external root for {probe}; tried: {candidates}")


RETAIL_ROOT = resolve_external(
    [REPO.parent / "58Server", Path.home() / "IdeaProjects" / "58Server"],
    "server58/MainServer_ScriptDLL64/ScriptDLL64.c")
RETAIL_C = RETAIL_ROOT / "server58" / "MainServer_ScriptDLL64" / "ScriptDLL64.c"
OUT_DIR = Path(__file__).resolve().parent

# 真端 vtable SetProgress 调用形态：  (**(code **)(*obj + 0xf0))(obj, 0x55d, 2);
# 兼容四参数变体 (obj, quest, 值, 0)（如 24154 的 0xf0(plVar3,0x5e5a,4,0)）。
SETPROG = re.compile(
    r"0xf0\)\)\(\s*[A-Za-z_0-9]+,\s*0x([0-9a-fA-F]{1,7})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*"
    r"(?:,\s*(?:0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*)?\)")
# 真端显式轴推进： 0x110)(obj, 0x10f2, 4, 5, ...)
AXISPROG = re.compile(
    r"0x110\)\)\(\s*[A-Za-z_0-9]+,\s*0x([0-9a-fA-F]{1,7})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})")


def scan_retail() -> dict[int, set[int]]:
    """提取真端全部 SetProgress / 显式轴推进的常量值，按任务 ID 聚合。"""
    text = RETAIL_C.read_text(encoding="utf-8", errors="ignore")
    setprog: dict[int, set[int]] = {}
    for hex_id, value in SETPROG.findall(text):
        setprog.setdefault(int(hex_id, 16), set()).add(int(value, 0))
    for hex_id, _from, to in AXISPROG.findall(text):
        setprog.setdefault(int(hex_id, 16), set()).add(int(to, 0))
    return setprog


def xml_transitions(quest_id: int):
    """解析任务 XML：节点 var0 表 + 每个带事件 transition 的（事件类型, source, target, target_var0）。"""
    path = QUESTS_DIR / f"{quest_id}.xml"
    if not path.is_file():
        return None
    root = ET.parse(path).getroot()
    nodes: dict[str, int | None] = {}
    for node in root.findall("nodes/node"):
        var = node.find("var[@name='var0']")
        nodes[node.get("label")] = int(var.get("value")) if var is not None else None
    transitions = []
    for tr in root.findall("transitions/transition"):
        event = tr.find("event")
        if event is None:
            continue
        for child in event:
            transitions.append((child.tag, tr.get("source"), tr.get("target"),
                                nodes.get(tr.get("target"))))
    return nodes, transitions


def main() -> int:
    if not RETAIL_C.is_file():
        print(f"missing retail source: {RETAIL_C}", file=sys.stderr)
        return 2
    setprog = scan_retail()
    xml_ids = sorted(int(p.stem) for p in QUESTS_DIR.glob("*.xml") if p.stem.isdigit())

    rows = []
    mismatch = []
    for quest_id in xml_ids:
        parsed = xml_transitions(quest_id)
        assert parsed is not None
        _nodes, transitions = parsed
        retail_vals = sorted(setprog.get(quest_id, set()))
        # 目标 var0 明细：event:target_var0 聚合，保留重复以计数
        detail = Counter()
        bad_vals = set()
        for tag, _src, _tgt, tval in transitions:
            if tval is None:
                detail[f"{tag}:None"] += 1
                continue
            detail[f"{tag}:{tval}"] += 1
            # 轴值 0 是结构性回边（拒绝页/重置/初始节点），真端 SetProgress 恒不写 0，不作越集判据
            if tval != 0 and tval not in retail_vals:
                bad_vals.add(tval)
        if not retail_vals:
            judgement = "NO_RETAIL_SETPROGRESS"
        elif not bad_vals:
            judgement = "MATCH"
        else:
            judgement = "MISMATCH"
        row = (quest_id, "|".join(map(str, retail_vals)),
               ";".join(f"{k}x{v}" for k, v in sorted(detail.items())),
               "|".join(map(str, sorted(bad_vals))) if bad_vals else "", judgement)
        rows.append(row)
        if judgement == "MISMATCH":
            mismatch.append(row)

    out = OUT_DIR / "fullscan-step-axis.tsv"
    with out.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\tretail_setprogress\txml_transition_targets(event:var0 xN)\tout_of_set_values\tjudgement\n")
        for row in rows:
            handle.write("\t".join(map(str, row)) + "\n")
    mism = OUT_DIR / "fullscan-mismatch.tsv"
    with mism.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\tretail_setprogress\txml_transition_targets(event:var0 xN)\tout_of_set_values\tjudgement\n")
        for row in mismatch:
            handle.write("\t".join(map(str, row)) + "\n")

    counts: dict[str, int] = {}
    for row in rows:
        counts[row[4]] = counts.get(row[4], 0) + 1
    print("total xml:", len(rows), "judgements:", dict(sorted(counts.items())))
    for row in mismatch:
        print("MISMATCH", row[0], "retail=", row[1], "bad=", row[3])
        print("   ", row[2][:200])
    return 0


if __name__ == "__main__":
    sys.exit(main())
