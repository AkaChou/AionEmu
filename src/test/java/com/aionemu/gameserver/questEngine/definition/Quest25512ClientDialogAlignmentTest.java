package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 25512 的真端 DataDriven 网格合同（定义来自生产驱动，P0-2 规范形）：简易接取直落 a0，
 * 30 杀网格逐态推进且各状态目标集一致，未满格无 QUEST_SELECT/1009 报告通道，满段 QUEST_SELECT
 * 无门禁翻领奖（分档奖励窗），对话 owner 仍独占 806105。
 * Verifies the retail DataDriven grid contract for quest 25512 (definition from the production
 * driver, canonical since P0-2): simple accept lands on a0, the thirty-kill grid advances state
 * by state with a uniform per-state target set, unfinished nodes keep no QUEST_SELECT/1009 report
 * channel, the saturated QUEST_SELECT enters reward ungated with the tiered window, and the
 * dialog owner stays exclusive to 806105.
 */
class Quest25512ClientDialogAlignmentTest {
	private static final int DIALOG_NPC = 806105;

	@Test
	void followsTheClientSimpleAcceptAndRewardPages() throws Exception {
		CompiledQuestDefinition compiled = definition();
		List<QuestTransition> transitions = compiled.definition().transitions();
		int required = requiredKills(compiled);

		// 未接态 QUEST_SELECT 下发的页 = 客户端任务页声明的页（否则客户端 load fail）。
		// The unaccepted QUEST_SELECT page is the client task page's declared entry page (page 4 only
		// where the client declares ask_quest_accept).
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			ClientAcceptEntryPageAssertions.expectedEntryPage(compiled.id()))),
			talk(transitions, "unaccepted", DIALOG_NPC, QuestDialogAction.QUEST_SELECT.id()).afterCommit());
		assertEquals("a0",
			talk(transitions, "unaccepted", DIALOG_NPC, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id()).targetNode());
		assertEquals(List.of(new AfterCommitAction.CloseDialog()),
			talk(transitions, "unaccepted", DIALOG_NPC, QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
				.afterCommit().stream().filter(AfterCommitAction.CloseDialog.class::isInstance).toList());

		// 未满格无 QUEST_SELECT/1009 报告通道（P0-2 规范形，页链不再由服务端驱动）。
		// Incomplete nodes keep no QUEST_SELECT/1009 report channel (canonical since P0-2; pages
		// are no longer server-driven).
		assertTrue(transitions.stream().noneMatch(transition ->
			"a0".equals(transition.sourceNode()) && transition.event() instanceof QuestEvent.TalkToNpc talkRoute
				&& talkRoute.npcId() == DIALOG_NPC && talkRoute.dialogId() != null
				&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "a0 不得保留报告通道路由");

		// 满段 QUEST_SELECT 直翻 REWARD 并按档位查表下发奖励窗（P0-2 规范形，1009 中转删除）。
		// The saturated node's QUEST_SELECT flips REWARD with the tiered window (canonical since
		// P0-2, no 1009 hop).
		QuestTransition deliver = talk(transitions, "a" + required, DIALOG_NPC,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals("reward", deliver.targetNode());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				compiled.definition().metadata().rewardGroups().size() - 1).orElseThrow().id())),
			deliver.afterCommit());
	}

	@Test
	void retainsOnlyTheLegacyAuthoritativeDialogNpc() throws Exception {
		Set<Integer> dialogNpcs = definition().definition().transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(java.util.stream.Collectors.toSet());

		assertEquals(Set.of(DIALOG_NPC), dialogNpcs);
		assertFalse(dialogNpcs.containsAll(Set.of(241547, 241548, 241550, 241551)));
	}

	@Test
	void killGridAdvancesStateByStateBeforeTheRetailReportDialog() throws Exception {
		CompiledQuestDefinition compiled = definition();
		QuestDefinition definition = compiled.definition();
		int required = requiredKills(compiled);

		// 击杀网格：每个未满格状态覆盖同一目标集并逐杀推进到下一状态（PACKET_ONLY）。
		// Kill grid: every unfinished state covers the same target set and advances one kill at a
		// time to the next state (PACKET_ONLY).
		Set<Integer> killNpcs = null;
		for (int kills = 0; kills < required; kills++) {
			final int state = kills;
			List<QuestTransition> edges = definition.transitions().stream()
				.filter(transition -> Objects.equals(transition.sourceNode(), "a" + state)
					&& transition.event() instanceof QuestEvent.KillNpc)
				.toList();
			assertFalse(edges.isEmpty(), "a" + kills + " 必须携带击杀边");
			Set<Integer> stateTargets = new TreeSet<>();
			for (QuestTransition edge : edges) {
				assertEquals("a" + (kills + 1), edge.targetNode());
				assertEquals(List.of(), edge.conditions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					edge.afterCommit());
				stateTargets.add(((QuestEvent.KillNpc) edge.event()).npcId());
			}
			if (killNpcs == null) {
				killNpcs = stateTargets;
			}
			assertEquals(killNpcs, stateTargets, "a" + kills + " 目标集必须与 a0 一致");
		}
		assertFalse(killNpcs.isEmpty());

		// 满格前领奖保持锁闭：未满格无 QUEST_SELECT/1009 报告通道；满格 QUEST_SELECT 直翻领奖。
		// Reward stays locked before the grid fills: unfinished nodes keep no QUEST_SELECT/1009
		// report channel while the saturated node's QUEST_SELECT flips reward directly.
		for (int kills = 0; kills < required; kills++) {
			final int state = kills;
			assertTrue(definition.transitions().stream().noneMatch(transition ->
				("a" + state).equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talkRoute
					&& talkRoute.npcId() == DIALOG_NPC && talkRoute.dialogId() != null
					&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> "a" + state + " 不得保留报告通道路由");
		}
		QuestTransition finish = talk(definition.transitions(), "a" + required, DIALOG_NPC,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals("reward", finish.targetNode());
		assertEquals(List.of(), finish.conditions());

		// 模拟：第 30 杀后仍 START，随后满段 QUEST_SELECT 进入 REWARD（真端把领奖入口放在交付之后）。
		// Simulation: after the 30th kill the quest is still START; the full-node QUEST_SELECT then
		// enters REWARD (the retail shape keeps reward entry behind the delivery).
		int sampleTarget = killNpcs.iterator().next();
		ProgressLayout layout = definition.progressLayout();
		QuestSnapshot snapshot = new QuestSnapshot(7, 25512, QuestStatus.START,
			layout.pack(Map.of("var0", required - 1)), Map.of());
		QuestTransition lastKill = definition.transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), "a" + (required - 1))
				&& transition.event().equals(new QuestEvent.KillNpc(sampleTarget)))
			.findFirst().orElseThrow();
		QuestMutationPlan killPlan = QuestMutationPlanner.plan(compiled, snapshot,
			new QuestEvent.KillNpc(sampleTarget), lastKill).orElseThrow();
		assertEquals(QuestStatus.START, killPlan.nextStatus(),
			"第 " + required + " 杀后应为 START");
		assertEquals(Map.of("var0", required), layout.unpack(killPlan.nextPackedVariables()));
		QuestSnapshot full = new QuestSnapshot(7, 25512, QuestStatus.START,
			killPlan.nextPackedVariables(), Map.of());
		QuestMutationPlan rewardPlan = QuestMutationPlanner.plan(compiled, full, finish.event(), finish)
			.orElseThrow();
		assertEquals(QuestStatus.REWARD, rewardPlan.nextStatus());
	}

	/** 领奖投影携带的击杀网格规模。 / The kill-grid size carried by the reward projection. */
	private static int requiredKills(CompiledQuestDefinition compiled) {
		return compiled.definition().nodes().stream()
			.filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow().projection().variables().get("var0");
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			int action) {
		return transitions.stream()
			// 生产定义带无 source 的 enter-world 自愈边（QE-046），按 source 过滤时必须容忍 null。
			// Production definitions carry source-less enter-world recovery edges, so null sources are skipped.
			.filter(transition -> Objects.equals(transition.sourceNode(), source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private CompiledQuestDefinition definition() {
		return ProductionQuestDefinitions.definition(25512);
	}
}
