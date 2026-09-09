package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证 28625/28626 在卡里加陈列柜上使用钥匙的客户端对话合同。
 * Verifies the client dialog contract for using the key at the Kaliga display cabinets in quests 28625/28626.
 */
class Quest28625And28626ClientDialogAlignmentTest {
	private static final int KALIGA_KEY_ID = 185000102;
	private static final int KALIGA_BOSS_ID = 217006;
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(28625, 730333),
		new QuestCase(28626, 730334));

	@Test
	void reportsTheKaligaCollectionAtTheDisplayCabinet() throws Exception {
		for (QuestCase questCase : CASES) {
			QuestDefinition definition = load(questCase.questId()).definition();
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));

			QuestTransition entry = route(definition, "started", "started",
				new QuestEvent.TalkToNpc(questCase.cabinetId(), QuestDialogAction.QUEST_SELECT.id()), null);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT5.id())),
				entry.afterCommit());

			QuestEvent itemCheck = new QuestEvent.TalkToNpc(questCase.cabinetId(),
				QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
			QuestTransition success = route(definition, "started", "reward", itemCheck, 0);
			assertEquals(List.of(new QuestCondition.HasItem(KALIGA_KEY_ID, 1)), success.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(KALIGA_KEY_ID, 1)), success.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				success.afterCommit());

			QuestTransition failure = route(definition, "started", "started", itemCheck, 1);
			assertEquals(List.of(), failure.conditions());
			assertEquals(List.of(), failure.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6.id())),
				failure.afterCommit());

			assertFalse(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& transition.event().equals(new QuestEvent.TalkToNpc(KALIGA_BOSS_ID,
						QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()))),
				"quest " + questCase.questId() + " must not check the key at Kaliga");
		}
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestEvent event, Integer priority) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.filter(candidate -> priority == null || priority.equals(candidate.priority()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Quest28625And28626ClientDialogAlignmentTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record QuestCase(int questId, int cabinetId) {
	}
}
