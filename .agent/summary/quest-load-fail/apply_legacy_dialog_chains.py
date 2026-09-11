#!/usr/bin/env python3
"""Rebuild quests 1463, 1901, and 2916 from their retail contracts."""

from pathlib import Path


ROOT = Path(__file__).resolve().parents[3]
QUEST_DIR = ROOT / "src/main/resources/aion/data/static_data/quest_definition/quests"


def transition(source, target, event, conditions=(), actions=(), after=(), priority=None):
    attributes = [f'source="{source}"', f'target="{target}"']
    if priority is not None:
        attributes.append(f'priority="{priority}"')
    lines = [f"    <transition {' '.join(attributes)}>", "      <event>", f"        {event}", "      </event>"]
    if conditions:
        lines.append("      <conditions>")
        lines.extend(f"        {condition}" for condition in conditions)
        lines.append("      </conditions>")
    if actions:
        lines.append("      <actions>")
        lines.extend(f"        {action}" for action in actions)
        lines.append("      </actions>")
    if after:
        lines.append("      <after-commit>")
        lines.extend(f"        {action}" for action in after)
        lines.append("      </after-commit>")
    lines.append("    </transition>")
    return "\n".join(lines)


def talk(source, target, npc_id, action, page=None, conditions=(), actions=(), after=(), priority=None):
    event = f'<dialog type="TALK_TO_NPC" npc-id="{npc_id}" action="{action}"/>'
    resolved_after = after
    if page is not None:
        resolved_after = (f'<dialog type="SHOW_QUEST_PAGE" page="{page}"/>',)
    return transition(source, target, event, conditions, actions, resolved_after, priority)


def npc_start(npc_id, target, page="SELECT1", selection_sources="unaccepted"):
    return (
        f'    <dialog type="NPC_START" npc-id="{npc_id}" source="unaccepted" target="{target}" '
        f'selection-sources="{selection_sources}" start-page="{page}"/>'
    )


def npc_complete(npc_id, source, fixed_reward_indices):
    return "\n".join((
        f'    <npc-complete npc-id="{npc_id}" source="{source}" target="complete" '
        f'fixed-reward-indices="{fixed_reward_indices}" '
        'actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" '
        'complete-reward-index="0" finish="SELECTION_DIALOG">',
        '      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>',
        "    </npc-complete>",
    ))


def node(label, status, var0):
    return "\n".join((
        f'    <node label="{label}" status="{status}">',
        f'      <var name="var0" value="{var0}"/>',
        "    </node>",
    ))


def document(quest_id, metadata, nodes, transitions):
    return "\n".join((
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<quest-definition id="{quest_id}" version="1">',
        metadata,
        "  <progress>",
        '    <bit-field name="var0" offset="0" width="3" min="0" max="7" '
        'persistence="PERSISTENT" scope="LOCAL"/>',
        "  </progress>",
        "  <nodes>",
        *nodes,
        "  </nodes>",
        "  <transitions>",
        *transitions,
        "  </transitions>",
        "</quest-definition>",
        "",
    ))


def write(quest_id, content):
    path = QUEST_DIR / f"{quest_id}.xml"
    path.write_text(content, encoding="utf-8")
    print(f"WROTE {path.relative_to(ROOT)}")


def build_1463():
    metadata = """  <metadata name="[Spy] Message to a Spy" display-name-id="1102563" min-level="24" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <work-items>
      <item id="182201382" count="1"/>
      <item id="182201383" count="1"/>
    </work-items>
    <rewards>
      <reward kind="EXP" id="0" amount="166650"/>
      <reward kind="AP" id="0" amount="100"/>
      <reward kind="ITEM" id="188100335" amount="69"/>
      <reward kind="ITEM" id="186000469" amount="2"/>
    </rewards>
  </metadata>"""
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("reward", "REWARD", 2),
        node("complete", "COMPLETE", 0),
    )
    transitions = (
        npc_start(203940, "s0"),
        talk("s0", "s0", 203903, "QUEST_SELECT", "SELECT2"),
        talk("s0", "s0", 203903, "SELECT2_1", "SELECT2_1"),
        talk("s0", "s0", 203903, "SELECT2_2", "SELECT2_2"),
        talk("s0", "s1", 203903, "SETPRO1",
             actions=('<give-item item-id="182201382" count="1"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s1", "s1", 204424, "QUEST_SELECT", "SELECT3"),
        talk("s1", "s1", 204424, "SELECT3_1", "SELECT3_1"),
        talk("s1", "reward", 204424, "SETPRO2",
             actions=('<give-item item-id="182201383" count="1"/>',
                      '<remove-item item-id="182201382" count="1"/>'),
             after=('<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>', "<close-dialog/>")),
        talk("reward", "reward", 203903, "QUEST_SELECT", "SELECT5"),
        npc_complete(203903, "reward", "0 1 2 3"),
    )
    write(1463, document(1463, metadata, nodes, transitions))


def build_1901():
    metadata = """  <metadata name="Krallic Language Potion" display-name-id="1102901" min-level="10" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <work-items>
      <item id="182206000" count="1"/>
    </work-items>
    <rewards>
      <reward kind="EXP" id="0" amount="29700"/>
      <reward kind="TITLE" id="8" amount="1"/>
    </rewards>
  </metadata>"""
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("s2", "START", 2),
        node("s3", "START", 3),
        node("s4", "START", 4),
        node("s5", "START", 5),
        node("reward-paid", "REWARD", 5),
        node("reward-alt", "REWARD", 6),
        node("complete", "COMPLETE", 0),
    )
    transitions = [
        npc_start(203830, "s0"),
        talk("unaccepted", "unaccepted", 203830, "SELECT1_1", "SELECT1_1"),
        talk("s0", "s0", 798026, "QUEST_SELECT", "SELECT2"),
        talk("s0", "s0", 798026, "SELECT2_1", "SELECT2_1"),
        talk("s0", "s0", 798026, "SELECT2_1_1", "SELECT2_1_1"),
        talk("s0", "s0", 798026, "SELECT2_2", "SELECT2_2",
             conditions=('<currency-at-least kind="KINAH" amount="10000"/>',),
             actions=('<decrease-currency kind="KINAH" amount="10000"/>',)),
        talk("s0", "s0", 798026, "SELECT2_3", "SELECT2_3",
             conditions=('<currency-below kind="KINAH" amount="10000"/>',)),
        talk("s0", "reward-paid", 798026, "SETPRO1",
             actions=('<set-variable field="var0" value="5"/>',),
             after=('<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>', "<close-dialog/>")),
        talk("s0", "s0", 798026, "SELECT2_3_1", "SELECT2_3_1"),
        talk("s0", "s0", 798026, "SELECT2_3_1_1", "SELECT2_3_1_1"),
        talk("s0", "s1", 798026, "SETPRO2",
             actions=('<set-variable field="var0" value="1"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s1", "s1", 798025, "QUEST_SELECT", "SELECT3"),
        talk("s1", "s1", 798025, "SELECT3_1", "SELECT3_1"),
        talk("s1", "s2", 798025, "SETPRO3",
             actions=('<set-variable field="var0" value="2"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s2", "s2", 203131, "QUEST_SELECT", "SELECT4"),
        talk("s2", "s2", 203131, "SELECT4_1", "SELECT4_1"),
        talk("s2", "s2", 203131, "SELECT4_1_1", "SELECT4_1_1"),
        talk("s2", "s3", 203131, "SETPRO4",
             actions=('<set-variable field="var0" value="3"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s3", "s3", 798003, "QUEST_SELECT", "SELECT5"),
        talk("s3", "s3", 798003, "SELECT5_1", "SELECT5_1"),
        talk("s3", "s4", 798003, "SETPRO5",
             actions=('<set-variable field="var0" value="4"/>',
                      '<give-item item-id="182206000" count="1"/>'),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s4", "s4", 798025, "QUEST_SELECT", "SELECT6"),
        talk("s4", "s4", 798025, "SELECT6_1", "SELECT6_1"),
        talk("s4", "s5", 798025, "SETPRO6",
             actions=('<set-variable field="var0" value="5"/>',
                      '<remove-item item-id="182206000" count="1"/>'),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s5", "s5", 798026, "QUEST_SELECT", "SELECT7"),
        talk("s5", "s5", 798026, "SELECT7_1", "SELECT7_1"),
        talk("s5", "reward-alt", 798026, "SETPRO7",
             actions=('<set-variable field="var0" value="6"/>',),
             after=('<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>', "<close-dialog/>")),
    ]
    for reward_node in ("reward-paid", "reward-alt"):
        transitions.extend((
            talk(reward_node, reward_node, 203864, "QUEST_SELECT", "SELECT8"),
            npc_complete(203864, reward_node, "0 1"),
        ))
    write(1901, document(1901, metadata, nodes, transitions))


def build_2916():
    metadata = """  <metadata name="Man in The Long Black Robe" display-name-id="1104116" min-level="15" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <items>
      <item id="182207007" count="1"/>
    </items>
    <rewards>
      <reward kind="EXP" id="0" amount="115950"/>
      <reward kind="ITEM" id="120000833" amount="1"/>
    </rewards>
    <drops>
      <drop npc-id="700211" item-id="182207007" chance="100" each-member="true" collecting-step="6"/>
    </drops>
  </metadata>"""
    nodes = (
        node("unaccepted", "NONE", 0),
        node("s0", "START", 0),
        node("s1", "START", 1),
        node("s2", "START", 2),
        node("s3", "START", 3),
        node("s4", "START", 4),
        node("s5", "START", 5),
        node("s6", "START", 6),
        node("reward", "REWARD", 6),
        node("complete", "COMPLETE", 0),
    )
    transitions = (
        npc_start(204141, "s0"),
        talk("unaccepted", "unaccepted", 204141, "SELECT1_1", "SELECT1_1"),
        talk("s0", "s0", 204152, "QUEST_SELECT", "SELECT2"),
        talk("s0", "s0", 204152, "SELECT2_1", "SELECT2_1"),
        talk("s0", "s1", 204152, "SETPRO1",
             actions=('<set-variable field="var0" value="1"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s1", "s1", 204150, "QUEST_SELECT", "SELECT3"),
        talk("s1", "s1", 204150, "SELECT3_1", "SELECT3_1"),
        talk("s1", "s2", 204150, "SETPRO2",
             actions=('<set-variable field="var0" value="2"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s2", "s2", 204151, "QUEST_SELECT", "SELECT4"),
        talk("s2", "s2", 204151, "SELECT4_1", "SELECT4_1"),
        talk("s2", "s2", 204151, "SELECT4_1_1", "SELECT4_1_1"),
        talk("s2", "s3", 204151, "SETPRO3",
             actions=('<set-variable field="var0" value="3"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s3", "s3", 798033, "QUEST_SELECT", "SELECT5"),
        talk("s3", "s3", 798033, "SELECT5_1", "SELECT5_1"),
        talk("s3", "s4", 798033, "SETPRO4",
             actions=('<set-variable field="var0" value="4"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        talk("s4", "s4", 203673, "QUEST_SELECT", "SELECT6"),
        talk("s4", "s4", 203673, "SELECT6_1", "SELECT6_1"),
        talk("s4", "s5", 203673, "SETPRO5",
             actions=('<set-variable field="var0" value="5"/>',),
             after=('<sync-quest-state mode="PACKET_ONLY"/>', "<close-dialog/>")),
        transition("s5", "s6", '<at-distance npc-id="700211"/>',
                   actions=('<set-variable field="var0" value="6"/>',),
                   after=('<sync-quest-state mode="PACKET_ONLY"/>',)),
        transition("s6", "s6", '<can-act template-id="700211" action-type="ACTION_ITEM_USE"/>'),
        talk("s6", "s6", 204141, "QUEST_SELECT", "SELECT7"),
        talk("s6", "reward", 204141, "CHECK_USER_HAS_QUEST_ITEM",
             conditions=('<has-item item-id="182207007" count="1"/>',),
             actions=('<remove-item item-id="182207007" count="1"/>',),
             after=('<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                    '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>')),
        talk("s6", "s6", 204141, "CHECK_USER_HAS_QUEST_ITEM", "SELECT7_2",
             conditions=('<has-item item-id="182207007" count="1" expected="false"/>',)),
        transition("s6", "s6", '<dialog type="TALK_TO_NPC" npc-id="204141" action="FINISH_DIALOG"/>',
                   after=('<dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/>',)),
        talk("reward", "reward", 204141, "QUEST_SELECT", after=(
            '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
            '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>',
        )),
        npc_complete(204141, "reward", "0 1"),
    )
    write(2916, document(2916, metadata, nodes, transitions))


if __name__ == "__main__":
    build_1463()
    build_1901()
    build_2916()
