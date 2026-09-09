package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定旧 handler 进入 REWARD 时保留 packed 任务变量的迁移合同。
 * Locks the migration contract that legacy handlers preserve packed quest variables when entering REWARD.
 */
class LegacyRewardStepProjectionRegressionTest {
	private static final List<Integer> QUEST_IDS = List.of(
		2393, 3722, 4722, 11149, 13965, 14010, 14015, 14020, 14040, 14050,
		15674, 23965, 24010, 24020, 24040, 24050, 25674, 30057, 30158, 30208);

	@Test
	void keepsLegacyRewardEntryAtTheExistingPackedStep() throws Exception {
		for (int questId : QUEST_IDS) {
			QuestDefinition definition = definition(questId).definition();
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));

			List<QuestTransition> rewardEntries = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.toList();
			assertFalse(rewardEntries.isEmpty(), "quest " + questId + " has no START -> REWARD route");
			assertTrue(rewardEntries.stream().flatMap(transition -> transition.actions().stream())
				.noneMatch(QuestAction.SetVariable.class::isInstance),
				"quest " + questId + " must not rewrite the legacy reward step");

			List<QuestTransition> recoveryRoutes = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() == null)
				.filter(transition -> transition.targetNode().equals("reward"))
				.filter(transition -> transition.event().equals(new QuestEvent.EnterWorld()))
				.toList();
			assertEquals(1, recoveryRoutes.size(), "quest " + questId + " recovery route");
			QuestTransition recovery = recoveryRoutes.getFirst();
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 1)), recovery.conditions());
			assertEquals(List.of(), recovery.actions());
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit());
			assertNull(recovery.priority());
		}
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = LegacyRewardStepProjectionRegressionTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
