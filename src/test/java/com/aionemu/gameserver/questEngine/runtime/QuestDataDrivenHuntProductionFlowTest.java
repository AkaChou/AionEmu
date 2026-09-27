package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
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
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

import org.junit.jupiter.api.AfterAll;
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
 * 验证档案馆 data-driven hunt 任务（16805/26805/16807/26807）的真端区域网格合同（定义来自生产驱动）：
 * 进区域系统发放接取；击杀网格 a0..aN 逐态推进且击杀集精确覆盖客户端变体（排除集不计数）；第 N 杀
 * 后仍为 START，满格 1009 无门禁进入领奖态；完成流按真端奖励发放并收尾 CompleteQuest。
 * Verifies the retail area-grid contract for the Archives data-driven hunts (definitions from the
 * production driver): area-entry system grant, per-state kill grids covering exactly the client
 * variants (excluded targets never count), reward entry via the ungated full-count 1009 route, and
 * the completion flow granting the retail rewards and ending in CompleteQuest.
 */
class QuestDataDrivenHuntProductionFlowTest {
	private static final Set<Integer> LIBRARIAN_NPC_IDS = Set.of(
		220305, 220308, 220311, 220314, 220317, 220323, 220326, 220329);
	private static final Set<Integer> RELIQUARIAN_NPC_IDS = Set.of(
		220307, 220310, 220313, 220316, 220319, 220325, 220328, 220331, 220411);
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16805, 806148, LIBRARIAN_NPC_IDS, 220305, 220327, 69327360),
		new QuestContract(26805, 806149, LIBRARIAN_NPC_IDS, 220305, 220327, 69327360),
		new QuestContract(16807, 806148, RELIQUARIAN_NPC_IDS, 220307, 220306, 80304192),
		new QuestContract(26807, 806149, RELIQUARIAN_NPC_IDS, 220307, 220306, 80304192));

	@AfterAll
	static void restoreSwitch() {
		System.clearProperty("aion.quest.retailDriver");
	}

	@TestFactory
	Stream<DynamicTest> preservesRetailHuntAndRewardFlow() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) {
		CompiledQuestDefinition compiled = definition(contract.questId());
		QuestDefinition definition = compiled.definition();
		// 网格规模 N 由领奖投影推导（真端 hunt 计数 = a0..aN）。
		// Grid size N derives from the reward projection (the retail hunt counter is a0..aN).
		int required = definition.nodes().stream()
			.filter(node -> node.label().equals("reward"))
			.findFirst().orElseThrow().projection().variables().get("var0");
		assertTrue(required > 0, () -> contract.questId() + " 领奖投影必须携带计数 " + required);

		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		for (int kills = 0; kills <= required; kills++) {
			assertNode(definition, "a" + kills, QuestStatus.START, kills);
		}
		assertNode(definition, "reward", QuestStatus.REWARD, required);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		// 进区域系统发放接取，无 NPC 接取路由。
		// Area-entry system grant, no NPC accept route.
		QuestTransition grant = transition(definition, "unaccepted", "a0", new QuestEvent.SystemGrant());
		assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			grant.afterCommit());

		// 击杀网格：每个未满格状态覆盖全部客户端变体，逐态推进 + PACKET_ONLY。
		// Kill grid: every unfinished state covers the full client variant set, advancing per state.
		Set<Integer> killNpcs = new TreeSet<>();
		for (int kills = 0; kills < required; kills++) {
			final int state = kills;
			List<QuestTransition> edges = definition.transitions().stream()
				.filter(candidate -> candidate.sourceNode().equals("a" + state))
				.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
				.toList();
			assertTrue(edges.size() >= contract.targetNpcIds().size(),
				() -> "a" + state + " 击杀边必须覆盖全部变体");
			for (QuestTransition edge : edges) {
				assertEquals("a" + (state + 1), edge.targetNode());
				if (edge.event() instanceof QuestEvent.KillNpc killNpc) {
					killNpcs.add(killNpc.npcId());
				}
			}
		}
		// 同名显示名族展开（M2-c 裁定）：真端名解析后并入本服实刷的同名兄弟 id，
		// 因此合同断言为"覆盖客户端变体"的超集，而非精确相等。
		// Display-name family expansion (M2-c adjudication): the retail names resolve to include
		// server-spawned sibling ids sharing the display name, so the contract asserts a superset
		// covering the client variants rather than exact equality.
		assertTrue(killNpcs.containsAll(contract.targetNpcIds()),
			() -> contract.questId() + " 击杀集必须覆盖客户端变体，实际 " + killNpcs);

		// 排除目标不产生任何击杀计划（客户端名单之外不计数）。
		// Excluded targets produce no kill plan (anything outside the client list never counts).
		QuestSnapshot start = snapshot(definition, QuestStatus.START, 0);
		assertTrue(dispatchAll(compiled, start, new QuestEvent.KillNpc(contract.excludedTargetNpcId())).isEmpty(),
			() -> contract.excludedTargetNpcId() + " 不得计数");

		// 模拟：N 次有效击杀逐态推进，第 N 杀后仍在 START（真端把领奖入口放在 1009 之后）。
		// Simulation: N valid kills advance state by state; after the Nth the quest is still START
		// (the retail shape keeps reward entry behind the 1009 route).
		QuestSnapshot snapshot = start;
		for (int kills = 1; kills <= required; kills++) {
			final int state = kills;
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status(), () -> "第 " + state + " 杀后应为 START");
			assertEquals(Map.of("var0", state), unpack(definition, snapshot));
		}

		// 满格 QUEST_SELECT 交付无门禁进入领奖态（P0-2 规范形，1009 中转删除）；
		// 未满格节点无 QUEST_SELECT/1009 报告通道。
		// The full-node QUEST_SELECT delivery enters reward ungated (canonical since P0-2, no 1009
		// hop); unfinished nodes keep no QUEST_SELECT/1009 report channel.
		QuestTransition finish = talk(definition, "a" + required, "reward", contract.reportNpcId(),
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(), finish.conditions());
		QuestMutationPlan rewardEntry = dispatch(compiled, snapshot, finish.event());
		assertEquals(QuestStatus.REWARD, rewardEntry.nextStatus());
		assertEquals(Map.of("var0", required), unpack(definition, nextSnapshot(snapshot, rewardEntry)));
		for (int kills = 0; kills < required; kills++) {
			final int state = kills;
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
				("a" + state).equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talkRoute
					&& talkRoute.dialogId() != null
					&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> "a" + state + " 不得保留报告通道路由");
		}

		assertRewardAndCompletion(compiled, definition, contract, required);
	}

	private static void assertRewardAndCompletion(CompiledQuestDefinition compiled,
			QuestDefinition definition, QuestContract contract, int required) {
		// 领奖态预览：NPC 默认对话与 1009 都只弹选择窗口。
		// Reward-state previews: the NPC default dialog and 1009 only show the selection window.
		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = transition(definition, "reward", "reward",
				new QuestEvent.TalkToNpc(contract.reportNpcId(), action.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		// 完成流：确认段按真端奖励发放（EXP QUEST_BASE 数额与真端表一致）并收尾 CompleteQuest。
		// Completion: confirm routes grant the retail rewards (EXP QUEST_BASE matching the retail
		// table amount) and end in CompleteQuest.
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == contract.reportNpcId()
				&& talk.dialogId() == QuestDialogAction.SELECTED_QUEST_REWARD1.id())
			.toList();
		assertEquals(1, completions.size());
		QuestTransition completion = completions.getFirst();
		long expAmount = contract.expReward();
		assertTrue(completion.actions().stream().anyMatch(action ->
				action instanceof QuestAction.GrantReward grant
					&& "EXP".equals(grant.kind()) && grant.amount() == expAmount
					&& grant.amountMode() == QuestRewardAmountMode.QUEST_BASE),
			() -> "完成动作必须按真端发放 EXP " + expAmount + "：" + completion.actions());
		assertTrue(completion.actions().stream().anyMatch(action ->
				action instanceof QuestAction.CompleteQuest),
			() -> "完成路由必须收尾 CompleteQuest " + completion);
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		// 完成模拟：REWARD 态走确认路由后 COMPLETE 且计数清零。
		// Completion simulation: the confirm route moves REWARD to COMPLETE with the counter reset.
		QuestSnapshot reward = snapshot(definition, QuestStatus.REWARD, required);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, completion.event(), completion)
			.orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(Map.of("var0", 0), definition.progressLayout().unpack(plan.nextPackedVariables()));
	}

	private static List<QuestMutationPlan> dispatchAll(CompiledQuestDefinition compiled,
			QuestSnapshot snapshot, QuestEvent event) {
		return compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = dispatchAll(compiled, snapshot, event);
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(QuestDefinition definition, QuestStatus status, int var0) {
		return new QuestSnapshot(7, definition.id(), status, definition.progressLayout().pack(
			Map.of("var0", var0)), Map.of());
	}

	private static Map<String, Integer> unpack(QuestDefinition definition, QuestSnapshot snapshot) {
		return definition.progressLayout().unpack(snapshot.packedVariables());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	/** 生产驱动定义。 / The production-driver definition. */
	private static CompiledQuestDefinition definition(int questId) {
		System.setProperty("aion.quest.retailDriver", "true");
		if (RetailQuestDriver.current().isEmpty()) {
			RetailQuestDriver.overlay(ImmutableQuestCatalog.fromEntries(List.of()));
		}
		return RetailQuestDriver.current().orElseThrow().definition(questId).orElseThrow();
	}

	/**
	 * 保存各任务的目标变体、报告 NPC、模拟样本、排除样本与真端 EXP。
	 * Holds per-quest target variants, report NPCs, the simulation sample, the excluded sample, and
	 * the retail EXP.
	 */
	private record QuestContract(int questId, int reportNpcId, Set<Integer> targetNpcIds,
			int sampleTargetNpcId, int excludedTargetNpcId, long expReward) {
	}
}
