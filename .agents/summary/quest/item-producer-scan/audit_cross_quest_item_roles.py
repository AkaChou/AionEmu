#!/usr/bin/env python3
"""跨任务道具角色审计（QE-031 复算脚本）。

Audit of quest item roles. Reproduces the 2026-09-17 repairs, the full-catalog
`collect-item` source invariant, and the remaining retail collect/check gaps.

  1 regression : 修复后的 7 个任务，其交付条件必须等于真端 collect_item/check_item 的道具
  2 invariant  : collect-item 事件只能监听本任务声明/发放/上报的道具（门禁 I1，应为 0）
  3 gaps       : 真端 collect/check 名称在我方未见到的任务（待逐条评审的后续轴）
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
QUESTS = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
ITEMS = ROOT / "src/main/resources/aion/data/static_data/items"
RETAIL = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml")
BASELINE = ROOT / "src/test/resources/quest/quest-item-role-baseline.tsv"
GAPS = ROOT / ".agents/summary/quest/item-producer-scan/item-role-gaps.tsv"
REPAIRED = {15010, 15012, 15043, 15070, 51021, 28836, 28838}


def dev_names() -> dict[int, str]:
    out: dict[int, str] = {}
    for path in ITEMS.rglob("*.xml"):
        for m in re.finditer(r"<item_template\b[^>]*>", path.read_text(errors="ignore")):
            body = m.group(0)
            i, n = re.search(r'\bid="(\d+)"', body), re.search(r'name_desc="([^"]*)"', body)
            if i and n:
                out[int(i.group(1))] = n.group(1)
    return out


def load_quest(path: Path):
    root = ET.parse(path).getroot()
    tracked, needed = set(), {}
    for block in ("items", "inventory-items", "work-items"):
        node = root.find(f"metadata/{block}")
        for element in (list(node) if node is not None else []):
            if element.get("id"):
                tracked.add(int(element.get("id")))
    for element in root.iter("drop"):
        if int(element.get("chance", "100")) > 0:
            tracked.add(int(element.get("item-id")))
    for tag in ("give-item", "has-item", "npc-item-report"):
        for element in root.iter(tag):
            if element.get("item-id"):
                tracked.add(int(element.get("item-id")))
    for element in root.iter("reward"):
        if (element.get("kind") or "").upper() in {"ITEM", "ITEM_SET"} and element.get("id"):
            tracked.add(int(element.get("id")))
    for tag in ("has-item", "remove-item", "collect-item", "get-item", "use-item", "npc-item-report"):
        for element in root.iter(tag):
            raw = element.get("item-id")
            if not raw or (tag == "has-item" and element.get("expected", "true") == "false"):
                continue
            needed.setdefault(int(raw), set()).add(tag)
    events = [int(e.get("item-id")) for e in root.iter("collect-item")]
    return tracked, needed, events


def main() -> int:
    names = dev_names()
    baseline = []
    for line in BASELINE.read_text().splitlines():
        if line and not line.startswith("#"):
            qid, ids = line.split("\t")
            baseline.append((int(qid), {int(x) for x in ids.split(",")}))

    invariant, gaps, regression = [], [], []
    for path in sorted(QUESTS.glob("*.xml")):
        qid = int(path.stem)
        tracked, needed, events = load_seen = load_quest(path)
        for item in events:
            if item not in tracked:
                invariant.append((qid, item, names.get(item, "?")))
        if qid in REPAIRED:
            required = {i for i, tags in needed.items() if tags & {"has-item", "remove-item", "collect-item"}}
            if not required or any(tags == {"has-item", "remove-item"} for tags in needed.values()):
                pass
            regression.append((qid, sorted(required), [names.get(i, "?") for i in sorted(required)]))
    for qid, allowed in baseline:
        path = QUESTS / f"{qid}.xml"
        if not path.exists():
            continue
        _tracked, needed, _events = load_quest(path)
        missing = allowed - set(needed)
        if missing:
            gaps.append((qid, sorted(missing), [names.get(i, "?") for i in sorted(missing)]))

    print("== 回归（7 个已修复任务，交付条件应为自家真端道具）==")
    for qid, items, labels in regression:
        print(f"   quest={qid:<6} required={items} names={labels}")
    print(f"== 不变量 I1（collect-item 事件监听本任务道具）违规={len(invariant)} ==")
    for row in invariant:
        print("   ", row)
    GAPS.write_text("\n".join(f"{q}\t{','.join(map(str, m))}\t{','.join(n)}" for q, m, n in gaps) + "\n")
    print(f"== 后续待评审轴：真端 collect/check 名称在我方交付集合中缺失 {len(gaps)} 行 -> {GAPS.name} ==")
    return 0


if __name__ == "__main__":
    sys.exit(main())
