package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1926 与 2938 的升级登记、推荐信交接和独占领奖合同。
 * Verifies the level-up acquisition, recommendation handoff, and exclusive reward contract for quests 1926 and 2938.
 */
class Quest1926And2938ClientDialogAlignmentTest {

	@Test
	void quest1926MatchesLegacyAndClientDialogContract() throws Exception {
		assertContract(1926, 14016, 203701, 203894, 182206022, 74700);
	}

	@Test
	void quest2938MatchesLegacyAndClientDialogContract() throws Exception {
		assertContract(2938, 24016, 203557, 204267, 182207026, 83700);
	}

	private static void assertContract(int questId, int prerequisiteId, int firstNpcId, int secondNpcId,
			int workItemId, int experience) throws Exception {
		QuestDefinition definition = definition(questId).definition();
		assertEquals(Set.of(prerequisiteId), definition.metadata().prerequisites());
		assertEquals(List.of(new QuestItemRequirement(workItemId, 1)),
			definition.metadata().questWorkItems());
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition levelUp = transition(definition, "unaccepted", new QuestEvent.LevelUp());
		assertEquals("started", levelUp.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), levelUp.conditions());
		assertEquals(List.of(), levelUp.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			levelUp.afterCommit());
		assertNull(levelUp.priority());

		assertPage(definition, "started", firstNpcId, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT1);
		assertPage(definition, "started", firstNpcId, QuestDialogAction.SELECT1_1,
			QuestDialogPage.SELECT1_1);

		QuestTransition handoff = route(definition, "started", firstNpcId, QuestDialogAction.SET_SUCCEED);
		assertEquals("reward", handoff.targetNode());
		assertEquals(List.of(), handoff.conditions());
		assertEquals(List.of(
			new QuestAction.GiveItem(workItemId, 1),
			new QuestAction.SetVariable("var0", 1)), handoff.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handoff.afterCommit());
		assertNull(handoff.priority());

		assertPage(definition, "reward", secondNpcId, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.DEFAULT_SUCCESS);
		QuestTransition report = route(definition, "reward", secondNpcId,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(workItemId, 1)), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), report.afterCommit());
		assertNull(report.priority());

		List<QuestTransition> completionRoutes = routes(definition, "reward", secondNpcId).stream()
			.filter(candidate -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) candidate.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertEquals("complete", completion.targetNode());
			assertEquals(List.of(), completion.conditions());
			assertEquals(List.of(
				new QuestAction.GrantReward("EXP", 0, experience, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)), completion.actions());
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
			assertNull(completion.priority());
		}

		assertEquals(22, definition.transitions().size());
		assertTrue(routes(definition, "unaccepted", firstNpcId).isEmpty());
		assertTrue(routes(definition, "unaccepted", secondNpcId).isEmpty());
		assertTrue(routes(definition, "started", secondNpcId).isEmpty());
		assertTrue(routes(definition, "reward", firstNpcId).isEmpty());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, npcId, action);
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
		assertNull(transition.priority());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, QuestEvent event) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source) && candidate.event().equals(event))
			.toList();
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " " + event);
		return routes.getFirst();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId).stream()
			.filter(candidate -> Integer.valueOf(action.id()).equals(
				((QuestEvent.TalkToNpc) candidate.event()).dialogId()))
			.toList();
		assertEquals(1, routes.size(),
			"quest " + definition.id() + " " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest1926And2938ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
