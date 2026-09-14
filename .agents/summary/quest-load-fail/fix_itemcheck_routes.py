#!/usr/bin/env python3
"""为 select5(2375) 按钮为 CHECK_USER_HAS_QUEST_ITEM(39) 的任务补 npc-item-report 上交块。

Add npc-item-report turn-in blocks (or explicit variant-item routes) for quests whose
client select5 page carries the CHECK_USER_HAS_QUEST_ITEM button.
"""
from __future__ import annotations

import argparse
import hashlib
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
MANIFEST = ROOT / ".agents/summary/quest-load-fail/itemcheck-manifest.csv"

# (quest, npc)：repoint 后 select5(2375) 按钮 39 缺上交路由的任务。
# (quest, npc): quests whose repointed select5 page lacks the item-check route.
TARGETS = [
    (1972, 203788), (1974, 203790), (1976, 203792), (1978, 203784),
    (1980, 203786), (1982, 203793), (2972, 204104), (2974, 204106),
    (2976, 204108), (2978, 204100), (2980, 204102), (2982, 204110),
]


def digest(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    edits: dict[Path, list[str]] = defaultdict(list)
    rows = []
    for q, npc in TARGETS:
        path = QUEST_DIR / f"{q}.xml"
        root = ET.parse(path).getroot()
        items = [(int(i.get("id")), int(i.get("count", "1")))
                 for i in root.findall("./metadata/items/item")]
        if not items:
            raise RuntimeError(f"{q}: no item requirements")
        if len(items) == 1:
            item_id, count = items[0]
            block = (f'\n    <npc-item-report npc-id="{npc}" source="started" target="reward"'
                     f' item-id="{item_id}" required="{count}"/>')
            rows.append({"quest_id": q, "npc": npc, "decision": "npc-item-report",
                         "detail": f"{item_id}:{count}"})
        else:
            # 变体物品（任一即可）：互斥条件 + 唯一优先级 + SELECT6 回落。
            # Variant items (any one accepted): exclusive conditions, unique priorities, SELECT6 fallback.
            blocks = []
            for index, (item_id, count) in enumerate(items):
                blocks.append(
                    f'\n    <transition source="started" target="reward" priority="{index}">\n'
                    '      <event>\n'
                    f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>\n'
                    '      </event>\n'
                    '      <conditions>\n'
                    f'        <has-item item-id="{item_id}" count="{count}"/>\n'
                    '      </conditions>\n'
                    '      <actions>\n'
                    f'        <remove-item item-id="{item_id}" count="{count}"/>\n'
                    '      </actions>\n'
                    '      <after-commit>\n'
                    '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
                    '        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>\n'
                    '      </after-commit>\n'
                    '    </transition>')
            blocks.append(
                f'\n    <transition source="started" target="started" priority="10">\n'
                '      <event>\n'
                f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>\n'
                '      </event>\n'
                '      <after-commit>\n'
                '        <dialog type="SHOW_QUEST_PAGE" page="SELECT6"/>\n'
                '      </after-commit>\n'
                '    </transition>')
            block = "".join(blocks)
            rows.append({"quest_id": q, "npc": npc, "decision": "explicit-variant-routes",
                         "detail": f"{len(items)} items"})
        edits[path].append(block)

    import csv
    with MANIFEST.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=["quest_id", "npc", "decision", "detail"])
        writer.writeheader()
        writer.writerows(rows)
    print(f"candidates={len(rows)} files={len(edits)} manifest={MANIFEST}")
    if not args.write:
        return 0
    for path, blocks in edits.items():
        before = path.read_bytes()
        source = before.decode("utf-8")
        source = source.replace("</transitions>", "".join(blocks) + "</transitions>", 1)
        ET.fromstring(source)
        if digest(path.read_bytes()) != digest(before):
            raise RuntimeError(f"concurrent change: {path}")
        path.write_text(source, encoding="utf-8")
    print(f"changed files={len(edits)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
