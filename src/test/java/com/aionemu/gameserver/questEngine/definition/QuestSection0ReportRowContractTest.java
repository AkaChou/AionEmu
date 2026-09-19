package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验击杀计数任务的 SECTION_0 报告行闭环合同：计数打满进入 REWARD 时，任务说明行索引
 * 必须等于客户端报告行（stage+1），continue 自环必须钉住所在阶段，且旧存档由 ENTER_WORLD
 * 迁移路线修复。
 *
 * <p>Verifies the SECTION_0 report-row closure contract for kill-counter quests: the journal row
 * index must advance to the client report row when the counters saturate, continuing self-loops must
 * pin their stage, and legacy REWARD saves must be repaired by a source-less ENTER_WORLD route.
 */
class QuestSection0ReportRowContractTest {
	private static final String CONTRACT_RESOURCE = "/quest/quest-section0-report-row-contract.tsv";
	/** 合同快照规模：低于该值说明基线被误删或 sweep 漏了任务。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 269;
	/** 至少需要这么多任务可以从全新 START 快照直接模拟到 REWARD（其余需要前置对话/物品）。 */
	private static final int EXPECTED_SIMULATED_QUESTS = 200;

	@Test
	void contractSnapshotKeepsItsCoverage() throws Exception {
		Map<Integer, int[]> contract = readings(CONTRACT_RESOURCE);
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"SECTION_0 report-row contract snapshot must keep its reviewed coverage");
		assertTrue(contract.values().stream().allMatch(row -> row[1] == row[0] + 1),
			"report row must be the row after the counter stage");
	}

	@Test
	void counterCompletionAdvancesSectionZeroToTheReportRow() throws Exception {
		for (Map.Entry<Integer, int[]> entry : readings(CONTRACT_RESOURCE).entrySet()) {
			int questId = entry.getKey();
			int stage = entry.getValue()[0];
			int reportRow = entry.getValue()[1];
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();

			QuestNode reward = definition.nodes().stream()
				.filter(node -> node.label().equals("reward"))
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " must define a reward node"));
			assertEquals(reportRow, reward.projection().variables().get("var0"),
				() -> "quest " + questId + " reward must project SECTION_0=" + reportRow);

			// 计数阶段所在节点：多阶段任务由 step2/k1 等节点承载，单阶段任务就是 started。
			// Stage owning node: multi-stage quests carry the counter row on step2/k1 nodes.
			String stageNode = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.START)
				.filter(node -> Integer.valueOf(stage).equals(node.projection().variables().get("var0")))
				.sorted(Comparator.comparing(node -> node.label().equals("started") ? 0 : 1))
				.map(QuestNode::label)
				.findFirst().orElseThrow(() -> new AssertionError(
					"quest " + questId + " must project SECTION_0=" + stage + " on a START node"));

			List<QuestTransition> continuing = definition.transitions().stream()
				.filter(transition -> stageNode.equals(transition.sourceNode()))
				.filter(transition -> stageNode.equals(transition.targetNode()))
				.filter(transition -> isKillEvent(transition.event()))
				.toList();
			for (QuestTransition transition : continuing) {
				assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(String field, int value)
							&& field.equals("var0") && value == stage),
					() -> "quest " + questId + " continuing self-loop must pin SECTION_0=" + stage);
			}

			List<QuestTransition> completing = definition.transitions().stream()
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> isKillEvent(transition.event()))
				.toList();
			assertFalse(completing.isEmpty(),
				() -> "quest " + questId + " must keep a completing kill route");
			for (QuestTransition transition : completing) {
				assertTrue(transition.actions().stream().anyMatch(action ->
						action instanceof QuestAction.SetVariable(String field, int value)
							&& field.equals("var0") && value == reportRow),
					() -> "quest " + questId + " completing kill route must set SECTION_0=" + reportRow);
				assertTrue(transition.afterCommit().contains(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
					() -> "quest " + questId + " completing kill route must refresh visibility");
			}

			boolean hasMigrationRepair = definition.transitions().stream()
				.filter(transition -> transition.sourceNode() == null)
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.EnterWorld)
				.anyMatch(transition -> transition.conditions().stream()
						.anyMatch(condition -> condition instanceof QuestCondition.StatusIs status
							&& status.status() == QuestStatus.REWARD)
					&& transition.conditions().stream()
						.anyMatch(condition -> limitsSectionZeroToStaleRow(condition, stage, reportRow))
					&& transition.actions().stream()
						.anyMatch(action -> action instanceof QuestAction.SetVariable(String field, int value)
							&& field.equals("var0") && value == reportRow)
					&& transition.afterCommit().contains(new AfterCommitAction.SyncQuestState(
						QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
			assertTrue(hasMigrationRepair,
				() -> "quest " + questId + " must repair legacy REWARD SECTION_0=" + stage + " saves");
		}
	}

	@Test
	void killSimulationLeavesTheJournalOnTheReportRow() throws Exception {
		int simulated = 0;
		List<Integer> notReachableFromZero = new ArrayList<>();
		for (Map.Entry<Integer, int[]> entry : readings(CONTRACT_RESOURCE).entrySet()) {
			int questId = entry.getKey();
			int reportRow = entry.getValue()[1];
			Optional<Integer> finalRow = simulateReportRow(load(questId));
			if (finalRow.isEmpty()) {
				notReachableFromZero.add(questId);
				continue;
			}
			assertEquals(reportRow, finalRow.orElseThrow(),
				() -> "quest " + questId + " must end on the client report row");
			simulated++;
		}
		final int simulatedCount = simulated;
		assertTrue(simulatedCount >= EXPECTED_SIMULATED_QUESTS,
			() -> "only " + simulatedCount + " quests could be simulated; not reachable from a fresh START: "
				+ notReachableFromZero);
	}

	private static boolean limitsSectionZeroToStaleRow(QuestCondition condition, int stage, int reportRow) {
		if (condition instanceof QuestCondition.QuestVariableIs(String field, int value)) {
			return field.equals("var0") && value == stage;
		}
		if (condition instanceof QuestCondition.VariableBelow(String field, int value)) {
			return field.equals("var0") && value == reportRow;
		}
		return false;
	}

	private static boolean isKillEvent(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	/**
	 * 从全新 START 快照按真实 planner 连续击杀；无法从零起点到达（需要前置对话/物品）时返回空。
	 * Simulates consecutive kills through the real planner; empty when a quest needs a prerequisite
	 * interaction before its first kill.
	 */
	private static Optional<Integer> simulateReportRow(CompiledQuestDefinition compiled) {
		QuestDefinition definition = compiled.definition();
		List<QuestEvent> events = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestSection0ReportRowContractTest::isKillEvent)
			.distinct()
			.toList();
		if (events.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : definition.progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		QuestStatus status = QuestStatus.START;
		for (int kills = 1; kills <= 400; kills++) {
			QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), status,
				definition.progressLayout().pack(variables), Map.of());
			QuestMutationPlan chosen = null;
			for (QuestEvent event : events) {
				for (QuestTransition transition : compiled.transitionsFor(event.type()).stream()
						.filter(candidate -> QuestEvent.matches(candidate.event(), event))
						.sorted(Comparator.comparingInt(candidate -> candidate.priority() == null
							? Integer.MAX_VALUE : candidate.priority()))
						.toList()) {
					Optional<QuestMutationPlan> plan = QuestMutationPlanner.plan(compiled, snapshot, event, transition);
					if (plan.isPresent()) {
						chosen = plan.orElseThrow();
						break;
					}
				}
				if (chosen != null) {
					break;
				}
			}
			if (chosen == null) {
				return Optional.empty();
			}
			variables = definition.progressLayout().unpack(chosen.nextPackedVariables());
			status = chosen.nextStatus();
			if (status == QuestStatus.REWARD) {
				return Optional.of(variables.getOrDefault("var0", 0));
			}
			if (status != QuestStatus.START) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	private static Map<Integer, int[]> readings(String resource) throws Exception {
		Map<Integer, int[]> contract = new LinkedHashMap<>();
		try (InputStream input = QuestSection0ReportRowContractTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing contract resource " + resource);
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#")) {
						continue;
					}
					String[] columns = line.split("\t");
					if (columns[0].equals("quest_id")) {
						continue;
					}
					contract.put(Integer.parseInt(columns[0].trim()),
						new int[] {Integer.parseInt(columns[1].trim()), Integer.parseInt(columns[2].trim())});
				}
			}
		}
		return contract;
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestSection0ReportRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
