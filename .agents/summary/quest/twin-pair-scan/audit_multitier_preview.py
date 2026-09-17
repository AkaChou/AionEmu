#!/usr/bin/env python3
"""多档奖励任务：每个 REWARD 节点必须能打开本档奖励窗口（修复后语义复算）。"""
from __future__ import annotations

from pathlib import Path
from xml.etree import ElementTree as ET

QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
PAGE_ID = {"SHOW_SELECT_QUEST_REWARD_WINDOW%d" % n: (5, 6, 7, 8, 45, 46)[n - 1] for n in range(1, 7)}
TIER_PAGE = (5, 6, 7, 8, 45, 46)
ID_TO_TIER = {pid: tier for tier, pid in enumerate(TIER_PAGE)}
ACTION_ID = {f"SELECTED_QUEST_REWARD{i}": 7 + i for i in range(1, 17)}
ACTION_ID.update({"SELECTED_QUEST_NOREWARD": 23, "SELECT_QUEST_REWARD": 1009, "USE_OBJECT": -1,
                  "CHECK_USER_HAS_QUEST_ITEM": 39, "QUEST_SELECT": 31})


def ids_of(element: ET.Element) -> list[int]:
    out: list[int] = []
    for key in ("action", "actions"):
        raw = element.get(key)
        if not raw:
            continue
        for token in raw.split():
            if ".." in token:
                start, end = token.split("..")
                if start in ACTION_ID and end in ACTION_ID:
                    out.extend(range(ACTION_ID[start], ACTION_ID[end] + 1))
                continue
            out.append(ACTION_ID.get(token, -999))
    return out


def pages_of(after: ET.Element | None) -> set[int]:
    out: set[int] = set()
    if after is None:
        return out
    for c in after:
        if c.tag == "dialog" and c.get("type") in ("SHOW_QUEST_PAGE", "SHOW_QUEST_DIALOG"):
            page = c.get("page") or ""
            if page in PAGE_ID:
                out.add(PAGE_ID[page])
    return out


def main() -> None:
    problems: list[str] = []
    scanned = 0
    for path in sorted(QUEST_DIR.glob("*.xml")):
        root = ET.parse(path).getroot()
        groups = root.findall("./metadata/reward-groups/group")
        if len(groups) < 2:
            continue
        nodes = {n.get("label"): n.get("status") for n in root.iter("node")}
        reward = {label for label, status in nodes.items() if status == "REWARD"}
        if not reward:
            continue
        scanned += 1
        indexes: dict[str, set[int]] = {label: set() for label in reward}
        previews: dict[str, set[int]] = {label: set() for label in reward}
        transitions = root.find("transitions")
        for block in transitions if transitions is not None else []:
            source = block.get("source")
            if block.tag == "transition":
                actions = block.find("actions")
                if actions is not None and source in reward and nodes.get(block.get("target")) == "COMPLETE":
                    for cq in actions.iter("complete-quest"):
                        if cq.get("reward-index") is not None:
                            indexes[source].add(int(cq.get("reward-index")))
                event = block.find("event")
                child = event[0] if event is not None and len(event) else None
                if child is not None and child.tag == "dialog" and child.get("type") == "TALK_TO_NPC" \
                        and source in reward and set(ids_of(child)) & {-1, 1009}:
                    previews[source].update(pages_of(block.find("after-commit")))
            elif block.tag == "npc-complete" and source in reward:
                index = int(block.get("complete-reward-index") or 0)
                indexes[source].add(index)
                if block.find("preview") is not None or block.get("preview-dialog-ids"):
                    previews[source].add(TIER_PAGE[index])
            elif block.tag == "equipment-exchange":
                for rg in block.findall("reward-group"):
                    index = int(rg.get("index"))
                    page = PAGE_ID.get(rg.get("page") or "")
                    if page is None:
                        problems.append(f"{path.stem} equipment-exchange tier {index} declares unknown page "
                                        f"{rg.get('page')}")
                        continue
                    if ID_TO_TIER[page] != index:
                        problems.append(f"{path.stem} equipment-exchange tier {index} opens window tier "
                                        f"{ID_TO_TIER[page]}")
                    indexes.setdefault(block.get("reward"), set()).add(index)
                    previews.setdefault(block.get("reward"), set()).add(page)
        for label in sorted(reward):
            allowed = {TIER_PAGE[i] for i in indexes[label] if 0 <= i <= 5}
            found = previews[label]
            if not found:
                problems.append(f"{path.stem} reward node {label} has no reward-window preview "
                                f"(indexes={sorted(indexes[label])})")
            elif found - allowed:
                problems.append(f"{path.stem} reward node {label} opens {sorted(found)} but only "
                                f"{sorted(allowed)} is allowed for indexes {sorted(indexes[label])}")
    print(f"MULTI_TIER_QUESTS={scanned}")
    print(f"MULTI_TIER_FINDINGS={len(problems)}")
    for p in problems:
        print(p)


if __name__ == "__main__":
    main()
