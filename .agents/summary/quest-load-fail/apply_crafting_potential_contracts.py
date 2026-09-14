#!/usr/bin/env python3
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"


@dataclass(frozen=True)
class SimpleCraftingQuest:
    quest_id: int
    start_npc: int
    stage_npc: int
    return_item: int
    first_work_items: tuple[int, ...]
    second_work_items: tuple[int, ...]


@dataclass(frozen=True)
class ConstructorQuest:
    quest_id: int
    start_npc: int
    stage_npc: int
    return_item: int
    first_work_item: int
    second_work_item: int
    first_cost: int
    second_cost: int
    first_recipe: int
    second_recipe: int


SIMPLE = (
    SimpleCraftingQuest(19014, 203790, 203791, 182206765, (152201806,), (152201807,)),
    SimpleCraftingQuest(19020, 203793, 203794, 182206766, (152201962,), (152201963,)),
    SimpleCraftingQuest(
        19026, 203792, 798013, 182206767, (152202049, 152020249), (152202050, 152020249)),
    SimpleCraftingQuest(
        19032, 203786, 203787, 182206768, (152202147, 152020248), (152202148, 152020248)),
    SimpleCraftingQuest(29014, 204106, 204107, 182207899, (152206808,), (152206809,)),
    SimpleCraftingQuest(29020, 204110, 204111, 182207900, (152206964,), (152206965,)),
    SimpleCraftingQuest(
        29026, 204108, 798060, 182207901, (152207051, 152029250), (152207052, 152029250)),
    SimpleCraftingQuest(
        29032, 204102, 204103, 182207902, (152207149, 152029249), (152207150, 152029249)),
)

CONSTRUCTORS = (
    ConstructorQuest(19057, 798450, 798451, 182206927, 152203543, 152203544, 167500, 223000,
		     155003543, 155003544),
    ConstructorQuest(29057, 798452, 798453, 182207980, 152208541, 152208542, 167500, 223000,
		     155008541, 155008542),
)


def work_items_block(items: tuple[int, ...]) -> str:
    rows = "\n".join(f'      <item id="{item_id}" count="1"/>' for item_id in items)
    return f"    <work-items>\n{rows}\n    </work-items>"


def replace_metadata_items(content: str, return_item: int, work_items: tuple[int, ...]) -> str:
    items = f'    <items>\n      <item id="{return_item}" count="1"/>\n    </items>'
    content = re.sub(r"    <items>.*?    </items>", items, content, count=1, flags=re.S)
    work = work_items_block(work_items)
    if "    <work-items>" in content:
        return re.sub(r"    <work-items>.*?    </work-items>", work, content, count=1, flags=re.S)
    return content.replace("    </items>", f"    </items>\n{work}", 1)


def start_routes(start_npc: int) -> str:
    return f"""    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT_NONE"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="ASK_QUEST_ACCEPT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_ASK_QUEST_ACCEPT_WINDOW"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_ACCEPT_1"/>
      </event>
      <conditions>
        <start-eligible/>
      </conditions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="QUEST_ACCEPT_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="QUEST_REFUSE_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="QUEST_REFUSE_1"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def simple_transitions(quest: SimpleCraftingQuest) -> str:
    first_give = "\n".join(
        f'        <give-item item-id="{item_id}" count="1"/>' for item_id in quest.first_work_items)
    second_give = "\n".join(
        f'        <give-item item-id="{item_id}" count="1"/>' for item_id in quest.second_work_items)
    return f"""  <transitions>
{start_routes(quest.start_npc)}
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SELECT1_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_2"/>
      </after-commit>
    </transition>
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SELECT1_3"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_3"/>
      </after-commit>
    </transition>
    <transition source="started0" target="started1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SETPRO10"/>
      </event>
      <actions>
{first_give}
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started0" target="started1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SETPRO20"/>
      </event>
      <actions>
{second_give}
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="QUEST_SELECT"/>
      </event>
      <conditions>
        <has-item item-id="{quest.return_item}" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="{quest.return_item}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started1" target="started1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="QUEST_SELECT"/>
      </event>
      <conditions>
        <has-item item-id="{quest.return_item}" count="1" expected="false"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="CHECK_USER_ITEM_FAIL"/>
      </after-commit>
    </transition>
    <transition source="started1" target="started1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <npc-complete npc-id="{quest.start_npc}" source="reward" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="CHECK_USER_HAS_QUEST_ITEM"/>
    </npc-complete>
  </transitions>"""


def constructor_retry_routes(quest: ConstructorQuest, source: str) -> str:
    return f"""    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SELECT1_2"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_2"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SELECT1_3"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_3"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="started1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SETPRO10"/>
      </event>
      <conditions>
        <currency-at-least kind="KINAH" amount="{quest.first_cost}"/>
      </conditions>
      <actions>
        <decrease-currency kind="KINAH" amount="{quest.first_cost}"/>
        <give-item item-id="{quest.first_work_item}" count="1"/>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SETPRO10"/>
      </event>
      <conditions>
        <currency-below kind="KINAH" amount="{quest.first_cost}"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT10_4_4"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="started1" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SETPRO20"/>
      </event>
      <conditions>
        <currency-at-least kind="KINAH" amount="{quest.second_cost}"/>
      </conditions>
      <actions>
        <decrease-currency kind="KINAH" amount="{quest.second_cost}"/>
        <give-item item-id="{quest.second_work_item}" count="1"/>
        <set-variable field="var0" value="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="SETPRO20"/>
      </event>
      <conditions>
        <currency-below kind="KINAH" amount="{quest.second_cost}"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT10_4_4"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def constructor_report_routes(quest: ConstructorQuest, source: str) -> str:
    return f"""    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="reward" priority="0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{quest.return_item}" count="1"/>
      </conditions>
      <actions>
        <remove-item item-id="{quest.return_item}" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}" priority="1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <recipe-known recipe-id="{quest.first_recipe}" expected="true"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT6"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}" priority="2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <recipe-known recipe-id="{quest.second_recipe}" expected="true"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT6"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}" priority="3">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <recipe-known recipe-id="{quest.first_recipe}" expected="false"/>
        <recipe-known recipe-id="{quest.second_recipe}" expected="false"/>
        <has-item item-id="{quest.first_work_item}" count="1"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT7"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}" priority="4">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <recipe-known recipe-id="{quest.first_recipe}" expected="false"/>
        <recipe-known recipe-id="{quest.second_recipe}" expected="false"/>
        <has-item item-id="{quest.second_work_item}" count="1"/>
      </conditions>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT7"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="started2" priority="5">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="CHECK_USER_HAS_QUEST_ITEM"/>
      </event>
      <conditions>
        <has-item item-id="{quest.return_item}" count="1" expected="false"/>
        <recipe-known recipe-id="{quest.first_recipe}" expected="false"/>
        <recipe-known recipe-id="{quest.second_recipe}" expected="false"/>
        <has-item item-id="{quest.first_work_item}" count="1" expected="false"/>
        <has-item item-id="{quest.second_work_item}" count="1" expected="false"/>
      </conditions>
      <actions>
        <set-variable field="var0" value="2"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="PACKET_ONLY"/>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT8"/>
      </after-commit>
    </transition>
    <transition source="{source}" target="{source}">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.start_npc}" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
"""


def constructor_transitions(quest: ConstructorQuest) -> str:
    return f"""  <transitions>
{start_routes(quest.start_npc)}
    <transition source="started0" target="started0">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1"/>
      </after-commit>
    </transition>
{constructor_retry_routes(quest, "started0")}
    <transition source="started2" target="started2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="{quest.stage_npc}" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT10"/>
      </after-commit>
    </transition>
{constructor_retry_routes(quest, "started2")}
{constructor_report_routes(quest, "started0")}
{constructor_report_routes(quest, "started1")}
{constructor_report_routes(quest, "started2")}
    <npc-complete npc-id="{quest.start_npc}" source="reward" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT"/>
    </npc-complete>
  </transitions>"""


def node_block(simple: bool) -> str:
    if simple:
        return """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started0" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="started1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>"""
    return """  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started0" status="START">
      <var name="var0" value="0"/>
    </node>
    <node label="started1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="started2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>"""


def write_quest(path: Path, nodes: str, transitions: str) -> None:
    content = path.read_text(encoding="utf-8")
    content, nodes_count = re.subn(r"  <nodes>.*?  </nodes>", nodes, content, count=1, flags=re.S)
    if nodes_count != 1:
        raise ValueError(f"{path}: expected one nodes block")
    content, transitions_count = re.subn(
        r"  <transitions>.*?</transitions>", transitions, content, count=1, flags=re.S)
    if transitions_count != 1:
        raise ValueError(f"{path}: expected one transitions block")
    path.write_text(content, encoding="utf-8")


def main() -> None:
    for quest in SIMPLE:
        path = QUEST_DIR / f"{quest.quest_id}.xml"
        content = path.read_text(encoding="utf-8")
        content = replace_metadata_items(
            content, quest.return_item, quest.first_work_items + quest.second_work_items)
        path.write_text(content, encoding="utf-8")
        write_quest(path, node_block(True), simple_transitions(quest))
    for quest in CONSTRUCTORS:
        path = QUEST_DIR / f"{quest.quest_id}.xml"
        content = path.read_text(encoding="utf-8")
        content = replace_metadata_items(
            content, quest.return_item, (quest.first_work_item, quest.second_work_item))
        path.write_text(content, encoding="utf-8")
        write_quest(path, node_block(False), constructor_transitions(quest))


if __name__ == "__main__":
    main()
