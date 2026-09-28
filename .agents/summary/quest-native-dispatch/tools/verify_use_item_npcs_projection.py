#!/usr/bin/env python3
"""复核 quest_use_item_npcs 旧登记表与 npc_template 投影的集合等价。

Verify set equality between the retired quest_use_item_npcs registry and the
ai="quest_use_item" ids in the NPC template shards the quest driver loads.
"""
from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

NPC_FILES = [
    "npc_template_200000_216188.xml", "npc_template_216189_235748.xml",
    "npc_template_235749_247606.xml", "npc_template_247607_270057.xml",
    "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
    "npc_template_800031_834289.xml", "npc_template_834290_885645.xml",
]
TAG = re.compile(r"<npc_template\b[^>]*>")
NPC_ID = re.compile(r'npc_id="(\d+)"')
AI = re.compile(r'ai="([^"]*)"')


def main() -> int:
    snapshot = Path(sys.argv[1])
    npc_dir = Path(sys.argv[2])
    old = {int(line) for line in snapshot.read_text(encoding="utf-8").splitlines()
           if line.strip() and not line.startswith("#")}
    new = set()
    for name in NPC_FILES:
        text = (npc_dir / name).read_text(encoding="utf-8")
        for tag in TAG.findall(text):
            ai = AI.search(tag)
            npc_id = NPC_ID.search(tag)
            if npc_id and ai and ai.group(1) == "quest_use_item":
                new.add(int(npc_id.group(1)))
    missing = sorted(old - new)
    extra = sorted(new - old)
    canon = "\n".join(str(i) for i in sorted(old)) + "\n"
    print(f"snapshot={snapshot} ids={len(old)} sha256={hashlib.sha256(snapshot.read_bytes()).hexdigest()}")
    print(f"projection_ids={len(new)}")
    print(f"missing_in_projection={len(missing)} {missing[:10]}")
    print(f"extra_in_projection={len(extra)} {extra[:10]}")
    print(f"canonical_ids_sha256={hashlib.sha256(canon.encode()).hexdigest()}")
    print("VERDICT=" + ("EQUIVALENT" if not missing and not extra else "DIFFERENT"))
    return 0 if not missing and not extra else 1


if __name__ == "__main__":
    sys.exit(main())
