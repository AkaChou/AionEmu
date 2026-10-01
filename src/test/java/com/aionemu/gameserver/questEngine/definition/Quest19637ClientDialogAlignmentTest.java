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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 19637 的真端 DataDriven 网格合同（定义来自生产驱动，P0-2 规范形）：接取直落 a0，
 * 10 杀网格逐态推进且击杀集覆盖客户端变体，未满格节点无 QUEST_SELECT/1009 报告通道，满段
 * QUEST_SELECT 无门禁翻领奖（分档奖励窗），以及客户端对话合同。
 * Verifies the retail DataDriven grid contract for quest 19637 (definition from the production
 * driver, canonical since P0-2): accept lands on a0, the ten-kill grid advances state by state
 * with kill sets covering the client variants, unfinished nodes keep no QUEST_SELECT/1009 report
 * channel, the saturated QUEST_SELECT enters reward ungated with the tiered window, and the
 * client dialog contract holds.
 */
class Quest19637ClientDialogAlignmentTest {
	private static final int QUEST_ID = 19637;
	private static final int CAINUS_NPC_ID = 798926;
	private static final Set<Integer> TARGET_MOBS = Set.of(215500, 215501, 215502, 215503);
	private static final int KILLS_REQUIRED = 10;

	@Test
	void restoresClientReportAndDialogContractOnTheRetailGrid() {
		CompiledQuestDefinition compiled = load();
		QuestDefinition definition = compiled.definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		for (int kills = 0; kills <= KILLS_REQUIRED; kills++) {
			final int state = kills;
			assertNode(definition, "a" + state, QuestStatus.START, Map.of("var0", state));
		}
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", KILLS_REQUIRED));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		// 接取对话：未接态 QUEST_SELECT(31) 下发的页必须是客户端任务页声明的页（真端接取窗 4 只在
		// 客户端声明 ask_quest_accept 时可用，否则发信页 select_none/select1，不然客户端 load fail）。
		// The unaccepted QUEST_SELECT(31) page must be one the client task HTML declares: the native ask
		// window 4 needs ask_quest_accept, otherwise the letter page (select_none/select1) is emitted or
		// the client reports load fail.
		QuestTransition startDialog = talk(definition, "unaccepted", "unaccepted", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			ClientAcceptEntryPageAssertions.expectedEntryPage(definition.id()))), startDialog.afterCommit());

		// 接受任务：QUEST_ACCEPT_SIMPLE(20000)，直落网格起点 a0
		QuestTransition acceptSimple = talk(definition, "unaccepted", "a0", QuestDialogAction.QUEST_ACCEPT_SIMPLE.id());
		assertTrue(acceptSimple.conditions().stream().anyMatch(QuestCondition.StartEligible.class::isInstance));
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), acceptSimple.afterCommit());

		// 未满杀期间网格节点无 QUEST_SELECT/1009 报告通道（P0-2 规范形，页链不再由服务端驱动）
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			"a0".equals(candidate.sourceNode()) && candidate.event() instanceof QuestEvent.TalkToNpc talkRoute
				&& talkRoute.dialogId() != null
				&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "a0 不得保留报告通道路由");

		// 满段交付路由：a10 的 QUEST_SELECT 无条件翻 REWARD 并按档位查表下发奖励窗（P0-2 规范形）
		QuestTransition deliver = talk(definition, "a10", "reward", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(), deliver.conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
			deliver.afterCommit());

		// 领奖预览：在 reward 节点收到 SELECT_QUEST_REWARD(1009)，下发 SHOW_SELECT_QUEST_REWARD_WINDOW1(5)
		QuestTransition previewReward = talk(definition, "reward", "reward", QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), previewReward.afterCommit());

		// 奖励选择：SELECTED_QUEST_REWARD1..6 均指向 complete 节点
		for (int rewardIndex = 1; rewardIndex <= 6; rewardIndex++) {
			final int actionId = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + rewardIndex - 1;
			QuestTransition choice = talk(definition, "reward", "complete", actionId);
			assertTrue(choice.actions().stream().anyMatch(QuestAction.CompleteQuest.class::isInstance));
		}
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source)
				&& Objects.equals(candidate.targetNode(), target)
				&& candidate.event().equals(new QuestEvent.TalkToNpc(CAINUS_NPC_ID, action)))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source)
				&& Objects.equals(candidate.targetNode(), target)
				&& candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definition(QUEST_ID);
	}
}
