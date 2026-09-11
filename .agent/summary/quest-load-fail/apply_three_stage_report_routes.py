#!/usr/bin/env python3
"""Rebuild the client-paged three-stage report quests 15550 and 25550."""

from apply_legacy_dialog_chains import (
    document,
    node,
    npc_complete,
    npc_start,
    talk,
    write,
)


def build(quest_id, metadata, start_npc, first_npc, second_npc, end_npc):
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("reward", "REWARD", 1),
        node("complete", "COMPLETE", 0),
    )
    transitions = (
        npc_start(start_npc, "s0", page="SELECT_NONE"),
        talk("s0", "s0", first_npc, "QUEST_SELECT", "SELECT2"),
        talk("s0", "s0", first_npc, "SELECT2_1", "SELECT2_1"),
        talk("s0", "s0", first_npc, "SELECT2_1_1", "SELECT2_1_1"),
        talk("s0", "s1", first_npc, "SETPRO2",
             actions=('<set-variable field="var0" value="1"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s1", "s1", second_npc, "QUEST_SELECT", "SELECT3"),
        talk("s1", "s1", second_npc, "SELECT3_1", "SELECT3_1"),
        talk("s1", "reward", second_npc, "SELECT_QUEST_REWARD",
             actions=('<set-variable field="var0" value="1"/>',),
             after=(
                 '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                 '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>',
             )),
        talk("reward", "reward", end_npc, "QUEST_SELECT", after=(
            '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
            '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>',
        )),
        npc_complete(end_npc, "reward", "0 1 2"),
    )
    write(quest_id, document(quest_id, metadata, nodes, transitions))


if __name__ == "__main__":
    build(15550, """  <metadata name="Iluma Field Guide" display-name-id="1802117" min-level="66" max-level="2147483647" category="SEEN_MARKER">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="77580"/>
      <reward kind="EXP" id="0" amount="6631200"/>
      <reward kind="ITEM" id="188055318" amount="1"/>
    </rewards>
  </metadata>""", 806114, 806113, 806134, 806089)
    build(25550, """  <metadata name="A Norsvold Story" display-name-id="1802188" min-level="66" max-level="2147483647" category="SEEN_MARKER">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="77580"/>
      <reward kind="EXP" id="0" amount="6631200"/>
      <reward kind="ITEM" id="188055318" amount="1"/>
    </rewards>
  </metadata>""", 806116, 806115, 806135, 806101)
