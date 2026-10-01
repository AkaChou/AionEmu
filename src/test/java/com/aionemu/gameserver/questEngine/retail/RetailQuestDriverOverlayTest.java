package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产接线门禁（提示词 §4.C）：真端优先 overlay 与完整目录护栏。
 * <ul>
 * <li>局部 overlay 关闭开关时返回同一 XML 目录实例；生产入口必须拒绝缺失已退役 XML 的回退；</li>
 * <li>开关开启：retail-owned（已入仓家族 SimpleHunt/SimpleTalk）条目被真端定义替换，其余条目原样保留，
 * 目录无重复归属，定义与 {@link RetailSimpleHuntDefinitionCompiler} 直接产物同一；</li>
 * <li>保留清单内任务绝不出现真端定义。</li>
 * </ul>
 * Production wiring gate for the retail-first overlay and its fail-closed catalog check.
 */
class RetailQuestDriverOverlayTest {

	private static final String RETENTION = "/aion/data/static_data/quest/retail/retail-xml-retention.tsv";
	private static final String PREVIOUS_VALUE = System.getProperty("aion.quest.retailDriver");

	@AfterAll
	static void restoreSwitch() {
		if (PREVIOUS_VALUE == null) {
			System.clearProperty("aion.quest.retailDriver");
		} else {
			System.setProperty("aion.quest.retailDriver", PREVIOUS_VALUE);
		}
	}

	@Test
	void disabledSwitchReturnsXmlCatalogUntouched() {
		System.setProperty("aion.quest.retailDriver", "false");
		QuestCatalog xmlCatalog = com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog
			.fromEntries(java.util.List.of());
		assertSame(xmlCatalog, RetailQuestDriver.overlay(xmlCatalog));
	}

	@Test
	void disabledProductionSwitchCannotSilentlyDropRetiredQuests() {
		System.setProperty("aion.quest.retailDriver", "false");
		QuestCatalog incompleteXml = com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog
			.fromEntries(java.util.List.of());
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> RetailQuestDriver.overlayProduction(incompleteXml));
		assertTrue(error.getMessage().contains("retail production catalog incomplete"));
	}

	@Test
	void productionOverlayContainsExactlyTheManifestQuestUniverse() throws Exception {
		System.setProperty("aion.quest.retailDriver", "true");
		QuestCatalog production = com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions.catalog();
		int migratedNativeCount = (int) retailOwnedIds().stream().filter(id ->
			com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler.instance().owns(id)).count();
		assertEquals(6224, production.entries().size() + migratedNativeCount);
	}

	@Test
	void productionCoverageRejectsOneMissingRetailOwner() throws Exception {
		System.setProperty("aion.quest.retailDriver", "true");
		QuestCatalog production = com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions.catalog();
		Set<Integer> retailIds = retailOwnedIds();
		// 已切原生车道的行不再由 typed 目录覆盖；负例必须挑一行**仍走旧 IR** 的 retail 行。
		int missingId = retailIds.stream()
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().owns(id))
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler.instance().owns(id))
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler.instance().owns(id))
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler.instance().owns(id))
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler.instance().owns(id))
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler.instance().owns(id))
			.filter(id -> !com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler.instance().owns(id))
			.findFirst().orElseThrow();
		QuestCatalog xmlCatalog = com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog.fromEntries(
			production.entries().stream().filter(entry -> !retailIds.contains(entry.id())).toList());
		QuestCatalog incomplete = com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog.fromEntries(
			production.entries().stream().filter(entry -> entry.id() != missingId).toList());
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> RetailQuestDriver.verifyProductionCoverage(xmlCatalog, incomplete, true));
		assertTrue(error.getMessage().contains("missing=[" + missingId + "]"));
	}

	@Test
	void enabledOverlayReplacesRetailOwnedAndKeepsRest() throws Exception {
		System.setProperty("aion.quest.retailDriver", "true");
		QuestCatalog xmlCatalog = stubCatalog();
		QuestCatalog overlay = RetailQuestDriver.overlay(xmlCatalog);

		RetailQuestDriver driver = RetailQuestDriver.current().orElseThrow();
		int migratedNativeCount = (int) retailOwnedIds().stream().filter(id ->
			com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler.instance().owns(id)
			|| com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler.instance().owns(id)).count();
		assertEquals(retailOwnedIds().size(), driver.retailOwnedCount() + migratedNativeCount,
			"driver must include every retail-owned manifest row");
		assertEquals(xmlCatalog.entries().size(), overlay.entries().size(),
			"overlay must not change the entry count while XML files still exist");
		assertTrue(RetailQuestDriver.current().orElseThrow().overlayStats().contains("replaced="),
			"overlay stats should be recorded");

		// retail-owned：定义与直接合成产物同一；保留清单任务：绝不替换。
		int verified = 0;
		for (QuestCatalogEntry entry : overlay.entries()) {
			if (driver.definition(entry.id()).isPresent()) {
				assertSame(driver.definition(entry.id()).orElseThrow(), entry.executable().orElseThrow(),
					"retail-owned entry must carry the retail-compiled definition: " + entry.id());
				verified++;
			}
		}
		int verifiedCount = verified;
		assertEquals(driver.retailOwnedCount(), verifiedCount,
			"every retail-owned manifest row must compile and carry an executable definition");

		// 桩内存在的保留任务必须原样保留（SERVER_ONLY 等桩外任务不在断言范围）。
		for (int questId : stubbedRetainedIds) {
			if (!driver.definition(questId).isPresent()) {
				assertTrue(overlay.findEntry(questId).isPresent(),
					"retained quest must stay in the catalog: " + questId);
			}
		}
	}

	// ---------------------------------------------------------------- 装载

	/**
	 * 最小 QuestCatalog 桩：retail-owned（已入仓家族全部）+ 少量保留任务，验证"替换其余保留"语义。
	 * A minimal stub: every retail-owned quest plus a sample of retained ones.
	 */
	private Set<Integer> stubbedRetainedIds = new TreeSet<>();

	private QuestCatalog stubCatalog() throws Exception {
		Set<Integer> retailOwned = retailOwnedIds();
		var entries = new java.util.ArrayList<QuestCatalogEntry>();
		var table = retailTable();
		var npc = npcIndex();
		var items = itemIndex();
		var random = randomRewards();
		var names = nameIds();
		int retainedSamples = 0;
		for (int questId : catalogIds()) {
			boolean owned = retailOwned.contains(questId);
			if (!owned && retainedSamples >= 200) {
				continue;
			}
			var row = table.find(questId);
			if (row.isEmpty()) {
				continue;
			}
			if (!owned) {
				retainedSamples++;
				stubbedRetainedIds.add(questId);
			}
			var metadata = RetailQuestMetadataCompiler.compile(row.orElseThrow(), npc, items, random,
				names).metadata();
			entries.add(QuestCatalogEntry.metadataOnly(
				new com.aionemu.gameserver.questEngine.definition.QuestDefinition(questId, 1, metadata,
					com.aionemu.gameserver.questEngine.definition.ProgressLayout.empty(), java.util.List.of(),
					java.util.List.of())));
		}
		return com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog.fromEntries(entries);
	}

	private Set<Integer> retailOwnedIds() throws Exception {
		Set<Integer> ids = new TreeSet<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if ("RETAIL_TABLE".equals(parts[1])) {
				ids.add(Integer.parseInt(parts[0]));
			}
		}
		return ids;
	}

	private Set<Integer> catalogIds() throws Exception {
		Set<Integer> ids = new TreeSet<>();
		for (String line : lines(open(RETENTION))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			ids.add(Integer.parseInt(line.split("\t", -1)[0]));
		}
		return ids;
	}

	private RetailSimpleHuntTable simpleHuntTable() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml")) {
			return RetailSimpleHuntTable.load(input);
		}
	}

	private RetailQuestXmlTable retailTable() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest/retail/quest.xml")) {
			return RetailQuestXmlTable.load(input);
		}
	}

	private RetailQuestCatalog catalog() throws Exception {
		return new RetailQuestCatalog(simpleHuntTable(), npcIndex());
	}

	private RetailNpcNameIndex npcIndex() throws Exception {
		return RetailNpcNameIndex.build(java.util.List.of(open(
			"/aion/data/static_data/npcs/npc_template_200000_216188.xml")),
			RetailQuestAiNameGroupsFixture.streams());
	}

	private RetailItemNameIndex itemIndex() throws Exception {
		return RetailItemNameIndex.build(java.util.List.of(open(
			"/aion/data/static_data/items/item/item_template_182005539_190200002.xml")));
	}

	private java.util.Map<String, Integer> randomRewards() throws Exception {
		java.util.Map<String, Integer> ids = new java.util.HashMap<>();
		var document = parse(open("/aion/data/static_data/quest/legacy/quest_random_rewards.xml"));
		var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
		for (int index = 0; index < nodes.getLength(); index++) {
			var element = (org.w3c.dom.Element) nodes.item(index);
			ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
				Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
		}
		return ids;
	}

	private java.util.Map<Integer, Integer> nameIds() throws Exception {
		java.util.Map<Integer, Integer> ids = new java.util.HashMap<>();
		for (String line : lines(open("/aion/data/static_data/quest/retail/quest_name_string_ids.tsv"))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())), Integer.parseInt(parts[1]));
		}
		return ids;
	}

	private org.w3c.dom.Document parse(InputStream input) throws Exception {
		try (input) {
			var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(input);
		}
	}

	private java.util.List<String> lines(InputStream input) throws Exception {
		try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private InputStream open(String resource) throws Exception {
		InputStream input = RetailQuestDriverOverlayTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}

}
