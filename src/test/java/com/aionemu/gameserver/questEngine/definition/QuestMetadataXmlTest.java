package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestMetadataXmlTest {
	@Test
	void xmlCarriesLegacyStaticMetadataWithoutDroppingFields() {
		String xml = """
				<quest-definition id="1001" version="1" ownership="CATALOG_ONLY">
				  <evidence><ref source="test" locator="quest/1001" statement="metadata"/></evidence>
				  <metadata name="legacy" display-name-id="1101001" min-level="2" max-level="55" category="MISSION"
				      rank="3" max-count-limited-quest="4" count-recover-limited-quest="2"
				      cannot-share="true" cannot-giveup="true" bounty-reward="true" use-class-reward="2"
				      combine-skill="9" combine-skill-point="399" timer="true" npc-faction-id="7"
				      mentor-type="MENTOR" target-type="FORCE" title-id="42">
				    <races><race id="ELYOS"/></races>
				    <classes><class id="FIGHTER"/></classes>
				    <gender id="FEMALE"/>
				    <repeat max-repeat-count="3" cooldown-seconds="60" daily="false" weekly="true" cycles="MON WED"/>
				    <prerequisites><quest id="1000" reward-mode="1"/></prerequisites>
				    <items><item id="182400001" count="5"/></items>
				    <inventory-items><item id="182400002" count="1"/></inventory-items>
				    <work-items><item id="182400003" count="2"/></work-items>
				    <rewards><reward kind="EXP" id="0" amount="10"/></rewards>
				    <extended-rewards><reward kind="GOLD" id="0" amount="20"/></extended-rewards>
				    <drops><drop npc-id="210001" item-id="182400001" chance="50" each-member="true" collecting-step="7" scope="ALLIANCE"/></drops>
				    <bonuses><bonus type="AP" level="10" skill="2"/></bonuses>
				    <kills><kill sequence="1"><npc id="210001"/><npc id="210002"/></kill></kills>
				    <start-conditions><condition type="finished" quest-id="999" reward-mode="1"/></start-conditions>
				    <class-rewards><class id="FIGHTER"><reward kind="ITEM" id="100000001" amount="1"/></class></class-rewards>
				  </metadata>
				</quest-definition>
				""";

		QuestMetadata metadata = QuestDefinitionXmlCompiler.compile(
				new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).definition().metadata();

		assertEquals("ELYOS", metadata.permittedRaces().iterator().next());
		assertEquals("FIGHTER", metadata.permittedClasses().iterator().next());
		assertEquals("FEMALE", metadata.permittedGender());
		assertEquals(3, metadata.repeatPolicy().maxRepeatCount());
		assertEquals(4, metadata.maxCountLimitedQuest());
		assertTrue(metadata.cannotShare());
		assertEquals(399, metadata.combineSkillPoint());
		assertEquals(2, metadata.questWorkItems().get(0).count());
		assertEquals(QuestDropScope.ALLIANCE, metadata.drops().get(0).scope());
		assertEquals("FIGHTER", metadata.classRewards().keySet().iterator().next());
		assertEquals(2, metadata.kills().get(0).npcIds().size());
	}
}
