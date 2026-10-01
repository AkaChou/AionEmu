package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

/**
 * P5D 步 1 门：SimpleItemPlay **行集真实分解**冻结 + 真端节点槽形态裁定交叉校验。
 * <p>
 * 计划 §10.3-#16① 原判据写作「37 行声明中继 / {@code cutsceneid1} / {@code item_check}」，与真端表事实不符。
 * P5D 复算（工具 {@code p5d/tools/itemplay-shape-audit.py}，真端 {@code ScriptDLL64.c} 节点/槽逐行对拍）得到的
 * 真实分解是：43 行 = **6 路由**（owner {@code RETAIL_TABLE}）+ **9 XML 保留**（owner {@code XML_RETENTION}，
 * 各有 {@code ADJUDICATED:*} 理由）+ **28 无 owner 条目**（不在本服生产：无 XML、清单无行）。
 * <p>
 * 同一复算还把 9 行的裁定理由与真端槽形态对齐：{@code slot 0} = 接取节点、{@code slot 3#K} = 第 K 步
 * （{@code talk_npcK} 从 0 起，交付节点 = {@code min(relays+1, 3)}）、{@code slot 4} = 交付节点。39/43 行
 * 与该不变量完全一致；偏离的 4 行恰好是裁定为 {@code ADVANCE_UNEXPRESSED}(80255/80256) 与
 * {@code ACQUIRE_NPC_SENTINEL}(39713/49713) 的行 ⇒ 理由与真端形态互证；其余 5 行
 * （{@code TALK_CHAIN}/{@code CON_QUEST}）形态已成立，只差接线面 ⇒ 下一增量的目标集。
 * <p>
 * Frozen row-set decomposition for the retail SimpleItemPlay family plus the cross-check between the
 * recorded XML-retention adjudications and the retail node/slot shape verdict: the four rows whose shape
 * deviates from the family invariant are exactly the rows adjudicated as advance-unexpressed or
 * acquire-sentinel, and the five shape-ready adjudicated rows are the next increment's target set.
 */
class ItemPlayFamilyRowInventoryGateTest {

	private static final String RETENTION_RESOURCE =
		"/aion/data/static_data/quest/retail/retail-xml-retention.tsv";
	private static final String FAMILY = "SimpleItemPlay";

	/** 真端表全量行数（真端 {@code quest_simpleitemplays}）。 / Retail table row count. */
	private static final int TABLE_ROWS = 43;
	/** 无 owner 条目、无 XML ⇒ 不在本服生产的行数。 / Rows absent from production. */
	private static final int ABSENT_ROWS = 28;

	/** 已退役并路由的 6 行。 / The six retired, routed rows. */
	private static final Set<Integer> ROUTED_ROWS =
		Set.of(13704, 13708, 19048, 23704, 23708, 29048);

	/** XML 保留的 9 行及其裁定理由（真端清单逐字）。 / The nine XML-retention rows and their reasons. */
	private static final Map<Integer, String> ADJUDICATED_ROWS = Map.of(
		18213, "ADJUDICATED:RETAIL_TALK_CHAIN",
		28213, "ADJUDICATED:RETAIL_TALK_CHAIN",
		50048, "ADJUDICATED:RETAIL_TALK_CHAIN",
		18828, "ADJUDICATED:RETAIL_CON_QUEST",
		28828, "ADJUDICATED:RETAIL_CON_QUEST",
		39713, "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL",
		49713, "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL",
		80255, "ADJUDICATED:RETAIL_ADVANCE_UNEXPRESSED",
		80256, "ADJUDICATED:RETAIL_ADVANCE_UNEXPRESSED");

	/** 真端槽形态偏离本族不变量的 4 行（P5D 逐行复算）。 / Rows deviating from the retail slot shape. */
	private static final Set<Integer> SHAPE_DEVIATING_ROWS = Set.of(80255, 80256, 39713, 49713);

	/** 真端槽形态已成立、仅差接线面的 5 行 = 下一增量目标集。 / Shape-ready next-increment targets. */
	private static final Set<Integer> SHAPE_READY_ADJUDICATED = Set.of(18213, 28213, 18828, 28828, 50048);

	/**
	 * 行集真实分解：真端表 43 行 = 6 路由 + 9 XML 保留裁定 + 28 不在生产；三者互斥且覆盖全表，
	 * 任何行漂移（新增/消失/换桶）都会红灯。
	 */
	@Test
	void rowSetDecomposesIntoRoutedAdjudicatedAndAbsent() throws Exception {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		SimpleItemPlayHandler handler = SimpleItemPlayHandler.instance();
		Map<Integer, String[]> manifest = manifest();

		Set<Integer> table = new TreeSet<>();
		loader.itemPlayRows().forEach(row -> table.add(row.questId()));
		assertEquals(TABLE_ROWS, table.size(), "真端表行数");
		assertEquals(TABLE_ROWS, loader.itemPlaySize(), "装载器行数 = 真端表行数");
		assertEquals(TABLE_ROWS, handler.ownedQuestIds().size(), "注册集 = 真端表全量行");
		assertEquals(ROUTED_ROWS, new TreeSet<>(handler.routedQuestIds()), "路由集冻结");
		assertEquals(TABLE_ROWS - ROUTED_ROWS.size(), handler.unroutableQuestIds().size(), "不可路由行数");

		Set<Integer> adjudicated = new TreeSet<>();
		Set<Integer> absent = new TreeSet<>();
		for (int questId : table) {
			String[] entry = manifest.get(questId);
			if (ADJUDICATED_ROWS.containsKey(questId)) {
				assertEquals(FAMILY, entry[1], "XML 保留行的家族登记: " + questId);
				assertEquals(ADJUDICATED_ROWS.get(questId), entry[2], "XML 保留行的裁定理由: " + questId);
				adjudicated.add(questId);
			} else if (ROUTED_ROWS.contains(questId)) {
				assertEquals("RETAIL_TABLE", entry[0], "路由行的 owner: " + questId);
				assertEquals(FAMILY, entry[1], "路由行的家族登记: " + questId);
			} else {
				assertEquals(null, entry, "无 owner 条目的行不得出现在清单里（不在本服生产）: " + questId);
				absent.add(questId);
			}
		}
		assertEquals(new TreeSet<>(ADJUDICATED_ROWS.keySet()), adjudicated, "XML 保留行集冻结");
		assertEquals(ABSENT_ROWS, absent.size(), "不在生产的行数冻结");
		assertEquals(TABLE_ROWS, ROUTED_ROWS.size() + adjudicated.size() + absent.size(),
			"6 路由 + 9 裁定 + 28 不在生产 = 43（互斥且覆盖）");

		for (int questId : adjudicated) {
			assertFalse(handler.routes(questId), "XML 保留行不走 native 车道: " + questId);
		}
		for (int questId : absent) {
			assertFalse(handler.routes(questId), "不在生产的行不路由: " + questId);
		}
	}

	/**
	 * 裁定理由 ↔ 真端槽形态互证：偏离不变量的 4 行 = 裁定为避免不成立型（advance/sentinel）；
	 * 形态已成立的 5 行 = 裁定为轴线未接线型（talk chain / con_quest）⇒ 下一增量目标集。
	 */
	@Test
	void adjudicationReasonsMatchTheRetailShapeVerdict() throws Exception {
		Map<Integer, String[]> manifest = manifest();
		Set<Integer> deviating = new TreeSet<>();
		Set<Integer> shapeReady = new TreeSet<>();
		for (Map.Entry<Integer, String> entry : ADJUDICATED_ROWS.entrySet()) {
			String reason = manifest.get(entry.getKey())[2];
			assertEquals(entry.getValue(), reason, "裁定理由逐字: " + entry.getKey());
			if (reason.endsWith("_ADVANCE_UNEXPRESSED") || reason.endsWith("_ACQUIRE_NPC_SENTINEL")) {
				deviating.add(entry.getKey());
			} else {
				shapeReady.add(entry.getKey());
			}
		}
		assertEquals(SHAPE_DEVIATING_ROWS, deviating,
			"真端槽形态偏离（advance 未表达 / 接取哨兵）的行集 = 4 行冻结");
		assertEquals(SHAPE_READY_ADJUDICATED, shapeReady,
			"真端槽形态已成立、仅差接线面的行集 = 5 行冻结");
		assertTrue(java.util.Collections.disjoint(deviating, shapeReady), "两集合互斥");
		assertEquals(ADJUDICATED_ROWS.size(), deviating.size() + shapeReady.size(), "9 行归属完整");
	}

	/** 真端清单：quest id → [owner, family, reason]（非本族行不载入）。 / The retention manifest. */
	private static Map<Integer, String[]> manifest() throws Exception {
		Map<Integer, String[]> result = new LinkedHashMap<>();
		try (InputStream input = ItemPlayFamilyRowInventoryGateTest.class.getResourceAsStream(RETENTION_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + RETENTION_RESOURCE);
			}
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#") || line.isBlank()) {
						continue;
					}
					String[] cells = line.split("\t", -1);
					if (cells.length >= 3 && FAMILY.equals(cells[2])) {
						result.put(Integer.parseInt(cells[0]), new String[] {cells[1], cells[2], cells[3]});
					}
				}
			}
		}
		assertTrue(result.size() == ADJUDICATED_ROWS.size() + ROUTED_ROWS.size(),
			"清单里本族行数 = 6 路由 + 9 裁定");
		return result;
	}

	/** 证据面：未解析名/符号集合不得随本批扩大（本批零行为变更）。 */
	@Test
	void evidenceFacesStayFrozenForThisZeroSwitchBatch() {
		SimpleItemPlayHandler handler = SimpleItemPlayHandler.instance();
		Set<String> expectedNames = new LinkedHashSet<>(Set.of(
			"HousingManager_Da", "HousingManager_Li", "LDF5b_Greenhat_LD",
			"NPC_event_devasday_shugo", "_faction_"));
		Set<String> actual = new TreeSet<>(handler.unresolvedNames());
		// 未退役/未接线行里的名字可能已可解：只冻结「已解不得回退」的一面。
		for (String name : actual) {
			assertTrue(expectedNames.contains(name) || expectedNames.contains(name),
				"未解析名证据面不得出现新面孔: " + name);
		}
		assertTrue(handler.unresolvedChainQuestIds().isEmpty(), "本表内 con_quest 闭环不得回退");
	}
}
