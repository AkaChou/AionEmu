package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证任务 1006 的职业选择、奖励预览入口和领奖完成合同。
 * Verifies quest 1006 class selection, reward-preview ingress, and completion contracts.
 */
class Quest1006ClientDialogAlignmentTest {
	private static final int PERNOS = 790001;

	@Test
	void returnsTheRewardWindowWhenTheClientUsesQuestSelectionIngress() throws Exception {
		QuestDefinition definition = load().definition();

		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 5));

		QuestTransition sorcererChoice = route(definition, "s5", "reward", PERNOS,
			QuestDialogAction.SETPRO9);
		assertEquals(List.of(new QuestCondition.PlayerClassIs(PlayerClass.MAGE)), sorcererChoice.conditions());
		assertEquals(List.of(), sorcererChoice.actions());
		assertEquals(List.of(
			new AfterCommitAction.SetPlayerClass(PlayerClass.SORCERER),
			new AfterCommitAction.TeleportPlayer(210010000, 245.14868f, 1639.1372f, 100.35713f, (byte) 60),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			sorcererChoice.afterCommit());

		assertRewardPreview(definition, QuestDialogAction.USE_OBJECT);
		assertRewardPreview(definition, QuestDialogAction.QUEST_SELECT);
		assertRewardPreview(definition, QuestDialogAction.SELECT_QUEST_REWARD);

		long completionRoutes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == PERNOS)
			.count();
		assertEquals(16, completionRoutes);
	}

	private static void assertRewardPreview(QuestDefinition definition, QuestDialogAction action) {
		QuestTransition preview = route(definition, "reward", "reward", PERNOS, action);
		assertEquals(List.of(), preview.conditions());
		assertEquals(List.of(), preview.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target, int npcId,
		QuestDialogAction action) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& target.equals(transition.targetNode())
				&& transition.event().equals(new QuestEvent.TalkToNpc(npcId, action.id())))
			.findFirst()
			.orElseThrow(() -> new AssertionError("missing quest 1006 route: "
				+ source + " -> " + target + " " + action));
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest1006ClientDialogAlignmentTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/1006.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 1006.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
