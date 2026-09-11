#!/usr/bin/env python3
"""Rebuild all retail coin-fountain quests from the legacy FountainRewards template."""

from apply_legacy_dialog_chains import document, node, talk, write


def build(quest_id, metadata, start_npcs):
    nodes = (
        node("unaccepted", "NONE", 0),
        node("reward", "REWARD", 0),
        node("complete", "COMPLETE", 0),
    )
    transitions = []
    for npc_id in start_npcs:
        for source in ("unaccepted", "complete"):
            transitions.extend((
                talk(source, source, npc_id, "USE_OBJECT", "SELECT1",
                     conditions=('<has-item item-id="186000469" count="1"/>',)),
                talk(source, source, npc_id, "QUEST_SELECT", "SELECT1",
                     conditions=('<has-item item-id="186000469" count="1"/>',)),
                talk(source, "reward", npc_id, "SETPRO1",
                     conditions=(
                         '<start-eligible/>',
                         '<has-item item-id="186000469" count="100"/>',
                     ),
                     after=(
                         '<sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>',
                         '<dialog type="SHOW_QUEST_PAGE" page="SHOW_SELECT_QUEST_REWARD_WINDOW1"/>',
                     )),
            ))
        transitions.append(talk("reward", "complete", npc_id, "SELECTED_QUEST_NOREWARD",
                                conditions=(
                                    '<has-item item-id="186000469" count="100"/>',
                                ),
                                actions=(
                                    '<remove-item item-id="186000469" count="100"/>',
                                    '<complete-quest reward-index="0"/>',
                                ),
                                after=(
                                    "<refresh-player-stats/>",
                                    '<sync-quest-state mode="COMPLETION"/>',
                                    '<dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/>',
                                )))
    write(quest_id, document(quest_id, metadata, nodes, transitions))


def metadata(name, display_name_id, min_level, race, bonus_level):
    return f"""  <metadata name="{name}" display-name-id="{display_name_id}" min-level="{min_level}" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="{race}"/>
    </races>
    <repeat max-repeat-count="255" cooldown-seconds="0" daily="false" weekly="false"/>
    <items>
      <item id="186000469" count="100"/>
    </items>
    <inventory-items>
      <item id="186000469" count="1"/>
    </inventory-items>
    <bonuses>
      <bonus level="{bonus_level}" type="MEDAL"/>
    </bonuses>
  </metadata>"""


if __name__ == "__main__":
    build(15205, metadata("Cygnea Fountain of Luck", 1801259, 55, "ELYOS", 2),
          (701429, 804788))
    build(25205, metadata("Boss Toss", 1801260, 55, "ASMODIANS", 2),
          (701430, 804759))
    build(15667, metadata("The Wishing Fountain", 1802938, 66, "ELYOS", 3),
          (805778,))
    build(25667, metadata("For Luck", 1802939, 66, "ASMODIANS", 3),
          (805753,))
    build(1717, metadata("Silver For The Fountain", 1104517, 25, "ELYOS", 1),
          (806559,))
    build(2717, metadata("Silver For The Fountain", 1104817, 25, "ASMODIANS", 1),
          (806560,))
