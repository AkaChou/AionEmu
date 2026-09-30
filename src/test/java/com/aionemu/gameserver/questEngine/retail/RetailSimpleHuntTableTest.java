package com.aionemu.gameserver.questEngine.retail;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真端 SimpleHunt 模板表 + 计数器语义回放（期望值来自 ScriptDLL64 反编译校验）。
 * Retail SimpleHunt table and counter semantics replay, with expectations derived from the decompile.
 */
class RetailSimpleHuntTableTest {

	private static final String TABLE = "/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml";

	@Test
	void loadsTheRetailTemplateTable() throws Exception {
		RetailSimpleHuntTable table = load();
		// 真端表 1,865 行；其中 6 行既无 countN 也无 monsterN（开发/占位行），加载器按"无计数器"跳过。
		assertEquals(1859, table.size(), "retail SimpleHunt rows carrying at least one counter slot");

		RetailSimpleHuntTable.Entry quest = table.find(1102).orElseThrow();
		assertEquals(1, quest.counters().size());
		assertEquals(3, quest.counter(1).orElseThrow().required());
		assertEquals(List.of("CherubimL_1_n", "CherubimL_2_n"), quest.counter(1).orElseThrow().monsters());
		assertEquals("Mires", quest.acquiredNpc());
		assertEquals("Mires", quest.rewardNpc());
		assertEquals("1103", quest.conQuest());
	}

	@Test
	void multiCounterGoalsMatchTheScriptDllRegistrations() throws Exception {
		RetailSimpleHuntTable table = load();

		// 1517：DLL 注册 (slot2,6) 与 (slot1,4)，完成值 0x184 = 4 | 6<<6
		RetailSimpleHuntTable.Entry quest1517 = table.find(1517).orElseThrow();
		assertEquals(4, quest1517.counter(1).orElseThrow().required());
		assertEquals(6, quest1517.counter(2).orElseThrow().required());
		assertEquals(0x184, RetailHuntCounterLayout.goal(quest1517.counters()));

		// 1365：DLL 注册 (slot1,5) 与 (slot2,5)，完成值 0x145 = 5 | 5<<6
		RetailSimpleHuntTable.Entry quest1365 = table.find(1365).orElseThrow();
		assertEquals(0x145, RetailHuntCounterLayout.goal(quest1365.counters()));

		// 1102：单计数器，完成值 3
		assertEquals(3, RetailHuntCounterLayout.goal(table.find(1102).orElseThrow().counters()));
	}

	@Test
	void counterArithmeticFollowsTheRetailHelper() throws Exception {
		RetailSimpleHuntTable.Entry quest = load().find(1102).orElseThrow();
		List<RetailSimpleHuntTable.Counter> counters = quest.counters();

		assertEquals(0, RetailHuntCounterLayout.counterValue(0, 1));
		assertTrue(RetailHuntCounterLayout.canIncrement(0, 1, 3));

		int packed = 0;
		for (int kill = 1; kill <= 3; kill++) {
			packed = RetailHuntCounterLayout.increment(packed, 1);
			assertEquals(kill, packed, "slot 1 increments by one");
		}
		assertFalse(RetailHuntCounterLayout.canIncrement(packed, 1, 3), "counter stops at countN");
		assertTrue(RetailHuntCounterLayout.isComplete(packed, counters));

		// 槽位 2 的步长是 1<<6，且两槽位互不干扰
		int mixed = RetailHuntCounterLayout.increment(0, 2);
		mixed = RetailHuntCounterLayout.increment(mixed, 2);
		mixed = RetailHuntCounterLayout.increment(mixed, 1);
		assertEquals(2, RetailHuntCounterLayout.counterValue(mixed, 2));
		assertEquals(1, RetailHuntCounterLayout.counterValue(mixed, 1), "slot 1 uses a 1-step increment");
	}

	static RetailSimpleHuntTable load() throws Exception {
		try (InputStream input = RetailSimpleHuntTableTest.class.getResourceAsStream(TABLE)) {
			assertNotNull(input, "missing retail SimpleHunt table resource");
			return RetailSimpleHuntTable.load(input);
		}
	}
}
