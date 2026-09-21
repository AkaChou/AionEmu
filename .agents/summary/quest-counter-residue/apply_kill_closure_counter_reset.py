#!/usr/bin/env python3
"""为 16821/26821 的 35 杀收口路线补清零动作，并追加 s3/s4/s5/reward 上线自愈边。

Rewrite the 35-kill closure routes of 16821/26821 so they reset the stage-local
counter, and append login self-heal edges for s3/s4/s5/reward (QE-044).
"""

from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"

CLOSURE_OPEN = '    <transition source="s2" target="s3" priority="0">\n'
HEAL_ANCHOR = '    <transition source="s3" target="s4">\n'
INCREMENT = '        <increment-variable field="var1" delta="1"/>\n'
RESET = '        <set-variable field="var1" value="0"/>\n'
COMMENT = (
    '        <!-- 击杀收口必须清空本阶段局部计数，否则 (35&lt;&lt;6)|3 会污染打包步数，\n'
    '             s3/s4/s5/reward 的客户端步骤门控与 boss SECTION_1&lt;1 计数都不再成立（QE-044）。\n'
    '             The kill closure must clear the stage-local counter; otherwise (35&lt;&lt;6)|3\n'
    '             pollutes the packed step and later client step gates stop working. -->\n'
)


def heal_block(quest_id: int) -> str:
    stages = [("s3", 3), ("s4", 4), ("s5", 5), ("reward", 6)]
    comment = (
        "    <!-- 上线自愈：旧存档把 var1=35 残留带进 s3/s4/s5/reward，打包步数被高位污染后\n"
        "         客户端步骤门控（含 region/movie/领奖对白）失效；登录时把残留计数清回 0（QE-044）。\n"
        "         Login self-heal: legacy saves carry var1=35 into s3/s4/s5/reward; clear it on login. -->\n"
    )
    blocks = []
    for label, step in stages:
        blocks.append(
            f'    <transition source="{label}" target="{label}">\n'
            '      <event>\n'
            '        <enter-world/>\n'
            '      </event>\n'
            '      <conditions>\n'
            f'        <variable-is field="var0" value="{step}"/>\n'
            '        <variable-at-least field="var1" value="1"/>\n'
            '      </conditions>\n'
            '      <actions>\n'
            '        <set-variable field="var1" value="0"/>\n'
            '      </actions>\n'
            '      <after-commit>\n'
            '        <sync-quest-state mode="PACKET_ONLY"/>\n'
            '      </after-commit>\n'
            '    </transition>\n'
        )
    return comment + "".join(blocks)


def rewrite(path: Path) -> tuple[int, int]:
    lines = path.read_text(encoding="utf-8").splitlines(keepends=True)
    out: list[str] = []
    in_closure = False
    closures = 0
    heals = 0
    for line in lines:
        if line.startswith('    <transition '):
            in_closure = line == CLOSURE_OPEN
            if line == HEAL_ANCHOR:
                out.append(heal_block(int(path.stem)))
                heals += 1
            elif in_closure:
                closures += 1
        if in_closure and line == INCREMENT:
            if closures == 1:
                out.append(COMMENT)
            out.append(RESET)
            continue
        out.append(line)
    if heals != 1:
        raise SystemExit(f"{path.name}: expected one heal anchor, found {heals}")
    path.write_text("".join(out), encoding="utf-8")
    return closures, heals


def main() -> int:
    for quest_id in (16821, 26821):
        path = QUEST_DIR / f"{quest_id}.xml"
        closures, heals = rewrite(path)
        print(f"{path.name}: closures={closures} heal_blocks={heals}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
