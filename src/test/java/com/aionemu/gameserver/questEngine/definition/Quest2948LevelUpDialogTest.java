package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证任务 2948 的升级自动登记与 NPC 对话合同。 / Verifies quest 2948 level-up acquisition and NPC dialog contracts. */
class Quest2948LevelUpDialogTest {
	private static final int REWARD_NPC_ID = 204274;

	@Test
	void matchesLegacyLevelUpAndNpcDialogContract() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));

		QuestTransition levelUp = transition(definition, "unaccepted", new QuestEvent.LevelUp());
		assertEquals("started", levelUp.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), levelUp.conditions());
		assertEquals(List.of(), levelUp.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), levelUp.afterCommit());

		QuestTransition startDialog = transition(definition, "started",
			new QuestEvent.TalkToNpc(REWARD_NPC_ID, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals("started", startDialog.targetNode());
		assertEquals(List.of(), startDialog.conditions());
		assertEquals(List.of(), startDialog.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			startDialog.afterCommit());

		QuestTransition reward = transition(definition, "started",
			new QuestEvent.TalkToNpc(REWARD_NPC_ID, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals("reward", reward.targetNode());
		assertEquals(List.of(), reward.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), reward.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			reward.afterCommit());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private CompiledQuestDefinition definition() throws Exception {
		try (InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/2948.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 2948.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
