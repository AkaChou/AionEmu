package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.retail.RetailLedgerRows;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;

/**
 * P5D 步 1/3 门：SimpleItemPlay **行集真实分解**冻结 + 原版节点槽形态裁定交叉校验 + 激活批证据面。
 * <p>
 * 计划 §10.3-#16① 原判据写作「37 行声明中继 / {@code cutsceneid1} / {@code item_check}」，与原版表事实不符。
 * P5D 复算（工具 {@code p5d/tools/itemplay-shape-audit.py}，原版 {@code ScriptDLL64.c} 节点/槽逐行对拍）得到的
 * 真实分解是：43 行 = **8 路由**（owner {@code RETAIL_TABLE}）+ **7 XML 保留**（owner {@code XML_RETENTION}，
 * 各有 {@code ADJUDICATED:*} 理由）+ **28 无 owner 条目**（不在本服生产：无 XML、清单无行）；
 * 其中 18213/28213 由「接线 ≠ 激活」转为激活（P5D 步 3：retention 重裁 + 删 XML + 删目录条目）。
 * <p>
 * 同一复算还把 9 行的裁定理由与原版槽形态对齐：{@code slot 0} = 接取节点、{@code slot 3#K} = 第 K 步
 * （{@code talk_npcK} 从 0 起，交付节点 = {@code min(relays+1, 3)}）、{@code slot 4} = 交付节点。39/43 行
 * 与该不变量完全一致；偏离的 4 行恰好是裁定为 {@code ADVANCE_UNEXPRESSED}(80255/80256) 与
 * {@code ACQUIRE_NPC_SENTINEL}(39713/49713) 的行 ⇒ 理由与原版形态互证；其余 5 行
 * （{@code TALK_CHAIN}/{@code CON_QUEST}）形态已成立；P5D 步 3 激活其中名字轴干净、owner 可退役的两行
 * （18213/28213），其余 3 行（{@code CON_QUEST} 跨表目标 18829/28829 不存在、50048 中继名零命中）保持 XML 保留。
 * <p>
 * Frozen row-set decomposition for the retail SimpleItemPlay family plus the cross-check between the
 * recorded XML-retention adjudications and the retail node/slot shape verdict: the four rows whose shape
 * deviates from the family invariant are exactly the rows adjudicated as advance-unexpressed or
 * acquire-sentinel; step 3 activates the two shape-ready rows with clean name axes (18213/28213).
 */
class ItemPlayFamilyRowInventoryGateTest {

	private static final String RETENTION_RESOURCE =
		"/aion/data/static_data/quest/retail/retail-xml-retention.xml";
	private static final String FAMILY = "SimpleItemPlay";

	/** 原版表全量行数（原版 {@code quest_simpleitemplays}）。 / Retail table row count. */
	private static final int TABLE_ROWS = 43;
	/** 原版表的 43 个 id。 / The 43 retail table ids. */
	private static final Set<Integer> TABLE_IDS = Set.of(
		9623, 19048, 29048, 41267, 41514, 41540, 41300, 41577, 41593, 18828, 28828, 12066, 22066, 50013,
		50014, 51013, 51014, 80255, 80256, 18014, 28014, 39713, 49713, 12525, 12562, 22525, 22562, 13054,
		13064, 23054, 23064, 23562, 13400, 13401, 23400, 23401, 13704, 13708, 23704, 23708, 50048, 18213,
		28213);
	/** 无 owner 条目、无 XML ⇒ 不在本服生产的行数。 / Rows absent from production. */
	private static final int ABSENT_ROWS = 28;

	/** 已退役并路由的 8 行（P5D 步 3 激活 18213/28213 后）。 / The eight retired, routed rows. */
	private static final Set<Integer> ROUTED_ROWS =
		Set.of(13704, 13708, 19048, 23704, 23708, 29048, 18213, 28213);

	/** XML 保留的 7 行及其裁定理由（原版清单逐字）。 / The seven XML-retention rows and their reasons. */
	private static final Map<Integer, String> ADJUDICATED_ROWS = Map.of(
		50048, "ADJUDICATED:RETAIL_TALK_CHAIN",
		18828, "ADJUDICATED:RETAIL_CON_QUEST",
		28828, "ADJUDICATED:RETAIL_CON_QUEST",
		39713, "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL",
		49713, "ADJUDICATED:RETAIL_ACQUIRE_NPC_SENTINEL",
		80255, "ADJUDICATED:RETAIL_ADVANCE_UNEXPRESSED",
		80256, "ADJUDICATED:RETAIL_ADVANCE_UNEXPRESSED");

	/** 原版槽形态偏离本族不变量的 4 行（P5D 逐行复算）。 / Rows deviating from the retail slot shape. */
	private static final Set<Integer> SHAPE_DEVIATING_ROWS = Set.of(80255, 80256, 39713, 49713);

	/** 原版槽形态已成立、仍留 XML 的 3 行（名字轴无解 ⇒ fail-closed）。 / Remaining shape-ready XML rows. */
	private static final Set<Integer> SHAPE_READY_ADJUDICATED = Set.of(18828, 28828, 50048);

	/**
	 * 原版 {@code quest.xml} 的等级门：{@code minlevel_permitted = 999} = **停用形**（原版
	 * {@code Quest::CanAcquireQuest} 对 {@code level < minlevel} 一律拒绝 ⇒ 不可接取；本车道
	 * {@code NativeQuestStartPort} 同一口径）。
	 */
	private static final Set<Integer> STOPPED_ROWS = Set.of(
		41267, 41514, 41540, 41300, 41577, 41593, 12066, 22066, 50013, 50014, 51013, 51014, 18014, 28014,
		12525, 12562, 22525, 22562, 13054, 13064, 23054, 23064, 23562, 13400, 13401, 23400, 23401);

	/** 可接取形的 16 行（{@code minlevel_permitted != 999}）。 / The sixteen acquirable rows. */
	private static final Set<Integer> LIVE_ROWS = Set.of(
		9623, 19048, 29048, 18213, 28213, 18828, 28828, 80255, 80256, 39713, 49713, 13704, 13708, 23704,
		23708, 50048);

	/**
	 * 行集真实分解：原版表 43 行 = 6 路由 + 9 XML 保留裁定 + 28 不在生产；三者互斥且覆盖全表，
	 * 任何行漂移（新增/消失/换桶）都会红灯。
	 */
	@Test
	void rowSetDecomposesIntoRoutedAdjudicatedAndAbsent() throws Exception {
		NativeQuestTableLoader loader = NativeQuestTableLoader.instance();
		SimpleItemPlayHandler handler = SimpleItemPlayHandler.instance();
		Map<Integer, String[]> manifest = manifest();

		Set<Integer> table = new TreeSet<>();
		loader.itemPlayRows().forEach(row -> table.add(row.questId()));
		assertEquals(TABLE_ROWS, table.size(), "原版表行数");
		assertEquals(TABLE_ROWS, loader.itemPlaySize(), "装载器行数 = 原版表行数");
		assertEquals(TABLE_ROWS, handler.ownedQuestIds().size(), "注册集 = 原版表全量行");
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
			"8 路由 + 7 裁定 + 28 不在生产 = 43（互斥且覆盖）");

		for (int questId : adjudicated) {
			assertFalse(handler.routes(questId), "XML 保留行不走 native 车道: " + questId);
		}
		for (int questId : absent) {
			assertFalse(handler.routes(questId), "不在生产的行不路由: " + questId);
		}
	}

	/**
	 * 裁定理由 ↔ 原版槽形态互证：偏离不变量的 4 行 = 裁定为避免不成立型（advance/sentinel）；
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
			"原版槽形态偏离（advance 未表达 / 接取哨兵）的行集 = 4 行冻结");
		assertEquals(SHAPE_READY_ADJUDICATED, shapeReady,
			"原版槽形态已成立、仍留 XML 的行集 = 3 行冻结");
		assertTrue(java.util.Collections.disjoint(deviating, shapeReady), "两集合互斥");
		assertEquals(ADJUDICATED_ROWS.size(), deviating.size() + shapeReady.size(), "7 行归属完整");
	}

	/**
	 * 可接取轴冻结：43 行按原版 {@code quest.xml} 的 {@code minlevel_permitted} 分桶（停用 27 / 可接取 16），
	 * 且**停用形上的长尾缺口没有运行期影响**——声明过场的两行（13400/23400）落在停用形里，
	 * 故 §10.3-#21 的「触发列缺失」不需要接线（原版与客户端 {@code quest.xml} 双向 999 一致）。
	 */
	@Test
	void acquirableAxisSplitsTheFamilyAndClosesTheCutsceneGap() throws Exception {
		Map<Integer, Integer> minLevel = minLevels();
		Set<Integer> stopped = new TreeSet<>();
		Set<Integer> live = new TreeSet<>();
		for (Map.Entry<Integer, Integer> entry : minLevel.entrySet()) {
			if (entry.getValue() == 999) {
				stopped.add(entry.getKey());
			} else {
				live.add(entry.getKey());
			}
		}
		assertEquals(STOPPED_ROWS, stopped, "停用形（minlevel_permitted = 999）行集冻结");
		assertEquals(LIVE_ROWS, live, "可接取形行集冻结");
		assertEquals(TABLE_ROWS, stopped.size() + live.size(), "两桶互斥且覆盖全表");

		// 过场行（原版表 cutsceneid1 859/860）在停用形里 ⇒ 触发列缺失无运行期影响（§10.3-#21 闭环）。
		assertTrue(stopped.containsAll(Set.of(13400, 23400)), "声明过场的两行必须是停用形");
		SimpleItemPlayHandler handler = SimpleItemPlayHandler.instance();
		for (int questId : Set.of(13400, 23400)) {
			assertTrue(handler.relayCount(questId) >= 1, "过场行同时是中继行: " + questId);
			assertFalse(handler.routes(questId), "停用形不得由 native 车道路由: " + questId);
		}

		// 可接取的中继行 = 6 行（其余中继行是停用形或不在生产）。
		Set<Integer> liveRelays = new TreeSet<>();
		for (int questId : LIVE_ROWS) {
			if (handler.relayCount(questId) > 0) {
				liveRelays.add(questId);
			}
		}
		// 可接取 + 有中继的 6 行：18213/28213（步 3 已激活为 RETAIL_TABLE）、
		// 39713/49713/50048（名字轴无解 ⇒ 留 XML 车道）、9623（保留清单无条目 ⇒ 不在本服生产）。
		assertEquals(Set.of(9623, 18213, 28213, 39713, 49713, 50048), liveRelays,
			"可接取的中继行集冻结（激活行 18213/28213 在其中）");
	}

	/**
	 * P5D 步 3 激活证据面：激活的 18213/28213 必须同时满足原版三源——① 表行声明两步中继且交付节点
	 * 落在 {@code slot 3}；② 原版 {@code quest.xml} 可接取（{@code minlevel_permitted = 51}，非停用形）
	 * 且前置 {@code finished_quest_cond1} 为同族上一环；③ 客户端页阶梯声明入口 {@code select1}(1011)、
	 * 问询窗 {@code ask_quest_accept}(4) 与两步页 {@code select2}(1352)/{@code select3}(1693)，
	 * 与 native 处理器的页序逐页一致；④ 处理器对两行路由且保留清单为 {@code RETAIL_TABLE}。
	 * <p>
	 * Step-3 activation evidence: both activated rows must satisfy the retail row shape, the retail
	 * {@code quest.xml} acquire axis and the client page ladder, and must actually be routed by the lane.
	 */
	@Test
	void activatedRelayRowsCarryTheRetailShapeAcquireAxisAndClientPageLadder() throws Exception {
		SimpleItemPlayHandler handler = SimpleItemPlayHandler.instance();
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		Map<Integer, String[]> manifest = manifest();
		Map<Integer, Integer> minLevel = minLevels();

		for (int questId : Set.of(18213, 28213)) {
			assertTrue(ROUTED_ROWS.contains(questId), "激活行必须在路由集里: " + questId);
			assertEquals("RETAIL_TABLE", manifest.get(questId)[0], "激活行 owner 必须退役: " + questId);
			assertEquals(51, minLevel.get(questId), "激活行原版等级门: " + questId);
			assertEquals(2, handler.relayCount(questId), "原版 slot 3 #0/#1 ⇒ 两步中继: " + questId);

			// 客户端页阶梯（原版客户端 quest.xml 派生的页动作契约）↔ native 页序逐页一致。
			assertTrue(contract.hasButtonPage(questId, QuestDialogPage.SELECT1.id()),
				"客户端必须声明入口页 select1: " + questId);
			assertTrue(contract.hasButtonPage(questId, QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id()),
				"客户端必须声明问询窗页 4: " + questId);
			assertEquals(SimpleItemPlayHandler.pageForStep(1), QuestDialogPage.SELECT2.id(),
				"第 1 步页 = 原版 select2");
			assertEquals(SimpleItemPlayHandler.pageForStep(2), QuestDialogPage.SELECT3.id(),
				"第 2 步页 = 原版 select3");
			for (int step = 1; step <= handler.relayCount(questId); step++) {
				assertTrue(contract.hasButtonPage(questId, SimpleItemPlayHandler.pageForStep(step)),
					"客户端必须声明第 " + step + " 步页: " + questId);
			}

			// 步物品轴：接取发第 1 步道具、第 2 步换物（原版 give_item/give_item2/remove_item2 逐列）。
			ItemStack acceptGive = handler.acceptGiveItem(questId);
			ItemStack stepTwoGive = handler.stepGiveItem(questId, 2);
			ItemStack stepTwoRemove = handler.stepRemoveItem(questId, 2);
			assertNotNull(acceptGive, "接取必须发放第 1 步道具: " + questId);
			assertNotNull(stepTwoGive, "第 2 步必须发放: " + questId);
			assertNotNull(stepTwoRemove, "第 2 步必须扣除: " + questId);
			assertEquals(handler.playItemId(questId), stepTwoGive.itemId(),
				"演出道具 = 第 2 步发放的道具: " + questId);
			assertEquals(acceptGive.itemId(), stepTwoRemove.itemId(),
				"第 2 步扣除接取发放的道具: " + questId);
			assertEquals(null, handler.stepGiveItem(questId, 1), "第 1 步无发放声明: " + questId);
		}
	}

	/** 原版 {@code quest.xml} 本族 43 行的 {@code minlevel_permitted}（独立重解析）。 */
	private static Map<Integer, Integer> minLevels() throws Exception {
		String text = readResource("/aion/data/static_data/quest/retail/quest.xml");
		Map<Integer, Integer> result = new TreeMap<>();
		Matcher block = Pattern.compile("<quest>.*?</quest>", Pattern.DOTALL).matcher(text);
		while (block.find()) {
			String body = block.group();
			Matcher id = Pattern.compile("<id>\\s*(\\d+)\\s*</id>").matcher(body);
			if (!id.find()) {
				continue;
			}
			int questId = Integer.parseInt(id.group(1));
			if (!TABLE_IDS.contains(questId)) {
				continue;
			}
			Matcher minLevel = Pattern.compile("<minlevel_permitted>\\s*(\\d+)").matcher(body);
			result.put(questId, minLevel.find() ? Integer.parseInt(minLevel.group(1)) : 0);
		}
		assertEquals(TABLE_ROWS, result.size(), "本族 43 行必须在原版 quest.xml 内有等级门");
		return result;
	}

	/** 读类路径资源为文本。 / Reads one classpath resource as text. */
	private static String readResource(String path) throws Exception {
		try (InputStream input = ItemPlayFamilyRowInventoryGateTest.class.getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** 原版清单：quest id → [owner, family, reason]（非本族行不载入）。 / The retention manifest. */
	private static Map<Integer, String[]> manifest() throws Exception {
		Map<Integer, String[]> result = new LinkedHashMap<>();
		for (Element row : RetailLedgerRows.rows(RETENTION_RESOURCE, "quest")) {
			String family = RetailLedgerRows.cell(row, "family");
			if (FAMILY.equals(family)) {
				result.put(Integer.parseInt(RetailLedgerRows.cell(row, "quest_id")),
					new String[] {RetailLedgerRows.cell(row, "owner"), family,
						RetailLedgerRows.cell(row, "reason")});
			}
		}
		assertTrue(result.size() == ADJUDICATED_ROWS.size() + ROUTED_ROWS.size(),
			"清单里本族行数 = 8 路由 + 7 裁定");
		return result;
	}

	/**
	 * 证据面**双向冻结**：未解析名集合必须逐元素等于下表（既不得新增——新面孔说明有名字解析回归或
	 * 新接线面未解；也不得消失——已解必须显式改表）。P5D 步 2 起中继名也进入解析面，
	 * {@code NPC_event_devasday_shugoseller}（50048 的 {@code talk_npc1}，静态数据里只有原版表命中）随之登记。
	 * 2026-10-05 显式改表（缺陷 S 批次对齐，非该批引入）：5 个组键（HousingManager_Da/Li、
	 * LDF5b_Greenhat_LD、NPC_event_devasday_shugo/shugoseller）随原版对话名组表接入原生解析面
	 * 而解析成功（组表 retail-quest-ai-name-groups.xml 成员 name_desc 展开命中 npc 模板），
	 * 仅剩系统发放哨兵 {@code _faction_} 未解析。
	 * Explicit table update 2026-10-05 (defect-S batch alignment, not introduced by it): five group
	 * keys now resolve through the retail dialog-name group table; only the system-grant sentinel
	 * {@code _faction_} remains unresolved.
	 */
	@Test
	void evidenceFacesStayFrozen() {
		SimpleItemPlayHandler handler = SimpleItemPlayHandler.instance();
		Set<String> expectedNames = new TreeSet<>(Set.of("_faction_"));
		assertEquals(expectedNames, new TreeSet<>(handler.unresolvedNames()), "未解析名证据面冻结");
		assertTrue(handler.unresolvedChainQuestIds().isEmpty(), "本表内 con_quest 闭环不得回退");
	}
}
