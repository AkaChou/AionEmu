package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 1141 将贝尔布亚的接取交接与酒桶报告、领奖 owner 分离。
 * Verifies quest 1141 keeps Belbua's acquisition handoff separate from the barrel report and reward owner.
 */
class Quest1141ClientDialogAlignmentTest {
	private static final int START_NPC = 730001;
	private static final int BARREL_NPC = 700122;

	@Test
	void keepsTheStartNpcSeparateFromTheBarrelReportAndRewardOwner() {
		QuestDefinition definition = load().definition();

		assertPage(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT1);
		assertTrue(routes(definition, "started", START_NPC).stream()
			.allMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(START_NPC, QuestDialogAction.FINISH_DIALOG.id()))));
		assertTrue(routes(definition, "reward", START_NPC).isEmpty());

		assertPage(definition, "started", BARREL_NPC, QuestDialogAction.USE_OBJECT,
			QuestDialogPage.SELECT5);
		QuestTransition report = route(definition, "started", BARREL_NPC,
			QuestDialogAction.SELECT_QUEST_REWARD);
		assertEquals("reward", report.targetNode());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			report.afterCommit());

		QuestTransition completion = route(definition, "reward", BARREL_NPC,
			QuestDialogAction.SELECTED_QUEST_REWARD1);
		assertEquals("complete", completion.targetNode());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = route(definition, source, npcId, action);
		assertEquals(source, transition.targetNode());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst().orElseThrow();
	}

	private static CompiledQuestDefinition load() {
		String resource = "/aion/data/static_data/quest_definition/quests/1141.xml";
		try (InputStream input = Objects.requireNonNull(
			Quest1141ClientDialogAlignmentTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
