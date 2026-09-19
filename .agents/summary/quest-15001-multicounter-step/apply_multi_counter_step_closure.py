#!/usr/bin/env python3
"""Close the SECTION_0 step index on step-0 multi-counter hunt quests.

Client contract (Aion 5.8 quest_monster.csv + quest_q*.html):
  SECTION_0 == 0 while the kill counters in SECTION_1..N are active.
  SECTION_0 == 1 when the summary moves to the report step.

The rewrite keeps the existing counter layout and adds the missing step-index
writes: started->started kill routes pin var0=0, started->reward completion
routes set var0=1, and the reward node projects var0=1.
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

QUESTS = (
    15001, 15020, 15073, 15100, 15104, 15203, 15406, 15407, 15408,
    15580, 15671, 25671, 25060, 18952,
)

# Some reward nodes intentionally projected only the step index; make the
# saturated counter projection explicit for the recovery-report route too.
REWARD_EXTRAS = {
    25671: (("var1", 1), ("var2", 6)),
    25060: (("var1", 1), ("var2", 1), ("var3", 1)),
}

ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src" / "main" / "resources" / "aion" / "data" / "static_data" / "quest_definition" / "quests"
REWARD_NODE = re.compile(r'<node label="reward"[^>]*>.*?</node>', re.S)
TRANSITION = re.compile(r"<transition\b.*?</transition>", re.S)

MIGRATION_MARKER = "<!-- SECTION_0 migration repair: legacy REWARD saves -->"


def patch_reward_node(text: str, quest_id: int) -> str:
    match = REWARD_NODE.search(text)
    if match is None:
        raise SystemExit(f"quest {quest_id}: reward node not found")
    node = match.group(0)
    if re.search(r'<var name="var0" value="\d+"\s*/>', node):
        node = re.sub(r'(<var name="var0" value=")\d+(")', r"\g<1>1\g<2>", node, count=1)
    else:
        opening = re.search(r'<node label="reward"[^>]*>\s*\n', node)
        if opening is None:
            raise SystemExit(f"quest {quest_id}: reward node is not pretty-printed")
        node = node[: opening.end()] + '      <var name="var0" value="1"/>\n' + node[opening.end():]
    extras = [
        (field, value)
        for field, value in REWARD_EXTRAS.get(quest_id, ())
        if not re.search(rf'<var name="{field}"\s', node)
    ]
    if extras:
        anchor = re.search(r'<var name="var0"[^>]*/>\s*\n', node)
        if anchor is None:
            raise SystemExit(f"quest {quest_id}: var0 anchor missing in reward node")
        inserted = "".join(
            f'      <var name="{field}" value="{value}"/>\n' for field, value in extras
        )
        node = node[: anchor.end()] + inserted + node[anchor.end():]
    return text[: match.start()] + node + text[match.end():]


def insert_action(block: str, action: str, quest_id: int) -> str:
    actions = re.search(r"<actions>\s*\n(\s*)(?=\S)", block)
    if actions is None:
        raise SystemExit(f"quest {quest_id}: transition without a pretty-printed actions block")
    return block[: actions.end()] + action + "\n" + actions.group(1) + block[actions.end():]


def patch_transition(match: re.Match[str], quest_id: int) -> str:
    block = match.group(0)
    opening = re.match(r"<transition\b[^>]*>", block)
    if opening is None or "<kill-npc" not in block:
        return block
    source = re.search(r'source="([^"]+)"', opening.group(0))
    target = re.search(r'target="([^"]+)"', opening.group(0))
    priority = re.search(r'priority="([^"]+)"', opening.group(0))
    if source is None or target is None or source.group(1) != "started":
        return block

    if target.group(1) == "started":
        action = '<set-variable field="var0" value="0"/>'
        if action in block:
            return block
        return insert_action(block, action, quest_id)
    if target.group(1) == "reward" and priority is not None and priority.group(1) == "0":
        action = '<set-variable field="var0" value="1"/>'
        if action not in block:
            block = insert_action(block, action, quest_id)
        block = block.replace(
            '<sync-quest-state mode="PACKET_ONLY"/>',
            '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
        )
        return block
    return block



def patch_migration_repair(text: str) -> str:
    if MIGRATION_MARKER in text:
        return text
    opening = re.search(r"<transitions[^>]*>\s*\n", text)
    if opening is None:
        raise SystemExit("transitions block is not pretty-printed")
    repair = (
        f'    {MIGRATION_MARKER}\n'
        '    <transition target="reward">\n'
        '      <event>\n'
        '        <enter-world/>\n'
        '      </event>\n'
        '      <conditions>\n'
        '        <status-is status="REWARD"/>\n'
        '        <variable-is field="var0" value="0"/>\n'
        '      </conditions>\n'
        '      <actions>\n'
        '        <set-variable field="var0" value="1"/>\n'
        '      </actions>\n'
        '      <after-commit>\n'
        '        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n'
        '      </after-commit>\n'
        '    </transition>\n'
    )
    return text[: opening.end()] + repair + text[opening.end():]

def patch_quest(quest_id: int, apply: bool) -> list[str]:
    path = QUEST_DIR / f"{quest_id}.xml"
    text = path.read_text(encoding="utf-8")
    updated = patch_reward_node(text, quest_id)
    updated = TRANSITION.sub(lambda m: patch_transition(m, quest_id), updated)
    updated = patch_migration_repair(updated)
    changes = []
    if updated != text:
        changes.append(f"{quest_id}: reward/transition step-index closure")
        if apply:
            path.write_text(updated, encoding="utf-8")
    return changes


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    all_changes = []
    for quest_id in QUESTS:
        all_changes.extend(patch_quest(quest_id, args.apply))
    mode = "APPLIED" if args.apply else "DRY-RUN"
    for change in all_changes:
        print(f"[{mode}] {change}")
    print(f"[{mode}] files changed: {len(all_changes)}")


if __name__ == "__main__":
    main()
