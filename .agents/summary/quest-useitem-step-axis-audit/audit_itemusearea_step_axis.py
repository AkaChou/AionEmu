#!/usr/bin/env python3
"""itemUseArea（使用道具取水/灌装）族步号轴审计：真端 SetProgress / 槽位注册 vs 生产 XML 的 item-play 轴值。

背景（2026-10-07，1361/1373 取水步骤空白修复 qe-1361-1373-water-step-axis）：
- 该族任务的"打水后"步号轴以真端 `SetProgress(questId, 值)` 与实际执行到的值为权威
  （1361: `SetProgress(0x551, 1)`；1373: `SetProgress(0x55d, 2)`），legacy `setQuestVar(N)` 同值；
  真端槽位注册 `FUN_180cb3070(_, _, questId, 3, 值, 0)` 的第 5 参 = 行 1 visible 槽位 [%3] 的绑定值，
  可作第三方交叉证据。`collect_progress` 与"末行索引"都不是轴判据。
- 错误来源样例：7d5bb5317 把 1373 打水后的 v2(2) 改成 v1(1)（按 collect_progress=1 推断）。

判据（本脚本的 MISMATCH 只是候选，必须逐任务人工复核真端函数体与 legacy 后再定案）：
- 每个 item-play（使用道具）transition 的目标节点 var0 应落在该任务真端 SetProgress 的取值集合内。

用法：
    python3 .agents/summary/quest-useitem-step-axis-audit/audit_itemusearea_step_axis.py
输出：
    itemusearea-step-axis-scan.tsv（逐任务一行）
    itemusearea-step-axis-mismatch.tsv（候选差异清单）
"""

from __future__ import annotations

import csv
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

REPO = next(p for p in Path(__file__).resolve().parents if (p / "pom.xml").is_file())
QUESTS_DIR = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"


def resolve_external(candidates: list[Path], probe: str) -> Path:
    """按同宿主目录约定解析外部根；容错若干常见宿主布局。"""
    for base in candidates:
        if (base / probe).exists():
            return base
    raise SystemExit(f"cannot resolve external root for {probe}; tried: {candidates}")


UNPAK = resolve_external(
    [REPO.parent / "PycharmProjects" / "unpak", Path.home() / "PycharmProjects" / "unpak"],
    "Quest_unpacked/quest_script_monster.csv")
RETAIL_ROOT = resolve_external(
    [REPO.parent / "58Server", Path.home() / "IdeaProjects" / "58Server"],
    "server58/MainServer_ScriptDLL64/ScriptDLL64.c")
SCRIPT_CSV = UNPAK / "Quest_unpacked" / "quest_script_monster.csv"
RETAIL_C = RETAIL_ROOT / "server58" / "MainServer_ScriptDLL64" / "ScriptDLL64.c"
OUT_DIR = Path(__file__).resolve().parent

# 真端 vtable SetProgress 调用形态：  (**(code **)(*obj + 0xf0))(obj, 0x55d, 2);
# 兼容四参数变体 (obj, quest, 值, 0)（如 24154 的 0xf0(plVar3,0x5e5a,4,0)）。
SETPROG = re.compile(
    r"0xf0\)\)\(\s*[A-Za-z_0-9]+,\s*0x([0-9a-fA-F]{1,7})\s*,\s*(0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*"
    r"(?:,\s*(?:0x[0-9a-fA-F]{1,4}|[0-9]{1,4})\s*)?\)")
# 真端槽位注册： FUN_180cb3070(&slot,&obj,0x551,3,1,0);
SLOTREG = re.compile(
    r"FUN_180cb3070\(\s*&[A-Za-z_0-9]+,\s*&[A-Za-z_0-9]+,\s*0x([0-9a-fA-F]{1,7}),\s*(\d+),\s*(0x[0-9a-fA-F]{1,8}|\d+),\s*(0x[0-9a-fA-F]{1,8}|\d+)\s*\)")


def load_family() -> dict[int, list[tuple[str, str, str, str]]]:
    family: dict[int, list[tuple[str, str, str, str]]] = {}
    with SCRIPT_CSV.open(encoding="utf-8", errors="ignore") as handle:
        for row in csv.DictReader(handle):
            if row.get("sourceType") != "itemUseArea":
                continue
            family.setdefault(int(row["questId"]), []).append(
                (row.get("progress", ""), row.get("item", ""), row.get("sourceName", ""), row.get("num", "")))
    return family


def scan_retail() -> tuple[dict[int, set[int]], dict[int, dict[int, int]]]:
    text = RETAIL_C.read_text(encoding="utf-8", errors="ignore")
    setprog: dict[int, set[int]] = {}
    for hex_id, value in SETPROG.findall(text):
        setprog.setdefault(int(hex_id, 16), set()).add(int(value, 0))
    slotreg: dict[int, dict[int, int]] = {}
    for hex_id, slot, value, _flag in SLOTREG.findall(text):
        slotreg.setdefault(int(hex_id, 16), {})[int(slot)] = int(value, 0)
    return setprog, slotreg


def xml_axis(quest_id: int):
    path = QUESTS_DIR / f"{quest_id}.xml"
    if not path.is_file():
        return None
    root = ET.parse(path).getroot()
    nodes: dict[str, int | None] = {}
    for node in root.findall("nodes/node"):
        var = node.find("var[@name='var0']")
        nodes[node.get("label")] = int(var.get("value")) if var is not None else None
    itemplay: list[tuple[str, str, int | None]] = []
    for tr in root.findall("transitions/transition"):
        event = tr.find("event")
        if event is None:
            continue
        if event.find("item-play") is None and event.find("use-item") is None:
            continue
        target = tr.get("target")
        itemplay.append((tr.get("source"), target, nodes.get(target)))
    return nodes, itemplay


def main() -> int:
    if not RETAIL_C.is_file():
        print(f"missing retail source: {RETAIL_C}", file=sys.stderr)
        return 2
    family = load_family()
    setprog, slotreg = scan_retail()
    rows = []
    mismatch = []
    for quest_id in sorted(family):
        entries = family[quest_id]
        retail_vals = sorted(setprog.get(quest_id, set()))
        slots = slotreg.get(quest_id, {})
        axis = xml_axis(quest_id)
        if axis is None:
            rows.append((quest_id, entries[0][1], entries[0][0], "|".join(map(str, retail_vals)),
                         slots.get(3, ""), "-", "-", "NO_XML"))
            continue
        nodes, itemplay = axis
        targets = sorted({t for _, _, t in itemplay if t is not None})
        reward_row = nodes.get("reward")
        judgement = "NO_ITEMPLAY"
        if itemplay:
            if not retail_vals:
                judgement = "NO_RETAIL_SETPROGRESS"
            elif all(t in retail_vals for t in targets):
                judgement = "MATCH"
            else:
                judgement = "MISMATCH"
        row = (quest_id, entries[0][1], entries[0][0], "|".join(map(str, retail_vals)),
               slots.get(3, ""), "|".join(map(str, targets)), str(reward_row), judgement)
        rows.append(row)
        if judgement == "MISMATCH":
            mismatch.append(row)

    out = OUT_DIR / "itemusearea-step-axis-scan.tsv"
    with out.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\titem\tprogress\tretail_setprogress\tslot3_registration\txml_itemplay_targets\txml_reward_row\tjudgement\n")
        for row in rows:
            handle.write("\t".join(map(str, row)) + "\n")
    mism = OUT_DIR / "itemusearea-step-axis-mismatch.tsv"
    with mism.open("w", encoding="utf-8") as handle:
        handle.write("quest_id\titem\tprogress\tretail_setprogress\tslot3_registration\txml_itemplay_targets\txml_reward_row\tjudgement\n")
        for row in mismatch:
            handle.write("\t".join(map(str, row)) + "\n")

    counts: dict[str, int] = {}
    for row in rows:
        counts[row[7]] = counts.get(row[7], 0) + 1
    print("family:", len(rows), "judgements:", dict(sorted(counts.items())))
    for row in mismatch:
        print("MISMATCH", row)
    return 0


if __name__ == "__main__":
    sys.exit(main())
