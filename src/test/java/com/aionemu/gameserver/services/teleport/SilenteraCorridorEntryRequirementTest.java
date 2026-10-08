package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.model.templates.portal.QuestReq;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import jakarta.xml.bind.JAXBContext;

/**
 * 锁定锡兰泰拉峡谷（回廊）入口必须完成本族「回廊进军准备」使命（天族 10035 / 魔族 20035）。
 * Locks Silentera Canyon (the corridor) entry behind the racial corridor-advance-preparation mission
 * (Elyos 10035 / Asmodians 20035).
 */
class SilenteraCorridorEntryRequirementTest {

	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");

	@Test
	void completedPreparationUnlocksCorridorTravel() {
		assertTrue(TeleportService2.isSilenteraCorridorEntryWorld(600010000));
		assertTrue(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ELYOS,
				questState(10035, QuestStatus.COMPLETE, 0)));
		assertTrue(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ASMODIANS,
				questState(20035, QuestStatus.COMPLETE, 0)));
	}

	@Test
	void unfinishedOrWrongRaceDoesNotUnlockTravel() {
		assertFalse(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ELYOS,
				questState(10035, QuestStatus.START, 8)));
		assertFalse(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ELYOS,
				questState(10035, QuestStatus.REWARD, 8)));
		assertFalse(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ELYOS,
				questState(20035, QuestStatus.COMPLETE, 8)));
		assertFalse(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ASMODIANS,
				questState(10035, QuestStatus.COMPLETE, 8)));
		assertFalse(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.PC_ALL,
				questState(10035, QuestStatus.COMPLETE, 8)));
		assertFalse(TeleportService2.meetsSilenteraCorridorEntryRequirement(Race.ELYOS, null));
		assertFalse(TeleportService2.isSilenteraCorridorEntryWorld(600110000));
	}

	@Test
	void liveCorridorGatesCarryTheFinishedPreparationGate() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		assertPortalGate(data, 730256, Race.ELYOS, 6000100, 10035);
		assertPortalGate(data, 730260, Race.ASMODIANS, 6000101, 20035);
	}

	@Test
	void corridorOriginsKeepTheBalaureaEntryGate() {
		// 2026-10-08 用户裁定：从回廊（含大师服变体）回英吉斯温/格尔克马罗斯仍走真端既有的
		// 10031 龙界门禁——不得为回廊起点做豁免（Zz 无 10031 被拦 = 真端口径）。
		// User ruling 2026-10-08: leaving the corridor keeps the retail Balaurea entry gate (10031);
		// no corridor-origin exemption.
		assertFalse(PortalService.isBalaureaEntryAllowed(210050000, 600010000, false, false));
		assertFalse(PortalService.isBalaureaEntryAllowed(220140000, 600010000, false, false));
		assertFalse(PortalService.isBalaureaEntryAllowed(210050000, 600110000, false, false));

		// 同世界/实例/已持任务三例维持既有口径。 / Same-world, instance and requirement cases are unchanged.
		assertTrue(PortalService.isBalaureaEntryAllowed(210050000, 210050000, false, false));
		assertTrue(PortalService.isBalaureaEntryAllowed(210050000, 300150000, true, false));
		assertTrue(PortalService.isBalaureaEntryAllowed(210050000, 110010000, false, true));
	}

	private static void assertPortalGate(Portal2Data data, int npcId, Race race, int locId, int questId) {
		PortalPath portalPath = data.getPortalDialog(npcId, 104, race);
		assertNotNull(portalPath, "missing portal path for NPC " + npcId);
		assertEquals(locId, portalPath.getLocId());

		PortalReq portalReq = portalPath.getPortalReq();
		assertNotNull(portalReq, "missing portal_req for NPC " + npcId);
		assertNotNull(portalReq.getQuestReq(), "missing quest_req for NPC " + npcId);
		assertEquals(List.of(questId), portalReq.getQuestReq().stream().map(QuestReq::getQuestId).toList());
		assertEquals(List.of(0), portalReq.getQuestReq().stream().map(QuestReq::getQuestStep).toList());
	}

	private static QuestState questState(int questId, QuestStatus status, int questVar) {
		return new QuestState(questId, status, questVar, 0, null, null, null);
	}
}
