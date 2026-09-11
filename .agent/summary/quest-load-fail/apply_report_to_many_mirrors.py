#!/usr/bin/env python3
"""Rebuild mirrored Elyos/Asmodian report-to-many quests 18805 and 28805."""

from apply_legacy_dialog_chains import document, node, npc_start, talk, write
from apply_legacy_dialog_chains import npc_complete as fixed_complete


def build(quest_id, metadata, start_npc, first_npc, second_npc, reward_npc):
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("reward", "REWARD", 1),
        node("complete", "COMPLETE", 0),
    )
    transitions = (
        npc_start(start_npc, "s0"),
        talk("s0", "s0", first_npc, "QUEST_SELECT", "SELECT2"),
        talk("s0", "s1", first_npc, "SETPRO1",
             actions=('<set-variable field="var0" value="1"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s1", "s1", second_npc, "QUEST_SELECT", "SELECT3"),
        talk("s1", "reward", second_npc, "SETPRO2",
             actions=('<set-variable field="var0" value="1"/>',),
             after=('<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>', "<close-dialog/>")),
        talk("reward", "reward", reward_npc, "QUEST_SELECT", "SELECT5"),
        fixed_complete(reward_npc, "reward", "0 1"),
    )
    write(quest_id, document(quest_id, metadata, nodes, transitions))


def build_18805():
    metadata = """  <metadata name="Going Thrifting" display-name-id="1136589" min-level="25" max-level="2147483647" category="SEEN_MARKER" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <repeat max-repeat-count="1" cooldown-seconds="0" daily="false" weekly="false"/>
    <rewards>
      <reward kind="EXP" id="0" amount="26868"/>
      <reward kind="ITEM" id="170190060" amount="1"/>
    </rewards>
  </metadata>"""
    build(18805, metadata, 830070, 830520, 730522, 830520)


def build_28805():
    metadata = """  <metadata name="Something Old, Something New" display-name-id="1136607" min-level="25" max-level="2147483647" category="SEEN_MARKER" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <repeat max-repeat-count="1" cooldown-seconds="0" daily="false" weekly="false"/>
    <rewards>
      <reward kind="EXP" id="0" amount="26868"/>
      <reward kind="ITEM" id="170190060" amount="1"/>
    </rewards>
  </metadata>"""
    build(28805, metadata, 830154, 830521, 730525, 830521)


if __name__ == "__main__":
    build_18805()
    build_28805()
