package com.aionemu.gameserver.services.teleport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aionemu.gameserver.dataholders.MultiReturnItemData;
import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.teleport.MultiReturn;
import com.aionemu.gameserver.model.templates.teleport.MultiReturnLocationList;
import com.aionemu.gameserver.model.templates.teleport.TeleportLocation;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import jakarta.xml.bind.JAXBContext;

class TeleportServiceQuestRequirementTest {

	private static final Path TELEPORTER_XML = Path.of("src/main/resources/aion/data/static_data/npc_teleporter.xml");
	private static final Path ITEM_TEMPLATES_XML = Path.of(
			"src/main/resources/aion/data/static_data/items/item/item_template_152209118_182005538.xml");
	private static final Path MULTI_RETURNS_XML = Path.of(
			"src/main/resources/aion/data/static_data/items/multi_returns.xml");
	private static final String ABYSS_RETURN_ITEMS_XPATH = "//item_template[@return_world='400010000']";
	private static final String BALAUREA_RETURN_ITEMS_XPATH =
			"//item_template[@return_world='210050000' or @return_world='210130000' or @return_world='220070000' or @return_world='220140000']";

	@Test
	void completedQuestAlwaysMeetsRequirement() {
		assertTrue(TeleportService2.meetsQuestRequirement(questState(10520, QuestStatus.COMPLETE, 0), 0));
	}

	@Test
	void configuredProgressStepAllowsActiveAndRewardQuest() {
		assertTrue(TeleportService2.meetsQuestRequirement(questState(10520, QuestStatus.START, 4), 4));
		assertTrue(TeleportService2.meetsQuestRequirement(questState(10520, QuestStatus.REWARD, 6), 4));
		assertFalse(TeleportService2.meetsQuestRequirement(questState(10520, QuestStatus.START, 3), 4));
		assertFalse(TeleportService2.meetsQuestRequirement(questState(10520, QuestStatus.START, 4), 0));
		assertFalse(TeleportService2.meetsQuestRequirement(null, 4));
	}

	@Test
	void abyssEntryRequiresTheTerminalQuestOfTheRacialChain() {
		assertEquals(1044, TeleportService2.getAbyssEntryQuestId(Race.ELYOS));
		assertEquals(2042, TeleportService2.getAbyssEntryQuestId(Race.ASMODIANS));
		assertEquals(0, TeleportService2.getAbyssEntryQuestId(Race.PC_ALL));

		assertTrue(TeleportService2.meetsAbyssEntryRequirement(Race.ELYOS,
				questState(1044, QuestStatus.COMPLETE, 0)));
		assertTrue(TeleportService2.meetsAbyssEntryRequirement(Race.ASMODIANS,
				questState(2042, QuestStatus.COMPLETE, 0)));
		// 链中段任务完成不构成入场资格。 / Completing a mid-chain quest does not qualify for entry.
		assertFalse(TeleportService2.meetsAbyssEntryRequirement(Race.ELYOS,
				questState(1922, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsAbyssEntryRequirement(Race.ASMODIANS,
				questState(2947, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsAbyssEntryRequirement(Race.ELYOS,
				questState(1044, QuestStatus.START, 9)));
		assertFalse(TeleportService2.meetsAbyssEntryRequirement(Race.ELYOS,
				questState(1044, QuestStatus.REWARD, 9)));
		assertFalse(TeleportService2.meetsAbyssEntryRequirement(Race.ASMODIANS, null));
	}

	@Test
	void balaureaEntryRequiresTheRacialMissionToBeCompleteOrBoardedShip() {
		assertEquals(10031, TeleportService2.getBalaureaEntryQuestId(Race.ELYOS));
		assertEquals(20031, TeleportService2.getBalaureaEntryQuestId(Race.ASMODIANS));
		assertEquals(3, TeleportService2.getBalaureaEntryQuestStep(Race.ELYOS));
		assertEquals(3, TeleportService2.getBalaureaEntryQuestStep(Race.ASMODIANS));
		assertEquals(0, TeleportService2.getBalaureaEntryQuestId(Race.PC_ALL));

		// 完成使命允许传送 / Complete quest allows entry
		assertTrue(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ELYOS,
				questState(10031, QuestStatus.COMPLETE, 0)));
		assertTrue(TeleportService2.meetsBalaureaEntryRequirement(Race.ASMODIANS, Race.ASMODIANS,
				questState(20031, QuestStatus.COMPLETE, 0)));

		// 登舰前步骤（var0 = 0, 1, 2）未激活龙界传送资格 / Steps before boarding ship disallowed
		assertFalse(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ELYOS,
				questState(10031, QuestStatus.START, 0)));
		assertFalse(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ELYOS,
				questState(10031, QuestStatus.START, 2)));

		// 登舰后及英吉斯温内后续步骤（var0 >= 3）均允许传送 / Boarded airship and beyond allowed
		assertTrue(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ELYOS,
				questState(10031, QuestStatus.START, 3)));
		assertTrue(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ELYOS,
				questState(10031, QuestStatus.START, 4)));
		assertTrue(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ELYOS,
				questState(10031, QuestStatus.REWARD, 10)));
		assertTrue(TeleportService2.meetsBalaureaEntryRequirement(Race.ASMODIANS, Race.ASMODIANS,
				questState(20031, QuestStatus.START, 3)));

		// 异族不满足 / Opposing race is disallowed
		assertFalse(TeleportService2.meetsBalaureaEntryRequirement(Race.ELYOS, Race.ASMODIANS,
				questState(10031, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsBalaureaEntryRequirement(Race.ASMODIANS, Race.ELYOS,
				questState(20031, QuestStatus.COMPLETE, 0)));
		assertFalse(TeleportService2.meetsBalaureaEntryRequirement(Race.ASMODIANS, Race.ASMODIANS, null));
	}

	@Test
	void everyDirectAbyssReturnItemUsesTheGuardedSkillPath() throws Exception {
		NodeList items = abyssReturnItems();
		assertEquals(4, items.getLength());

		Map<Integer, Race> expectedItems = Map.of(
				164000097, Race.ELYOS,
				164000098, Race.ASMODIANS,
				164000403, Race.ELYOS,
				164000404, Race.ASMODIANS);
		for (int index = 0; index < items.getLength(); index++) {
			Element item = (Element) items.item(index);
			int itemId = Integer.parseInt(item.getAttribute("id"));
			assertEquals(expectedItems.get(itemId), Race.valueOf(item.getAttribute("race")), "item " + itemId);
			assertEquals(1, evaluateCount(item, "./actions/skilluse[@skillid='8198']"), "item " + itemId);
		}
	}

	@Test
	void everyDirectBalaureaReturnItemUsesTheGuardedSkillPath() throws Exception {
		NodeList items = balaureaReturnItems();
		assertEquals(4, items.getLength());

		Map<Integer, Race> expectedItems = Map.of(
				164000106, Race.ELYOS,
				164000107, Race.ASMODIANS,
				164000532, Race.ELYOS,
				164000533, Race.ASMODIANS);
		for (int index = 0; index < items.getLength(); index++) {
			Element item = (Element) items.item(index);
			int itemId = Integer.parseInt(item.getAttribute("id"));
			assertEquals(expectedItems.get(itemId), Race.valueOf(item.getAttribute("race")), "item " + itemId);
			assertEquals(1, evaluateCount(item, "./actions/skilluse[@skillid='8198']"), "item " + itemId);
		}
	}

	@Test
	void noInggisonReturnItemTargetsTheMasterWorld() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ITEM_TEMPLATES_XML.toFile());
		NodeList masterItems = (NodeList) XPathFactory.newInstance().newXPath().evaluate(
				"//item_template[@return_world='210130000']", document, XPathConstants.NODESET);

		assertEquals(0, masterItems.getLength());
	}

	@Test
	void abyssMultiReturnDestinationsRemainBehindTheItemActionGate() throws Exception {
		MultiReturnItemData data = (MultiReturnItemData) JAXBContext.newInstance(MultiReturnItemData.class)
				.createUnmarshaller().unmarshal(MULTI_RETURNS_XML.toFile());

		assertHasDestination(findMultiReturn(data, 6), 400010000);
		assertHasDestination(findMultiReturn(data, 7), 400010000);
		assertHasDestination(findMultiReturn(data, 8), 400010000);
		assertHasDestination(findMultiReturn(data, 9), 400010000);

		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ITEM_TEMPLATES_XML.toFile());
		var xpath = XPathFactory.newInstance().newXPath();
		assertMultiReturnItems((NodeList) xpath.evaluate(
				"//item_template/actions/multireturn[@id='6']/parent::actions/parent::item_template",
				document, XPathConstants.NODESET), Race.ELYOS, List.of(164020005, 164020007, 164020009));
		assertMultiReturnItems((NodeList) xpath.evaluate(
				"//item_template/actions/multireturn[@id='7']/parent::actions/parent::item_template",
				document, XPathConstants.NODESET), Race.ASMODIANS, List.of(164020006, 164020008, 164020010));
	}

	@Test
	void balaureaMultiReturnDestinationsRemainBehindTheItemActionGate() throws Exception {
		MultiReturnItemData data = (MultiReturnItemData) JAXBContext.newInstance(MultiReturnItemData.class)
				.createUnmarshaller().unmarshal(MULTI_RETURNS_XML.toFile());

		assertHasDestination(findMultiReturn(data, 6), 210050000);
		assertHasDestination(findMultiReturn(data, 7), 220070000);
	}

	@Test
	void capitalTeleportersAllowSanctuaryReturnFromInstanceRollbackStep() throws Exception {
		TeleporterData data = (TeleporterData) JAXBContext.newInstance(TeleporterData.class)
				.createUnmarshaller().unmarshal(TELEPORTER_XML.toFile());

		assertQuestGate(data, 203726, 444, 10520, 4);
		assertQuestGate(data, 204191, 438, 20520, 4);
		assertEquals(0, data.getTeleporterTemplateByNpcId(804561).getTeleLocIdData()
				.getTeleportLocation(444).getRequiredQuestStep());
	}

	private static void assertQuestGate(TeleporterData data, int npcId, int locationId, int questId, int questStep) {
		TeleportLocation location = data.getTeleporterTemplateByNpcId(npcId).getTeleLocIdData().getTeleportLocation(locationId);
		assertEquals(questId, location.getRequiredQuest());
		assertEquals(questStep, location.getRequiredQuestStep());
	}

	private static NodeList abyssReturnItems() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ITEM_TEMPLATES_XML.toFile());
		return (NodeList) XPathFactory.newInstance().newXPath().evaluate(ABYSS_RETURN_ITEMS_XPATH,
				document, XPathConstants.NODESET);
	}

	private static NodeList balaureaReturnItems() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ITEM_TEMPLATES_XML.toFile());
		return (NodeList) XPathFactory.newInstance().newXPath().evaluate(BALAUREA_RETURN_ITEMS_XPATH,
				document, XPathConstants.NODESET);
	}

	private static int evaluateCount(Element root, String expression) throws Exception {
		return ((NodeList) XPathFactory.newInstance().newXPath().evaluate(expression, root,
				XPathConstants.NODESET)).getLength();
	}

	private static void assertHasDestination(MultiReturn multiReturn, int worldId) {
		assertNotNull(multiReturn);
		List<MultiReturnLocationList> locations = multiReturn.getMultiReturnList();
		assertNotNull(locations);
		assertTrue(locations.stream().anyMatch(location -> location.getWorldId() == worldId));
	}

	private static MultiReturn findMultiReturn(MultiReturnItemData data, int id) {
		assertNotNull(data.getMultiReturns());
		return data.getMultiReturns().stream().filter(item -> item.getId() == id).findFirst().orElse(null);
	}

	private static void assertMultiReturnItems(NodeList items, Race expectedRace, List<Integer> expectedIds) {
		assertEquals(expectedIds.size(), items.getLength());
		List<Integer> actualIds = new java.util.ArrayList<>();
		for (int index = 0; index < items.getLength(); index++) {
			Element item = (Element) items.item(index);
			assertEquals(expectedRace, Race.valueOf(item.getAttribute("race")));
			actualIds.add(Integer.parseInt(item.getAttribute("id")));
		}
		assertEquals(expectedIds, actualIds);
	}

	private static QuestState questState(int questId, QuestStatus status, int questVar) {
		return new QuestState(questId, status, questVar, 0, null, null, null);
	}
}
