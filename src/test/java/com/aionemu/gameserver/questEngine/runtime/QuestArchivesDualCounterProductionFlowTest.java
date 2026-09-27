package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestRewardAmountMode;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证档案馆双计数任务（16806/26806）的顺序 SECTION 链合同（定义来自生产驱动）：进区域系统发放；
 * 段 1 = 30 只士兵族（链态 a{0..30}b0），段 2 = 2 只 Fire named（a30b{1..2}）；乱序不计数——
 * 士兵未打满前 Boss 击杀不产生任何计划；满段 1009 无门禁进领奖，完成流发放真端奖励。
 * Verifies the sequential SECTION-chain contract for the Archives dual-counter quests (16806/26806,
 * definitions from the production driver): area-entry system grant; stage 1 = 30 soldiers
 * (chain states a{0..30}b0), stage 2 = 2 Fire named (a30b{1..2}); out-of-order kills never count —
 * boss kills produce no plan before the soldier section fills; the full node's 1009 enters reward
 * ungated and completion grants the retail rewards.
 */
class QuestArchivesDualCounterProductionFlowTest {
	private static final int SOLDIER_COUNT = 30;
	private static final int BOSS_COUNT = 2;
	private static final Set<Integer> SOLDIER_NPC_IDS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	private static final Set<Integer> BOSS_NPC_IDS = Set.of(
		857450, 857452, 857454, 857456, 857458, 857459);
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16806, 806148),
		new QuestContract(26806, 806149));

	@TestFactory
	Stream<DynamicTest> walksTheSequentialSectionChainInRetailStageOrder() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> zero = Map.of("var0", 0, "var1", 0);
		Map<String, Integer> soldiersFull = Map.of("var0", SOLDIER_COUNT, "var1", 0);
		Map<String, Integer> full = Map.of("var0", SOLDIER_COUNT, "var1", BOSS_COUNT);
		assertNode(definition, "unaccepted", QuestStatus.NONE, zero);
		assertNode(definition, "a0b0", QuestStatus.START, zero);
		assertNode(definition, "a30b0", QuestStatus.START, soldiersFull);
		assertNode(definition, "a30b1", QuestStatus.START,
			Map.of("var0", SOLDIER_COUNT, "var1", 1));
		assertNode(definition, "a30b2", QuestStatus.START, full);
		assertNode(definition, "reward", QuestStatus.REWARD, full);
		assertNode(definition, "complete", QuestStatus.COMPLETE, zero);

		// 进区域系统发放接取，无 NPC 接取路由。
		// Area-entry system grant, no NPC accept route.
		QuestTransition grant = transition(definition, "unaccepted", "a0b0",
			new QuestEvent.SystemGrant(), null);
		assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			grant.afterCommit());

		// 段 1 击杀边：a{0..29}b0 覆盖士兵变体（同名族超集），逐态推进；段 2 边挂在 a30b{0..1}。
		// Stage-1 kill edges: a{0..29}b0 covers the soldier variants (display-name superset),
		// state by state; stage-2 edges hang off a30b{0..1}.
		Set<Integer> soldierTargets = new TreeSet<>();
		for (int state = 0; state < SOLDIER_COUNT; state++) {
			final int kills = state;
			List<QuestTransition> edges = killEdges(definition, "a" + kills + "b0");
			assertTrue(edges.size() >= SOLDIER_NPC_IDS.size(),
				() -> "a" + kills + "b0 必须覆盖全部士兵变体");
			for (QuestTransition edge : edges) {
				assertEquals("a" + (kills + 1) + "b0", edge.targetNode(), edge::toString);
				if (edge.event() instanceof QuestEvent.KillNpc killNpc) {
					soldierTargets.add(killNpc.npcId());
				}
			}
		}
		assertTrue(soldierTargets.containsAll(SOLDIER_NPC_IDS),
			() -> contract.questId() + " 士兵击杀集必须覆盖真端变体，实际 " + soldierTargets);
		Set<Integer> bossTargets = new TreeSet<>();
		for (int state = 0; state < BOSS_COUNT; state++) {
			final int kills = state;
			List<QuestTransition> edges = killEdges(definition, "a" + SOLDIER_COUNT + "b" + kills);
			assertTrue(edges.size() >= BOSS_NPC_IDS.size(),
				() -> "a30b" + kills + " 必须覆盖全部 Boss 变体");
			for (QuestTransition edge : edges) {
				assertEquals("a" + SOLDIER_COUNT + "b" + (kills + 1), edge.targetNode(), edge::toString);
				if (edge.event() instanceof QuestEvent.KillNpc killNpc) {
					bossTargets.add(killNpc.npcId());
				}
			}
		}
		assertTrue(bossTargets.containsAll(BOSS_NPC_IDS),
			() -> contract.questId() + " Boss 击杀集必须覆盖真端变体，实际 " + bossTargets);

		// 乱序不计数：士兵段进行中，Boss 击杀不产生任何计划（客户端 SECTION 链门控）。
		// Out-of-order kills never count: while the soldier section runs, a boss kill produces
		// no plan at all (the client's SECTION chain gate).
		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START, zero, definition);
		assertTrue(killEdges(definition, "a0b0").stream().noneMatch(edge ->
				edge.event() instanceof QuestEvent.KillNpc killNpc && BOSS_NPC_IDS.contains(killNpc.npcId())),
			() -> contract.questId() + " 段 1 不得登记 Boss 击杀边");
		assertNoMatch(compiled, snapshot, new QuestEvent.KillNpc(857450));

		// 顺序推进模拟：30 只士兵逐态推进后，2 只 Boss 收尾。
		// Sequential walk: 30 soldier kills advance state by state, then 2 boss kills finish.
		for (int kills = 1; kills <= SOLDIER_COUNT; kills++) {
			final int state = kills;
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(220306)));
			assertEquals(QuestStatus.START, snapshot.status(), () -> "第 " + state + " 杀后应为 START");
			assertEquals(Map.of("var0", state, "var1", 0),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}
		for (int kills = 1; kills <= BOSS_COUNT; kills++) {
			snapshot = nextSnapshot(snapshot, dispatch(compiled, snapshot, new QuestEvent.KillNpc(857450)));
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", SOLDIER_COUNT, "var1", kills),
				definition.progressLayout().unpack(snapshot.packedVariables()));
		}

		// 满段 QUEST_SELECT 无门禁直翻领奖（P0-2 顺序链规范形，1009 中转删除）；
		// 未满节点无 QUEST_SELECT/1009 报告通道（提前上交不可达）。
		// The full node's QUEST_SELECT flips reward ungated (canonical sequential shape, no 1009
		// hop); unfinished nodes keep no QUEST_SELECT/1009 report channel.
		QuestTransition finish = transition(definition, "a30b2", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.QUEST_SELECT.id()), null);
		assertEquals(List.of(), finish.conditions());
		for (String label : List.of("a0b0", "a15b0", "a30b0", "a30b1")) {
			final String node = label;
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
				node.equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == contract.reportNpcId() && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> node + " 不得保留报告通道路由");
		}

		assertRewardAndCompletion(compiled, definition, contract, full);
	}

	private static void assertRewardAndCompletion(CompiledQuestDefinition compiled,
			QuestDefinition definition, QuestContract contract, Map<String, Integer> full) {
		// 未满节点无 QUEST_SELECT/1009 报告通道（P0-2 顺序链规范形）。
		// Unfinished nodes keep no QUEST_SELECT/1009 report channel (canonical sequential shape).
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			"a0b0".equals(candidate.sourceNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == contract.reportNpcId() && talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "a0b0 不得保留报告通道路由");

		// 满段节点 QUEST_SELECT 直翻领奖；领奖态 1009 预览只弹选择窗口（完成流保留）。
		// The full node's QUEST_SELECT flips reward directly; the reward-state 1009 preview from
		// the completion flow only opens the selection window.
		QuestTransition success = transition(definition, "a30b2", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.QUEST_SELECT.id()), null);
		assertEquals(List.of(), success.conditions());
		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()), null);
		assertEquals(List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			preview.afterCommit());

		QuestEvent completionEvent = new QuestEvent.TalkToNpc(
			contract.reportNpcId(), QuestDialogAction.SELECTED_QUEST_REWARD1.id());
		QuestTransition completion = transition(definition, "reward", "complete", completionEvent, null);
		List<QuestAction> expectedActions = List.of(
			new QuestAction.GrantReward("EXP", 0, 74815776, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 166100009, 10),
			new QuestAction.CompleteQuest(0));
		assertEquals(expectedActions, completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		// 完成模拟：REWARD 态确认后 COMPLETE 且计数清零。
		// Completion simulation: the confirm route moves REWARD to COMPLETE with counters reset.
		QuestSnapshot reward = snapshot(contract.questId(), QuestStatus.REWARD, full, definition);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, completionEvent, completion)
			.orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 0),
			definition.progressLayout().unpack(plan.nextPackedVariables()));
	}

	private static List<QuestTransition> killEdges(QuestDefinition definition, String source) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
			.toList();
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static void assertNoMatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		assertTrue(compiled.definition().transitions().stream().noneMatch(transition ->
			QuestMutationPlanner.plan(compiled, snapshot, event, transition).isPresent()));
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status,
			Map<String, Integer> variables, QuestDefinition definition) {
		return new QuestSnapshot(7, questId, status, definition.progressLayout().pack(variables), Map.of());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, Integer priority) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source))
			.filter(candidate -> Objects.equals(candidate.targetNode(), target))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> priority == null || Objects.equals(candidate.priority(), priority))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event + " " + priority);
		return matches.getFirst();
	}

	/** 生产驱动定义（XML 已退役）。 / The production-driver definition (the XML is retired). */
	private static CompiledQuestDefinition load(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}

	private record QuestContract(int questId, int reportNpcId) {
	}
}
