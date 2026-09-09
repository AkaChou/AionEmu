package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 14051 的旧版收集交付阶段与 Aion 5.8 客户端对话动作合同。
 * Verifies quest 14051's legacy collection turn-in stages and Aion 5.8 client dialog action contract.
 */
class Quest14051ClientDialogAlignmentTest {
	private static final int TURN_IN_NPC_ID = 204549;
	private static final int NEXT_NPC_ID = 730026;
	private static final int AGRINT_ROOT_SAMPLE_ID = 182215337;
	private static final int ROTRON_ROOT_SAMPLE_ID = 182215338;
	private static final int COMBINED_ROOT_SAMPLES_ID = 182215339;

	@Test
	void followsLegacyCollectionDialogChainBeforeCheckingItems() throws Exception {
		QuestDefinition definition = definition().definition();
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 3));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 3));

		// 204549 的三页介绍必须先按客户端 action 链翻页，再用 SETPRO2 进入 var0=2。
		// NPC 204549 must follow the client action chain before SETPRO2 enters var0=2.
		QuestTransition entry = talk(definition, "s1", "s1", QuestDialogAction.QUEST_SELECT);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), entry.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			entry.afterCommit());

		assertPage(definition, "s1", QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "s1", QuestDialogAction.SELECT2_1_1, QuestDialogPage.SELECT2_1_1);

		QuestTransition setpro2 = talk(definition, "s1", "s2", QuestDialogAction.SETPRO2);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), setpro2.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), setpro2.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), setpro2.afterCommit());

		// SELECT3(1693) 的 39 动作才是收集物交付入口，成功后推进到 var0=3。
		// The CHECK_USER_HAS_QUEST_ITEM(39) action on SELECT3(1693) is the item turn-in entry and advances to var0=3.
		QuestTransition select3 = talk(definition, "s2", "s2", QuestDialogAction.QUEST_SELECT);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), select3.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())),
			select3.afterCommit());

		QuestTransition checkSuccess = transition(definition, "s2", "s3",
			new QuestEvent.TalkToNpc(TURN_IN_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 0);
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 2),
			new QuestCondition.HasItem(AGRINT_ROOT_SAMPLE_ID, 3),
			new QuestCondition.HasItem(ROTRON_ROOT_SAMPLE_ID, 3)), checkSuccess.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(AGRINT_ROOT_SAMPLE_ID, 3),
			new QuestAction.RemoveItem(ROTRON_ROOT_SAMPLE_ID, 3),
			new QuestAction.GiveItem(COMBINED_ROOT_SAMPLES_ID, 1),
			new QuestAction.SetVariable("var0", 3)), checkSuccess.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_OK.id())),
			checkSuccess.afterCommit());

		QuestTransition checkFailure = transition(definition, "s2", "s2",
			new QuestEvent.TalkToNpc(TURN_IN_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 1);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), checkFailure.conditions());
		assertEquals(List.of(), checkFailure.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			checkFailure.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			talk(definition, "s2", "s2", QuestDialogAction.FINISH_DIALOG).afterCommit());

		// 收集交付成功后的下一步仍由 var0=3 的 NPC 730026 接管。
		// After the collection turn-in succeeds, NPC 730026 owns the next var0=3 step.
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT4.id())),
			talk(definition, "s3", "s3", NEXT_NPC_ID, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertPage(definition, "s3", QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target,
			QuestDialogAction action) {
		return talk(definition, source, target, source.equals("s3") ? NEXT_NPC_ID : TURN_IN_NPC_ID, action);
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static void assertPage(QuestDefinition definition, String source, QuestDialogAction action,
			QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			talk(definition, source, source, action).afterCommit());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, int priority) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source)
				&& candidate.targetNode().equals(target)
				&& candidate.event().equals(event)
				&& Integer.valueOf(priority).equals(candidate.priority()))
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
				"/aion/data/static_data/quest_definition/quests/14051.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 14051.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
