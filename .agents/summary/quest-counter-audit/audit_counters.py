#!/usr/bin/env python3
"""只读审计：比较任务 XML 的击杀计数器阈值与客户端 quest_monster.csv 的分段门控。

Read-only audit comparing quest XML kill-counter thresholds against the client
quest_monster.csv section gates (SECTION_1 / SECTION_2).

仅报告，不修改任何文件。 / Report only; nothing is written.
"""
import csv
import re
import sys
from pathlib import Path

CLIENT_CSV = Path("/Users/mc/PycharmProjects/unpak/Quest_unpacked/quest_monster.csv")
QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")


def client_sections() -> dict[int, dict[int, int]]:
    gates: dict[int, dict[int, int]] = {}
    with CLIENT_CSV.open(encoding="utf-8", errors="ignore") as handle:
        for row in csv.reader(handle):
            if len(row) < 2 or not row[0].isdigit():
                continue
            quest_id = int(row[0])
            for index, gate in re.findall(r"SECTION_(\d+)<(\d+)", row[1]):
                gates.setdefault(quest_id, {})[int(index)] = int(gate)
    return gates


def xml_counter_requirements(text: str) -> dict[str, int]:
    """返回 var1/var2 的完成阈值：<set-variable field="varN" value="Y"/> 的最大 Y。"""
    required: dict[str, int] = {}
    for field, value in re.findall(r'<set-variable field="(var\d+)" value="(\d+)"/>', text):
        required[field] = max(required.get(field, 0), int(value))
    return required


def main() -> int:
    gates = client_sections()
    mismatches = []
    checked = 0
    for path in sorted(QUEST_DIR.glob("*.xml")):
        quest_id = int(path.stem)
        client = gates.get(quest_id)
        if not client:
            continue
        section1 = client.get(1)
        if section1 is None:
            continue
        text = path.read_text(encoding="utf-8")
        if "kill-npc" not in text:
            continue
        required = xml_counter_requirements(text)
        # 客户端 SECTION_1 门控 N 表示需要 N 次击杀；XML 用 var1 目标值表达同一合同。
        xml_target = required.get("var1")
        if xml_target is None:
            continue
        checked += 1
        if xml_target != section1:
            mismatches.append((quest_id, section1, xml_target, path.name))
    print(f"checked={checked} mismatches={len(mismatches)}")
    for quest_id, client_value, xml_value, name in mismatches:
        print(f"  {quest_id}: client SECTION_1<{client_value} vs xml var1 target {xml_value} ({name})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
