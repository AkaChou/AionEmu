package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "真端优先 / XML 降级" 判定与表驱动击杀推进测试。
 * Tests for the retail-first / XML-fallback decision and table-driven kill advance.
 */
class RetailQuestCatalogTest {

	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final List<String> NPC_TEMPLATES = List.of(
		"npc_template_200000_216188.xml", "npc_template_216189_235748.xml", "npc_template_235749_247606.xml",
		"npc_template_247607_270057.xml", "npc_template_270058_286320.xml", "npc_template_286321_800030.xml",
		"npc_template_800031_834289.xml", "npc_template_834290_885645.xml");

	@Test
	void decidesRetailTableVersusXmlFallback() throws Exception {
		RetailQuestCatalog catalog = catalog();

		// 真端 SimpleHunt 行存在 → 表驱动（无需 XML）
		assertTrue(catalog.isTableDriven(1517));
		assertEquals(RetailQuestCatalog.Source.RETAIL_TABLE, catalog.sourceOf(1517, true));

		// 真端 SimpleHunt 无行但本仓库有 XML → 降级（例如 DataDriven / 脚本任务）
		assertFalse(catalog.isTableDriven(10501));
		assertEquals(RetailQuestCatalog.Source.XML_FALLBACK, catalog.sourceOf(10501, true));

		// 两边都没有 → 未移植
		assertEquals(RetailQuestCatalog.Source.UNKNOWN, catalog.sourceOf(999999, false));
		assertEquals(1859, catalog.retailSimpleHuntRows());
	}

	@Test
	void advancesKillCountersStraightFromTheRetailTable() throws Exception {
		RetailQuestCatalog catalog = catalog();

		// 1102：count1=3，怪 CherubimL_1_n/CherubimL_2_n（210133/210134）
		int packed = 0;
		for (int kill = 1; kill <= 3; kill++) {
			packed = catalog.onKill(1102, 210133, packed).orElseThrow();
			assertEquals(kill, packed, "table-driven counter follows Section_0");
		}
		assertTrue(catalog.onKill(1102, 210133, packed).isEmpty(), "counter stops at countN");
		assertTrue(catalog.simpleHuntPlan(1102).orElseThrow().isComplete(packed));

		// 不受理的怪不算数
		assertTrue(catalog.onKill(1102, 210999, 0).isEmpty());

		// 1517：两个槽位，槽位 2 的步长是 1<<6
		int two = catalog.onKill(1517, 214112, 0).orElseThrow();   // monster2 组
		assertEquals(1 << 6, two);
		two = catalog.onKill(1517, 214118, two).orElseThrow();    // monster1 组
		assertEquals((1 << 6) + 1, two);
	}

	private static RetailQuestCatalog catalog() throws Exception {
		RetailSimpleHuntTable table = RetailSimpleHuntTableTest.load();
		List<InputStream> streams = new ArrayList<>();
		try {
			for (String name : NPC_TEMPLATES) {
				InputStream input = RetailQuestCatalogTest.class.getResourceAsStream(NPC_DIR + name);
				assertNotNull(input, "missing npc template " + name);
				streams.add(input);
			}
			return new RetailQuestCatalog(table, RetailNpcNameIndex.build(streams, RetailQuestAiNameGroupsFixture.streams()));
		} finally {
			for (InputStream stream : streams) {
				stream.close();
			}
		}
	}
}
