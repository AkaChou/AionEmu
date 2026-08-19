package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证任务 25698 只在完成五次击杀后开放客户端报告和领奖路径。
 * Verifies quest 25698 exposes the client report and reward path only after five kills.
 */
class Quest25698ClientDialogAlignmentTest {
	private static final int QUEST_NPC = 806804;

	@Test
	void gatesTheClientReportPageOnCompletedKillProgress() {
		QuestDefinition definition = load().definition();

		assertFalse(hasRoute(definition, "started", QuestDialogAction.QUEST_SELECT));
		QuestTransition report = route(definition, "h5", QuestDialogAction.QUEST_SELECT);
		assertEquals("h5", report.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.DEFAULT_SUCCESS.id())), report.afterCommit());

		QuestTransition preview = route(definition, "h5", QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", preview.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			preview.afterCommit());
	}

	private static boolean hasRoute(QuestDefinition definition, String source, QuestDialogAction action) {
		return definition.transitions().stream()
			.anyMatch(transition -> transition.sourceNode().equals(source)
				&& transition.event().equals(new QuestEvent.TalkToNpc(QUEST_NPC, action.id())));
	}

	private static QuestTransition route(QuestDefinition definition, String source,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(QUEST_NPC, action.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/25698.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest25698ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
