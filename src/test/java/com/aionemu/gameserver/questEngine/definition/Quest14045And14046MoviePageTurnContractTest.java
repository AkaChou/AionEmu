package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务 14045/14046 的影片翻页动作必须同时下发目标客户端页面，避免客户端停留在上一页循环重发。
 * Regression coverage that the movie page-turn actions of quests 14045/14046 also emit the target client page.
 */
class Quest14045And14046MoviePageTurnContractTest {

	@Test
	void rumorsOnWingsMoviePageTurnAlsoShowsSelect1_1_1() throws Exception {
		QuestDefinition definition = definition(14045).definition();
		assertNode(definition, "started", Map.of("var0", 0));

		QuestTransition pageTurn = transition(definition, "started", "started",
			new QuestEvent.TalkToNpc(278506, QuestDialogAction.SELECT1_1_1.id()));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), pageTurn.conditions());
		assertEquals(List.of(), pageTurn.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(272),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT1_1_1.id())),
			pageTurn.afterCommit());
		assertTrue(definition.transitions().stream().anyMatch(candidate ->
			candidate.event().equals(new QuestEvent.TalkToNpc(278506, QuestDialogAction.SETPRO1.id()))));
	}

	@Test
	void piecingTheMemoryMoviePageTurnAlsoShowsSelect2_1() throws Exception {
		QuestDefinition definition = definition(14046).definition();
		assertNode(definition, "s1", Map.of("var0", 1));

		QuestTransition pageTurn = transition(definition, "s1", "s1",
			new QuestEvent.TalkToNpc(203834, QuestDialogAction.SELECT2_1.id()));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), pageTurn.conditions());
		assertEquals(List.of(), pageTurn.actions());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(102),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2_1.id())),
			pageTurn.afterCommit());
		assertTrue(definition.transitions().stream().anyMatch(candidate ->
			candidate.event().equals(new QuestEvent.TalkToNpc(203834, QuestDialogAction.SETPRO2.id()))));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.START, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private CompiledQuestDefinition definition(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = getClass().getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
