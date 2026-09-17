#!/usr/bin/env python3
"""核对多档任务的“进入窗口档位”与“结算档位”是否一致（1114 型缺陷）。"""
from __future__ import annotations

from pathlib import Path
from xml.etree import ElementTree as ET

QUEST_DIR = Path("src/main/resources/aion/data/static_data/quest_definition/quests")
TIER_PAGE = (5, 6, 7, 8, 45, 46)
PAGE_ID = {"SHOW_SELECT_QUEST_REWARD_WINDOW%d" % n: TIER_PAGE[n - 1] for n in range(1, 7)}


def signature(rewards):
    return tuple(sorted(rewards))


def main() -> None:
    problems = []
    for path in sorted(QUEST_DIR.glob("*.xml")):
        root = ET.parse(path).getroot()
        groups = root.findall("./metadata/reward-groups/group")
        if len(groups) < 2:
            continue
        group_signatures = {index: signature([(r.get("kind"), r.get("id"), r.get("amount"))
                                             for r in group.iter("reward")])
                            for index, group in enumerate(groups)}
        nodes = {n.get("label"): n.get("status") for n in root.iter("node")}
        reward = {label for label, status in nodes.items() if status == "REWARD"}
        if not reward:
            continue
        entry_page = {label: set() for label in reward}
        persisted = {label: set() for label in reward}
        inline = {label: [] for label in reward}
        transitions = root.find("transitions")
        for block in transitions if transitions is not None else []:
            source, target = block.get("source"), block.get("target")
            if target in reward and block.tag == "transition":
                after = block.find("after-commit")
                if after is not None:
                    for c in after:
                        if c.tag == "dialog" and c.get("page") in PAGE_ID:
                            entry_page[target].add(PAGE_ID[c.get("page")])
            if source in reward and block.tag == "transition" and nodes.get(target) == "COMPLETE":
                actions = block.find("actions")
                if actions is None:
                    continue
                grants = [(c.get("kind"), c.get("id"), c.get("amount")) for c in actions
                          if c.tag == "grant-reward"]
                for cq in actions.iter("complete-quest"):
                    if cq.get("reward-index") is not None:
                        persisted[source].add(int(cq.get("reward-index")))
                if grants:
                    inline[source].append(signature(grants))
            if source in reward and block.tag == "npc-complete":
                persisted[source].add(int(block.get("complete-reward-index") or 0))
        for label in sorted(reward):
            pages = entry_page[label]
            if len(pages) != 1:
                continue
            page = next(iter(pages))
            inline_tiers = set()
            for sig in inline[label]:
                for index, group_sig in group_signatures.items():
                    if group_sig == sig:
                        inline_tiers.add(index)
            # 手写 grant-reward 的档位优先；仅在没有内联奖励时才回落到持久化索引。
            tiers = inline_tiers if inline_tiers else (
                set(persisted[label]) if len(persisted[label]) == 1 else set())
            if len(tiers) != 1:
                continue
            tier = next(iter(tiers))
            if not (0 <= tier < len(TIER_PAGE)):
                continue
            if TIER_PAGE[tier] != page:
                problems.append(f"{path.stem} reward node {label} opens page {page} (tier "
                                f"{TIER_PAGE.index(page) + 1}) but settles tier {tier + 1} "
                                f"(page {TIER_PAGE[tier]})")
    print(f"TIER_ALIGNMENT_FINDINGS={len(problems)}")
    for p in problems:
        print(p)


if __name__ == "__main__":
    main()
