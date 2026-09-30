#!/usr/bin/env python3
"""回滚 Phase 4-2 对「本服可达目标」的误删，并产出例外台账。

三类判定（任一成立即回滚，且必须写入台账）：
  A. KILL_DECLARATION      —— 任务 XML 的 <kills> 声明了该 npc，收窄后声明与击杀转移不一致；
  B. SERVER_SPAWN_VARIANT  —— 本服 handler/legacy quest_data 使用的是该 id（真端表给的是同 boss 的另一个模板）；
  C. RETAIL_TARGET_UNREACHABLE —— 收窄后保留的 id 在本服没有任何 spawn/handler 可达，任务将无法完成。

用法：python3 restore_server_targets.py [--apply]
"""
from __future__ import annotations

import argparse
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
PROD = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests"
OUT = REPO / "src/test/resources/quest/quest-simple-hunt-server-target-exceptions.tsv"

RESTORE = [
    # quest, field, npc_id, reason, evidence
    (1179, "var0", 211099, "RETAIL_TARGET_UNREACHABLE", "no-reachable-spawn:210343"),
    (1179, "var0", 211135, "RETAIL_TARGET_UNREACHABLE", "no-reachable-spawn:210343"),
    (1179, "var0", 211153, "RETAIL_TARGET_UNREACHABLE", "no-reachable-spawn:210343"),
    (1179, "var0", 211960, "RETAIL_TARGET_UNREACHABLE", "no-reachable-spawn:210343"),
    (1179, "var0", 211961, "RETAIL_TARGET_UNREACHABLE", "spawn:210050000_Inggison.xml"),
    (1470, "var0", 214621, "SERVER_SPAWN_VARIANT",
     "src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FireTempleInstance.java"),
    (24275, "var0", 214621, "SERVER_SPAWN_VARIANT",
     "src/main/java/com/aionemu/gameserver/instance/handlers/scripts/FireTempleInstance.java"),
    (1497, "var0", 211830, "KILL_DECLARATION", "quest <kills> sequence 1"),
    (1497, "var0", 211831, "KILL_DECLARATION", "quest <kills> sequence 1"),
    (1497, "var1", 212070, "KILL_DECLARATION", "quest <kills> sequence 2"),
    (1497, "var1", 212071, "KILL_DECLARATION", "quest <kills> sequence 2"),
]


def patch_dimension(text: str, field: str, ids: set[int]) -> tuple[str, bool]:
    pattern = re.compile(r'<dimension\b[^>]*field="%s"[^>]*npc-ids="([^"]*)"[^>]*/>' % re.escape(field))

    def repl(match: re.Match[str]) -> str:
        tag, current = match.group(0), match.group(1)
        merged = sorted({int(x) for x in current.split()} | ids)
        return tag.replace(f'npc-ids="{current}"', f'npc-ids="{" ".join(str(i) for i in merged)}"')

    return pattern.subn(repl, text, count=1)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    by_quest: dict[int, dict[str, set[int]]] = {}
    for quest, field, npc, _reason, _evidence in RESTORE:
        by_quest.setdefault(quest, {}).setdefault(field, set()).add(npc)
    for quest, fields in sorted(by_quest.items()):
        path = PROD / f"{quest}.xml"
        text = path.read_text(encoding="utf-8")
        for field, ids in fields.items():
            text, count = patch_dimension(text, field, ids)
            if count != 1:
                raise SystemExit(f"failed to patch {path} {field}")
            print(f"  {quest}.xml {field} += {' '.join(str(i) for i in sorted(ids))}")
        if args.apply:
            path.write_text(text, encoding="utf-8")
    if args.apply:
        with OUT.open("w", encoding="utf-8") as fh:
            fh.write("# 服务端可达击杀目标例外台账（真端表口径之外、但本服运行期必须接受的目标）\n")
            fh.write("# 生成/复核：.agents/summary/scriptdll-quest-driver/restore_server_targets.py\n")
            fh.write("# reason: KILL_DECLARATION | SERVER_SPAWN_VARIANT | RETAIL_TARGET_UNREACHABLE\n")
            fh.write("quest_id\tfield\tnpc_id\treason\tevidence\n")
            for quest, field, npc, reason, evidence in RESTORE:
                fh.write(f"{quest}\t{field}\t{npc}\t{reason}\t{evidence}\n")
    print("dry-run" if not args.apply else "applied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
