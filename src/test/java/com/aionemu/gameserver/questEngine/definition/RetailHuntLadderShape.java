package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原版 DataDriven 纯 Hunt 行的"行阶梯 + 段计数"形状断言。
 * <p>
 * 原版 {@code data_driven_quest.xml} 的 hunt 行以 {@code <data>} 块给出进度行、块内以 {@code "monster 名单 计数;"}
 * 给出该行的并行目标；客户端 {@code quest_monster.csv} 用同一编号投影：
 * {@code Progress(SECTION_0==行号; SECTION_m<该行第 m 个目标计数)}。因此编译结果必须是唯一的行阶梯形：
 * 布局 {@code var0}（行号）+ {@code var1..varN}（当前行的段计数）；行节点只钉行号（段计数保持自由，自环不得
 * 落在被投影钉住的字段上）；每段一条"未满自环 +1"（优先级 1）与一条"本行收口"（优先级 0，推进行号并清零本行计数，
 * 末行不清零、满计数留在领奖投影里）。
 * <p>
 * 该夹具让同族回归测试共用一份形状合同，而不是各自复写旧网格/串行链的节点名与字段号。
 * <p>
 * Shape assertions for the true-server DataDriven pure-hunt "row ladder + segment counters" form. The
 * retail DD row declares its progress rows through {@code <data>} blocks and each row's parallel targets
 * through {@code "monster list count;"}; the client quest journal mirrors the same numbering as
 * {@code Progress(SECTION_0==row; SECTION_m<count)}. The compiled definition must therefore be the single
 * row-ladder shape: layout {@code var0} (row index) plus {@code var1..varN} (current row's segment
 * counters); row nodes pin the row index only; every segment carries one unsatisfied-self-loop increment
 * (priority 1) and one row-closing edge (priority 0, advancing the row and clearing the row's counters,
 * except on the final row where the saturated counts stay in the reward projection).
 */
public final class RetailHuntLadderShape {

	private RetailHuntLadderShape() {
	}

	/**
	 * 断言行阶梯形状并走一遍完整击杀流程。
	 * <p>
	 * {@code rows} 的第 k 项是第 k 行的段计数（每段 1..4 段、计数 1..63）；夹具从生产 IR 反推每段的
	 * 目标集合，再逐段饱和地走完整个阶梯，要求最终落在领奖行。
	 * <p>
	 * Asserts the row-ladder shape and then walks the whole ladder one segment at a time. Entry k of
	 * {@code rows} lists row k's segment counts (1..4 segments, counts 1..63); the fixture derives each
	 * segment's target set from the production IR and saturates every segment until the reward row.
	 */
	public static void assertLadder(CompiledQuestDefinition compiled, List<List<Integer>> rows) {
		Objects.requireNonNull(compiled, "compiled");
		Objects.requireNonNull(rows, "rows");
		QuestDefinition definition = compiled.definition();
		int rowCount = rows.size();
		int maxSegments = rows.stream().mapToInt(List::size).max().orElseThrow();
		assertTrue(rowCount >= 1 && rowCount <= 63, () -> "row count out of the 6-bit ladder: " + rowCount);
		for (List<Integer> row : rows) {
			assertTrue(row.size() >= 1 && row.size() <= 4, () -> "row width out of SECTION_1..4: " + row);
			for (int required : row) {
				assertTrue(required >= 1 && required <= 63, () -> "segment count out of the 6-bit slot: " + required);
			}
		}

		assertLayout(definition, maxSegments);
		assertNodes(definition, rows);
		Map<String, Set<Integer>> segmentTargets = assertKillEdges(definition, rows);
		assertWalk(compiled, rows, segmentTargets);
	}

	/** 布局 = {@code var0} + {@code var1..varMax}，各 6 位且按段序连续偏移。 / Layout contract. */
	private static void assertLayout(QuestDefinition definition, int maxSegments) {
		List<BitField> fields = definition.progressLayout().fields();
		assertEquals(1 + maxSegments, fields.size(),
			() -> "row ladder must declare var0 plus one counter per segment: " + fields);
		for (int index = 0; index < fields.size(); index++) {
			BitField field = fields.get(index);
			assertEquals("var" + index, field.name(), () -> "field naming must follow var0..varN: " + fields);
			assertEquals(6 * index, field.offset(), () -> "field offsets must stay contiguous: " + fields);
			assertEquals(6, field.width(), () -> "row ladder slots are 6-bit (SECTION vocabulary): " + fields);
		}
	}

	/** 节点 = 未接 / 行 0..n-1 / 领奖行 / 完成，行节点只钉行号。 / Node contract. */
	private static void assertNodes(QuestDefinition definition, List<List<Integer>> rows) {
		int rowCount = rows.size();
		List<String> expected = new ArrayList<>();
		expected.add("unaccepted");
		expected.add("started");
		for (int row = 1; row < rowCount; row++) {
			expected.add("s" + row);
		}
		expected.add("reward");
		expected.add("complete");
		assertEquals(new TreeSet<>(expected),
			new TreeSet<>(definition.nodes().stream().map(QuestNode::label).toList()),
			() -> "the row ladder owns exactly " + expected);

		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		for (int row = 1; row < rowCount; row++) {
			assertNode(definition, "s" + row, QuestStatus.START, Map.of("var0", row));
		}
		Map<String, Integer> rewardRow = new LinkedHashMap<>();
		rewardRow.put("var0", rowCount);
		List<Integer> finalRow = rows.getLast();
		for (int segment = 0; segment < finalRow.size(); segment++) {
			rewardRow.put("var" + (segment + 1), finalRow.get(segment));
		}
		assertNode(definition, "reward", QuestStatus.REWARD, rewardRow);
		Map<String, Integer> zero = new LinkedHashMap<>();
		zero.put("var0", 0);
		for (int segment = 1; segment <= rows.stream().mapToInt(List::size).max().orElseThrow(); segment++) {
			zero.put("var" + segment, 0);
		}
		assertNode(definition, "complete", QuestStatus.COMPLETE, zero);
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElse(null);
		assertNotNull(node, () -> "missing ladder node " + label);
		assertEquals(status, node.projection().status(), () -> label + " status");
		assertEquals(variables, node.projection().variables(), () -> label + " projection");
	}

	/**
	 * 击杀边合同：每段一条未满自环（优先级 1）与一条行收口边（优先级 0），两者挂同一批目标。
	 * <p>
	 * Kill-edge contract: each segment carries one under-filled self-loop (priority 1) and one
	 * row-closing edge (priority 0) over the same target set.
	 */
	private static Map<String, Set<Integer>> assertKillEdges(QuestDefinition definition, List<List<Integer>> rows) {
		int rowCount = rows.size();
		Map<String, Set<Integer>> targets = new LinkedHashMap<>();
		for (int row = 0; row < rowCount; row++) {
			List<Integer> requirements = rows.get(row);
			boolean finalRow = row == rowCount - 1;
			String source = row == 0 ? "started" : "s" + row;
			String target = finalRow ? "reward" : "s" + (row + 1);
			for (int segment = 0; segment < requirements.size(); segment++) {
				final int segmentIndex = segment;
				int required = requirements.get(segment);
				boolean parallel = requirements.size() > 1;
				String section = "var" + (segment + 1);
				List<QuestCondition> selfLoopGate = List.of(
					new QuestCondition.VariableBelow(section, parallel ? required : required - 1));
				List<QuestCondition> closingGate = new ArrayList<>();
				// 收口边先钉本段下界（这一杀把它打满），并行行再补"其余段已满"——三段式原版写法。
				// The closing edge binds the own segment first (this kill saturates it) and, on a parallel
				// row, additionally requires every other segment saturated (the retail three-edge canon).
				closingGate.add(new QuestCondition.VariableAtLeast(section, required - 1));
				if (parallel) {
					for (int other = 0; other < requirements.size(); other++) {
						if (other != segment) {
							closingGate.add(new QuestCondition.VariableAtLeast("var" + (other + 1),
								requirements.get(other)));
						}
					}
				}
				List<QuestAction> closingActions = new ArrayList<>();
				closingActions.add(new QuestAction.SetVariable("var0", row + 1));
				if (!finalRow) {
					for (int other = 0; other < requirements.size(); other++) {
						closingActions.add(new QuestAction.SetVariable("var" + (other + 1), 0));
					}
				}

				Set<Integer> selfLoopTargets = new LinkedHashSet<>();
				Set<Integer> closingTargets = new LinkedHashSet<>();
				for (QuestTransition transition : definition.transitions()) {
					if (!(transition.event() instanceof QuestEvent.KillNpc killNpc)
							|| !source.equals(transition.sourceNode())) {
						continue;
					}
					final String slot = section;
					if (source.equals(transition.targetNode()) && Integer.valueOf(1).equals(transition.priority())
							&& selfLoopGate.equals(transition.conditions())) {
						assertEquals(List.of(new QuestAction.IncrementVariable(slot, 1)), transition.actions(),
							() -> "self-loop must only increment " + slot + ": " + transition);
						assertTrue(selfLoopTargets.add(killNpc.npcId()),
							() -> "duplicate self-loop for " + killNpc.npcId() + " in " + source);
					} else if (target.equals(transition.targetNode()) && Integer.valueOf(0).equals(transition.priority())
							&& closingGate.equals(transition.conditions())) {
						assertEquals(closingActions, transition.actions(),
							() -> "row-closing edge must advance the row and clear its counters: " + transition);
						assertTrue(closingTargets.add(killNpc.npcId()),
							() -> "duplicate row-closing edge for " + killNpc.npcId() + " in " + source);
					}
				}
				assertEquals(selfLoopTargets, closingTargets,
					() -> source + " segment " + segmentIndex + " must pair each target with both edges");
				assertTrue(!selfLoopTargets.isEmpty(),
					() -> source + " segment " + segmentIndex + " owns no kill target");
				assertTrue(targets.values().stream().noneMatch(previous ->
						!java.util.Collections.disjoint(previous, selfLoopTargets)),
					() -> "ladder rows must not share kill targets: " + selfLoopTargets);
				targets.put(source + "/" + section, selfLoopTargets);
			}
			// 行节点不得保留任何其它击杀边（旧网格/串行的逐态推进与串行门控都已退役）。
			// Row nodes keep no other kill edges (the legacy per-state grid and chained gating retired).
			int rowIndex = row;
			List<QuestTransition> leftover = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc)
				.filter(transition -> source.equals(transition.sourceNode()))
				.filter(transition -> !(source.equals(transition.targetNode())
					|| ("s" + (rowIndex + 1)).equals(transition.targetNode())
					|| "reward".equals(transition.targetNode())))
				.toList();
			assertEquals(List.of(), leftover, () -> "unexpected kill targets out of " + source);
		}
		return targets;
	}

	/** 逐段饱和走完阶梯：段计数自由累加、行号只在收口时推进、末行落在领奖行。 / Saturation walk. */
	private static void assertWalk(CompiledQuestDefinition compiled, List<List<Integer>> rows,
			Map<String, Set<Integer>> segmentTargets) {
		QuestDefinition definition = compiled.definition();
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : definition.progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		QuestStatus status = QuestStatus.START;
		for (int row = 0; row < rows.size(); row++) {
			final int rowIndex = row;
			String source = row == 0 ? "started" : "s" + row;
			for (int segment = 0; segment < rows.get(row).size(); segment++) {
				final int segmentIndex = segment;
				int required = rows.get(row).get(segment);
				String section = "var" + (segment + 1);
				final int npcId = segmentTargets.get(source + "/" + section).iterator().next();
				for (int kill = 0; kill < required; kill++) {
					final int attempt = kill;
					final Map<String, Integer> before = Map.copyOf(variables);
					QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), status,
						definition.progressLayout().pack(variables), Map.of());
					QuestMutationPlan plan = plan(compiled, snapshot, npcId);
					assertNotNull(plan, () -> "ladder kills must stay plannable at row " + rowIndex + " segment "
						+ segmentIndex + " kill " + (attempt + 1) + " with " + before);
					variables = definition.progressLayout().unpack(plan.nextPackedVariables());
					status = plan.nextStatus();
				}
				// 收口那一杀同时推进行号并清零本行计数（非末行），因此段计数的语义上界是 required：
				// 并行段在收口前饱和到 required，单段行随收口清零（required-1 是它的自环上界），
				// 末行由领奖投影钉住 required。
				// The closing kill advances the row and clears its counters (non-final rows), so a
				// segment counter is bounded by required: parallel segments saturate at required before
				// the close, single-segment rows are cleared by the closing kill, and the final row is
				// pinned to required by the reward projection.
				final String slotName = section;
				final int expected = required;
				final boolean finalRow = row == rows.size() - 1;
				final int observed = variables.get(slotName);
				assertTrue(observed <= expected,
					() -> "segment counter " + slotName + " must not exceed " + expected + " (was " + observed + ")");
				if (finalRow) {
					assertEquals(expected, observed,
						() -> "the final row keeps " + slotName + " saturated in the reward projection");
				}
			}
			final int advanced = row + 1;
			assertEquals(advanced, variables.get("var0"),
				() -> "row " + rowIndex + " must close exactly once its segments are saturated");
		}
		assertEquals(QuestStatus.REWARD, status, "the saturated ladder must land on the reward row");
		assertEquals(rows.size(), variables.get("var0"), "the reward row is the row after the last block");
	}

	private static QuestMutationPlan plan(CompiledQuestDefinition compiled, QuestSnapshot snapshot, int npcId) {
		QuestEvent event = new QuestEvent.KillNpc(npcId);
		for (QuestTransition transition : compiled.definition().transitions()) {
			var candidate = QuestMutationPlanner.plan(compiled, snapshot, event, transition);
			if (candidate.isPresent()) {
				return candidate.get();
			}
		}
		return null;
	}
}
