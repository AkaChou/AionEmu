package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;

/**
 * 表车道 native 掉落的跨族护栏（2026-10-08，第三/四例同因事故后补）：退役 XML 的 {@code <drops>}
 * 随 catalog 退场后，每一个 {@code owner=RETAIL_TABLE} 且声明真端 {@code drop_*} 列的行都必须落在
 * **已注册掉落的车道**里（Talk / Collect / UseItem / DataDriven 四族——各族从 quest.xml 列在 native
 * 侧注册，经 {@code QuestEngine#questDrops} 聚合），且在其所属车道路由（DD 的冻结 ∩ drop 3 行钉为
 * 显式惰性残差）；{@code XML_RETENTION} 行保持单一 owner，由目录供源，native 四车道不得路由。
 * <p>
 * 任何新的表车道族（或行迁移）带入 drop 列时本门翻红，强制同批接线掉落面与族门。不属台账的行
 * （非生产全集）不在本门范围。
 * <p>
 * Cross-family guard for the native drop faces: every RETAIL_TABLE row declaring retail drop columns
 * must belong to a lane that registers drops and must be routed there (the DD freeze is pinned as a
 * lazy residue), while XML_RETENTION rows stay catalog-only (single owner). A new table-lane family
 * carrying drop columns turns this gate red until its drop face is wired and gated.
 */
class RetailDropLaneCoverageGateTest {

	/** 台账资源（测试副本；与主副本逐字节一致由 RetailOwnershipGateTest 守护）。 / The retention ledger. */
	private static final String RETENTION = "/quest/retail-xml-retention.xml";
	/** 已注册掉落的车道族（其余族真端 drop 列 0 行；出现即新缺口）。 / The drop-registering lane families. */
	private static final Set<String> DROP_LANES =
		Set.of("SimpleTalk", "SimpleCollectItem", "SimpleUseItem", "DataDriven");
	/** DD 冻结 ∩ drop 的惰性残差（ZONE_ABSENT；解冻后此处翻红强制复核与重生成证据）。 */
	private static final Set<Integer> DD_FROZEN_DROP_RESIDUE = Set.of(15602, 15605, 15608);

	@Test
	void everyNativeDropRowBelongsToARoutedRegisteredLane() throws Exception {
		Map<Integer, String[]> ledger = new TreeMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION, "quest")) {
			ledger.put(RetailLedgerRows.intCell(row, "quest_id"),
				new String[] {RetailLedgerRows.cell(row, "owner"), RetailLedgerRows.cell(row, "family")});
		}
		NativeQuestXmlTable questXml = NativeQuestXmlTable.instance();
		SimpleTalkHandler talk = SimpleTalkHandler.instance();
		SimpleCollectItemHandler collect = SimpleCollectItemHandler.instance();
		SimpleUseItemHandler useItem = SimpleUseItemHandler.instance();
		DataDrivenNativeRuntime dataDriven = DataDrivenNativeRuntime.instance();

		Map<String, Integer> histogram = new TreeMap<>();
		Set<Integer> xmlRetained = new TreeSet<>();
		Set<Integer> nativeDropRows = new TreeSet<>();
		for (Map.Entry<Integer, String[]> entry : ledger.entrySet()) {
			int questId = entry.getKey();
			if (!questXml.hasRetailDropColumns(questId)) {
				continue;
			}
			String owner = entry.getValue()[0];
			if ("XML_RETENTION".equals(owner)) {
				xmlRetained.add(questId);
				continue;
			}
			assertEquals("RETAIL_TABLE", owner, "台账 owner 只允许 RETAIL_TABLE/XML_RETENTION: " + questId);
			histogram.merge(entry.getValue()[1], 1, Integer::sum);
			nativeDropRows.add(questId);
		}
		// ① 车道归属闭合：带 drop 列的 native 行必须落在四张已注册车道之一（新族出现即红）。
		for (String family : histogram.keySet()) {
			assertTrue(DROP_LANES.contains(family),
				"带真端 drop 列的 native 行出现在未注册掉落的族：" + family);
		}
		assertEquals(Map.of("SimpleTalk", 654, "SimpleCollectItem", 177, "SimpleUseItem", 1,
			"DataDriven", 163), histogram, "native drop 行按族冻结（2026-10-08 复算）");
		// SimpleTalk 654 含 4105：其掉落只写在第 2 槽（drop_monster_2/drop_item_2），预筛放宽到任意槽后才注册。
		assertEquals(163, xmlRetained.size(), "XML_RETENTION ∩ drop 列冻结（目录供源）");
		// ② 路由闭合：每行在所属车道路由（DD 冻结 ∩ drop 的 3 行 = 显式惰性残差）。
		for (int questId : nativeDropRows) {
			String family = ledger.get(questId)[1];
			if (DD_FROZEN_DROP_RESIDUE.contains(questId)) {
				assertEquals("DataDriven", family, "惰性残差必须属于 DataDriven 族: " + questId);
				assertTrue(dataDriven.owns(questId), "残差行仍在 DD 注册集: " + questId);
				assertTrue(dataDriven.frozenQuestIds().containsKey(questId), "残差行必须在冻结桶: " + questId);
				assertFalse(dataDriven.routes(questId), "残差行不可路由（不可达 ⇒ 掉落惰性）: " + questId);
				continue;
			}
			boolean routed = switch (family) {
				case "SimpleTalk" -> talk.routes(questId);
				case "SimpleCollectItem" -> collect.routes(questId);
				case "SimpleUseItem" -> useItem.routes(questId);
				case "DataDriven" -> dataDriven.routes(questId);
				default -> false;
			};
			assertTrue(routed, "带 drop 列的 native 行必须在所属车道路由（否则掉落面缺供）："
				+ family + " " + questId);
		}
		// ③ 单一 owner 负例：XML_RETENTION 行不得由 native 四车道路由。
		for (int questId : xmlRetained) {
			assertFalse(talk.routes(questId), "XML_RETENTION 行不得由 native 供源（Talk）: " + questId);
			assertFalse(collect.routes(questId), "XML_RETENTION 行不得由 native 供源（Collect）: " + questId);
			assertFalse(useItem.routes(questId), "XML_RETENTION 行不得由 native 供源（UseItem）: " + questId);
			assertFalse(dataDriven.routes(questId), "XML_RETENTION 行不得由 native 供源（DD）: " + questId);
		}
	}
}
