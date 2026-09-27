package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestAreaIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 验证双阵营紧急军令 1877–1887 与 2877–2887 的真端军衔网格合同（真端优先，定义来自生产驱动）：
 * <ul>
 * <li>接取 = 世界 quest_area 进区域系统发放（{@code SystemGrant} + {@code StartEligible}，无 NPC 接取路由）；
 * 进度 = 军衔阈值击杀网格 a0..a3，每步 {@code KillRanked(value1)}（victim 军衔 ≥ 阈值计数）；
 * 满格 1009 恢复路线在未满格节点上带 {@code var0=3} 门禁；交付/完成流与猎杀族规范形同一。</li>
 * <li>本合同由生产驱动定义（retail table value1 = 军衔阈值，value0 = 击杀数）逐任务锁定，
 * 替代迁移期 XML 的 started/k1/k2 节点形（已退役）。</li>
 * </ul>
 * Verifies the retail ranked-grid contracts for the faction-paired urgent orders 1877–1887 and
 * 2877–2887 (retail-first, definitions come from the production driver): area-entry system grant,
 * a KillRanked threshold grid a0..a3, gated full-count 1009 recovery routes, and the canonical
 * hand-in/completion flows shared with the hunt family.
 */
class Quest2877To2887UrgentOrdersFlowTest {
	private static final String ELYOS_START_ZONE = "TEMINON_LANDING_400010000";
	private static final String ASMODIAN_START_ZONE = "PRIMUM_LANDING_400010000";
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(1877, ELYOS_START_ZONE, Set.of(1876), 1, 278503, 3166358, 162000027, 10),
		new QuestContract(1878, ELYOS_START_ZONE, Set.of(1876, 1877), 2, 278502, 3166358, 162000050, 10),
		new QuestContract(1879, ELYOS_START_ZONE, Set.of(1876, 1878), 3, 278503, 3166358, 164000079, 10),
		new QuestContract(1880, ELYOS_START_ZONE, Set.of(1876, 1879), 4, 278502, 3166358, 169000010, 100),
		new QuestContract(1881, ELYOS_START_ZONE, Set.of(1876, 1880), 5, 278503, 3166358, 160001274, 10),
		new QuestContract(1882, ELYOS_START_ZONE, Set.of(1876, 1881), 6, 278502, 3166358, 164000095, 5),
		new QuestContract(1883, ELYOS_START_ZONE, Set.of(1876, 1882), 7, 278503, 3166358, 161000003, 5),
		new QuestContract(1884, ELYOS_START_ZONE, Set.of(1876, 1883), 8, 278502, 3166358, 188054203, 5),
		new QuestContract(1885, ELYOS_START_ZONE, Set.of(1876, 1884), 9, 278501, 3166358, 188100335, 500),
		new QuestContract(1886, ELYOS_START_ZONE, Set.of(1876, 1885), 10, 278501, 3518176, 188055829, 1),
		new QuestContract(1887, ELYOS_START_ZONE, Set.of(1876, 1886), 15, 278501, 3518176, 188055830, 1),
		new QuestContract(2877, ASMODIAN_START_ZONE, Set.of(2876), 1, 278016, 3166358, 162000027, 10),
		new QuestContract(2878, ASMODIAN_START_ZONE, Set.of(2876, 2877), 2, 278017, 3166358, 162000050, 10),
		new QuestContract(2879, ASMODIAN_START_ZONE, Set.of(2876, 2878), 3, 278016, 3166358, 164000079, 10),
		new QuestContract(2880, ASMODIAN_START_ZONE, Set.of(2876, 2879), 4, 278017, 3166358, 169000010, 100),
		new QuestContract(2881, ASMODIAN_START_ZONE, Set.of(2876, 2880), 5, 278016, 3166358, 160002274, 10),
		new QuestContract(2882, ASMODIAN_START_ZONE, Set.of(2876, 2881), 6, 278017, 3166358, 164000095, 5),
		new QuestContract(2883, ASMODIAN_START_ZONE, Set.of(2876, 2882), 7, 278016, 3166358, 161000003, 5),
		new QuestContract(2884, ASMODIAN_START_ZONE, Set.of(2876, 2883), 8, 278017, 3166358, 188054203, 5),
		new QuestContract(2885, ASMODIAN_START_ZONE, Set.of(2876, 2884), 9, 278001, 3166358, 188100335, 500),
		new QuestContract(2886, ASMODIAN_START_ZONE, Set.of(2876, 2885), 10, 278001, 3518176, 188055829, 1),
		new QuestContract(2887, ASMODIAN_START_ZONE, Set.of(2876, 2886), 15, 278001, 3518176, 188055830, 1));

	@AfterAll
	static void restoreSwitch() {
		System.clearProperty("aion.quest.retailDriver");
	}

	@TestFactory
	Stream<DynamicTest> matchesRetailRankedGridContracts() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	/** 区域发放登记：每个军令任务必须绑定 quest_area（RetailAreaEngine 进区域发放的证据）。 */
	@Test
	void everyUrgentOrderStaysAreaGranted() throws Exception {
		RetailQuestAreaIndex areas;
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/definitions/compact/ai/ai-areas.xml")) {
			areas = RetailQuestAreaIndex.load(input);
		}
		for (QuestContract contract : CONTRACTS) {
			assertTrue(areas.isBound(contract.questId()),
				() -> contract.questId() + " 必须有 quest_area 绑定（进区域发放）");
		}
	}

	private static void assertContract(QuestContract contract) {
		QuestDefinition definition = definition(contract.questId());
		// 真端链式开放（quest.xml startConditions）：1876（链根）与上一位军令 finished，其余兄弟
		// noacquired（排除自身与链根）；前置锁由发放侧 StartEligible + 链条件共同表达。
		// Retail chain gating via quest.xml startConditions: root + previous finished, other siblings
		// not-acquired; prerequisites are expressed by StartEligible plus these chain conditions.
		int chainBase = contract.questId() / 1000 * 1000 + 876;
		Set<Integer> finished = new java.util.TreeSet<>();
		Set<Integer> noacquired = new java.util.TreeSet<>();
		var groups = definition.metadata().startConditionGroups();
		assertEquals(1, groups.size());
		for (QuestStartCondition condition : groups.getFirst().conditions()) {
			switch (condition.type()) {
				case "finished" -> finished.add(condition.questId());
				case "noacquired" -> noacquired.add(condition.questId());
				default -> fail("未知开始条件类型 " + condition.type());
			}
		}
		Set<Integer> expectedFinished = new java.util.TreeSet<>();
		expectedFinished.add(chainBase);
		if (contract.questId() > chainBase + 1) {
			expectedFinished.add(contract.questId() - 1);
		}
		assertEquals(expectedFinished, finished,
			() -> contract.questId() + " finished 链条件");
		Set<Integer> expectedNoacquired = new java.util.TreeSet<>();
		for (int sibling = chainBase + 1; sibling <= chainBase + 11; sibling++) {
			if (sibling != contract.questId()) {
				expectedNoacquired.add(sibling);
			}
		}
		assertEquals(expectedNoacquired, noacquired,
			() -> contract.questId() + " noacquired 链条件");
		assertEquals(List.of(
			new QuestReward("EXP", 0, contract.exp()),
			new QuestReward("ITEM", contract.rewardItemId(), contract.rewardItemCount())),
			definition.metadata().rewards());

		// 网格节点：unaccepted + a0..a3（var0 = 已击杀数 0..3）+ reward(3) + complete(0)。
		// Grid nodes: unaccepted + a0..a3 (var0 = kills 0..3) + reward(3) + complete(0).
		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		for (int kills = 0; kills <= 3; kills++) {
			assertNode(definition, "a" + kills, QuestStatus.START, kills);
		}
		assertNode(definition, "reward", QuestStatus.REWARD, 3);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		// 接取 = 进区域系统发放（quest_area 由 RetailAreaEngine 消费），无任何 NPC 接取路由。
		// Acquire = area-entry system grant (quest_area consumed by RetailAreaEngine), no NPC route.
		QuestTransition grant = transition(definition, "unaccepted", "a0",
			new QuestEvent.SystemGrant());
		assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions());
		assertEquals(List.of(), grant.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			grant.afterCommit());
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.sourceNode().equals("unaccepted")
				&& candidate.event() instanceof QuestEvent.TalkToNpc),
			"系统发放行不得有 NPC 接取路由");

		// 军衔阈值击杀网格：a0→a1→a2→a3，每步 KillRanked(value1) + PACKET_ONLY。
		// Ranked-threshold kill grid: a0→a1→a2→a3, each KillRanked(value1) + PACKET_ONLY.
		assertKillStep(definition, "a0", "a1", contract.rankId());
		assertKillStep(definition, "a1", "a2", contract.rankId());
		assertKillStep(definition, "a2", "a3", contract.rankId());

		// 网格自续：任意未满格节点的 QUEST_SELECT 只发报告页，不推进。
		// Grid continuation: QUEST_SELECT on any node re-shows the report page without advancing.
		for (int kills = 0; kills <= 3; kills++) {
			String node = "a" + kills;
			QuestTransition loop = talk(definition, node, node, contract.rewardNpcId(),
				QuestDialogAction.QUEST_SELECT.id());
			assertEmptyRoute(loop);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
				loop.afterCommit());
		}

		// 满格恢复：未满格节点的 1009 必须带 var0=3 门禁；a3 的 1009 无门禁直接进领奖态。
		// Full-count recovery: 1009 from unfinished nodes must gate on var0=3; a3's is unconditional.
		for (int kills = 0; kills < 3; kills++) {
			QuestTransition gated = talk(definition, "a" + kills, "reward", contract.rewardNpcId(),
				QuestDialogAction.SELECT_QUEST_REWARD.id());
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 3)), gated.conditions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), gated.afterCommit());
		}
		QuestTransition finish = talk(definition, "a3", "reward", contract.rewardNpcId(),
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEmptyRoute(finish);
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			finish.afterCommit());

		// 领奖态预览：NPC 默认对话与 1009 都只弹选择窗口，不推进。
		// Reward-state previews: the NPC default dialog and 1009 only show the selection window.
		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = talk(definition, "reward", "reward", contract.rewardNpcId(),
				action.id());
			assertEmptyRoute(preview);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		// 完成流：SELECTED_QUEST_REWARD1..NOREWARD(8..23) 逐个发放 EXP+ITEM 并完成。
		// Completion: SELECTED_QUEST_REWARD1..NOREWARD(8..23) each grants EXP+ITEM and completes.
		List<QuestAction> completionActions = List.of(
			new QuestAction.GrantReward("EXP", 0, contract.exp(), QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", contract.rewardItemId(), contract.rewardItemCount(),
				QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0));
		List<AfterCommitAction> completionAfterCommit = List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id()));
		for (int action = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				action <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); action++) {
			QuestTransition completion = talk(definition, "reward", "complete", contract.rewardNpcId(), action);
			assertEquals(List.of(), completion.conditions());
			assertEquals(completionActions, completion.actions());
			assertEquals(completionAfterCommit, completion.afterCommit());
		}

		// 真端网格族无自动领奖旁路（迁移期 XML 有 108 路线；真端形状未提供）。
		// The retail grid family has no auto-reward bypass (the migrated XML had a 108 route).
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.QuestDialog(int id) && id == 108),
			"真端形状不得有自动领奖旁路");
		assertEquals(30, definition.transitions().size());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static void assertKillStep(QuestDefinition definition, String source, String target, int minRank) {
		QuestTransition kill = transition(definition, source, target, new QuestEvent.KillRanked(minRank));
		// 真端等级窗（ScriptDLL "PvP Target Level Gap"）：紧急军令 value3=5 → 单边上界
		// killer - victim <= 5、无下界。
		// Retail level window (ScriptDLL "PvP Target Level Gap"): the urgent orders carry value3=5,
		// i.e. one-sided upper bound killer - victim <= 5 with no lower bound.
		assertEquals(List.of(new QuestCondition.PvpVictimLevelDelta(Integer.MIN_VALUE, 5)),
			kill.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kill.afterCommit());
	}

	private static void assertEmptyRoute(QuestTransition transition) {
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	/** 生产驱动定义（retail table value1 = 军衔阈值）。 / The production-driver definition. */
	private static QuestDefinition definition(int questId) {
		System.setProperty("aion.quest.retailDriver", "true");
		if (RetailQuestDriver.current().isEmpty()) {
			RetailQuestDriver.overlay(com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog
				.fromEntries(List.of()));
		}
		var compiled = RetailQuestDriver.current().orElseThrow().definition(questId);
		assertTrue(compiled.isPresent(), () -> "生产驱动缺少 " + questId + " 的定义");
		return compiled.orElseThrow().definition();
	}

	/**
	 * 保存每个连续紧急军令任务的权威差异字段（rankId = KillRanked 最低阈值）。
	 * Holds the authoritative fields of each urgent order (rankId = the KillRanked minimum threshold).
	 */
	private record QuestContract(int questId, String startZone, Set<Integer> prerequisites, int rankId, int rewardNpcId,
			long exp, int rewardItemId, int rewardItemCount) {
	}
}
