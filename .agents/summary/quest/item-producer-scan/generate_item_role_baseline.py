#!/usr/bin/env python3
"""生成 QE-031 门禁基线：真端 collect_item/check_item 名称 -> 我方道具 ID。

Builds the item-role baseline used by QuestItemSourceContractGateTest. A row is emitted only when
every retail collect/check name of that quest resolves to an item id through `name_desc`, so an
unresolved name can never create a false violation.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
ITEMS = ROOT / "src/main/resources/aion/data/static_data/items"
RETAIL = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest.xml")
OUT = ROOT / "src/test/resources/quest/quest-item-role-baseline.tsv"


def main() -> int:
    name2id: dict[str, int] = {}
    for path in ITEMS.rglob("*.xml"):
        for m in re.finditer(r"<item_template\b[^>]*>", path.read_text(errors="ignore")):
            body = m.group(0)
            i, n = re.search(r'\bid="(\d+)"', body), re.search(r'name_desc="([^"]*)"', body)
            if i and n:
                name2id.setdefault(n.group(1), int(i.group(1)))

    rows = []
    for block in re.findall(r"<quest>(.*?)</quest>", RETAIL.read_text(encoding="utf-8", errors="ignore"), re.S):
        m = re.search(r"<id>(\d+)</id>", block)
        if not m:
            continue
        names = set()
        for field, value in re.findall(r"<([a-z_0-9]+)>([^<]*)</\1>", block):
            if field.startswith(("collect_item", "check_item")) and value.strip():
                names.add(value.split()[0])
        if not names or not names <= set(name2id):
            continue
        rows.append((int(m.group(1)), sorted({name2id[n] for n in names})))
    rows.sort()
    OUT.write_text("# questId\tretailCollectCheckItemIds\n"
                   + "\n".join(f"{q}\t{','.join(map(str, ids))}" for q, ids in rows) + "\n")
    print(f"baseline rows={len(rows)} -> {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
