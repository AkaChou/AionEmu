#!/usr/bin/env python3
# 真端 con_quest / cutscene 轴的全族只读审计（P4 残余批证据工具，不参与运行时）。
# Read-only audit of the retail con_quest / cutscene axes across every switched family.
#
# 事实来源：真端表 Quest_*.xml（家族列 con_quest / cutsceneid1 / cs1_haction）与 XML 定义目录存在性。
# 判据（与本仓逐行对拍门同一条不变量）：con_quest 的下一环必须在本行的交付 NPC 上可接取
# （本车道接取路由按 NPC 建表 ⇒ 该等价物即真端 0x1e 槽 `<mgr>+0x1a8(player, con_quest)` 的接取窗）。
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[5]
RETAIL = ROOT / "src/main/resources/aion/data/static_data/quest/retail"
DEFS = ROOT / "src/main/resources/aion/data/static_data/quest/definitions/quests"

ROW = re.compile(r'<id id="(\d+)">(.*?)</id>', re.S)
FIELD = re.compile(r"<(\w+)>(.*?)</\1>", re.S)


def parse(path):
    text = path.read_text(encoding="utf-8")
    return {int(m.group(1)): dict(FIELD.findall(m.group(2))) for m in ROW.finditer(text)}


def main():
    tables = {}
    for path in sorted(RETAIL.glob("Quest_*.xml")):
        family = path.stem.removeprefix("Quest_")
        tables[family] = parse(path)
    index = {}
    for family in tables:
        for quest_id in tables[family]:
            index.setdefault(quest_id, []).append(family)

    def acquire_npc(family, quest_id):
        row = tables.get(family, {}).get(quest_id)
        if row is None:
            return None
        for column in ("acquired_npc_name", "task_npc"):
            if row.get(column):
                return row[column]
        return None

    out = []
    summary = {}
    for family in sorted(tables):
        counts = {"IN_TABLE": 0, "SIBLING": 0, "USE_ITEM": 0, "NO_ROW": 0, "NO_ROW_XML": 0,
                  "MISMATCH": 0}
        cutscenes = []
        for quest_id, row in sorted(tables[family].items()):
            target = row.get("con_quest")
            if target:
                target = int(target)
                reward = row.get("reward_npc_name")
                families = index.get(target, [])
                target_acquire = None
                target_family = None
                for candidate in families:
                    value = acquire_npc(candidate, target)
                    if value:
                        target_acquire, target_family = value, candidate
                        break
                if not families:
                    kind = "NO_ROW_XML" if (DEFS / f"{target}.xml").exists() else "NO_ROW"
                elif target_acquire is None:
                    kind = "USE_ITEM" if "SimpleUseItem" in families else "MISMATCH"
                elif target_family == family:
                    kind = "IN_TABLE"
                else:
                    kind = "SIBLING"
                if kind in ("IN_TABLE", "SIBLING") and reward != target_acquire:
                    kind = "MISMATCH"
                counts[kind] += 1
                out.append((family, quest_id, target, reward, target_acquire, target_family or "", kind))
            if row.get("cutsceneid1"):
                cutscenes.append((quest_id, int(row["cutsceneid1"]),
                                  int(row["cs1_haction"]) if row.get("cs1_haction") else -1))
        summary[family] = (counts, cutscenes)

    print("family\tcon_quest\tIN_TABLE\tSIBLING\tUSE_ITEM\tNO_ROW\tNO_ROW_XML\tMISMATCH\tcutscene")
    for family in sorted(tables):
        counts, cutscenes = summary[family]
        total = sum(counts.values())
        print(f"{family}\t{total}\t{counts['IN_TABLE']}\t{counts['SIBLING']}\t{counts['USE_ITEM']}\t"
              f"{counts['NO_ROW']}\t{counts['NO_ROW_XML']}\t{counts['MISMATCH']}\t{len(cutscenes)}")
    print()
    print("family\tquest_id\ttarget\treward_npc\ttarget_acquire\ttarget_family\tclass")
    for row in out:
        print("\t".join(str(value) for value in row))
    print()
    print("family\tquest_id\tmovie\taction")
    for family in sorted(tables):
        for quest_id, movie, action in summary[family][1]:
            print(f"{family}\t{quest_id}\t{movie}\t{action}")
    mismatches = [row for row in out if row[-1] == "MISMATCH"]
    return 1 if mismatches else 0


if __name__ == "__main__":
    sys.exit(main())
