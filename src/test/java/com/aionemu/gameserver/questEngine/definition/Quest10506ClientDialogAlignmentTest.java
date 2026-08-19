package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证任务 10506 在完成前会经过正式服的 SELECT8 客户端页面链。
 * Verifies quest 10506 reaches the retail SELECT8 client page chain before completion.
 */
class Quest10506ClientDialogAlignmentTest {
	private static final int DIALOG_NPC = 804710;

	@Test
	void keepsTheSelect8ChainAndSetSucceedCompletionRoute() {
		QuestDefinition definition = load().definition();

		assertPage(definition, "s7", QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT8);
		assertPage(definition, "s7", QuestDialogAction.SELECT8_1, QuestDialogPage.SELECT8_1);
		assertFalse(hasRoute(definition, "s7", QuestDialogAction.SELECT8_1_1));

		QuestTransition completeStep = route(definition, "s7", QuestDialogAction.SET_SUCCEED);
		assertEquals("reward", completeStep.targetNode());
		assertEquals(List.of(
			new QuestAction.GiveItem(182215613, 1),
			new QuestAction.SetVariable("var0", 8)), completeStep.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), completeStep.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source,
		QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, action);
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static boolean hasRoute(QuestDefinition definition, String source, QuestDialogAction action) {
		return definition.transitions().stream()
			.anyMatch(transition -> transition.sourceNode().equals(source)
				&& transition.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC, action.id())));
	}

	private static QuestTransition route(QuestDefinition definition, String source, QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(DIALOG_NPC, action.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/10506.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest10506ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
