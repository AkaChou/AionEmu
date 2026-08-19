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
 * 验证任务 26803 的自动登记、击杀完成与客户端对话合同。
 * Verifies quest 26803 automatic acquisition, kill completion, and client dialog contracts.
 */
class Quest26803ClientDialogAlignmentTest {
	private static final int DIALOG_NPC_ID = 806149;
	private static final Set<Integer> LIBRARIANS = Set.of(
		220307, 220310, 220313, 220316, 220319, 220325, 220328, 220331, 220411);

	@Test
	void keepsAreaAcquisitionSeparateFromTheRewardDialog() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 30));
		assertEquals(List.of(List.of("finished:26802")), startConditionGroups(definition));

		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(26802))), levelUp.conditions());
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

		assertFalse(hasTalk(definition, "unaccepted", QuestDialogAction.QUEST_SELECT.id()));
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
	void finalKillEntersRewardBeforeTheNpcReport() throws Exception {
		QuestDefinition definition = definition().definition();
		QuestEvent librarians = new QuestEvent.KillNpcSet(LIBRARIANS);
		QuestTransition continuing = transition(definition, "started", "started", librarians);
		assertEquals(Integer.valueOf(1), continuing.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableBelow("var1", 29)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", librarians);

		assertEquals(Integer.valueOf(0), completion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 0),
			new QuestCondition.VariableAtLeast("var1", 29)), completion.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var1", 30),
			new QuestAction.SetVariable("var0", 1)), completion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), completion.afterCommit());
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
				"/aion/data/static_data/quest_definition/quests/26803.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 26803.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
