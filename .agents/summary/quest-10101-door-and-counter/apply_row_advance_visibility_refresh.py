#!/usr/bin/env python3
"""把「任务说明行推进」的提交后同步升到 VISIBILITY_REFRESH（区/nearby 刷新）。

Raises the after-commit sync of journal-row advancing transitions to VISIBILITY_REFRESH: the GM
`//quest set` command additionally calls updateZone() + updateNearbyQuests(), which is what made the
client re-render the journal row on the live server; PACKET_ONLY alone kept the stale row.

Dry run by default; pass --apply to write.
"""
import argparse
import re
import sys

EDITS = {
    10101: [("s3", "s4"), ("s4", "s5"), ("s5", "s6"), ("s6", "s7"), ("s7", "s8")],
    20101: [("s3", "s4"), ("s4", "s5"), ("s5", "s6"), ("s6", "s7"), ("s7", "s8")],
    14021: [("s6", "s7")],
    24014: [("s4", "s5")],
}
PATH = "src/main/resources/aion/data/static_data/quest_definition/quests/{}.xml"
TRANSITION_RE = re.compile(r"<transition\b.*?</transition>", re.S)


def patch(quest_id, pairs, apply):
    path = PATH.format(quest_id)
    text = open(path, encoding="utf-8").read()
    changed = 0

    def replace(match):
        nonlocal changed
        block = match.group(0)
        source = re.search(r'<transition source="([^"]+)"', block).group(1)
        target = re.search(r'target="([^"]+)"', block).group(1)
        if (source, target) not in pairs:
            return block
        if 'mode="PACKET_ONLY"' not in block:
            sys.exit(f"{quest_id}: {source}->{target} has no PACKET_ONLY sync to upgrade")
        changed += 1
        return block.replace('mode="PACKET_ONLY"', 'mode="VISIBILITY_REFRESH"')

    text = TRANSITION_RE.sub(replace, text)
    if changed != len(pairs):
        sys.exit(f"{quest_id}: patched {changed} of {len(pairs)} transitions")
    if apply:
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(text)
    print(f"quest {quest_id}: {changed} row-advance sync(s) -> VISIBILITY_REFRESH "
          f"({'applied' if apply else 'dry-run'})")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    for quest_id, pairs in EDITS.items():
        patch(quest_id, pairs, args.apply)


if __name__ == "__main__":
    main()
