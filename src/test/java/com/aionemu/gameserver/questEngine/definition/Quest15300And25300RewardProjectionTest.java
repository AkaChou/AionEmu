package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 15300/25300 交还后保留旧 handler 的 REWARD packed step。
 * Verifies that 15300/25300 preserve the legacy REWARD packed step after handover.
 */
class Quest15300And25300RewardProjectionTest {

	@Test
	void elyosQuestKeepsTheLegacyRewardStepAndRecoversPersistedMismatch() throws Exception {
		assertContract(15300, 805362, 182215903, 805327);
	}

	@Test
	void asmodianQuestKeepsTheLegacyRewardStepAndRecoversPersistedMismatch() throws Exception {
		assertContract(25300, 805365, 182215871, 805339);
	}

	private static void assertContract(int questId, int handoverNpcId, int workItemId,
			int rewardNpcId) throws Exception {
		CompiledQuestDefinition compiled = definition(questId);
		QuestDefinition definition = compiled.definition();
		assertRewardNode(definition, Map.of("var0", 13));

		QuestTransition handover = transition(definition, "s13",
			new QuestEvent.TalkToNpc(handoverNpcId, QuestDialogAction.SETPRO14.id()));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 13)), handover.conditions());
		assertEquals(List.of(new QuestAction.GiveItem(workItemId, 1)), handover.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handover.afterCommit());
		assertNull(handover.priority());

		QuestMutationPlan handoverPlan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 13)), handover.event(),
			handover).orElseThrow();
		assertEquals(QuestStatus.REWARD, handoverPlan.nextStatus());
		assertEquals(13, unpack(compiled, handoverPlan).get("var0"));

		QuestTransition recovery = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", 14)), recovery.conditions());
		assertEquals(List.of(), recovery.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit());
		assertNull(recovery.priority());

		QuestMutationPlan recoveryPlan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 14)), recovery.event(),
			recovery).orElseThrow();
		assertEquals(QuestStatus.REWARD, recoveryPlan.nextStatus());
		assertEquals(13, unpack(compiled, recoveryPlan).get("var0"));

		QuestTransition report = transition(definition, "reward",
			new QuestEvent.TalkToNpc(rewardNpcId, QuestDialogAction.USE_OBJECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.DEFAULT_SUCCESS.id())), report.afterCommit());
		QuestTransition preview = transition(definition, "reward",
			new QuestEvent.TalkToNpc(rewardNpcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
	}

	private static QuestTransition transition(QuestDefinition definition, String source,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " route " + event);
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static void assertRewardNode(QuestDefinition definition, Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest15300And25300RewardProjectionTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
