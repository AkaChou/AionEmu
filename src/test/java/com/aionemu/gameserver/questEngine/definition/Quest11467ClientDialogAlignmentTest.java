package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 11467 的计时接取在成功页之后保留客户端结束对话路由。
 * Verifies quest 11467 keeps the client finish route after its timed acceptance page.
 */
class Quest11467ClientDialogAlignmentTest {
	private static final int START_NPC = 799527;

	@Test
	void closesTheAcceptedPageFromTheCommittedStartState() {
		QuestDefinition definition = load().definition();

		QuestTransition accept = route(definition, "unaccepted", QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.StartQuestTimer(480, new QuestTimerPolicy(
				new QuestTimerPolicy.Identity("11467-queen", QuestTimerPolicy.Scope.PLAYER_QUEST),
				QuestTimerPolicy.Persistence.SESSION, QuestTimerPolicy.OverwritePolicy.REPLACE,
				QuestTimerPolicy.Delivery.AT_MOST_ONCE)),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())),
			accept.afterCommit());

		QuestTransition finish = route(definition, "started", QuestDialogAction.FINISH_DIALOG);
		assertEquals("started", finish.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(
			QuestDialogPage.SELECT_QUEST.id())), finish.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(START_NPC, action.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/11467.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest11467ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
