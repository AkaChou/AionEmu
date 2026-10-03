package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产接线门禁（提示词 §4.C）：真端优先 overlay 与完整目录护栏（P7 步 f 后口径）。
 * <ul>
 * <li>局部 overlay 关闭开关时返回同一 XML 目录实例；生产入口必须拒绝缺失已退役 XML 的回退；</li>
 * <li>生产全集恒等式：6224 = 生产目录条目 + 原生覆盖（七族 handler owns ∨ DD 运行时 owned——
 *     可路由或**显式冻结**）；</li>
 * <li>覆盖负例：目录缺一个 XML_RETENTION 行 ⇒ 拒启；</li>
 * <li>overlay 直通：开启开关后目录内容不变（旧 IR 编译产物已随 DD 车道删除）。</li>
 * </ul>
 * Production wiring gate for the retail-first overlay and its fail-closed catalog check (post step f).
 */
class RetailQuestDriverOverlayTest {

	private static final String RETENTION = "/aion/data/static_data/quest/retail/retail-xml-retention.xml";
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
		// 生产全集恒等式（P7 步 f）：6224 = 目录条目 + 原生覆盖；DD 运行时 owned 含可路由 1444 与
		// 显式冻结 23（FreezeReason 登记 §10.3 = 非静默丢弃）。
		// Universe identity: catalog entries + native coverage (family handlers + the DD runtime,
		// whose owned set includes routed and explicitly frozen rows).
		int nativeCovered = 0;
		for (int questId : retailOwnedIds()) {
			boolean covered = com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler.instance().owns(questId)
				|| com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance().owns(questId);
			if (covered) {
				nativeCovered++;
			}
		}
		assertEquals(6224, production.entries().size() + nativeCovered);
		assertEquals(1467,
			com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime.instance().ownedQuestIds().size(),
			"DD 运行时必须接管 DataDriven 全部 1467 行（routed 1444 + frozen 23）");
	}

	@Test
	void productionCoverageRejectsOneMissingXmlRetainedRow() throws Exception {
		System.setProperty("aion.quest.retailDriver", "true");
		QuestCatalog production = com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions.catalog();
		// 负例改挑 XML_RETENTION 行（P7 步 f 后 RETAIL_TABLE 行全部原生覆盖，目录缺行只可能发生在
		// XML 保留侧）；缺行必须拒启并点名缺的 id。
		// Negative case uses an XML_RETENTION row (every RETAIL_TABLE row is native-covered now).
		int resolved = -1;
		for (int questId : catalogIds()) {
			String owner = retentionOwners().get(questId);
			if ("XML_RETENTION".equals(owner) && production.findEntry(questId).isPresent()) {
				resolved = questId;
				break;
			}
		}
		final int missingId = resolved;
		assertTrue(missingId > 0, "必须能挑到一个 XML_RETENTION 负例行");
		QuestCatalog incomplete = com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog.fromEntries(
			production.entries().stream().filter(entry -> entry.id() != missingId).toList());
		IllegalStateException error = assertThrows(IllegalStateException.class,
			() -> RetailQuestDriver.verifyProductionCoverage(incomplete, incomplete, true));
		assertTrue(error.getMessage().contains("missing=[" + missingId + "]"));
	}

	@Test
	void enabledOverlayPassesTheCatalogThroughUntouched() throws Exception {
		System.setProperty("aion.quest.retailDriver", "true");
		QuestCatalog xmlCatalog = stubCatalog();
		QuestCatalog overlay = RetailQuestDriver.overlay(xmlCatalog);

		// 旧 IR 编译车道已退场：overlay 直通，条目数与内容不变。
		// The old IR compile lane is gone: the overlay passes the catalog through untouched.
		assertEquals(xmlCatalog.entries().size(), overlay.entries().size(),
			"overlay must not change the entry count");
		java.util.Iterator<QuestCatalogEntry> expected = xmlCatalog.entries().iterator();
		java.util.Iterator<QuestCatalogEntry> actual = overlay.entries().iterator();
		while (expected.hasNext()) {
			QuestCatalogEntry entry = expected.next();
			assertSame(entry, actual.next(), "overlay must keep every entry instance: " + entry.id());
		}
		assertNotNull(RetailQuestDriver.current().orElseThrow().retailMetadataOf(10033).orElseThrow(),
			"元数据底座仍可复算 DD 行的 quest.xml 元数据");
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
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			if ("RETAIL_TABLE".equals(RetailLedgerRows.cell(row, "owner"))) {
				ids.add(Integer.parseInt(RetailLedgerRows.cell(row, "quest_id")));
			}
		}
		return ids;
	}

	private Map<Integer, String> retentionOwners() throws Exception {
		Map<Integer, String> owners = new java.util.HashMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			owners.putIfAbsent(Integer.parseInt(RetailLedgerRows.cell(row, "quest_id")),
				RetailLedgerRows.cell(row, "owner"));
		}
		return owners;
	}

	private Set<Integer> catalogIds() throws Exception {
		Set<Integer> ids = new TreeSet<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			ids.add(Integer.parseInt(RetailLedgerRows.cell(row, "quest_id")));
		}
		return ids;
	}

	private RetailQuestXmlTable retailTable() throws Exception {
		try (InputStream input = open("/aion/data/static_data/quest/retail/quest.xml")) {
			return RetailQuestXmlTable.load(input);
		}
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
		for (Element row : RetailLedgerRows.rows(
				"/aion/data/static_data/quest/retail/quest_name_string_ids.xml", "name_string_id")) {
			ids.put(Integer.parseInt(RetailLedgerRows.cell(row, "key").substring("STR_QUEST_NAME_Q".length())),
				Integer.parseInt(RetailLedgerRows.cell(row, "string_id")));
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

	private InputStream open(String resource) throws Exception {
		InputStream input = RetailQuestDriverOverlayTest.class.getResourceAsStream(resource);
		assertNotNull(input, "missing resource " + resource);
		return input;
	}

}
