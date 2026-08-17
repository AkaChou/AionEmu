package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证任务 10522 的自动登记与 NPC 对话合同。 / Verifies quest 10522 automatic acquisition and NPC dialog contracts. */
class Quest10522AutoStartDialogTest {
	private static final int REWARD_NPC_ID = 806075;

	@Test
	void matchesLegacyAutoStartAndNpcDialogContract() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1));
		assertEquals(List.of(List.of("finished:10521")), startConditionGroups(definition));

		assertAutoStart(definition, new QuestEvent.LevelUp());
		assertAutoStart(definition, new QuestEvent.ZoneMissionEnd());

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

		assertEquals(Set.of(REWARD_NPC_ID), dialogNpcIds(definition));
	}

	private static void assertAutoStart(QuestDefinition definition, QuestEvent event) {
		QuestTransition transition = transition(definition, "unaccepted", event);
		assertEquals("started", transition.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)), transition.afterCommit());
	}

	private static List<List<String>> startConditionGroups(QuestDefinition definition) {
		return definition.metadata().startConditionGroups().stream()
			.map(group -> group.conditions().stream()
				.map(condition -> condition.type() + ":" + condition.questId()).toList())
			.toList();
	}

	private static Set<Integer> dialogNpcIds(QuestDefinition definition) {
		return definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestEvent.TalkToNpc.class::isInstance)
			.map(QuestEvent.TalkToNpc.class::cast)
			.map(QuestEvent.TalkToNpc::npcId)
			.collect(java.util.stream.Collectors.toSet());
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
				"/aion/data/static_data/quest_definition/quests/10522.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 10522.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
