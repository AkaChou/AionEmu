#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"


def replace_block(content: str, tag: str, replacement: str, path: Path) -> str:
    updated, count = re.subn(rf"  <{tag}>.*?</{tag}>", replacement, content, count=1, flags=re.S)
    if count != 1:
        raise ValueError(f"{path}: expected one <{tag}> block")
    return updated


def write_quest(quest_id: int, nodes: str, transitions: str, progress: str | None = None) -> None:
    path = QUEST_DIR / f"{quest_id}.xml"
    content = path.read_text(encoding="utf-8")
    if progress is not None:
        content = replace_block(content, "progress", progress, path)
    content = replace_block(content, "nodes", nodes, path)
    content = replace_block(content, "transitions", transitions, path)
    path.write_text(content, encoding="utf-8")


def start_routes(start_npc: int, first_node: str = "s0") -> str:
    return f"""  <transitions>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="{first_node}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_ACCEPT_SIMPLE"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_REFUSE_SIMPLE"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def direct_talk_step(source: str, target: str, npc_id: int, page: QuestPage,
                     action: str, give_item: int | None) -> str:
    give = "" if give_item is None else f"        <give-item item-id=\"{give_item}\" count=\"1\"/>\n"
    return f"""    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="{page.select}"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{target}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>
      </event>
      <actions>
{give}      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def chained_talk_step(source: str, target: str, npc_id: int, first_page: str,
                      second_page: str, action: str) -> str:
    return f"""    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="{first_page}"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{second_page}"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="{second_page}"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{target}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>
      </event>
      <actions>
        <set-variable field="var0" value="{target[1:]}"/>
        <set-variable field="var1" value="0"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def kill_step(source: str, target: str, npc_ids: list[int], count: int,
              target_value: int) -> str:
    npc_text = " ".join(str(npc_id) for npc_id in npc_ids)
    threshold = count - 1
    return f"""    <transition source="{source}" target="{source}" priority="1">
      <event>
        <kill-npc npc-ids="{npc_text}"/>
      </event>
      <conditions>
        <variable-below field="var1" value="{threshold}"/>
      </conditions>
      <actions>
        <increment-variable field="var1" delta="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{target}" priority="0">
      <event>
        <kill-npc npc-ids="{npc_text}"/>
      </event>
      <conditions>
        <variable-at-least field="var1" value="{threshold}"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="{target_value}"/>
        <set-variable field="var1" value="0"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
      </after-commit>
    </transition>
"""


def reward_tail(start_npc: int, fixed_rewards: str) -> str:
    return f"""    <transition source="reward" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="DEFAULT_SUCCESS"/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{start_npc}" source="reward" target="complete" fixed-reward-indices="{fixed_rewards}" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>"""


class QuestPage:
    def __init__(self, select: str) -> None:
        self.select = select


def apply_15321() -> None:
    progress = """  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
    <bit-field name="var1" offset="6" width="6" min="0" max="59" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>"""
    nodes = """  <nodes>
    <node label="unaccepted" status="NONE"><var name="var0" value="0"/></node>
    <node label="s0" status="START"><var name="var0" value="0"/><var name="var1" value="0"/></node>
    <node label="s1" status="START"><var name="var0" value="1"/><var name="var1" value="0"/></node>
    <node label="s2" status="START"><var name="var0" value="2"/><var name="var1" value="0"/></node>
    <node label="s3" status="START"><var name="var0" value="3"/><var name="var1" value="0"/></node>
    <node label="s4" status="START"><var name="var0" value="4"/><var name="var1" value="0"/></node>
    <node label="s5" status="START"><var name="var0" value="5"/><var name="var1" value="0"/></node>
    <node label="s6" status="START"><var name="var0" value="6"/><var name="var1" value="0"/></node>
    <node label="s7" status="START"><var name="var0" value="7"/><var name="var1" value="0"/></node>
    <node label="s8" status="START"><var name="var0" value="8"/><var name="var1" value="0"/></node>
    <node label="s9" status="START"><var name="var0" value="9"/><var name="var1" value="0"/></node>
    <node label="s10" status="START"><var name="var0" value="10"/><var name="var1" value="0"/></node>
    <node label="s11" status="START"><var name="var0" value="11"/><var name="var1" value="0"/></node>
    <node label="reward" status="REWARD"><var name="var0" value="12"/><var name="var1" value="0"/></node>
    <node label="complete" status="COMPLETE"><var name="var0" value="0"/><var name="var1" value="0"/></node>
  </nodes>"""
    transitions = start_routes(805330)
    transitions += chained_talk_step("s0", "s1", 805332, "SELECT1", "SELECT1_1", "SETPRO1")
    transitions += kill_step("s1", "s2", [235829, 235831, 235851], 30, 2)
    transitions += chained_talk_step("s2", "s3", 805333, "SELECT3", "SELECT3_1", "SETPRO3")
    transitions += kill_step("s3", "s4", [235915, 235917, 235920], 30, 4)
    transitions += chained_talk_step("s4", "s5", 805334, "SELECT5", "SELECT5_1", "SETPRO5")
    transitions += kill_step("s5", "s6", list(range(236307, 236335)) + list(range(236530, 236558)), 10, 6)
    transitions += chained_talk_step("s6", "s7", 805335, "SELECT7", "SELECT7_1", "SETPRO7")
    transitions += kill_step("s7", "s8", [233909, 233911, 233912, 233914, 233916, 233955, 234159], 30, 8)
    transitions += chained_talk_step("s8", "s9", 805336, "SELECT9", "SELECT9_1", "SETPRO9")
    transitions += kill_step("s9", "s10", [234248, 234250, 234251, 234518], 30, 10)
    transitions += chained_talk_step("s10", "s11", 805337, "SELECT11", "SELECT11_1", "SETPRO11")
    transitions += kill_step("s11", "reward", [234269, 234271, 234272], 30, 12)
    transitions += reward_tail(805330, "0 1")
    write_quest(15321, nodes, transitions, progress)


def apply_daily_pair(quest_id: int, start_npc: int, npcs: list[int], items: list[int]) -> None:
    nodes = """  <nodes>
    <node label="unaccepted" status="NONE"><var name="var0" value="0"/></node>
    <node label="stage0" status="START"><var name="var0" value="0"/></node>
    <node label="stage1" status="START"><var name="var0" value="1"/></node>
    <node label="stage2" status="START"><var name="var0" value="2"/></node>
    <node label="stage3" status="START"><var name="var0" value="3"/></node>
    <node label="reward" status="REWARD"><var name="var0" value="4"/></node>
    <node label="complete" status="COMPLETE"><var name="var0" value="0"/></node>
  </nodes>"""
    pages = [("SELECT1", "SELECT2", "SETPRO1"), ("SELECT2", "SELECT3", "SETPRO2"),
             ("SELECT3", "SELECT4", "SETPRO3"), ("SELECT4", "SELECT5", "SET_SUCCEED")]
    transitions = start_routes(start_npc, "stage0")
    for index in range(4):
        first_page, action, target = pages[index][0], pages[index][2], f"stage{index + 1}" if index < 3 else "reward"
        item = items[index]
        page = QuestPage(first_page)
        transitions += direct_talk_step(f"stage{index}", target, npcs[index], page, action, item)
    transitions += reward_tail(start_npc, "0 1 2")
    write_quest(quest_id, nodes, transitions)
    path = QUEST_DIR / f"{quest_id}.xml"
    content = path.read_text(encoding="utf-8")
    content = re.sub(r"    <work-items>.*?    </work-items>\n?", "", content, count=1, flags=re.S)
    work_items = "\n".join(f'      <item id="{item}" count="1"/>' for item in items)
    content = content.replace(
        "    <rewards>",
        f"    <work-items>\n{work_items}\n    </work-items>\n    <rewards>",
        1,
    )
    path.write_text(content, encoding="utf-8")


def main() -> None:
    apply_15321()
    apply_daily_pair(15590, 806114, [806224, 806225, 806226, 806227],
                     [182215978, 182215979, 182215980, 182215981])
    apply_daily_pair(25590, 806116, [806228, 806229, 806230, 806231],
                     [182215982, 182215983, 182215984, 182215985])


if __name__ == "__main__":
    main()
