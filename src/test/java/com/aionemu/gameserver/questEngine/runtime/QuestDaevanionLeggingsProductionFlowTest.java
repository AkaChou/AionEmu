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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证守护者套装护腿任务的 60 次实时击杀计数与交付页面合同。
 * Verifies the 60-kill live counter and turn-in page contract for both Daevanion leggings quests.
 */
class QuestDaevanionLeggingsProductionFlowTest {
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(15314, Set.of(233945, 233946, 233947, 233948, 233949, 233950, 233951, 233952),
			805328, 182215835, 182215868),
		new QuestContract(25314, Set.of(233902, 233903, 233904, 233905, 233906, 233907, 233908),
			805340, 182215850, 182215880));

	@TestFactory
	Stream<DynamicTest> preservesTheRetailSixtyKillAndTurnInFlow() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest("quest " + contract.questId(),
			() -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));

		QuestEvent configuredEvent = new QuestEvent.KillNpcSet(contract.killNpcIds());
		QuestTransition continuing = transition(definition, "s2", "s2", configuredEvent);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(new QuestCondition.VariableBelow("var1", 59)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertPacketOnly(continuing);

		QuestTransition completing = transition(definition, "s2", "s3", configuredEvent);
		assertEquals(0, completing.priority());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 59)), completing.conditions());
		assertEquals(List.of(
			new QuestAction.SetVariable("var0", 3),
			new QuestAction.SetVariable("var1", 0)), completing.actions());
		assertPacketOnly(completing);

		int packed = definition.progressLayout().pack(Map.of("var0", 2, "var1", 0));
		QuestEvent kill = new QuestEvent.KillNpc(contract.killNpcIds().iterator().next());
		for (int count = 1; count <= 59; count++) {
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(contract.questId(), packed), kill, continuing).orElseThrow();
			packed = plan.nextPackedVariables();
			assertEquals(Map.of("var0", 2, "var1", count), definition.progressLayout().unpack(packed));
		}
		assertTrue(QuestMutationPlanner.plan(compiled, snapshot(contract.questId(), packed), kill, continuing)
			.isEmpty());
		QuestMutationPlan finalKill = QuestMutationPlanner.plan(compiled,
			snapshot(contract.questId(), packed), kill, completing).orElseThrow();
		assertEquals(QuestStatus.START, finalKill.nextStatus());
		assertEquals(Map.of("var0", 3, "var1", 0),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));

		assertPage(definition, contract.turnInNpcId(), QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		assertPage(definition, contract.turnInNpcId(), QuestDialogAction.SELECT4_1, QuestDialogPage.SELECT4_1);
		assertPage(definition, contract.turnInNpcId(), QuestDialogAction.SELECT4_1_1, QuestDialogPage.SELECT4_1_1);

		QuestTransition turnIn = transition(definition, "s3", "reward",
			new QuestEvent.TalkToNpc(contract.turnInNpcId(), QuestDialogAction.SET_SUCCEED.id()));
		assertEquals(List.of(), turnIn.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(contract.requiredItemId(), 1),
			new QuestAction.GiveItem(contract.workItemId(), 1),
			new QuestAction.SetVariable("var0", 4)), turnIn.actions());
		assertNull(turnIn.priority());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), turnIn.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, int npcId, QuestDialogAction action,
			QuestDialogPage page) {
		QuestTransition transition = transition(definition, "s3", "s3",
			new QuestEvent.TalkToNpc(npcId, action.id()));
		assertEquals(List.of(), transition.conditions());
		assertEquals(List.of(), transition.actions());
		assertNull(transition.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())), transition.afterCommit());
	}

	private static void assertPacketOnly(QuestTransition transition) {
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			transition.afterCommit());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
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
		try (InputStream input = QuestDaevanionLeggingsProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存双阵营任务由 retail 步骤和 Aion 5.8 客户端证明的差异字段。
	 * Holds faction-specific fields proven by the retail steps and Aion 5.8 client.
	 */
	private record QuestContract(int questId, Set<Integer> killNpcIds, int turnInNpcId,
			int requiredItemId, int workItemId) {
	}
}
