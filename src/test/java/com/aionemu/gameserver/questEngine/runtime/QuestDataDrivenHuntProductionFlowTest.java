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
import com.aionemu.gameserver.questEngine.retail.RetailClientHuntProgressRows;
import com.aionemu.gameserver.questEngine.definition.RetailHuntLadderShape;

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
		// 形状 = 真端行阶梯：客户端任务书登记 `Progress(SECTION_0==0; SECTION_1<count)`，因此击杀
		// 计数落在 var1、var0 是行号（收口时推进到 1），领奖行钉住满计数（QE-051）。
		// Shape = the retail row ladder: the client journal declares
		// `Progress(SECTION_0==0; SECTION_1<count)`, so kills count on var1 with var0 as the row
		// index (advanced to 1 when the row closes) and the reward row pins the saturated count.
		List<RetailClientHuntProgressRows.Row> clientRows =
			RetailClientHuntProgressRows.defaultHuntProgressRows().rows(contract.questId());
		assertEquals(1, clientRows.size(), () -> contract.questId() + " 客户端任务书必须只登记一行");
		List<List<Integer>> ladder = List.of(List.of(clientRows.getFirst().count()));
		RetailHuntLadderShape.assertLadder(compiled, ladder);
		int required = ladder.getFirst().getFirst();

		// 进区域系统发放接取，无 NPC 接取路由。
		// Area-entry system grant, no NPC accept route.
		QuestTransition grant = transition(definition, "unaccepted", "started", new QuestEvent.SystemGrant());
		assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			grant.afterCommit());

		// 击杀边：行 0 的每个客户端变体一条未满自环 + 一条收口边；覆盖为超集（M2-c 同名族展开）。
		// Kill edges: one unsatisfied self loop and one closing edge per client variant on row 0;
		// coverage is a superset (M2-c display-name family expansion).
		Set<Integer> killNpcs = new TreeSet<>();
		List<QuestTransition> edges = definition.transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
			.toList();
		assertTrue(edges.size() >= contract.targetNpcIds().size(),
			() -> "started 击杀边必须覆盖全部变体");
		for (QuestTransition edge : edges) {
			assertTrue("started".equals(edge.targetNode()) || "reward".equals(edge.targetNode()),
				() -> "行 0 的击杀边只能是自环或收口：" + edge);
			if (edge.event() instanceof QuestEvent.KillNpc killNpc) {
				killNpcs.add(killNpc.npcId());
			}
		}
		assertTrue(killNpcs.containsAll(contract.targetNpcIds()),
			() -> contract.questId() + " 击杀集必须覆盖客户端变体，实际 " + killNpcs);

		// 排除目标不产生任何击杀计划（客户端名单之外不计数）。
		// Excluded targets produce no kill plan (anything outside the client list never counts).
		QuestSnapshot start = snapshot(definition, QuestStatus.START, 0, 0);
		assertTrue(dispatchAll(compiled, start, new QuestEvent.KillNpc(contract.excludedTargetNpcId())).isEmpty(),
			() -> contract.excludedTargetNpcId() + " 不得计数");

		// 模拟：前 N-1 杀留在行 0（var1 累加），第 N 杀直接收口进领奖态并钉住满计数
		// （真端行阶梯的收口边就是领奖入口，不再有 aN 节点或 1009 中转）。
		// Simulation: kills 1..N-1 stay on row 0 (var1 accumulates) and the Nth kill closes the row
		// straight into the reward state with the saturated count pinned (the retail closing edge is
		// the reward entry; there is no aN node and no 1009 hop).
		QuestSnapshot snapshot = start;
		for (int kills = 1; kills < required; kills++) {
			final int counted = kills;
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status(), () -> "第 " + counted + " 杀后应仍在行 0");
			assertEquals(Map.of("var0", 0, "var1", counted), unpack(definition, snapshot));
		}
		QuestMutationPlan closing = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		snapshot = nextSnapshot(snapshot, closing);
		assertEquals(QuestStatus.REWARD, snapshot.status(),
			() -> "第 " + required + " 杀必须收口进领奖态");
		assertEquals(Map.of("var0", 1, "var1", required), unpack(definition, snapshot));

		// 未收口的行不得保留报告通道路由（满段收口即进领奖态，无 1009 / QUEST_SELECT 中转）。
		// The unfinished row keeps no report channel (the closing edge enters the reward state; no
		// 1009 / QUEST_SELECT hop).
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			"started".equals(candidate.sourceNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talkRoute
				&& talkRoute.dialogId() != null
				&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "started 不得保留报告通道路由");

		assertRewardAndCompletion(compiled, definition, contract, required);
	}

	private static void assertRewardAndCompletion(CompiledQuestDefinition compiled,
			QuestDefinition definition, QuestContract contract, int required) {
		// 领奖态预览：真端行阶梯的领奖态入口是交付 NPC 的 QUEST_SELECT(31) 与默认对话(-1)，
		// 两者都只弹分档选择窗（页 5）而不推进状态（旧网格的 USE_OBJECT / SELECT_QUEST_REWARD
		// 预览形随族切换退场）。
		// Reward-state previews: the retail ladder's reward-state entries are the delivery npc's
		// QUEST_SELECT(31) and the default dialog (-1); both only show the tier window (page 5)
		// without advancing (the old grid's USE_OBJECT / SELECT_QUEST_REWARD previews retired).
		for (int dialogId : List.of(QuestDialogAction.QUEST_SELECT.id(), -1)) {
			QuestTransition preview = transition(definition, "reward", "reward",
				new QuestEvent.TalkToNpc(contract.reportNpcId(), dialogId));
			assertEquals(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()), preview.afterCommit().getLast(),
				() -> "领奖态预览必须以分档选择窗收尾：" + preview);
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
		// 领奖态快照 = 行阶梯的领奖投影（var0 = 1 行后、var1 = 末行满计数）。
		// The reward snapshot matches the ladder's reward projection (var0 = the row after the
		// block, var1 = the final row's saturated count).
		QuestSnapshot reward = snapshot(definition, QuestStatus.REWARD, 1, required);
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, reward, completion.event(), completion)
			.orElseThrow();
		assertEquals(QuestStatus.COMPLETE, plan.nextStatus());
		assertEquals(Map.of("var0", 0, "var1", 0),
			definition.progressLayout().unpack(plan.nextPackedVariables()));
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

	private static QuestSnapshot snapshot(QuestDefinition definition, QuestStatus status, int var0, int var1) {
		return new QuestSnapshot(7, definition.id(), status, definition.progressLayout().pack(
			Map.of("var0", var0, "var1", var1)), Map.of());
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
