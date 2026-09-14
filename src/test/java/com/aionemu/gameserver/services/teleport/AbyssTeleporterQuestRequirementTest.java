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
import org.w3c.dom.Document;

import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.portal.PortalPath;
import com.aionemu.gameserver.model.templates.portal.PortalReq;
import com.aionemu.gameserver.model.templates.portal.PortalUse;
import com.aionemu.gameserver.model.templates.portal.QuestReq;

import jakarta.xml.bind.JAXBContext;

/**
 * 锁定主城欧比斯传送门必须完成对应阵营的入场任务。
 * Locks capital Abyss teleport gates to their racial entry quest completion.
 */
class AbyssTeleporterQuestRequirementTest {

	private static final Path PORTAL_TEMPLATES = Path.of(
			"src/main/resources/aion/data/static_data/portals/portal_template2.xml");
	private static final Path QUEST_DATA = Path.of(
			"src/main/resources/aion/data/static_data/quest_data/quest_data.xml");

	@Test
	void capitalGatesRequireTheRacialAbyssEntryQuest() throws Exception {
		Portal2Data data = (Portal2Data) JAXBContext.newInstance(Portal2Data.class)
				.createUnmarshaller().unmarshal(PORTAL_TEMPLATES.toFile());

		assertGate(data, 730059, 4000102, Race.ELYOS, 1044);
		assertGate(data, 730060, 4000103, Race.ELYOS, 1044);
		assertGate(data, 730061, 4000104, Race.ELYOS, 1044);
		assertGate(data, 730062, 4000105, Race.ASMODIANS, 2042);
		assertGate(data, 730063, 4000106, Race.ASMODIANS, 2042);
		assertGate(data, 730064, 4000107, Race.ASMODIANS, 2042);
	}

	@Test
	void entryQuestIsTheTerminalQuestOfEachRacialChain() throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(QUEST_DATA.toFile());

		assertChainLink(document, 1921, 1920);
		assertChainLink(document, 1922, 1921);
		assertChainLink(document, 1044, 1922);
		assertChainLink(document, 2946, 2945);
		assertChainLink(document, 2947, 2946);
		assertChainLink(document, 2042, 2947);
	}

	@Test
	void externalAbyssPortalsRequireTheRacialQuestOutsideInstances() {
		assertFalse(PortalService.isAbyssEntryAllowed(400010000, 110010000, false, false));
		assertTrue(PortalService.isAbyssEntryAllowed(400010000, 400010000, false, false));
		assertTrue(PortalService.isAbyssEntryAllowed(400010000, 320080000, true, false));
		assertTrue(PortalService.isAbyssEntryAllowed(400010000, 110010000, false, true));
	}

	private static void assertGate(Portal2Data data, int npcId, int locId, Race race, int questId) {
		PortalUse portalUse = data.getPortalUse(npcId);
		assertNotNull(portalUse, "missing portal_use for NPC " + npcId);

		PortalPath portalPath = portalUse.getPortalPath(race);
		assertNotNull(portalPath, "missing " + race + " portal path for NPC " + npcId);
		assertEquals(locId, portalPath.getLocId());
		assertEquals(race, portalPath.getRace());

		PortalReq portalReq = portalPath.getPortalReq();
		assertNotNull(portalReq, "missing portal_req for NPC " + npcId);
		assertEquals(45, portalReq.getMinLevel());
		assertEquals(List.of(questId), portalReq.getQuestReq().stream().map(QuestReq::getQuestId).toList());
		assertEquals(List.of(0), portalReq.getQuestReq().stream().map(QuestReq::getQuestStep).toList());
	}

	private static void assertChainLink(Document document, int questId, int prerequisiteQuestId) throws Exception {
		String expression = "boolean(/quests/quest[@id='%d']/start_conditions/finished[@quest_id='%d'])"
				.formatted(questId, prerequisiteQuestId);
		assertEquals("true", XPathFactory.newInstance().newXPath().evaluate(expression, document),
				"quest " + questId + " should require quest " + prerequisiteQuestId);
	}
}
