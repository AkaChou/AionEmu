package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证任务 26802 的接取、进度完成与客户端对话合同。
 * Verifies quest 26802 acquisition, progress completion, and client dialog contracts.
 */
class Quest26802ClientDialogAlignmentTest {
	private static final int DIALOG_NPC_ID = 806149;
	private static final Set<Integer> LIBRARIANS = Set.of(
		220306, 220309, 220312, 220315, 220318, 220324, 220327, 220330);
	private static final Set<Integer> SUB_BOSSES = Set.of(857450, 857452, 857454, 857456, 857458, 857459);

	@Test
	void restoresLegacyAcquisitionAndClientDialogContract() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE,
			Map.of("var0", 0, "var1", 0, "var2", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "reward", QuestStatus.REWARD,
			Map.of("var0", 1, "var1", 30, "var2", 2));
		assertEquals(List.of(List.of("finished:26801")), startConditionGroups(definition));

		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(26801))), levelUp.conditions());
		assertEquals(List.of(), levelUp.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), levelUp.afterCommit());

		QuestTransition zoneMissionEnd = transition(definition, "unaccepted", "started",
			new QuestEvent.ZoneMissionEnd());
		assertEquals(List.of(new QuestCondition.StartEligible()), zoneMissionEnd.conditions());
		assertEquals(List.of(), zoneMissionEnd.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
			zoneMissionEnd.afterCommit());

		QuestTransition offer = talk(definition, "unaccepted", "unaccepted",
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(), offer.conditions());
		assertEquals(List.of(), offer.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE.id())),
			offer.afterCommit());

		QuestTransition simpleAccept = talk(definition, "unaccepted", "started",
			QuestDialogAction.QUEST_ACCEPT_SIMPLE.id());
		assertEquals(List.of(new QuestCondition.StartEligible()), simpleAccept.conditions());
		assertEquals(List.of(), simpleAccept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), simpleAccept.afterCommit());

		QuestTransition refuse = talk(definition, "unaccepted", "unaccepted",
			QuestDialogAction.QUEST_REFUSE_SIMPLE.id());
		assertEquals(List.of(), refuse.conditions());
		assertEquals(List.of(), refuse.actions());
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), refuse.afterCommit());

		assertFalse(hasTalk(definition, "unaccepted", QuestDialogAction.ASK_QUEST_ACCEPT.id()));
		assertFalse(hasTalk(definition, "unaccepted", QuestDialogAction.QUEST_ACCEPT_1.id()));
		assertFalse(hasTalk(definition, "started", QuestDialogAction.QUEST_SELECT.id()));
		QuestTransition report = talk(definition, "reward", "reward", QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(), report.conditions());
		assertEquals(List.of(), report.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			report.afterCommit());

		QuestTransition rewardPreview = talk(definition, "reward", "reward",
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(), rewardPreview.conditions());
		assertEquals(List.of(), rewardPreview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardPreview.afterCommit());
	}

	@Test
	void finalKillInEitherCounterEntersRewardBeforeReporting() throws Exception {
		QuestDefinition definition = definition().definition();

		QuestTransition librarianCompletion = transition(definition, "started", "reward",
			new QuestEvent.KillNpcSet(LIBRARIANS));
		assertEquals(Integer.valueOf(0), librarianCompletion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 29),
			new QuestCondition.VariableAtLeast("var2", 2)), librarianCompletion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 30),
			new QuestAction.SetVariable("var0", 1)), librarianCompletion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), librarianCompletion.afterCommit());

		QuestTransition bossCompletion = transition(definition, "started", "reward",
			new QuestEvent.KillNpcSet(SUB_BOSSES));
		assertEquals(Integer.valueOf(0), bossCompletion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 30),
			new QuestCondition.VariableAtLeast("var2", 1)), bossCompletion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var2", 2),
			new QuestAction.SetVariable("var0", 1)), bossCompletion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), bossCompletion.afterCommit());

		QuestTransition recoveredReport = talk(definition, "started", "reward",
			QuestDialogAction.SELECT_QUEST_REWARD.id());
		assertEquals(List.of(
			new QuestCondition.VariableAtLeast("var1", 30),
			new QuestCondition.VariableAtLeast("var2", 2)), recoveredReport.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), recoveredReport.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			recoveredReport.afterCommit());
	}

	private static List<List<String>> startConditionGroups(QuestDefinition definition) {
		return definition.metadata().startConditionGroups().stream()
			.map(group -> group.conditions().stream()
				.map(condition -> condition.type() + ":" + condition.questId()).toList())
			.toList();
	}

	private static boolean hasTalk(QuestDefinition definition, String source, int action) {
		return definition.transitions().stream()
			.anyMatch(candidate -> candidate.sourceNode().equals(source)
				&& candidate.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action)));
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(DIALOG_NPC_ID, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
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

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/26802.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 26802.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
