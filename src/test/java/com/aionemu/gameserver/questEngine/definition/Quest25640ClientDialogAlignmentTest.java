package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证任务 25640 只在完成三十次击杀后开放客户端报告和领奖路径。
 * Verifies quest 25640 exposes the client report and reward path only after thirty kills.
 */
class Quest25640ClientDialogAlignmentTest {
	private static final int QUEST_NPC = 806101;

	@Test
	void gatesTheClientReportPageOnCompletedKillProgress() {
		QuestDefinition definition = load().definition();

		assertFalse(hasRoute(definition, "started", QuestDialogAction.QUEST_SELECT));
		QuestTransition report = route(definition, "h30", QuestDialogAction.QUEST_SELECT);
		assertEquals("h30", report.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.DEFAULT_SUCCESS.id())), report.afterCommit());

		QuestTransition preview = route(definition, "h30", QuestDialogAction.SELECT_QUEST_REWARD);
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
		String resource = "/aion/data/static_data/quest_definition/quests/25640.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest25640ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
