package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1913ClientDialogAlignmentTest {
	private static final int START_NPC = 203758;
	private static final int PROGRESS_NPC = 203726;
	private static final int REPORT_NPC = 203097;

	@Test
	void followsTheClientAcceptProgressReportAndRewardLifecycle() throws Exception {
		QuestDefinition definition = definition().definition();
		List<QuestTransition> transitions = definition.transitions();

		// S2：接取窗由 QUEST_SELECT 直发（页 4）；select1 页梯与 1007 中转随规范接取段退场。
		// S2 canonical accept: QUEST_SELECT opens page 4 directly; the select1 ladder and the 1007 relay retire.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())),
			talk(transitions, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals("started",
			talk(transitions, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1).targetNode());
		assertEquals("unaccepted",
			talk(transitions, "unaccepted", START_NPC, QuestDialogAction.QUEST_REFUSE_1).targetNode());

		QuestTransition progress = talk(transitions, "started", PROGRESS_NPC, QuestDialogAction.SETPRO1);
		assertEquals("started1", progress.targetNode());
		assertTrue(progress.actions().contains(new QuestAction.SetVariable("var0", 1)));
		assertEquals(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			progress.afterCommit().getFirst());
		AfterCommitAction.TeleportPlayer teleport = assertInstanceOf(AfterCommitAction.TeleportPlayer.class,
			progress.afterCommit().get(1));
		assertEquals(210030000, teleport.worldId());
		assertEquals(new AfterCommitAction.CloseDialog(), progress.afterCommit().getLast());

		// S2：交付 = QUEST_SELECT(started1→reward) 空门直翻领奖态并下发奖励窗；SELECT5 报告页与 1009
		// 检查中转随规范交付段退场（未集齐零路由，关窗兜底交 DialogService）。传送后的语义靠交付边锚在
		// started1 体现。
		// S2 canonical delivery: QUEST_SELECT(started1→reward) flips REWARD with the reward window; the
		// report page and the 1009 check relay retire. The teleport-gated stage is the started1 anchor.
		QuestTransition delivery = talk(transitions, "started1", REPORT_NPC, QuestDialogAction.QUEST_SELECT);
		assertEquals("reward", delivery.targetNode());
		assertEquals(List.of(), delivery.conditions());
		assertEquals(List.of(), delivery.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			deliveryWindowPage(definition.metadata()))), delivery.afterCommit());
		assertTrue(transitions.stream().noneMatch(transition ->
			"started1".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == REPORT_NPC
				&& Integer.valueOf(QuestDialogAction.SELECT_QUEST_REWARD.id()).equals(talk.dialogId())),
			"quest 1913 的 1009 检查中转必须随规范交付段退场");

		QuestTransition completion = talk(transitions, "reward", REPORT_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	private static QuestTransition talk(List<QuestTransition> transitions, String source, int npcId,
			QuestDialogAction action) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && Integer.valueOf(action.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();
	}

	/** 交付窗页（与 RetailSimpleCollectItemDefinitionCompiler.deliveryWindowPage 同口径：档位查表，零奖励组回落窗 1）。 */
	private static int deliveryWindowPage(QuestMetadata metadata) {
		return metadata.rewardGroups().isEmpty()
			? QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()
			: QuestDialogPage.rewardWindowForTier(metadata.rewardGroups().size() - 1).orElseThrow().id();
	}

	private CompiledQuestDefinition definition() throws Exception {
		return ProductionQuestDefinitions.definitionInOverlay(1913);
	}
}
