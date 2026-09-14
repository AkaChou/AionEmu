#!/usr/bin/env python3
"""精确定位与 1192 同型的高危任务。

1192 的真实形态: XML 里确实存在覆盖该 work item 的 remove-item,
但它只挂在其中一条进入 REWARD 的转换上; 玩家走另一条进入 REWARD 的转换时
物品不会被交出, 而完成时又因缺少 <work-items> 声明而不被引擎清理 -> 残留。

判定: 存在 ≥2 条 target 状态为 REWARD 的转换, 且其中至少一条不携带
覆盖全部 legacy work items 的 remove-item。
"""
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(".").resolve()
QUEST_DATA = ROOT / "src/main/resources/aion/data/static_data/quest_data/quest_data.xml"
DEF_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"
CATALOG = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"

legacy = {}
for quest in ET.parse(QUEST_DATA).getroot().iter("quest"):
    node = quest.find("quest_work_items")
    if node is None:
        continue
    items = [(int(w.get("item_id")), int(w.get("count") or 1)) for w in node.findall("quest_work_item")]
    if items:
        legacy[int(quest.get("id"))] = items

executable = {int(d.get("id")) for d in ET.parse(CATALOG).getroot().iter("definition")
              if d.get("mode") == "EXECUTABLE"}

risky, legacy_style = [], []
for qid, items in sorted(legacy.items()):
    if qid not in executable:
        continue
    path = DEF_DIR / f"{qid}.xml"
    if not path.exists():
        continue
    root = ET.parse(path).getroot()
    if root.find("metadata/work-items") is not None:
        continue  # 已声明 -> 完成时引擎自动清理, 不属本审计
    required = {i for i, _ in items}
    status_of = {n.get("label"): n.get("status") for n in root.iter("node")}

    into_reward = [tr for tr in root.iter("transition") if status_of.get(tr.get("target")) == "REWARD"]
    into_reward += [tr for tr in root.iter("dialog") if status_of.get(tr.get("target")) == "REWARD"]
    if len(into_reward) < 2:
        continue

    def removed_by(tr):
        return {int(e.get("item-id")) for e in tr.iter("remove-item") if e.get("item-id")}

    bare = [tr for tr in into_reward if not required.issubset(removed_by(tr))]
    if bare:
        entry = (qid, items, [tr.get("source") or "(source-less)" for tr in bare],
                 len(into_reward), sorted({tr.get("source") or "(source-less)" for tr in bare}))
        if any(s not in ("reward",) for s in entry[2]):
            legacy_style.append(entry)
        else:
            risky.append(entry)

print(f"=== A 类: 入 REWARD 存在非 reward 来源的裸转换（与 1192 同型）: {len(legacy_style)} ===")
for qid, items, sources, total, _ in legacy_style:
    print(f"  {qid}: work={items} 入REWARD共{total}条, 裸转换源={sources}")
print()
print(f"=== B 类: 仅 reward 自环缺移除（通常是预览路由, 需人工核对）: {len(risky)} ===")
print(" ".join(str(q) for q, *_ in risky))
