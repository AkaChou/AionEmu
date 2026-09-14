XML_DIR = "src/main/resources/aion/data/static_data/quest_definition/quests"

# 30061
content_30061 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="30061" version="1">
  <metadata name="Cache-ing in on Turmoil" display-name-id="1114282" min-level="55" max-level="2147483647" category="IMPORTANT">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="EXP" id="0" amount="1184073"/>
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
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="800165" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798927" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798927" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798927" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="799381" source="step1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="799381" source="reward" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 30161
content_30161 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="30161" version="1">
  <metadata name="Hexway Hideout" display-name-id="1114284" min-level="55" max-level="2147483647" category="IMPORTANT">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="EXP" id="0" amount="1184073"/>
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
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="800170" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799225" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799225" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799225" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="799383" source="step1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="799383" source="reward" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 30107
content_30107 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="30107" version="1">
  <metadata name="Deciphering Drakan" display-name-id="1114239" min-level="52" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <work-items>
      <item id="182209183" count="1"/>
    </work-items>
    <rewards>
      <reward kind="EXP" id="0" amount="6517414"/>
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
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <use-item item-id="182209183"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SHOW_ASK_QUEST_ACCEPT_WINDOW"/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="QUEST_ACTION" action="FINISH_DIALOG"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="started">
      <event>
        <dialog type="QUEST_ACTION" action="QUEST_ACCEPT_1"/>
      </event>
      <actions>
        <give-item item-id="182209183" count="1"/>
      </actions>
      <after-commit>
        <sync-quest-state mode="VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="QUEST_ACTION" action="QUEST_REFUSE_1"/>
      </event>
      <after-commit>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799029" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799029" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799029" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="799333" source="step1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="799333" source="reward" target="complete" fixed-reward-indices="0" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 11139
content_11139 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="11139" version="1">
  <metadata name="The Bad News" display-name-id="1125115" min-level="53" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="35250"/>
      <reward kind="EXP" id="0" amount="4020773"/>
      <reward kind="ITEM" id="188050586" amount="1"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="11111"/>
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
    <dialog type="NPC_START" npc-id="799075" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="799075" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798971" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798971" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798971" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="798979" source="step1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="798979" source="reward" target="complete" fixed-reward-indices="0 1 2" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 11455
content_11455 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="11455" version="1">
  <metadata name="When The Time Is Ripe" display-name-id="1125505" min-level="51" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ELYOS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="38140"/>
      <reward kind="EXP" id="0" amount="3244812"/>
      <reward kind="SELECTABLE_ITEM" id="111100969" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="111300974" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="111500960" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="111600952" amount="1"/>
      <reward kind="SELECTABLE_ITEM" id="111301466" amount="1"/>
    </rewards>
    <start-conditions>
      <condition type="finished" quest-id="11454"/>
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
    <dialog type="NPC_START" npc-id="799070" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798933" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798933" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="798933" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="798946" source="step1" target="reward" page="SELECT5"/>
    <npc-complete npc-id="798946" source="reward" target="complete" fixed-reward-indices="0 1" complete-reward-index="0" finish="SELECTION_DIALOG">
      <choice action="SELECTED_QUEST_REWARD1" reward-index="2"/>
      <choice action="SELECTED_QUEST_REWARD2" reward-index="3"/>
      <choice action="SELECTED_QUEST_REWARD3" reward-index="4"/>
      <choice action="SELECTED_QUEST_REWARD4" reward-index="5"/>
      <choice action="SELECTED_QUEST_REWARD5" reward-index="6"/>
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

# 4011
content_4011 = """<?xml version="1.0" encoding="UTF-8"?>
<quest-definition id="4011" version="1">
  <metadata name="An Old Settler's Letter" display-name-id="1114010" min-level="23" max-level="2147483647" category="QUEST" cannot-share="true">
    <races>
      <race id="ASMODIANS"/>
    </races>
    <rewards>
      <reward kind="GOLD" id="0" amount="10760"/>
      <reward kind="EXP" id="0" amount="117433"/>
      <reward kind="ITEM" id="160002003" amount="6"/>
      <reward kind="ITEM" id="186000007" amount="1"/>
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
    <node label="reward" status="REWARD">
      <var name="var0" value="1"/>
    </node>
    <node label="complete" status="COMPLETE">
      <var name="var0" value="0"/>
    </node>
  </nodes>
  <transitions>
    <dialog type="NPC_START" npc-id="730139" source="unaccepted" target="started" selection-sources="unaccepted" start-page="SELECT1"/>
    <transition source="unaccepted" target="unaccepted">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="730139" action="SELECT1_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT1_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205132" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2"/>
      </after-commit>
    </transition>
    <transition source="started" target="started">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205132" action="SELECT2_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT2_1"/>
      </after-commit>
    </transition>
    <transition source="started" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="205132" action="SETPRO1"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203522" action="QUEST_SELECT"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3"/>
      </after-commit>
    </transition>
    <transition source="step1" target="step1">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203522" action="SELECT3_1"/>
      </event>
      <after-commit>
        <dialog type="SHOW_QUEST_PAGE" page="SELECT3_1"/>
      </after-commit>
    </transition>
    <transition source="step1" target="reward">
      <event>
        <dialog type="TALK_TO_NPC" npc-id="203522" action="SETPRO2"/>
      </event>
      <after-commit>
        <sync-quest-state mode="LEVEL_AND_VISIBILITY_REFRESH"/>
        <close-dialog/>
      </after-commit>
    </transition>
    <dialog type="NPC_REPORT" npc-id="205132" source="reward" target="reward" page="DEFAULT_SUCCESS"/>
    <npc-complete npc-id="205132" source="reward" target="complete" fixed-reward-indices="0 1 2 3" actions="SELECTED_QUEST_REWARD1..SELECTED_QUEST_NOREWARD" complete-reward-index="0" finish="SELECTION_DIALOG">
      <preview actions="USE_OBJECT SELECT_QUEST_REWARD"/>
    </npc-complete>
  </transitions>
</quest-definition>
"""

with open(f"{XML_DIR}/30061.xml", "w", encoding="utf-8") as f:
    f.write(content_30061)
with open(f"{XML_DIR}/30161.xml", "w", encoding="utf-8") as f:
    f.write(content_30161)
with open(f"{XML_DIR}/30107.xml", "w", encoding="utf-8") as f:
    f.write(content_30107)
with open(f"{XML_DIR}/11139.xml", "w", encoding="utf-8") as f:
    f.write(content_11139)
with open(f"{XML_DIR}/11455.xml", "w", encoding="utf-8") as f:
    f.write(content_11455)
with open(f"{XML_DIR}/4011.xml", "w", encoding="utf-8") as f:
    f.write(content_4011)

print("Updated 30061, 30161, 30107, 11139, 11455, 4011")
