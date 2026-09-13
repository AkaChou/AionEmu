content = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="2414" version="1">
  <metadata name="Hreidmar The Furious" display-name-id="1103614" min-level="35" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <prerequisites>
      <quest id="2412"/>
    </prerequisites>
    <rewards>
      <reward kind="GOLD" id="0" amount="6190"/>
      <reward kind="EXP" id="0" amount="806224"/>
      <reward kind="ITEM" id="186000008" amount="2"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="2413"/>
    </start-conditions>
  </metadata>
  <progress>
    <bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/>
  </progress>
  <nodes>
    <node label="unaccepted" status="NONE">
      <var name="var0" value="0"/>
    </node>
    <node label="started" status="START">
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
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="204369" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204369" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204361" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204361" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204361" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="204369" source="started1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="204369" source="reward" target="complete" fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

with open("src/main/resources/aion/data/static_data/quest_definition/quests/2414.xml", "w", encoding="utf-8") as f:
    f.write(content)
print("Updated 2414")
