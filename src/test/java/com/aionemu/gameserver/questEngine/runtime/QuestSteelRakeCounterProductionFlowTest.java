package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证钢耙号双阵营任务的实时击杀计数和第二阶段 NPC 页面链。
 * Verifies live kill counters and second-stage NPC dialog chains for both Steel Rake quests.
 */
class QuestSteelRakeCounterProductionFlowTest {
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(3205, Set.of(215049, 219024), 804601, 204535),
		new QuestContract(4205, Set.of(218972, 218974, 218975, 218976, 218979, 218977), 205233, 204792));

	@TestFactory
	Stream<DynamicTest> preservesKillProgressAndSecondStageDialogOwnership() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of());
		assertNode(definition, "s16", QuestStatus.START, Map.of("var0", 16));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 16));

		QuestTransition kill = transition(definition, "started", "started",
			new QuestEvent.KillNpcSet(contract.killNpcIds()));
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 15)), kill.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), kill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kill.afterCommit());
		assertNull(kill.priority());

		int packed = compiled.definition().progressLayout().pack(Map.of("var0", 0));
		QuestEvent killEvent = new QuestEvent.KillNpc(contract.killNpcIds().iterator().next());
		for (int count = 1; count <= 15; count++) {
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(contract.questId(), packed), killEvent, kill).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
			packed = plan.nextPackedVariables();
			assertEquals(Map.of("var0", count), definition.progressLayout().unpack(packed));
		}
		assertTrue(QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), packed), killEvent, kill)
			.isEmpty());

		QuestTransition advance = talk(definition, "started", "s16", contract.counterNpcId(),
			QuestDialogAction.SETPRO2);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 15)), advance.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 16)), advance.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), advance.afterCommit());
		int beforeRequired = definition.progressLayout().pack(Map.of("var0", 14));
		assertFalse(QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), beforeRequired),
			advance.event(), advance).isPresent());

		QuestMutationPlan advancePlan = QuestMutationPlanner.plan(compiled,
			snapshot(contract.questId(), packed), advance.event(), advance).orElseThrow();
		assertEquals(QuestStatus.START, advancePlan.nextStatus());
		assertEquals(Map.of("var0", 16), definition.progressLayout().unpack(advancePlan.nextPackedVariables()));

		assertPage(definition, "s16", contract.handoffNpcId(), QuestDialogAction.QUEST_SELECT,
			QuestDialogPage.SELECT3);
		assertPage(definition, "s16", contract.handoffNpcId(), QuestDialogAction.SELECT3_1,
			QuestDialogPage.SELECT3_1);
		assertTrue(definition.transitions().stream().noneMatch(candidate -> candidate.event().equals(
			new QuestEvent.TalkToNpc(contract.counterNpcId(), QuestDialogAction.SELECT3_1.id()))));

		QuestTransition reward = talk(definition, "s16", "reward", contract.handoffNpcId(),
			QuestDialogAction.SET_SUCCEED);
		assertEquals(List.of(), reward.conditions());
		assertEquals(List.of(), reward.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), reward.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		QuestTransition transition = talk(definition, source, source, npcId, action);
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			QuestDialogAction action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action.id()));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static QuestSnapshot snapshot(int questId, int packedVariables) {
		return new QuestSnapshot(7, questId, QuestStatus.START, packedVariables, Map.of());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestSteelRakeCounterProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存两个阵营任务由旧 handler 和 Aion 5.8 客户端证明的差异字段。
	 * Holds faction-specific fields proven by the legacy handlers and Aion 5.8 client.
	 */
	private record QuestContract(int questId, Set<Integer> killNpcIds, int counterNpcId, int handoffNpcId) {
	}
}
