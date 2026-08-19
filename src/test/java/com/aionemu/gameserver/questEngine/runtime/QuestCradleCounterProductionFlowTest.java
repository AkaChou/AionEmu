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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证永恒摇篮双阵营狩猎任务的实时计数不会被 source 投影锁死，
 * 并在第 5 次或第 10 次有效击杀时立即进入领奖。
 * Verifies that faction-paired Cradle hunts keep live counters outside the source projection and enter reward on the
 * 5th or 10th valid kill.
 */
class QuestCradleCounterProductionFlowTest {
	private static final List<QuestContract> CONTRACTS = List.of(
		new QuestContract(16828, 806282, Set.of(220470, 220471, 220472, 220594),
			220470, 5, true, true, true),
		new QuestContract(26828, 806287, Set.of(220470, 220471, 220472, 220473, 220594),
			220470, 5, false, false, false),
		new QuestContract(16829, 806282, Set.of(220458, 220465, 220466, 220469, 220475, 220476, 220477, 220479),
			220458, 10, true, true, true),
		new QuestContract(26829, 806287, Set.of(220474, 220475, 220476, 220477, 220479),
			220474, 10, true, false, false));

	@TestFactory
	Stream<DynamicTest> completesCradleHuntsOnTheConfiguredFinalKill() {
		return CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertContract(contract)));
	}

	private static void assertContract(QuestContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD,
			Map.of("var0", 1, "var1", contract.requiredKills()));

		QuestEvent targets = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition continuing = transition(definition, "started", "started", targets);
		assertEquals(1, continuing.priority());
		assertEquals(continuingConditions(contract), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "started", "reward", targets);
		assertEquals(0, completion.priority());
		assertEquals(completionConditions(contract), completion.conditions());
		assertEquals(completionActions(contract), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
			completion.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), QuestStatus.START,
			Map.of("var0", 0, "var1", 0), definition);
		for (int kills = 1; kills < contract.requiredKills(); kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", 0, "var1", kills), unpack(definition, snapshot));
		}

		QuestMutationPlan finalKill = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		assertEquals(QuestStatus.REWARD, finalKill.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", contract.requiredKills()),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalKill.requiredActions());

		assertRewardPages(definition, contract.reportNpcId());
	}

	private static List<QuestCondition> continuingConditions(QuestContract contract) {
		List<QuestCondition> conditions = new ArrayList<>();
		if (contract.guardsVar0()) {
			conditions.add(new QuestCondition.QuestVariableIs("var0", 0));
		}
		conditions.add(new QuestCondition.VariableBelow("var1", contract.requiredKills() - 1));
		return conditions;
	}

	private static List<QuestCondition> completionConditions(QuestContract contract) {
		List<QuestCondition> conditions = new ArrayList<>();
		if (contract.guardsVar0()) {
			conditions.add(new QuestCondition.QuestVariableIs("var0", 0));
		}
		conditions.add(contract.exactFinalCounter()
			? new QuestCondition.QuestVariableIs("var1", contract.requiredKills() - 1)
			: new QuestCondition.VariableAtLeast("var1", contract.requiredKills() - 1));
		return conditions;
	}

	private static List<QuestAction> completionActions(QuestContract contract) {
		if (contract.incrementOnFinalKill()) {
			return List.of(new QuestAction.IncrementVariable("var1", 1));
		}
		return List.of(
			new QuestAction.SetVariable("var1", contract.requiredKills()),
			new QuestAction.SetVariable("var0", 1));
	}

	private static void assertRewardPages(QuestDefinition definition, int reportNpcId) {
		QuestTransition success = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			success.afterCommit());

		QuestTransition preview = transition(definition, "reward", "reward",
			new QuestEvent.TalkToNpc(reportNpcId, QuestDialogAction.SELECT_QUEST_REWARD.id()));
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), preview.afterCommit());
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status,
			Map<String, Integer> variables, QuestDefinition definition) {
		return new QuestSnapshot(7, questId, status, definition.progressLayout().pack(variables), Map.of());
	}

	private static Map<String, Integer> unpack(QuestDefinition definition, QuestSnapshot snapshot) {
		return definition.progressLayout().unpack(snapshot.packedVariables());
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

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestCradleCounterProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存四个任务的目标、阈值条件和末次计数动作差异。
	 * Holds target, threshold-condition, and final-counter action differences for the four quests.
	 */
	private record QuestContract(int questId, int reportNpcId, Set<Integer> targetNpcIds,
			int sampleTargetNpcId, int requiredKills, boolean guardsVar0,
			boolean exactFinalCounter, boolean incrementOnFinalKill) {
	}
}
