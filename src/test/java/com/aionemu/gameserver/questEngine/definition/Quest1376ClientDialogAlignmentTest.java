package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定任务 1376 的接取、最终击杀报告和奖励归属合同。
 * Locks quest 1376's acceptance, final-kill report, and reward-owner contracts.
 */
class Quest1376ClientDialogAlignmentTest {
	private static final Path QUEST_PATH = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/1376.xml");
	private static final int START_NPC = 203947;
	private static final int REPORT_NPC = 203964;

	@Test
	void followsTheRetailReportOwnerFromTheFinalKillNode() throws Exception {
		QuestDefinition definition = load();

		assertEquals(new NodeProjection(QuestStatus.START, Map.of("var0", 7)), node(definition, "k7").projection());
		assertTrue(talkRoutes(definition, "unaccepted", REPORT_NPC).isEmpty());
		assertTrue(talkRoutes(definition, "started", REPORT_NPC).isEmpty());

		QuestTransition reportPage = singleTalkRoute(definition, "k7", REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertEquals("k7", reportPage.targetNode());
		assertTrue(reportPage.conditions().isEmpty());
		assertTrue(reportPage.actions().isEmpty());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			reportPage.afterCommit());

		QuestTransition report = singleTalkRoute(definition, "k7", REPORT_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		assertTrue(talkRoutes(definition, "k7", START_NPC).isEmpty());
		assertTrue(talkRoutes(definition, "reward", START_NPC).isEmpty());

		QuestTransition completion = singleTalkRoute(definition, "reward", REPORT_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 33860, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 1244918, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000003, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static QuestTransition singleTalkRoute(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 1376 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& talk.dialogId() == action.id())
			.toList();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(node -> label.equals(node.label()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_PATH)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
