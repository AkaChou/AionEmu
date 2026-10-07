#!/usr/bin/env python3
"""1929「暗黑碎片·烙印之石」发放契约改写：只发一次 + 发放后展示装备引导页。

证据（本主题目录）：
- 客户端 QUEST_Q1929.html：select5_1_1 按钮 = HACTION_SELECT5_2（文案「结束对话。」）→ select5_2 页
  （「请您打开窗口，装备我给您的结晶」= 装备引导页）；select5_2 按钮 = HACTION_SELECT5_3（「结束对话。」）。
- 孪生任务 2900（Asmodian，同一 stigma 家族）：legacy SELECT_ACTION_3058 分支 = 发放 + step 96→99 +
  展示教学页 3058；var 99 时同动作重发教学页；teaching 页按钮 → DMW(1) 开烙印窗（现行 2900.xml 同形）。
- 旧 AionEmu giveQuestItem：existentItemCount >= itemCount 时不发放（lore 语义防重复）。
- 用户实机 2026-10-07：每次对话重复发放 1400000xx 且整链重放（旧写法 SELECT5_3 发放且不推进 step）。

改写内容：
1. spawned98 的 11 条 SELECT5_3 发放分支 → 11 + 11 条 SELECT5_2 分支：
   未持有 ⇒ give-item + step=95；已持有 ⇒ 仅 step=95；after-commit 一律 sync(PACKET_ONLY) + SHOW_QUEST_PAGE SELECT5_2。
2. 新增 granted95 节点（step=95）及其路由：QUEST_SELECT / USE_OBJECT / SELECT5_2 → 教学页，
   SELECT5_3 → 烙印之窗（dialog 1），equip-item ×4 → equipped96。
3. 删除 spawned98 上被分支覆盖的无条件 SELECT5_2 页导航；spawned98 保留 equip-item ×4（旧缺陷残留
   状态下先装备者仍可推进到 equipped96，避免死锁）。
"""

from __future__ import annotations

import sys
from pathlib import Path

REPO = next(parent for parent in Path(__file__).resolve().parents if (parent / "pom.xml").is_file())
XML = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests/1929.xml"

STONES = (
    ("GLADIATOR", 140000003),
    ("TEMPLAR", 140000003),
    ("ASSASSIN", 140000003),
    ("RANGER", 140000003),
    ("SORCERER", 140000002),
    ("SPIRIT_MASTER", 140000002),
    ("CLERIC", 140000002),
    ("CHANTER", 140000003),
    ("GUNSLINGER", 140000004),
    ("SONGWEAVER", 140000004),
    ("AETHERTECH", 140000004),
)

NODE_ANCHOR = '''    <node label="spawned98" status="START">
      <var name="step" value="98"/>
    </node>
'''

GRANT_BLOCK_HEAD = '''    <transition source="spawned98" target="spawned98">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="SELECT5_3"/>
      </event>
'''

EQUIP_HEAD = '''    <transition source="spawned98" target="equipped96">'''

OBSOLETE_NAV = '''    <transition source="spawned98" target="spawned98">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="SELECT5_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_2"/>
      </after-commit>
    </transition>
'''

GRANT_COMMENT = '''    <!-- 发放 + 推进：客户端 select5_1_1 的「结束对话。」= HACTION_SELECT5_2，服务端在此发放烙印之石
         并进入 granted95（教学页 select5_2 即该按钮的客户端目标页）；已持有 lore 道具时只推进不重发。
         Grant and advance: the client's select5_1_1 button (HACTION_SELECT5_2) hands over the stigma and
         enters granted95 (the tutorial page select5_2 is that button's client-side target); a held lore
         item advances the step without a second grant. -->
'''


def grant_branch(player_class: str, stone: int) -> str:
    return f'''    <transition source="spawned98" target="granted95">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="SELECT5_2"/>
      </event>
      <conditions>
        <advanced-class-is class="{player_class}"/>
        <has-item item-id="{stone}" count="1" expected="false"/>
      </conditions>
      <actions>
        <give-item item-id="{stone}" count="1"/>
        <set-variable field="step" value="95"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_2"/>
      </after-commit>
    </transition>
'''


def held_branch(player_class: str, stone: int) -> str:
    return f'''    <transition source="spawned98" target="granted95">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="SELECT5_2"/>
      </event>
      <conditions>
        <advanced-class-is class="{player_class}"/>
        <has-item item-id="{stone}" count="1"/>
      </conditions>
      <actions>
        <set-variable field="step" value="95"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT5_2"/>
      </after-commit>
    </transition>
'''


NODE_INSERT = '''    <node label="granted95" status="START">
      <var name="step" value="95"/>
    </node>
'''

GRANTED_COMMENT = '''    <!-- granted95：烙印已发放但未装备。再次对话由教学页（select5_2）承接，绝不再发放；
         教学页按钮 HACTION_SELECT5_3 开烙印窗口，装备事件推进到 equipped96。
         granted95 is the granted-but-unequipped state: talking again lands on the tutorial page
         (select5_2) and never grants another stigma; the tutorial button HACTION_SELECT5_3 opens the
         stigma window and the equip event advances to equipped96. -->
'''


def page_branch(action: str, page: str) -> str:
    return f'''    <transition source="granted95" target="granted95">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="{action}"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>
      </after-commit>
    </transition>
'''


def stigma_window_branch() -> str:
    return '''    <transition source="granted95" target="granted95">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="SELECT5_3"/>
      </event>
      <after-commit>
        <show-dialog-window dialog-id="1"/>
      </after-commit>
    </transition>
'''


def equip_branch(stone: int) -> str:
    return f'''    <transition source="granted95" target="equipped96">
      <event>
        <equip-item item-id="{stone}"/>
      </event>
      <actions>
        <set-variable field="step" value="96"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
'''


def main() -> int:
    text = XML.read_text(encoding="utf-8")
    if "\r" in text:
        print("unexpected CRLF line endings", file=sys.stderr)
        return 1
    if "granted95" in text:
        print("already rewritten (granted95 present)", file=sys.stderr)
        return 1

    # 1) node declaration
    assert text.count(NODE_ANCHOR) == 1, "spawned98 node anchor not unique"
    text = text.replace(NODE_ANCHOR, NODE_ANCHOR + NODE_INSERT)

    # 2) 11 SELECT5_3 grant branches -> 11 grant + 11 held branches on SELECT5_2
    start = text.index(GRANT_BLOCK_HEAD)
    end = text.index(EQUIP_HEAD)
    assert text.count(GRANT_BLOCK_HEAD) == 11, "expected 11 SELECT5_3 branches sharing the head"
    assert text[start:end].count('action="SELECT5_3"') == 11, "expected 11 SELECT5_3 branches"
    replacement = GRANT_COMMENT + "".join(grant_branch(c, s) for c, s in STONES) + "".join(
        held_branch(c, s) for c, s in STONES)
    text = text[:start] + replacement + text[end:]

    # 3) granted95 routes (inserted before the retained equip branches)
    granted = (GRANTED_COMMENT + page_branch("QUEST_SELECT", "SELECT5_2")
               + page_branch("USE_OBJECT", "SELECT5_2") + page_branch("SELECT5_2", "SELECT5_2")
               + stigma_window_branch()
               + "".join(equip_branch(stone) for stone in (140000001, 140000002, 140000003, 140000004)))
    anchor = text.index(EQUIP_HEAD)
    text = text[:anchor] + granted + text[anchor:]

    # 4) drop the superseded unconditional SELECT5_2 navigation
    assert text.count(OBSOLETE_NAV) == 1, "obsolete SELECT5_2 navigation not unique"
    text = text.replace(OBSOLETE_NAV, "")

    XML.write_text(text, encoding="utf-8")
    print(f"rewritten {XML.relative_to(REPO)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
