package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 锁定任务 2223 击杀后的领奖 owner、Aion 5.8 客户端报告页及奖励选择链。
 * Locks quest 2223's post-kill reward owner, Aion 5.8 client report page, and reward-selection chain.
 */
class Quest2223ClientDialogAlignmentTest {
	private static final int GEFION = 203616;
	private static final int MYTHICAL_MONSTER = 211621;

	@Test
	void returnsToGefionBeforeOpeningTheNativeRewardWindow() throws Exception {
		QuestDefinition definition = definition();

		assertNode(definition, "unaccepted", QuestStatus.NONE, 0);
		assertNode(definition, "started", QuestStatus.START, 0);
		assertNode(definition, "step1", QuestStatus.START, 1);
		assertNode(definition, "reward", QuestStatus.REWARD, 1);
		assertNode(definition, "complete", QuestStatus.COMPLETE, 0);

		QuestTransition kill = transition(definition, "step1", "reward", new QuestEvent.KillNpc(MYTHICAL_MONSTER));
		assertEquals(List.of(), kill.conditions());
		assertEquals(List.of(), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), kill.afterCommit());

		assertPage(definition, "reward", GEFION, QuestDialogAction.USE_OBJECT, QuestDialogPage.SELECT5);
		QuestTransition preview = talk(definition, "reward", "reward", GEFION,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		assertCompletion(definition, QuestDialogAction.SELECTED_QUEST_REWARD1, 120001131);
		assertCompletion(definition, QuestDialogAction.SELECTED_QUEST_REWARD2, 120001132);
	}

	private static void assertCompletion(QuestDefinition definition, QuestDialogAction action, int selectedItemId) {
		QuestTransition completion = talk(definition, "reward", "complete", GEFION, action);
		assertEquals(List.of(), completion.conditions());
		assertEquals(List.of(
			new QuestAction.GrantReward("GOLD", 0, 3490, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("EXP", 0, 23250, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", selectedItemId, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 162000048, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(10)), completion.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, source, npcId, action);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.toList();
		assertEquals(1, routes.size(), "quest 2223 " + source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(new NodeProjection(status, Map.of("var0", var0)), node.projection());
	}

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Quest2223ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/2223.xml")) {
			if (input == null) throw new IllegalStateException("missing quest definition 2223.xml");
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
