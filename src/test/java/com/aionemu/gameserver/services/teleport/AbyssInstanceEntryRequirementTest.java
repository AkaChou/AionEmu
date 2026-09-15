package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.InstanceExitData;
import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.model.templates.portal.PortalUse;
import com.aionemu.gameserver.model.templates.portal.QuestReq;

import jakarta.xml.bind.JAXBContext;

/**
 * 锁定出口通向欧比斯的副本必须完成对应阵营的欧比斯入场任务后才能进入。
 * Locks instances whose exit leads into the Abyss behind the racial Abyss entry quest.
 */
class AbyssInstanceEntryRequirementTest {

	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");
	private static final Path PORTAL_LOCS = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_loc.xml");
	private static final Path INSTANCE_EXITS = Path.of(
			"src/main/resources/aion/data/static_data/instance_exit/instance_exit.xml");
	private static final int ABYSS_WORLD_ID = 400010000;

	@Test
	void darkPoetaEntryRequiresTheRacialAbyssEntryQuest() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		assertEntryGate(data, 730186, 10000, Race.ELYOS, 1044);
		assertEntryGate(data, 730185, 10000, Race.ASMODIANS, 2042);
		assertEntryGate(data, 805296, 10000, Race.ELYOS, 1044);
		assertEntryGate(data, 805296, 10000, Race.ASMODIANS, 2042);
		assertEntryGate(data, 832753, 10000, Race.ELYOS, 1044);
		assertEntryGate(data, 832754, 10000, Race.ASMODIANS, 2042);
		assertEntryGate(data, 804677, 10003, Race.ELYOS, 1044);
		assertEntryGate(data, 804677, 10003, Race.ASMODIANS, 2042);
		assertEntryGate(data, 804682, 10003, Race.ELYOS, 1044);
		assertEntryGate(data, 804682, 10003, Race.ASMODIANS, 2042);
		assertEntryGate(data, 835261, 10003, Race.ELYOS, 1044);
		assertEntryGate(data, 835261, 10003, Race.ASMODIANS, 2042);
		assertEntryGate(data, 835266, 10003, Race.ELYOS, 1044);
		assertEntryGate(data, 835266, 10003, Race.ASMODIANS, 2042);
	}

	@Test
	void darkPoetaAbyssGateLeadsIntoTheAbyss() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());
		PortalUse gate = data.getPortalUse(731666);
		assertNotNull(gate, "missing Dark Poeta Abyss gate portal_use 731666");

		assertEquals(List.of(4000108), gate.getPortalPaths().stream().map(PortalPath::getLocId).toList());
		assertTrue(portalLocTargetsWorld(4000108, ABYSS_WORLD_ID),
				"Dark Poeta Abyss gate 731666 must lead into the Abyss");
	}

	@Test
	void instancesWhoseExitLeadsIntoTheAbyssAreDetectedFromExitData() throws Exception {
		InstanceExitData previous = DataManager.INSTANCE_EXIT_DATA;
		try {
			DataManager.INSTANCE_EXIT_DATA = (InstanceExitData) JAXBContext.newInstance(InstanceExitData.class)
					.createUnmarshaller().unmarshal(INSTANCE_EXITS.toFile());

			assertTrue(TeleportService2.isAbyssExitInstance(300050000, Race.ELYOS));
			assertTrue(TeleportService2.isAbyssExitInstance(300060000, Race.ASMODIANS));
			assertTrue(TeleportService2.isAbyssExitInstance(300070000, Race.ELYOS));
			assertTrue(TeleportService2.isAbyssExitInstance(300090000, Race.ASMODIANS));
			assertTrue(TeleportService2.isAbyssExitInstance(301720000, Race.ELYOS));
			assertTrue(TeleportService2.isAbyssExitInstance(310160000, Race.ELYOS));
			assertTrue(TeleportService2.isAbyssExitInstance(320160000, Race.ASMODIANS));

			// 黑暗普埃塔的配置出口是天族因特尔蒂卡/魔族贝鲁斯兰；其欧比斯出口是副本内传送 NPC，
			// 因此必须在入口门户上单独配置欧比斯入场任务前置。
			// Dark Poeta's configured exit is Heiron/Beluslan; its Abyss route is an in-instance portal,
			// so the Abyss entry requirement is attached to its entry portals instead.
			assertFalse(TeleportService2.isAbyssExitInstance(300040000, Race.ELYOS));
			assertFalse(TeleportService2.isAbyssExitInstance(300040000, Race.ASMODIANS));
			assertTrue(TeleportService2.grantsAbyssAccess(300040000, Race.ELYOS));
			assertTrue(TeleportService2.grantsAbyssAccess(300040000, Race.ASMODIANS));
			assertTrue(TeleportService2.grantsAbyssAccess(300050000, Race.ELYOS));
			assertFalse(TeleportService2.grantsAbyssAccess(310110000, Race.ELYOS));
		} finally {
			DataManager.INSTANCE_EXIT_DATA = previous;
		}
	}

	private static void assertEntryGate(Portal2Data data, int npcId, int dialogId, Race race, int questId) {
		PortalPath portalPath = data.getPortalDialog(npcId, dialogId, race);
		assertNotNull(portalPath, "missing portal path for NPC " + npcId + " dialog " + dialogId + " race " + race);
		assertEquals(3000400, portalPath.getLocId(), "NPC " + npcId + " must enter Dark Poeta");
		assertTrue(portalPath.isInstance(), "NPC " + npcId + " must enter an instance");

		PortalReq portalReq = portalPath.getPortalReq();
		assertNotNull(portalReq, "missing portal_req for NPC " + npcId);
		assertEquals(List.of(questId), portalReq.getQuestReq().stream().map(QuestReq::getQuestId).toList());
		assertEquals(List.of(0), portalReq.getQuestReq().stream().map(QuestReq::getQuestStep).toList());
	}

	private static boolean portalLocTargetsWorld(int locId, int worldId) throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(PORTAL_LOCS.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		String expression = "boolean(//portal_loc[@loc_id='%d' and @world_id='%d'])".formatted(locId, worldId);
		return "true".equals(xpath.evaluate(expression, document));
	}
}
