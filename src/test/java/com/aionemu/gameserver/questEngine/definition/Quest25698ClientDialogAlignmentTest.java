package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 25698 的真端 DataDriven 网格合同（定义来自生产驱动，P0-2 规范形）：五杀网格逐态
 * 推进且各状态目标集覆盖零售名单，未满格节点无 QUEST_SELECT/1009 报告通道，满格 QUEST_SELECT
 * 无门禁翻领奖（分档奖励窗），网格上没有旁路能提前打开领奖窗口。
 * Verifies the retail DataDriven grid contract for quest 25698 (definition from the production
 * driver, canonical since P0-2): the five-kill grid advances state by state with per-state target
 * sets covering the retail list, unfinished nodes keep no QUEST_SELECT/1009 report channel, the
 * saturated QUEST_SELECT enters reward ungated with the tiered window, and no grid side door opens
 * the reward window early.
 */
class Quest25698ClientDialogAlignmentTest {
	private static final int QUEST_NPC = 806804;
	private static final int KILLS_REQUIRED = 5;
	/** 零售 progress_info 登记的击杀名单（网格必须覆盖的超集下界）。 / Retail progress_info kill list (lower bound the grid must cover). */
	private static final Set<Integer> RETAIL_TARGETS = Set.of(885487, 885488, 885489, 885490);

	@Test
	void gatesTheClientReportPageOnCompletedKillProgress() {
		QuestDefinition definition = load().definition();
		// 网格规模由领奖投影承载（真端把击杀计数投影为 var0=5）。
		// The grid size is carried by the reward projection (the retail counter projects var0=5).
		assertEquals(KILLS_REQUIRED, rewardProjection(definition), "25698 kill counter ceiling");

		// 击杀网格：每个未满格状态覆盖零售名单并逐杀推进到下一状态（PACKET_ONLY）。
		// Kill grid: every unfinished state covers the retail list and advances one kill at a time
		// to the next state (PACKET_ONLY).
		for (int kills = 0; kills < KILLS_REQUIRED; kills++) {
			final int state = kills;
			List<QuestTransition> edges = killEdges(definition, state);
			assertFalse(edges.isEmpty(), () -> "a" + state + " 必须携带击杀边");
			Set<Integer> targets = new TreeSet<>();
			for (QuestTransition edge : edges) {
				assertEquals("a" + (state + 1), edge.targetNode());
				assertEquals(List.of(), edge.conditions());
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					edge.afterCommit());
				targets.add(((QuestEvent.KillNpc) edge.event()).npcId());
			}
			assertTrue(targets.containsAll(RETAIL_TARGETS),
				() -> "a" + state + " 击杀集必须覆盖零售名单，实际 " + targets);
		}

		// 未满格无 QUEST_SELECT/1009 报告通道（P0-2 规范形，页链不再由服务端驱动）。
		// Incomplete nodes keep no QUEST_SELECT/1009 report channel (canonical since P0-2; pages
		// are no longer server-driven).
		for (int kills = 0; kills < KILLS_REQUIRED; kills++) {
			final int state = kills;
			assertTrue(definition.transitions().stream().noneMatch(transition ->
				("a" + state).equals(transition.sourceNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talkRoute
					&& talkRoute.npcId() == QUEST_NPC && talkRoute.dialogId() != null
					&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> "a" + state + " 不得保留报告通道路由");
		}
		// 满格节点：QUEST_SELECT 无条件直翻领奖并按档位查表下发奖励窗（P0-2 规范形，1009 中转删除）。
		// Saturated node: the QUEST_SELECT flips reward ungated with the tiered window (canonical
		// since P0-2, no 1009 hop).
		QuestTransition finish = talk(definition, "a" + KILLS_REQUIRED, QuestDialogAction.QUEST_SELECT.id());
		assertEquals("reward", finish.targetNode());
		assertEquals(List.of(), finish.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			finish.afterCommit());

		// 反向断言：网格节点上除满段 QUEST_SELECT 交付与 1009 预览外的任何路由都不得打开领奖窗口。
		// Negative side: no grid route other than the delivery QUEST_SELECT and the 1009 preview may
		// open the reward window.
		assertEquals(0, definition.transitions().stream()
			.filter(transition -> isGridNode(definition, transition.sourceNode()))
			.filter(transition -> !(transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == QUEST_NPC && (Integer.valueOf(QuestDialogAction.SELECT_QUEST_REWARD.id())
					.equals(talk.dialogId())
					|| Integer.valueOf(QuestDialogAction.QUEST_SELECT.id()).equals(talk.dialogId()))))
			.filter(transition -> transition.afterCommit().stream()
				.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog show
					&& show.dialogId() == QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()))
			.count(), "no non-delivery grid route may open the reward window");
	}

	/** 领奖投影携带的击杀网格规模。 / The kill-grid size carried by the reward projection. */
	private static int rewardProjection(QuestDefinition definition) {
		return definition.nodes().stream()
			.filter(node -> "reward".equals(node.label()))
			.findFirst().orElseThrow().projection().variables().get("var0");
	}

	private static boolean isGridNode(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.anyMatch(node -> node.label().equals(label)
				&& node.projection().status() == QuestStatus.START);
	}

	private static List<QuestTransition> killEdges(QuestDefinition definition, int state) {
		return definition.transitions().stream()
			// 生产定义可能带无 source 的自愈边，按 source 过滤时必须容忍 null。
			// Production definitions may carry source-less recovery edges, so null sources are skipped.
			.filter(transition -> Objects.equals(transition.sourceNode(), "a" + state))
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
			.toList();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int action) {
		return definition.transitions().stream()
			.filter(transition -> Objects.equals(transition.sourceNode(), source))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(QUEST_NPC, action)))
			.findFirst().orElseThrow();
	}

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definition(25698);
	}
}
