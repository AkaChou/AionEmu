#!/usr/bin/env python3
"""Rebuild the three Silentera Canyon statue-spawn dialog quests."""

from apply_legacy_dialog_chains import document, node, talk, transition, write


def npc_start(npc_id, target, accept_actions):
    lines = [
        f'    <dialog type="NPC_START" npc-id="{npc_id}" source="unaccepted" target="{target}" '
        'selection-sources="unaccepted" start-page="SELECT1">',
        "      <accept-actions>",
    ]
    lines.extend(f"        {action}" for action in accept_actions)
    lines.extend(("      </accept-actions>", "    </dialog>"))
    return "\n".join(lines)


def npc_complete(npc_id, source, choice_indices):
    lines = [
        f'    <npc-complete npc-id="{npc_id}" source="{source}" target="complete" '
        'fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">',
    ]
    for offset, reward_index in enumerate(choice_indices, start=1):
        lines.append(
            f'      <choice action="SELECTED_QUEST_REWARD{offset}" reward-index="{reward_index}"/>')
    lines.extend((
        '      <preview actions="SELECT_QUEST_REWARD"/>',
        "    </npc-complete>",
    ))
    return "\n".join(lines)


def build(quest_id, metadata, start_npc, work_item, reward_npc, object_npc,
          spawned_npc, slot, world_id, x, y, z, heading, choice_indices):
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("reward", "REWARD", 1),
        node("complete", "COMPLETE", 0),
    )
    transitions = (
        npc_start(
            start_npc,
            "s0",
            (f'<give-item item-id="{work_item}" count="1"/>',),
        ),
        transition("s0", "s0",
                   f'<can-act template-id="{object_npc}" action-type="ACTION_ITEM_USE"/>'),
        talk("s0", "s1", object_npc, "USE_OBJECT",
             conditions=(f'<has-item item-id="{work_item}" count="1"/>',),
             actions=(f'<remove-item item-id="{work_item}" count="1"/>',
                      '<set-variable field="var0" value="1"/>'),
             after=(
                 f'<spawn-npc-current-or-default slot="{slot}" template-id="{spawned_npc}" '
                 f'world-id="{world_id}" x="{x}" y="{y}" z="{z}" heading="{heading}"/>',
                 '<sync-quest-state mode="PACKET_ONLY"/>',
                 "<close-dialog/>",
             )),
        talk("s1", "s1", spawned_npc, "QUEST_SELECT", "SELECT2"),
        talk("s1", "s1", spawned_npc, "SELECT2_1", "SELECT2_1"),
        talk("s1", "s1", spawned_npc, "SELECT2_1_1", "SELECT2_1_1"),
        talk("s1", "reward", spawned_npc, "SETPRO1",
             actions=('<set-variable field="var0" value="1"/>',),
             after=(
                 '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                 f'<despawn-npc slot="{slot}"/>',
                 "<close-dialog/>",
             )),
        talk("reward", "reward", reward_npc, "USE_OBJECT", "SELECT5"),
        talk("reward", "reward", reward_npc, "QUEST_SELECT", "SELECT5"),
        npc_complete(reward_npc, "reward", choice_indices),
    )
    write(quest_id, document(quest_id, metadata, nodes, transitions))


def build_30056():
    metadata = """  <metadata name="Dirvisia's Sorrow" display-name-id="1114270" min-level="53" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <work-items>
      <item id="182209223" count="1"/>
      <item id="182209224" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="143620"/>
      <reward kind="EXP" id="0" amount="5989348"/>
      <reward kind="SELECTABLE_ITEM" id="167000551" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="167000552" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="167000555" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="167000558" amount="1"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="30055"/>
    </start-conditions>
  </metadata>"""
    build(30056, metadata, 798929, 182209223, 203901, 700569, 799034,
          "dirvisia", 600010000, "555.8842", "307.8092", "310.24997", "0", (2, 3, 4, 5))


def build_30156():
    metadata = """  <metadata name="Nep's Love" display-name-id="1114277" min-level="53" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <work-items>
      <item id="182209253" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="71810"/>
      <reward kind="EXP" id="0" amount="5989348"/>
      <reward kind="SELECTABLE_ITEM" id="167000551" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="167000558" amount="1"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="30155" reward-mode="1"/>
    </start-conditions>
  </metadata>"""
    build(30156, metadata, 799234, 182209253, 204304, 700570, 799339,
          "sinigalla-nep", 600010000, "545.308", "1232.3855", "304.35193", "73", (2, 3))


def build_30157():
    metadata = """  <metadata name="Vili's Mind" display-name-id="1114278" min-level="53" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <work-items>
      <item id="182209254" count="1"/>
    </work-items>
    <rewards>
      <reward kind="GOLD" id="0" amount="71810"/>
      <reward kind="EXP" id="0" amount="5989348"/>
      <reward kind="SELECTABLE_ITEM" id="167000552" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="167000555" amount="1"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="30155" reward-mode="2"/>
    </start-conditions>
  </metadata>"""
    build(30157, metadata, 204304, 182209254, 799234, 700570, 799339,
          "sinigalla-vili", 600010000, "545.3877", "1232.0298", "304.3357", "76", (2, 3))


if __name__ == "__main__":
    build_30056()
    build_30156()
    build_30157()
