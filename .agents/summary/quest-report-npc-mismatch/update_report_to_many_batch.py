import xml.etree.ElementTree as ET

XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

# 1648
content_1648 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="1648" version="1">
  <metadata name="Undead War Alert" display-name-id="1102848" min-level="45" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="EXP" id="0" amount="2020220"/>
      <reward kind="ITEM" id="186000004" amount="3"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="1636"/>
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
    <node label="step1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="step2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="204545" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204612" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204612" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204612" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204500" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204500" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204500" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="204590" source="step2" target="reward" page="SELECT5"/>
    <npc-complete npc-id="204590" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 2231
content_2231 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="2231" version="1">
  <metadata name="Sibling Rivalry" display-name-id="1103431" min-level="13" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="1000"/>
      <reward kind="EXP" id="0" amount="17267"/>
      <reward kind="ITEM" id="186000007" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100000181" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100100181" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100200236" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100500171" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100600202" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100900199" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="101300176" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="101500192" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="101700183" amount="1"/>
    </rewards>
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
    <node label="step1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="step2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="203620" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203620" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203609" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203609" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203609" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203612" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203612" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203612" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="203610" source="step2" target="reward" page="SELECT5"/>
    <npc-complete npc-id="203610" source="reward" target="complete" fixed-reward-indices="0 1 2" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="3"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="4"/>
      <choice action="SELECTED_QUEST_REWARD3" reward-index="5"/>
      <choice action="SELECTED_QUEST_REWARD4" reward-index="6"/>
      <choice action="SELECTED_QUEST_REWARD5" reward-index="7"/>
      <choice action="SELECTED_QUEST_REWARD6" reward-index="8"/>
      <choice action="SELECTED_QUEST_REWARD7" reward-index="9"/>
      <choice action="SELECTED_QUEST_REWARD8" reward-index="10"/>
      <choice action="SELECTED_QUEST_REWARD9" reward-index="11"/>
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 2641
content_2641 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="2641" version="1">
  <metadata name="Powwow with the Mau" display-name-id="1103941" min-level="37" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="EXP" id="0" amount="1582400"/>
      <reward kind="ITEM" id="186000008" amount="3"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="2640"/>
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
    <node label="step1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="step2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="step3" status="START">
      <var name="var0" value="3"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="3"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="204817" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204795" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204795" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204795" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204798" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204798" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204798" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="step2" target="step2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204796" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4"/>
      </after-commit>
    </transition>
    <transition source="step2" target="step2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204796" action="SELECT4_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT4_1"/>
      </after-commit>
    </transition>
    <transition source="step2" target="step3">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204796" action="SETPRO3"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="204700" source="step3" target="reward" page="SELECT5"/>
    <npc-complete npc-id="204700" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 2653
content_2653 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="2653" version="1">
  <metadata name="[Spy] Finding Bollvig" display-name-id="1103953" min-level="41" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="EXP" id="0" amount="2954681"/>
      <reward kind="ITEM" id="186000009" amount="3"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="2652"/>
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
    <node label="step1" status="START">
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
    <dialog type="NPC_START" npc-id="204650" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204655" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204655" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="204655" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="204775" source="step1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="204775" source="reward" target="complete" fixed-reward-indices="0 1" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 2724
content_2724 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="2724" version="1">
  <metadata name="Missing in Action" display-name-id="1103724" min-level="25" max-level="2147483647" category="QUEST">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="1000"/>
      <reward kind="EXP" id="0" amount="169600"/>
      <reward kind="ITEM" id="186000007" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100000305" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100100300" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100200331" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100500293" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100600329" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="100900325" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="101300303" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="101500307" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="101700290" amount="1"/>
    </rewards>
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
    <node label="step1" status="START">
      <var name="var0" value="1"/>
    </node>
    <node label="step2" status="START">
      <var name="var0" value="2"/>
    </node>
    <node label="reward" status="REWARD">
      <var name="var0" value="2"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="278002" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278002" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278014" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278014" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278014" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278089" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278089" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step2">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="278089" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="278097" source="step2" target="reward" page="SELECT5"/>
    <npc-complete npc-id="278097" source="reward" target="complete" fixed-reward-indices="0 1 2" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="3"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="4"/>
      <choice action="SELECTED_QUEST_REWARD3" reward-index="5"/>
      <choice action="SELECTED_QUEST_REWARD4" reward-index="6"/>
      <choice action="SELECTED_QUEST_REWARD5" reward-index="7"/>
      <choice action="SELECTED_QUEST_REWARD6" reward-index="8"/>
      <choice action="SELECTED_QUEST_REWARD7" reward-index="9"/>
      <choice action="SELECTED_QUEST_REWARD8" reward-index="10"/>
      <choice action="SELECTED_QUEST_REWARD9" reward-index="11"/>
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

for qid, text in [("1648", content_1648), ("2231", content_2231), ("2641", content_2641), ("2653", content_2653), ("2724", content_2724)]:
    with open(f"{XML_DIR}/{qid}.xml", "w", encoding="utf-8") as f:
        f.write(text)
    print(f"Updated {qid}")
