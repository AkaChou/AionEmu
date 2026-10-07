#!/usr/bin/env python3
"""1929.xml：把发放流程改回「步数保持 98」的真端形状（不再推进 95），用 lore 道具持有量门控。

实机证据链（DIAGNOSIS 第 5–8 轮）：
- 步数 98（过场动画后）⇒ 客户端烙印凹槽显示开启；
- 步数推进到 95（无论有没有关窗包、无论多少次重发槽位数）⇒ 凹槽关闭且会话内不可恢复；
- 用户口径：「以前 xml 的时候不会有这个问题」——退役 XML/真端在发放时**不推进步数**（98 → 装备后才到 96）。

因此：
1. 删除 granted95 节点与其全部路由（节点本就是本次修复自造的）；
2. 22 条发放分支回到 spawned98 自环，去掉 `<set-variable field="step" value="95"/>`；
   交付分支 after-commit = sync + 打开烙印窗口；已持有分支 = 只开窗口；
3. 入口按 lore 道具门控（priority=0）：已持有 ⇒ 直接出示装备引导页 SELECT5_2；
   未持有 ⇒ 原有剧情页（fallback 路由标 priority=1）；
4. 引导页按钮（SELECT5_3）⇒ 打开烙印窗口（页 1，绑定对话对象）。

在仓库根目录运行：
  python3 .agents/summary/quest-1929-stigma-dialog/restore_step98_grant_flow.py
"""
from pathlib import Path
import re
import sys

REPO = Path(__file__).resolve().parents[3]
XML = REPO / "src/main/resources/aion/data/static_data/quest/definitions/quests/1929.xml"

STONE_BY_CLASS = {
    "GLADIATOR": 140000003, "TEMPLAR": 140000003, "ASSASSIN": 140000003,
    "RANGER": 140000003, "CHANTER": 140000003,
    "SORCERER": 140000002, "SPIRIT_MASTER": 140000002, "CLERIC": 140000002,
    "GUNSLINGER": 140000004, "SONGWEAVER": 140000004, "AETHERTECH": 140000004,
}

TEMPLATE = """    <transition source="spawned98" target="spawned98"{priority}>
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="{action}"/>
      </event>
{conditions}      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="{page}"/>
      </after-commit>
    </transition>
"""

GRANT_COMMENT = """    <!-- 发放分支（真端形状，步数保持 98）：
         客户端的烙印凹槽展开态跟随教学步数——步数 98 时开启，一旦推进到 95（无论是否关窗、
         关窗前后、之后重发多少次槽位数）就关闭且会话内不可恢复，只有重登才会由登录包序重建。
         真端/退役 XML 在发放时同样不推进步数（98 → 装备后才到 96），故此处分发只给道具、开烙印窗口。
         首次对话因此只有一个「结束对话」；再次对话由入口的持有量门控直接出示装备引导页。
         Grant branches (retail shape, step stays 98): the client's stigma-slot expansion follows the
         tutorial step - open at 98, and once the step advances to 95 it closes and cannot be recovered
         in-session (no packet and no re-announce helps; only a re-login rebuilds it via the enter-world
         packet sequence). Retail/retired XML does not advance the step on the grant either (98 -> 96 only
         after the equip), so these branches hand over the item and open the stigma window. The first
         conversation thus keeps a single end-dialog; re-talks are routed by the held-item entry gate
         straight to the equip-guide page. -->
"""


def main() -> int:
    text = XML.read_text(encoding="utf-8")
    original = text

    # 1) drop the granted95 node and every route that leaves/enters it
    text, node_count = re.subn(r'\n    <node label="granted95"[^>]*>.*?</node>', '', text, flags=re.S)
    text, route_count = re.subn(r'\n    <transition[^>]*source="granted95".*?</transition>', '', text,
                                flags=re.S)

    # 2) grant branches back to the spawned98 self-loop, step untouched
    def fix_grant(match: "re.Match[str]") -> str:
        block = match.group(0).replace('target="granted95"', 'target="spawned98"')
        block = re.sub(r'\n\s*<set-variable field="step" value="95"/>', '', block)
        if 'expected="false"' not in block:
            # 已持有分支没有状态变化：同步包本就是多余的（不变量：无状态变化不发同步）
            block = re.sub(r'\n\s*<sync-quest-state mode="PACKET_ONLY"/>', '', block)
        return block

    text, grant_count = re.subn(r'    <transition source="spawned98" target="granted95">.*?</transition>',
                                fix_grant, text, flags=re.S)

    # 3) entry gate: the held-item route takes priority 0, the story fallback priority 1
    pattern = re.compile(
        r'    <transition source="spawned98" target="spawned98">(?=\n'
        r'      <event>\n'
        r'        <dialog type="TALK_TO_NPC" npc-id="205111" action="QUEST_SELECT"/>)')
    text, hits = pattern.subn(
        lambda m: m.group(0).replace(
            '<transition source="spawned98" target="spawned98">',
            '<transition source="spawned98" target="spawned98" priority="1">'), text)
    if hits != 1:
        print(f"expected exactly one unconditional QUEST_SELECT route, found {hits}")
        return 1

    insertions = [TEMPLATE.format(
        priority=' priority="1"', action="USE_OBJECT",
        conditions="", page="SELECT5_1")]
    for action in ("QUEST_SELECT", "USE_OBJECT"):
        for player_class, stone in STONE_BY_CLASS.items():
            insertions.append(TEMPLATE.format(
                priority=' priority="0"', action=action,
                conditions=("      <conditions>\n"
                            f'        <advanced-class-is class="{player_class}"/>\n'
                            f'        <has-item item-id="{stone}" count="1"/>\n'
                            "      </conditions>\n"),
                page="SELECT5_2"))
    # guide-page button opens the stigma window (bound to the dialog peer by the dialog port)
    insertions.append("""    <transition source="spawned98" target="spawned98">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205111" action="SELECT5_3"/>
      </event>
      <after-commit>
        <show-dialog-window dialog-id="1"/>
      </after-commit>
    </transition>
""")

    anchor = "    <transition source=\"spawned98\" target=\"spawned98\" priority=\"1\">"
    if anchor not in text:
        print("entry-gate anchor missing; aborting")
        return 1
    text = text.replace(anchor, GRANT_COMMENT + "".join(insertions) + anchor, 1)

    if text == original:
        print("nothing changed")
        return 1
    XML.write_text(text, encoding="utf-8")
    print(f"granted95 node removed: {node_count}; granted95 routes removed: {route_count}; "
          f"grant branches retargeted: {grant_count}; entry routes added: {len(insertions)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
