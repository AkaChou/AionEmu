#!/usr/bin/env python3
"""为已 sweep 的任务补一条「领奖对话框自愈」路线。

ENTER_WORLD 迁移只在登录/换图时触发；玩家若跨部署保持在线（或热更 XML），
REWARD 存档的 var0 仍旧，reward 源节点的投影匹配会失败 -> 点 NPC 无响应。
本脚本复制任务自身的 reward->reward 对话框路线，追加一条 source-less 修复：
status REWARD + var0 < 报告行 -> 写报告行 + 原路线的 after-commit 响应。
"""
from __future__ import annotations
import argparse, re
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
QUEST_DIR = REPO / "src/main/resources/aion/data/static_data/quest_definition/quests"
TSV = REPO / "src/test/resources/quest/quest-section0-report-row-contract.tsv"
MARKER = "<!-- SECTION_0 dialog repair: stale REWARD saves on first NPC interaction -->"
TRANSITION = re.compile(r"<transition\b.*?</transition>", re.S)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    rows = []
    for line in TSV.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or line.startswith("quest_id"):
            continue
        quest, stage, report = (int(x) for x in line.split("\t"))
        rows.append((quest, stage, report))
    patched, skipped = [], []
    for quest, stage, report in rows:
        path = QUEST_DIR / f"{quest}.xml"
        text = path.read_text(encoding="utf-8")
        if MARKER in text or "SECTION_0 migration repair" not in text:
            skipped.append((quest, "no sweep migration route"))
            continue
        source = None
        for match in TRANSITION.finditer(text):
            block = match.group(0)
            head = re.match(r"<transition\b[^>]*>", block).group(0)
            if 'source="reward"' not in head or 'target="reward"' not in head:
                continue
            dialog = re.search(r'<dialog type="TALK_TO_NPC" npc-id="(\d+)" action="(\w+)"/>', block)
            after = re.search(r"<after-commit>.*?</after-commit>", block, re.S)
            if dialog and after:
                source = (dialog.group(1), dialog.group(2), after.group(0))
                break
        if source is None:
            skipped.append((quest, "no copyable reward dialog route"))
            continue
        npc, action, after = source
        after = after.replace('<sync-quest-state mode="PACKET_ONLY"/>',
                              '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>')
        if '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>' not in after:
            after = after.replace("      <after-commit>\n",
                                  '      <after-commit>\n        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>\n', 1)
        repair = (
            f"    {MARKER}\n"
            '    <transition target="reward">\n'
            '      <event>\n'
            f'        <dialog type="TALK_TO_NPC" npc-id="{npc}" action="{action}"/>\n'
            '      </event>\n'
            '      <conditions>\n'
            '        <status-is status="REWARD"/>\n'
            f'        <variable-below field="var0" value="{report}"/>\n'
            '      </conditions>\n'
            '      <actions>\n'
            f'        <set-variable field="var0" value="{report}"/>\n'
            '      </actions>\n'
            + after.replace("\n", "\n") + "\n"
            '    </transition>\n'
        )
        opening = re.search(r"<transitions[^>]*>\s*\n", text)
        updated = text[:opening.end()] + repair + text[opening.end():]
        if args.apply:
            path.write_text(updated, encoding="utf-8")
        patched.append(quest)
    print(f"[{'APPLIED' if args.apply else 'DRY-RUN'}] dialog repair patched: {len(patched)}")
    print(f"[{'APPLIED' if args.apply else 'DRY-RUN'}] skipped: {len(skipped)}")
    for quest, reason in skipped[:30]:
        print(f"  SKIP {quest}: {reason}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
