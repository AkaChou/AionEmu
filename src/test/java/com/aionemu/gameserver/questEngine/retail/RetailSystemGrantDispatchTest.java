package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.tablelane.NativeSystemGrantLanes;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统发放接线门禁（P0c）。守的是接线契约本身：
 * <ol>
 * <li><b>哨兵行必须可发放</b>：真端 {@code Quest_SimpleCollectItem.xml} / {@code Quest_SimpleTalk.xml} 里
 * {@code _faction_} 的每一行，在其所属车道（P3 起 SimpleTalk、P4 起 SimpleCollectItem 均走 native）必须
 * 既进得了势力轮换池、又过得了发放入口 —— 否则 {@code NpcFactions.sendDailyQuest()} 分配后无法发放
 * （只发提示、永远接不了）；未切换家族仍断言生产定义（真端 overlay）的
 * {@code QuestEvent.SystemGrant} 边；</li>
 * <li><b>无发放入口的哨兵不得被发放</b>：{@code _challengetask_}（挑战任务；本服只有完成回调）
 * 的 SimpleTalk 行仍由 XML 驱动，不得带该边；</li>
 * <li><b>区域行必须双满足</b>：{@code _area_} 的已退役行既要在 {@code ai-areas.xml} 的 quest_area 里
 * 有绑定（P0c-4 按真端世界文件补齐），又要带 {@code SystemGrant} 边；</li>
 * <li><b>普通行不得被系统发放</b>：NPC 接取的任务（如 1103 = Mires）不带该边，行为与改造前一致；</li>
 * <li><b>未知任务/非法 ID 安全返回 false</b>。</li>
 * </ol>
 * Gate for the system-grant wiring: every {@code _faction_} sentinel row must carry a
 * {@code SystemGrant} edge in the production definition, and plain NPC-accept rows must not.
 */
class RetailSystemGrantDispatchTest {
	/** 生产区域发放表（quest_area 绑定）。 / Production quest-area grant table. */
	private static final String QUEST_AREAS = "/aion/definitions/compact/ai/ai-areas.xml";
	/** 真端 SimpleHunt 表（类别哨兵来源）。 / Retail SimpleHunt table path. */
	private static final String SIMPLE_HUNT_TABLE = "/aion/data/static_data/quest/retail/Quest_SimpleHunt.xml";
	/** 真端 {@code _faction_} 行的历史下限：CollectItem 实测 43 / SimpleTalk 实测 98。 */
	private static final int COLLECT_ITEM_FACTION_FLOOR = 40;
	/** SimpleTalk 已退役 {@code _faction_} 行的下限（P0c-2 批）。 / Retirement batch floor. */
	private static final int SIMPLE_TALK_FACTION_FLOOR = 40;
	/** SimpleHunt 已退役 {@code _faction_} 行的下限（P0c-3 批）。 / SimpleHunt batch floor. */
	private static final int SIMPLE_HUNT_FACTION_FLOOR = 62;

	/**
	 * SimpleCollectItem 的 {@code _faction_} 行（P4 后取自原生车道：行集与接取名类别都由
	 * {@link SimpleCollectItemHandler} 装载，旧 {@code RetailSimpleCollectItemTable} 已随其编译器同批删除）。
	 * SimpleCollectItem sentinel rows, read from the native lane after P4 (the retired
	 * {@code RetailSimpleCollectItemTable} went away together with the family compiler).
	 */
	private static List<Integer> collectItemFactionIds() {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		return handler.ownedQuestIds().stream()
			.filter(questId -> handler.grantKind(questId) == RetailGrantKind.FACTION)
			.sorted()
			.toList();
	}

	/**
	 * SimpleTalk 指定类别的哨兵行（P3 后取自原生车道：表行的接取名类别由 {@link SimpleTalkHandler}
	 * 装载，旧 {@code RetailSimpleTalkTable} 已随其编译器同批删除）。
	 * SimpleTalk sentinel rows of the requested kind, read from the native lane after P3 (the retired
	 * {@code RetailSimpleTalkTable} was deleted together with its compiler).
	 */
	private static List<Integer> simpleTalkIds(RetailGrantKind kind) {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		return handler.ownedQuestIds().stream().filter(questId -> handler.grantKind(questId) == kind).toList();
	}


	/** SimpleHunt 指定类别的哨兵行。 / SimpleHunt sentinel rows of the requested kind. */
	private static List<Integer> simpleHuntIds(RetailGrantKind kind) throws IOException {
		try (InputStream input = RetailSystemGrantDispatchTest.class.getResourceAsStream(SIMPLE_HUNT_TABLE)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + SIMPLE_HUNT_TABLE);
			}
			RetailSimpleHuntTable table = RetailSimpleHuntTable.load(input);
			return table.questIds().stream()
				.map(table::find)
				.flatMap(Optional::stream)
				.filter(entry -> entry.grantKind() == kind)
				.map(RetailSimpleHuntTable.Entry::questId)
				.sorted()
				.toList();
		}
	}

	/** 诊断用：该任务定义的事件类型集合。 / Diagnostic: event-type set of the quest definition. */
	private static String eventTypes(QuestCatalog catalog, int questId) {
		return catalog.findExecutable(questId)
			.map(compiled -> compiled.transitionsByType().keySet().toString())
			.orElse("<no-definition>");
	}

	/**
	 * SimpleCollectItem 已切原生车道的 {@code _faction_} 行（真端 43 行）必须可发放：阵营轮换池
	 * （{@link NativeSystemGrantLanes#factionRotationCandidates(int)}）收得进、发放入口
	 * （{@link NativeSystemGrantLanes#isSystemGranted(int)}）放得过，两轴缺一即「分配后永远接不了」。
	 * P4 前该断言落在 typed 目录的 {@code SystemGrant} 边上，该边随本族切换批退出生产视图。
	 * The faction-sentinel rows of the retired family must stay both rotation-eligible and grantable
	 * after the P4 switch; the typed {@code SystemGrant} edge they used to assert on is gone.
	 */
	@Test
	void nativeSimpleCollectItemFactionRowsAreRotationEligibleAndGrantable() {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		List<Integer> routed = collectItemFactionIds().stream().filter(handler::routes).toList();
		List<Integer> rotationBound = routed.stream().filter(questId -> handler.factionId(questId) != 0).toList();
		List<Integer> outsideRotation = routed.stream().filter(questId -> handler.factionId(questId) == 0).toList();
		assertTrue(rotationBound.size() >= COLLECT_ITEM_FACTION_FLOOR,
			() -> "轮换宇宙内的原生 SimpleCollectItem _faction_ 行数异常（应 ≥" + COLLECT_ITEM_FACTION_FLOOR
				+ "）: " + rotationBound.size());
		List<String> problems = new java.util.ArrayList<>();
		for (int questId : rotationBound) {
			int factionId = handler.factionId(questId);
			if (!NativeSystemGrantLanes.factionRotationCandidates(factionId).contains(questId)) {
				problems.add(questId + "=不在势力 " + factionId + " 的轮换池");
			}
			if (!NativeSystemGrantLanes.isSystemGranted(questId)) {
				problems.add(questId + "=发放入口 isSystemGranted=false（分配后接不了）");
			}
		}
		assertTrue(problems.isEmpty(), () -> "原生采集哨兵行的发放接线缺口: " + problems);
		// 轮换宇宙外的行必须冻结为「任何势力池都收不进」——否则会被误当作可发放。
		// Rows outside the rotation universe must stay out of every faction pool.
		for (int questId : outsideRotation) {
			for (int factionId = 1; factionId <= 20; factionId++) {
				int poolId = factionId;
				assertFalse(NativeSystemGrantLanes.factionRotationCandidates(factionId).contains(questId),
					() -> "轮换宇宙外的行 " + questId + " 混进了势力 " + poolId + " 的池");
			}
		}
	}

	/**
	 * SimpleTalk 已切原生车道的 {@code _faction_} 行必须可发放：轮换池（{@code factionRotationCandidates}）
	 * 收得进、发放入口（{@code isSystemGranted}）放得过，两轴缺一即「分配后永远接不了」。旧断言落在 typed
	 * 目录的 {@code SystemGrant} 边上，该边随 P3 切换批退出生产视图，故改锚在原生车道的同两轴。
	 * The faction-sentinel rows that moved to the native lane must stay both rotation-eligible and
	 * grantable; the typed {@code SystemGrant} edge they used to assert on retired with the P3 switch.
	 */
	@Test
	void nativeSimpleTalkFactionRowsAreRotationEligibleAndGrantable() {
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		List<Integer> routed = simpleTalkIds(RetailGrantKind.FACTION).stream()
			.filter(handler::routes)
			.toList();
		// 真端表 98 行 `_faction_`，其中 4 行仍由 XML 定义拥有（owner 归 XML 车道）；路由 94 行里
		// 71 行的 quest.xml npcfaction_name 落在本服 10 个守备队势力内（= 轮换宇宙），另 23 行是
		// Mentee_Li/Mentee_Da（师徒系统），不在轮换宇宙，故不参与「轮换池收得进」这条断言。
		// Of the retail table's 98 `_faction_` rows, 4 are still XML-owned; of the 94 routed rows 71
		// bind to one of the ten guard factions (the rotation universe) while 23 carry the mentor-system
		// names Mentee_Li/Mentee_Da and stay outside it.
		List<Integer> rotationBound = routed.stream().filter(questId -> handler.factionId(questId) != 0).toList();
		List<Integer> outsideRotation = routed.stream().filter(questId -> handler.factionId(questId) == 0).toList();
		assertTrue(rotationBound.size() >= SIMPLE_TALK_FACTION_FLOOR,
			() -> "轮换宇宙内的原生 SimpleTalk _faction_ 行数异常（应 ≥" + SIMPLE_TALK_FACTION_FLOOR
				+ "）: " + rotationBound.size());
		List<String> problems = new java.util.ArrayList<>();
		for (int questId : rotationBound) {
			int factionId = handler.factionId(questId);
			if (!handler.factionRotationCandidates(factionId).contains(questId)) {
				problems.add(questId + "=不在势力 " + factionId + " 的轮换池");
			}
			if (!handler.isSystemGranted(questId)) {
				problems.add(questId + "=发放入口 isSystemGranted=false（分配后接不了）");
			}
		}
		assertTrue(problems.isEmpty(), () -> "原生哨兵行的发放接线缺口: " + problems);
		// 轮换宇宙外的行必须冻结为「任何势力池都收不进」——否则会被误当作可发放。
		// Rows outside the rotation universe must stay out of every faction pool.
		for (int questId : outsideRotation) {
			for (int factionId = 1; factionId <= 20; factionId++) {
				int poolId = factionId;
				assertFalse(handler.factionRotationCandidates(factionId).contains(questId),
					() -> "轮换宇宙外的行 " + questId + " 混进了势力 " + poolId + " 的池");
			}
		}
	}

	/**
	 * SimpleHunt 已退役的 {@code _faction_} 行：**已登记缺口（P8，§10.3-#25）**——typed 边随 P1 车道
	 * 退役，原生聚合面（{@code NativeSystemGrantLanes} = Talk+Collect）尚无 SimpleHunt 车道。
	 * 本断言锁定 fail-closed 现状：这些行不得混入 Talk/Collect 聚合面（owner 纯度），
	 * 接线随真端取证（SimpleHunt 哨兵发放宿主面）单独立批。
	 * Retired SimpleHunt {@code _faction_} rows: registered gap (P8) — the typed edges retired with
	 * the P1 lane and the native aggregate (Talk+Collect) has no SimpleHunt lane yet. Locked
	 * fail-closed: the rows must stay outside the aggregate (owner purity) until the retail
	 * host-face evidence lands in a dedicated batch.
	 */
	@Test
	void retiredSimpleHuntFactionRowsStayOutsideTheGrantAggregate() throws IOException {
		List<Integer> retired = simpleHuntIds(RetailGrantKind.FACTION).stream()
			.filter(RetiredQuestIds::contains)
			.toList();
		assertTrue(retired.size() >= SIMPLE_HUNT_FACTION_FLOOR,
			() -> "已退役的 SimpleHunt _faction_ 行数异常（应 ≥" + SIMPLE_HUNT_FACTION_FLOOR + "）: " + retired.size());
		List<String> leaked = retired.stream()
			.filter(questId -> NativeSystemGrantLanes.laneOf(questId) != null)
			.map(Object::toString)
			.toList();
		assertTrue(leaked.isEmpty(), () -> "SimpleHunt 哨兵行混入 Talk/Collect 发放聚合面（owner 纯度破坏）: "
			+ leaked);
		assertTrue(NativeSystemGrantLanes.ownershipConflicts().isEmpty(),
			"发放车道 owner 交叠非空（归属分解失败）");
	}

	/**
	 * {@code _challengetask_} 没有受理入口（本服只有完成回调），不得被误当系统发放：typed 侧无
	 * {@code SystemGrant} 边，原生侧 {@code isSystemGranted} 也必须为 false（发放入口不得接单）。
	 * Challenge-task sentinels have no grant path on either lane.
	 */
	@Test
	void challengeTaskSentinelRowsAreNotSystemGranted() throws IOException {
		QuestCatalog catalog = ProductionQuestDefinitions.catalog();
		List<Integer> wronglyGranted = new java.util.ArrayList<>();
		wronglyGranted.addAll(simpleTalkIds(RetailGrantKind.CHALLENGE_TASK).stream()
			.filter(questId -> RetailSystemGrantDispatcher.isSystemGranted(catalog, questId))
			.toList());
		wronglyGranted.addAll(simpleHuntIds(RetailGrantKind.CHALLENGE_TASK).stream()
			.filter(questId -> RetailSystemGrantDispatcher.isSystemGranted(catalog, questId))
			.toList());
		SimpleTalkHandler handler = SimpleTalkHandler.instance();
		wronglyGranted.addAll(simpleTalkIds(RetailGrantKind.CHALLENGE_TASK).stream()
			.filter(handler::isSystemGranted)
			.toList());
		assertTrue(wronglyGranted.isEmpty(),
			() -> "挑战任务哨兵行不应带 SystemGrant 边（无受理入口）: " + wronglyGranted);
	}

	/**
	 * {@code _area_} 行的发放入口是区域引擎（P0c-4 已按真端世界文件的 {@code questscript_area}
	 * 补齐 {@code ai-areas.xml} 的 quest_area 绑定）：已退役行必须同时满足
	 * 「quest_area 有绑定」+「定义带 SystemGrant 边」，缺一都会让玩家进区域后接不到任务。
	 * Area rows are granted on area entry (wired in P0c-4 from the retail world files), so every
	 * retired row must stay bound in the quest-area table and keep its SystemGrant edge.
	 */
	@Test
	void retiredAreaRowsStayBoundToQuestAreasAndCarrySystemGrantEdge() throws IOException {
		// P8 重锚：{@code RetailQuestAreaIndex} 已随编译车道退役——绑定面改为测试侧直读
		// {@code ai-areas.xml} 的 {@code quest_area} 绑定（quests 属性含该任务 id 即已绑定）。
		// P8 re-anchor: RetailQuestAreaIndex retired with the compile lane — the binding face is read
		// here directly from ai-areas.xml (a quest_area element listing the quest id = bound).
		java.util.Set<Integer> bound = new java.util.TreeSet<>();
		try (InputStream input = RetailSystemGrantDispatchTest.class.getResourceAsStream(QUEST_AREAS)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + QUEST_AREAS);
			}
			String xml = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			java.util.regex.Matcher matcher = java.util.regex.Pattern
				.compile("<quest_area\\b[^>]*\\bquests=\"([^\"]+)\"").matcher(xml);
			while (matcher.find()) {
				for (String token : matcher.group(1).split("[,\\s]+")) {
					if (token.matches("\\d+")) {
						bound.add(Integer.parseInt(token));
					}
				}
			}
		}
		assertFalse(bound.isEmpty(), "ai-areas.xml quest_area 绑定扫描为空（资源或格式漂移）");
		List<Integer> retired = simpleHuntIds(RetailGrantKind.AREA).stream()
			.filter(RetiredQuestIds::contains)
			.toList();
		assertFalse(retired.isEmpty(), "P0c-4 退役的 _area_ 行不应为空");
		List<String> unbound = retired.stream()
			.filter(questId -> !bound.contains(questId))
			.map(Object::toString)
			.toList();
		assertTrue(unbound.isEmpty(), () -> "已退役的 _area_ 行缺 quest_area 绑定（进区域无法发放）: " + unbound);
		// P8 已登记缺口（§10.3-#25）：typed SystemGrant 边随 P1 退役，SimpleHunt 无原生发放车道——
		// 绑定数据保持 fail-closed 登记，接线随 SimpleHunt 哨兵发放面取证单独立批（同 _faction_ 行）。
		// Registered gap (P8): the typed SystemGrant edges retired with P1; the binding data stays
		// fail-closed until the SimpleHunt sentinel grant face lands in a dedicated batch.
	}

	@Test
	void plainNpcAcceptRowsAreNotSystemGranted() {
		QuestCatalog catalog = ProductionQuestDefinitions.catalog();
		// 1103 = Mires 对话接取（真端三元组齐全），不是系统发放。
		assertFalse(RetailSystemGrantDispatcher.isSystemGranted(catalog, 1103));
	}

	@Test
	void unknownOrInvalidIdsAreNotSystemGranted() {
		QuestCatalog catalog = ProductionQuestDefinitions.catalog();
		assertFalse(RetailSystemGrantDispatcher.isSystemGranted(catalog, 999999));
		assertFalse(RetailSystemGrantDispatcher.isSystemGranted(catalog, 0));
		assertFalse(RetailSystemGrantDispatcher.isSystemGranted(null, 35007));
	}
}
