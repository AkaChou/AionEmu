package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import jakarta.xml.bind.JAXBContext;

/**
 * 锁定伊鲁玛/诺斯珀德交通必须由本阵营 65 级高阶守护者使命的实际传送步骤解锁。
 * Locks Iluma/Norsvold travel behind the racial level-65 ArchDaeva mission teleport step.
 */
class ArchDaevaEntryRequirementTest {

	private static final int ILUMA = 210100000;
	private static final int NORSVOLD = 220110000;
	private static final Path TELEPORTER_XML = Path.of(
			"src/main/resources/aion/data/static_data/npc_teleporter.xml");
	private static final Path ITEM_TEMPLATES_XML = Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_template_152209118_182005538.xml");

	@Test
	void reachingTheRacialTeleportStepUnlocksTheHomeWorld() {
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA,
			questState(10520, QuestStatus.START, 3)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA,
			questState(10520, QuestStatus.START, 4)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA,
			questState(10520, QuestStatus.REWARD, 5)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA,
			questState(10520, QuestStatus.COMPLETE, 0)));

		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, NORSVOLD,
			questState(20520, QuestStatus.START, 3)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, NORSVOLD,
			questState(20520, QuestStatus.START, 4)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, NORSVOLD,
			questState(20520, QuestStatus.COMPLETE, 0)));
	}

	@Test
	void earlierStepsWrongQuestAndMissingStateDoNotUnlockTravel() {
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA,
			questState(10520, QuestStatus.START, 0)));
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA,
			questState(20520, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, ILUMA, null));
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, NORSVOLD,
			questState(20520, QuestStatus.START, 3)));
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, NORSVOLD,
			questState(10520, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, NORSVOLD, null));
	}

	@Test
	void oppositeRaceAndUngatedWorldsKeepTheirExistingRules() {
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ASMODIANS, ILUMA,
			questState(20520, QuestStatus.START, 0)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, NORSVOLD,
			questState(10520, QuestStatus.START, 0)));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.PC_ALL, ILUMA, null));
		assertTrue(TeleportService2.meetsArchDaevaEntryRequirement(Race.ELYOS, 110010000, null));
	}

	@Test
	void capitalTeleportersCarryTheRacialQuestStepGate() throws Exception {
		TeleporterData data = (TeleporterData) JAXBContext.newInstance(TeleporterData.class)
			.createUnmarshaller().unmarshal(TELEPORTER_XML.toFile());

		assertQuestGate(data, 203726, 444, 10520, 4);
		assertQuestGate(data, 204191, 438, 20520, 4);
	}

	@Test
	void racialReturnScrollsTargetTheGatedHomeWorlds() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ITEM_TEMPLATES_XML.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		assertEquals(1, ((NodeList) xpath.evaluate(
			"//item_template[@id='164000405' and @return_world='210100000']",
			document, XPathConstants.NODESET)).getLength());
		assertEquals(1, ((NodeList) xpath.evaluate(
			"//item_template[@id='164000406' and @return_world='220110000']",
			document, XPathConstants.NODESET)).getLength());
	}

	private static void assertQuestGate(TeleporterData data, int npcId, int locationId, int questId,
			int questStep) {
		TeleportLocation location = data.getTeleporterTemplateByNpcId(npcId).getTeleLocIdData()
			.getTeleportLocation(locationId);
		assertNotNull(location, "missing telelocation " + locationId + " for NPC " + npcId);
		assertEquals(questId, location.getRequiredQuest());
		assertEquals(questStep, location.getRequiredQuestStep());
	}

	private static QuestState questState(int questId, QuestStatus status, int questVar) {
		return new QuestState(questId, status, questVar, 0, null, null, null);
	}
}
