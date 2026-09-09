package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14053 自动接取时只同步状态，不打开任务 HTML 页面。
 * Verifies that quest 14053 automatic acquisition only synchronizes state and does not open quest HTML.
 */
class Quest14053LevelUpDialogTest {
	@Test
	void keepsAutomaticStartsFreeOfQuestHtmlPages() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));

		QuestTransition levelUp = transition(definition, "unaccepted", "started", new QuestEvent.LevelUp());
		assertEquals(List.of(
			new QuestCondition.StartEligible(),
			new QuestCondition.QuestsFinished(Set.of(14050))), levelUp.conditions());
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
	}

	@Test
	void setpro2TeleportsToAtalanteBeforeClosingDialog() throws Exception {
		QuestDefinition definition = definition().definition();
		QuestTransition setpro2 = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(204020, QuestDialogAction.SETPRO2.id()));

		assertEquals(List.of(
			new AfterCommitAction.TeleportPlayer(210040000, 2006.7308f, 1392.1913f, 118.125f, (byte) 4),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), setpro2.afterCommit());
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
			"/aion/data/static_data/quest_definition/quests/14053.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14053.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
