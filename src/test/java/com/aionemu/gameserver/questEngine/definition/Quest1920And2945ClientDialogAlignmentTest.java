package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证任务 1920 与 2945 的升级登记、双 NPC 对话链和领奖合同。
 * Verifies the level-up acquisition, two-NPC dialog chain, and reward contract for quests 1920 and 2945.
 */
class Quest1920And2945ClientDialogAlignmentTest {

	@Test
	void quest1920MatchesLegacyAndClientDialogContract() throws Exception {
		assertContract(1920, 203752, 203876);
	}

	@Test
	void quest2945MatchesLegacyAndClientDialogContract() throws Exception {
		assertContract(2945, 204075, 204088);
	}

	private static void assertContract(int questId, int firstNpcId, int secondNpcId) throws Exception {
		QuestDefinition definition = definition(questId).definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "started1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(new QuestCondition.StartEligible()), levelUp.conditions());
		assertEquals(List.of(), levelUp.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			levelUp.afterCommit());

		assertPage(definition, "started", firstNpcId, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(definition, "started", firstNpcId, QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertPage(definition, "started", firstNpcId, QuestDialogAction.SELECT1_1_1,
			QuestDialogPage.SELECT1_1_1);

		QuestTransition firstStep = talk(definition, "started", firstNpcId, QuestDialogAction.SETPRO1);
		assertEquals("started1", firstStep.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), firstStep.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), firstStep.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()),
			firstStep.afterCommit());

		assertPage(definition, "started1", secondNpcId, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "started1", secondNpcId, QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);

		QuestTransition setReward = talk(definition, "started1", secondNpcId, QuestDialogAction.SET_SUCCEED);
		assertEquals("reward", setReward.targetNode());
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), setReward.conditions());
		assertEquals(List.of(), setReward.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()),
			setReward.afterCommit());

		QuestTransition report = talk(definition, "reward", firstNpcId, QuestDialogAction.USE_OBJECT);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());

		QuestTransition preview = talk(definition, "reward", firstNpcId,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", preview.targetNode());
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());

		QuestTransition completion = talk(definition, "reward", firstNpcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
		assertEquals(List.of(), completion.conditions());
		assertEquals(List.of(
			new QuestAction.GrantReward("EXP", 0, 8325278, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("AP", 0, 250, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 188051194, 2, QuestRewardAmountMode.EXACT),
			new QuestAction.CompleteQuest(0)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());

		assertFalse(definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals("reward"))
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.anyMatch(talk -> talk.npcId() == secondNpcId));
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, npcId, action);
		assertEquals(source, transition.targetNode());
		int expectedVar = source.equals("started") ? 0 : 1;
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", expectedVar)),
			transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, null, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> target == null || candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
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

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest1920And2945ClientDialogAlignmentTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
