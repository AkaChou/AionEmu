package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 校验任务 2009 的电影播放后继续对话页面合同。
 * Verifies quest 2009 continuation-dialog page contracts after movie playback.
 */
class Quest2009MovieDialogTest {
	@Test
	void movieSelectionsRestoreTheContinueDialogAfterPlaybackStarts() throws Exception {
		QuestDefinition definition = load();
		assertNode(definition, "s1", QuestStatus.START, 1);
		assertNode(definition, "s2", QuestStatus.START, 2);

		assertMovieDialog(definition, "s1", 204182, QuestDialogAction.SELECT2_1,
			121, QuestDialogPage.SELECT2_1);
		assertMovieDialog(definition, "s2", 204075, QuestDialogAction.SELECT3_1,
			122, QuestDialogPage.SELECT3_1);
	}

	private static void assertMovieDialog(QuestDefinition definition, String node, int npcId,
			QuestDialogAction action, int movieId, QuestDialogPage page) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(node)
				&& transition.targetNode().equals(node)
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size());

		QuestTransition transition = matches.getFirst();
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertNull(transition.priority());
		assertEquals(List.of(
			new AfterCommitAction.PlayMovie(movieId),
			new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition load() throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/2009.xml";
		try (InputStream input = Quest2009MovieDialogTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource)).definition();
		}
	}
}
