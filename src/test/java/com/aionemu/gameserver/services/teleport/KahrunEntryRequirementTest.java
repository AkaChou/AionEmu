package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.model.templates.portal.QuestReq;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import jakarta.xml.bind.JAXBContext;

/**
 * 锁定哥尔哈交通必须由本阵营 65 级使命的实际传送到达步骤解锁。
 * Locks Kahrun travel behind the racial level-65 mission step that teleports the player there.
 */
class KahrunEntryRequirementTest {

	private static final Path TELEPORTER_XML = Path.of(
			"src/main/resources/aion/data/static_data/npc_teleporter.xml");
	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");

	@Test
	void reachingKahrunTeleportStepUnlocksTravel() {
		assertTrue(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS,
				questState(10100, QuestStatus.START, 1)));
		assertTrue(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS,
				questState(10100, QuestStatus.START, 4)));
		assertTrue(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS,
				questState(10100, QuestStatus.REWARD, 4)));
		assertTrue(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS,
				questState(10100, QuestStatus.COMPLETE, 0)));

		assertTrue(TeleportService2.meetsKahrunEntryRequirement(Race.ASMODIANS,
				questState(20100, QuestStatus.START, 1)));
		assertTrue(TeleportService2.meetsKahrunEntryRequirement(Race.ASMODIANS,
				questState(20100, QuestStatus.COMPLETE, 0)));
	}

	@Test
	void earlierStepsAndWrongRaceDoNotUnlockTravel() {
		assertFalse(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS,
				questState(10100, QuestStatus.START, 0)));
		assertFalse(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS,
				questState(20100, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsKahrunEntryRequirement(Race.ASMODIANS,
				questState(20100, QuestStatus.START, 0)));
		assertFalse(TeleportService2.meetsKahrunEntryRequirement(Race.ASMODIANS,
				questState(10100, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsKahrunEntryRequirement(Race.PC_ALL,
				questState(10100, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsKahrunEntryRequirement(Race.ELYOS, null));
	}

	@Test
	void everyKahrunNpcTeleportCarriesTheRacialQuestGate() throws Exception {
		TeleporterData data = (TeleporterData) JAXBContext.newInstance(TeleporterData.class)
				.createUnmarshaller().unmarshal(TELEPORTER_XML.toFile());

		assertNpcTeleport(data, 801667, 376, 10100);
		assertNpcTeleport(data, 804782, 376, 10100);
		assertNpcTeleport(data, 805773, 376, 10100);
		assertNpcTeleport(data, 801668, 377, 20100);
		assertNpcTeleport(data, 804753, 377, 20100);
		assertNpcTeleport(data, 805748, 377, 20100);
	}

	@Test
	void capitalCorridorPortalsCarryTheRacialQuestGate() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		assertPortalGate(data, 805608, 104, 6001001, 10100);
		assertPortalGate(data, 805622, 104, 6001000, 20100);
	}

	private static void assertNpcTeleport(TeleporterData data, int npcId, int locId, int questId) {
		TeleportLocation location = data.getTeleporterTemplateByNpcId(npcId).getTeleLocIdData()
				.getTeleportLocation(locId);
		assertNotNull(location, "missing telelocation " + locId + " for NPC " + npcId);
		assertEquals(questId, location.getRequiredQuest());
		assertEquals(1, location.getRequiredQuestStep());
	}

	private static void assertPortalGate(Portal2Data data, int npcId, int dialogId, int locId, int questId) {
		PortalPath portalPath = data.getPortalDialog(npcId, dialogId, Race.PC_ALL);
		assertNotNull(portalPath, "missing portal path for NPC " + npcId + " dialog " + dialogId);
		assertEquals(locId, portalPath.getLocId());

		PortalReq portalReq = portalPath.getPortalReq();
		assertNotNull(portalReq, "missing portal_req for NPC " + npcId);
		assertEquals(List.of(questId), portalReq.getQuestReq().stream().map(QuestReq::getQuestId).toList());
		assertEquals(List.of(1), portalReq.getQuestReq().stream().map(QuestReq::getQuestStep).toList());
	}

	private static QuestState questState(int questId, QuestStatus status, int questVar) {
		return new QuestState(questId, status, questVar, 0, null, null, null);
	}
}
