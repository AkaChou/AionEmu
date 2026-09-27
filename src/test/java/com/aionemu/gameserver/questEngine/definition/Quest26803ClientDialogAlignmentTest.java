package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 26803（魔族侧 3단계 보상 구간 일반몹）的真端区域网格合同（定义来自生产驱动）：
 * <ul>
 * <li>接取 = 世界 quest_area 进区域系统发放（{@code SystemGrant} + {@code StartEligible}，无 NPC 接取
 * 路由，迁移期的 EnterWorld/WorldIs/链条件随退役入 git 历史）；</li>
 * <li>进度 = 30 杀网格 a0..a30，击杀集精确覆盖客户端 9 个变体，
 * 与 16804/26803/26807 同族一致；</li>
 * <li>满格恢复：未满格节点的 1009 带 {@code var0=30} 门禁，a30 无门禁；完成流 = 确认段 8..23 按职业
 * 条件展开（11 职业 × 16 = 176 条），无自动领奖旁路。</li>
 * </ul>
 * Verifies the retail area-grid contract for quest 16803 (definitions from the production driver):
 * area-entry system grant, a 30-kill grid a0..a30 whose kill set covers the nine client variants,
 * full-count-gated 1009 recovery routes, and the class-expanded completion flow.
 */
class Quest26803ClientDialogAlignmentTest {
	private static final int QUEST_ID = 26803;
	private static final int DIALOG_NPC_ID = 806149;
	private static final int REQUIRED_KILLS = 30;
	private static final int EXPECTED_NODES = 34;
	private static final int EXPECTED_TRANSITIONS = 511;
	// 击杀集合必须覆盖客户端 9 个变体：第 9 个是 Cube Artifact(220411)，与 16807/26803/26807 同族一致。
	// The kill set must cover 客户端 9 个变体; the ninth is the Cube Artifact (220411).
	private static final Set<Integer> RELIQUARIANS = Set.of(
			220307, 220310, 220313, 220316, 220319, 220325, 220328, 220331, 220411);

	@AfterAll
	static void restoreSwitch() {
		System.clearProperty("aion.quest.retailDriver");
	}

	@Test
	void areaGrantStartsTheGridWithoutNpcRoutes() {
		QuestDefinition definition = definition().definition();
		assertEquals(EXPECTED_NODES, definition.nodes().size());
		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		for (int kills = 0; kills <= REQUIRED_KILLS; kills++) {
			assertNode(definition, "a" + kills, QuestStatus.START, kills);
		}
		assertNode(definition, "reward", QuestStatus.REWARD, REQUIRED_KILLS);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		QuestTransition grant = transition(definition, "unaccepted", "a0",
			new QuestEvent.SystemGrant());
		assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			grant.afterCommit());
		// 进区域发放行不得有 NPC 接取路由。
		// Area-granted rows must not carry any NPC accept route.
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.sourceNode().equals("unaccepted")
				&& candidate.event() instanceof QuestEvent.TalkToNpc),
			"系统发放行不得有 NPC 接取路由");
	}

	@Test
	void killGridCoversAllNineClientVariants() {
		QuestDefinition definition = definition().definition();
		Set<Integer> killNpcs = new TreeSet<>();
		for (int kills = 0; kills < REQUIRED_KILLS; kills++) {
			final int state = kills;
			List<QuestTransition> edges = definition.transitions().stream()
				.filter(candidate -> candidate.sourceNode().equals("a" + state))
				.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
				.toList();
			assertEquals(RELIQUARIANS.size(), edges.size(), () -> "a" + state + " 击杀边数");
			for (QuestTransition edge : edges) {
				assertEquals("a" + (state + 1), edge.targetNode(), () -> "a" + state + " 击杀推进目标");
				killNpcs.add(((QuestEvent.KillNpc) edge.event()).npcId());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					edge.afterCommit());
			}
		}
		assertEquals(RELIQUARIANS, killNpcs, "击杀集必须精确覆盖客户端 9 个变体");
		assertTrue(definition.transitions().stream()
			.filter(candidate -> "a30".equals(candidate.sourceNode())
				&& candidate.event() instanceof QuestEvent.KillNpc)
			.toList().isEmpty(), "满格节点不得再有击杀边");
	}

	@Test
	void fullCountGatesRecoveryAndCompletionStaysClassExpanded() {
		QuestDefinition definition = definition().definition();
		// 满格恢复：未满格节点的 1009 必须带 var0=30 门禁；a30 无门禁直接进领奖态。
		// Full-count recovery: 1009 from unfinished nodes must gate on var0=30; a30's is unconditional.
		for (int kills = 0; kills < REQUIRED_KILLS; kills++) {
			final int state = kills;
			QuestTransition gated = talk(definition, "a" + state, "reward",
				QuestDialogAction.SELECT_QUEST_REWARD.id());
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", REQUIRED_KILLS)),
				gated.conditions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), gated.afterCommit());
		}
		QuestTransition finish = talk(definition, "a" + REQUIRED_KILLS, "reward",
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(), finish.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			finish.afterCommit());

		// 领奖态预览：NPC 默认对话与 1009 都只弹选择窗口。
		// Reward-state previews: the NPC default dialog and 1009 only show the selection window.
		for (QuestDialogAction action : List.of(QuestDialogAction.USE_OBJECT,
				QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = talk(definition, "reward", "reward", action.id());
			assertEquals(List.of(), preview.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
		}

		// 完成流：确认段 8..23 × 11 职业条件展开（176 条），固定奖励 + CompleteQuest 收尾。
		// Completion: confirm ids 8..23 expanded per class condition (176 routes) ending in CompleteQuest.
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "complete".equals(candidate.targetNode()))
			.toList();
		assertEquals(176, completions.size());
		Set<Integer> actionIds = new TreeSet<>();
		for (QuestTransition completion : completions) {
			assertTrue(completion.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == DIALOG_NPC_ID
					&& talk.dialogId() >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& talk.dialogId() <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(),
				() -> "完成路由必须挂确认段 " + completion.event());
			assertEquals(1, completion.conditions().size(), () -> "完成路由按职业条件展开 " + completion);
			assertTrue(completion.conditions().get(0) instanceof QuestCondition.AdvancedClassIs,
				() -> "完成条件必须是职业条件 " + completion.conditions());
			assertTrue(completion.actions().stream().anyMatch(action ->
					action instanceof QuestAction.CompleteQuest),
				() -> "完成路由必须收尾 CompleteQuest " + completion);
			assertEquals(List.of(new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
			if (completion.event() instanceof QuestEvent.TalkToNpc talk) {
				actionIds.add(talk.dialogId());
			}
		}
		for (int id = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
				id <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id(); id++) {
			final int dialogId = id;
			assertTrue(actionIds.contains(dialogId), () -> "确认段缺少 dialogId " + dialogId);
		}
	}

	@Test
	void incompleteProgressCannotReachRewardOrTheRewardWindow() {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		for (int kills : List.of(0, 15, REQUIRED_KILLS - 1)) {
			Map<String, Integer> variables = Map.of("var0", kills);
			QuestSnapshot snapshot = snapshot(definition, variables);
			for (QuestTransition route : definition.transitions()) {
				if (!(route.event() instanceof QuestEvent.TalkToNpc)) {
					continue;
				}
				QuestMutationPlanner.plan(compiled, snapshot, route.event(), route).ifPresent(plan -> {
					boolean rewards = plan.nextStatus() == QuestStatus.REWARD
						|| plan.nextStatus() == QuestStatus.COMPLETE
						|| plan.requiredActions().stream().anyMatch(action ->
							action instanceof QuestAction.GrantReward
								|| action instanceof QuestAction.GrantSelectedReward
								|| action instanceof QuestAction.CompleteQuest)
						|| plan.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
							QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
					assertFalse(rewards, () -> "quest " + QUEST_ID + " premature reward at " + variables
						+ " via " + route);
				});
			}
		}
	}

	private static QuestSnapshot snapshot(QuestDefinition definition, Map<String, Integer> variables) {
		return new QuestSnapshot(7, definition.id(), QuestStatus.START,
			definition.progressLayout().pack(variables), Map.of());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action)))
			.findFirst().orElseThrow(() -> new AssertionError(source + " -> " + target + " action " + action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static CompiledQuestDefinition definition() {
		System.setProperty("aion.quest.retailDriver", "true");
		if (RetailQuestDriver.current().isEmpty()) {
			RetailQuestDriver.overlay(com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog
				.fromEntries(List.of()));
		}
		return RetailQuestDriver.current().orElseThrow().definition(QUEST_ID).orElseThrow();
	}
}
