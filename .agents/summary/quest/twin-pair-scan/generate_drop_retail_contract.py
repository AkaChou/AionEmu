#!/usr/bin/env python3
"""从真端 Aion 5.8 客户端 quest.xml 生成掉落契约基线 TSV。

Generates the retail drop-contract baseline consumed by QuestDropContractGateTest.
The retail file names items and monsters, so the baseline stores a name-independent
structure: per-monster drop-chance histogram plus the number of distinct drop items.
"""
from __future__ import annotations

import re
import sys
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET

DEFAULT_RETAIL = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml")
QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
OUTPUT = Path("src/test/resources/quest/quest-drop-retail-contract.tsv")
FIELD = re.compile(r"^drop_(monster|item|prob)_(\d+)$")

HEADER = [
    "# 真端 Aion 5.8 客户端 quest.xml 掉落契约基线（由 Quest_unpacked/quest.xml 生成）。",
    "# Retail Aion 5.8 client quest.xml drop-contract baseline (generated from Quest_unpacked/quest.xml).",
    "# 列 / columns: questId <TAB> distinctDropItems <TAB> per-monster chance histogram (chance:monsterCount, ...)",
    "# 语义 / semantics: 生产目录允许比基线多来源（>=），但不得少于基线的怪物数或丢失概率档；",
    "#                production must not offer fewer monsters per chance than retail (extra sources are allowed).",
    "# 生成脚本 / generator: .agents/summary/quest/twin-pair-scan/generate_drop_retail_contract.py",
]


def main() -> None:
    retail = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_RETAIL
    if not retail.exists():
        raise SystemExit(f"retail quest.xml not found: {retail}")
    rows = []
    for quest in ET.parse(retail).getroot().findall("quest"):
        raw = quest.findtext("id")
        if not raw or not (QUEST_DIR / f"{raw}.xml").exists():
            continue
        groups: dict[int, dict[str, str]] = {}
        for child in quest:
            match = FIELD.match(child.tag)
            if not match:
                continue
            text = (child.text or "").strip()
            if text:
                groups.setdefault(int(match.group(2)), {})[match.group(1)] = text
        drops = [group for group in groups.values() if group.get("monster") and group.get("item")]
        if not drops:
            continue
        chances: Counter[int] = Counter()
        items: set[str] = set()
        for group in drops:
            prob = int(group.get("prob") or "0")
            items.add(group["item"])
            for _ in group["monster"].split():
                chances[prob] += 1
        histogram = ",".join(f"{prob}:{count}" for prob, count in sorted(chances.items()))
        rows.append((int(raw), len(items), histogram))
    rows.sort()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(HEADER + [f"{qid}\t{items}\t{hist}" for qid, items, hist in rows]) + "\n")
    print(f"wrote {len(rows)} rows to {OUTPUT}")


if __name__ == "__main__":
    main()
