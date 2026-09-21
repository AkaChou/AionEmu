package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestWorldFacts;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 10527/10528 与魔族镜像 20527/20528 的领奖行投影：交付后任务书必须指向最后一行“和代理人维达对话”，
 * 旧存档的 REWARD/var0=14 与 REWARD/var0=11 由进入世界自愈边纠正。
 * Verifies the reward-row projection of 10527/10528 and their Asmodian mirrors 20527/20528: after the
 * handover the journal must point at the final "talk to agent Vida" row, while saves persisted as
 * REWARD/var0=14 and REWARD/var0=11 are repaired on enter-world.
 */
class ArchdaevaRewardRowContractTest {

	@Test
	void quest10527AdvancesJournalToTheFinalRowWhenTheWorkItemIsUsed() throws Exception {
		CompiledQuestDefinition compiled = definition(10527);
		assertEquals(15, rewardRow(compiled.definition()));

		QuestTransition handover = transition(compiled.definition(), "s14",
			new QuestEvent.UseItem(182216075));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 14)), handover.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182216075, 1)), handover.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), handover.afterCommit());
		assertNull(handover.priority());
		assertPlannedHandover(compiled, handover, 14, 15, 182216075);
	}

	@Test
	void quest10528AdvancesJournalToTheFinalRowOnHandover() throws Exception {
		CompiledQuestDefinition compiled = definition(10528);
		assertEquals(12, rewardRow(compiled.definition()));

		QuestTransition handover = transition(compiled.definition(), "s11",
			new QuestEvent.TalkToNpc(806292, QuestDialogAction.SET_SUCCEED.id()));
		assertEquals(List.of(), handover.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 12),
			new QuestAction.RemoveItem(182216076, 1)), handover.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), handover.afterCommit());
		assertNull(handover.priority());
		assertPlannedHandover(compiled, handover, 11, 12, 182216076);
	}

	@Test
	void quest10525AdvancesJournalToTheFinalRowWhenTheWorkItemIsUsed() throws Exception {
		CompiledQuestDefinition compiled = definition(10525);
		assertEquals(7, rewardRow(compiled.definition()));

		QuestTransition handover = transition(compiled.definition(), "s6",
			new QuestEvent.UseItem(182216072));
		assertEquals(List.of(new QuestCondition.ZoneIs("LF6_ITEMUSEAREA_Q10525")),
			handover.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(182216072, 1),
			new QuestAction.GiveItem(182216073, 1)), handover.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), handover.afterCommit());
		assertNull(handover.priority());
		assertPlannedHandover(compiled, handover, 6, 7, 182216072, "LF6_ITEMUSEAREA_Q10525");
	}

	@Test
	void persistedRewardRowsAreRepairedOnEnterWorld() throws Exception {
		assertRecovery(10527, 14, 15);
		assertRecovery(10528, 11, 12);
		assertRecovery(10525, 6, 7);
		assertRecovery(20525, 6, 7);
	}

	@Test
	void asmodianMirrorsKeepTheSameRewardRow() throws Exception {
		assertMirrorRewardRow(10527, 20527, 15);
		assertMirrorRewardRow(10528, 20528, 12);
		assertMirrorRewardRow(10525, 20525, 7);
	}

	private static void assertPlannedHandover(CompiledQuestDefinition compiled,
			QuestTransition handover, int fromVar, int toVar, int workItemId) {
		assertPlannedHandover(compiled, handover, fromVar, toVar, workItemId, null);
	}

	private static void assertPlannedHandover(CompiledQuestDefinition compiled,
			QuestTransition handover, int fromVar, int toVar, int workItemId, String zone) {
		QuestSnapshot snapshot = snapshot(compiled, QuestStatus.START, Map.of("var0", fromVar),
			Map.of(workItemId, 1));
		if (zone != null) {
			snapshot = snapshot.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of(zone)));
		}
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot, handover.event(), handover).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(toVar, unpack(compiled, plan).get("var0"));
	}

	private static void assertRecovery(int questId, int staleVar, int repairedVar) throws Exception {
		CompiledQuestDefinition compiled = definition(questId);
		QuestTransition recovery = recoveryRoute(compiled.definition());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", staleVar)), recovery.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", repairedVar)), recovery.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit());
		assertNull(recovery.priority());

		QuestMutationPlan recoveryPlan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.REWARD, Map.of("var0", staleVar), Map.of()),
			recovery.event(), recovery).orElseThrow();
		assertEquals(QuestStatus.REWARD, recoveryPlan.nextStatus());
		assertEquals(repairedVar, unpack(compiled, recoveryPlan).get("var0"));
	}

	private static void assertMirrorRewardRow(int questId, int mirrorId, int expectedRewardRow)
			throws Exception {
		assertEquals(expectedRewardRow, rewardRow(definition(questId).definition()),
			() -> "quest " + questId + " reward journal row");
		assertEquals(rewardRow(definition(questId).definition()),
			rewardRow(definition(mirrorId).definition()),
			() -> "quest " + questId + " and mirror " + mirrorId + " reward journal row");
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables().get("var0");
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

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(),
			() -> "quest " + definition.id() + " reward recovery route");
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = ArchdaevaRewardRowContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
