#!/usr/bin/env python3
"""Rebuild the mirrored Elyos/Asmodian sage quests 1988 and 2988."""

from apply_legacy_dialog_chains import document, node, npc_start, talk, transition, write


def npc_complete(npc_id):
    return "\n".join((
        f'    <npc-complete npc-id="{npc_id}" source="reward" target="complete" '
        'fixed-reward-indices="0" '
        'actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
        'complete-reward-index="0" finish="SELECTION_DIALOG"/>',
    ))


def build(quest_id, metadata, start_npc, first_npc, second_npc, final_npc,
          second_has_extra_page):
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("s2", "START", 2),
        node("reward", "REWARD", 2),
        node("complete", "COMPLETE", 0),
    )
    transitions = [
        npc_start(start_npc, "s0"),
        talk("s0", "s0", first_npc, "QUEST_SELECT", "SELECT2"),
        talk("s0", "s0", first_npc, "SELECT2_1", "SELECT2_1"),
        talk("s0", "s1", first_npc, "SETPRO1",
             actions=('<set-variable field="var0" value="1"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s1", "s1", second_npc, "QUEST_SELECT", "SELECT3"),
        talk("s1", "s1", second_npc, "SELECT3_1", "SELECT3_1"),
    ]
    if second_has_extra_page:
        transitions.append(talk("s1", "s1", second_npc, "SELECT3_1_1", "SELECT3_1_1"))
    transitions.extend((
        talk("s1", "s2", second_npc, "SETPRO2",
             actions=('<set-variable field="var0" value="2"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s2", "s2", final_npc, "QUEST_SELECT", "SELECT4"),
        talk("s2", "s2", final_npc, "SELECT4_1", "SELECT4_1"),
        talk("s2", "s2", final_npc, "SELECT4_2", "SELECT4_2"),
        transition("s2", "s2",
                   f'<dialog type="TALK_TO_NPC" npc-id="{final_npc}" action="FINISH_DIALOG"/>',
                   after=("<close-dialog/>",)),
        talk("s2", "reward", final_npc, "SELECT_QUEST_REWARD",
             conditions=('<has-item item-id="186000039" count="1"/>',),
             actions=('<remove-item item-id="186000039" count="1"/>',),
             after=(
                 '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                 '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>',
             )),
        talk("reward", "reward", final_npc, "QUEST_SELECT", after=(
            '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
            '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>',
        )),
        npc_complete(final_npc),
    ))
    write(quest_id, document(quest_id, metadata, nodes, transitions))


def build_1988():
    metadata = """  <metadata name="A Meeting with a Sage" display-name-id="1102988" min-level="30" max-level="2147483647" category="QUEST">
    <races>
      <race id="ELYOS"/>
    </races>
    <items>
      <item id="186000039" count="1"/>
    </items>
    <rewards>
      <reward kind="EXP" id="0" amount="291412"/>
    </rewards>
  </metadata>"""
    build(1988, metadata, 203725, 203989, 798018, 203771, True)


def build_2988():
    metadata = """  <metadata name="The Wise in Disguise" display-name-id="1104188" min-level="30" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <items>
      <item id="186000039" count="1"/>
    </items>
    <rewards>
      <reward kind="EXP" id="0" amount="291412"/>
    </rewards>
  </metadata>"""
    build(2988, metadata, 204182, 204338, 204213, 204146, False)


if __name__ == "__main__":
    build_1988()
    build_2988()
