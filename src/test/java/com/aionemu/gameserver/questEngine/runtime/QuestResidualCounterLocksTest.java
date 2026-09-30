package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestNode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 E2E 报告刷新后剩余 NO_MATCH/TRANSACTION_FAILURE 批次的修复合同：
 * 25324/17541 已由真端 DataDriven 顺序 SECTION 链驱动（链式前缀节点、击杀边只推进首个未满段、
 * 满链节点才可无门禁报告——旧 XML 的 h3/started 实时计数投影锁随退役入 git 历史）；
 * 28504/15322/25322 的阶段完成边不再对已达阈值的字段执行越界自增。
 * Locks the repair contract for the residual NO_MATCH/TRANSACTION_FAILURE batch found after the
 * E2E report refresh: 25324/17541 are retail sequential SECTION chains now (chained prefix nodes,
 * kill edges advancing only the first unfinished stage, reporting gated behind the full chain — the
 * legacy live-counter projection locks retired with the XML), and the stage-completion edges of
 * 28504/15322/25322 no longer over-increment fields past their threshold.
 */
class QuestResidualCounterLocksTest {
	/** 25324 的真端报告/接取 NPC（DF5_Maschine_E）。 / The retail report NPC of 25324. */
	private static final int QUEST_25324_REPORT_NPC = 805343;

	@Test
	void quest25324FinalStageReportsThroughTheSequentialChain() throws Exception {
		CompiledQuestDefinition definition = productionLoad(25324);
		QuestDefinition raw = definition.definition();
		// 25324 已由真端三段顺序链驱动（20/20/20）：满链节点投影三段满计数；P0-2 顺序链规范形下
		// 满链节点 QUEST_SELECT 无门禁直翻领奖，未满链节点无 QUEST_SELECT/1009 报告通道，
		// 提前上交不可达——旧 XML 的 h3 节点与 var1 投影锁一并退役。
		// 25324 is a retail three-stage sequential chain (20/20/20): the full chain node projects
		// all three counters; in the canonical sequential shape the full node's QUEST_SELECT flips
		// reward ungated and unfinished nodes keep no QUEST_SELECT/1009 report channel — the legacy
		// h3 node and its var1 projection lock are gone.
		List<Integer> counts = List.of(20, 20, 20);
		List<List<Integer>> chain = chainedPrefixStates(counts);
		String fullLabel = chainLabel(chain.getLast());
		assertEquals(Map.of("var0", 20, "var1", 20, "var2", 20),
			node(definition, fullLabel).projection().variables());

		for (List<Integer> state : chain.subList(0, chain.size() - 1)) {
			String source = chainLabel(state);
			// 未满链节点：无 QUEST_SELECT/1009 报告通道（P0-2 规范形，页链不再由服务端驱动）。
			// Unfinished chain nodes: no QUEST_SELECT/1009 report channel (canonical since P0-2).
			assertTrue(raw.transitions().stream().noneMatch(candidate ->
				source.equals(candidate.sourceNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == QUEST_25324_REPORT_NPC && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
						|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
				() -> source + " 不得保留报告通道路由");
		}
		QuestTransition report = raw.transitions().stream()
			.filter(candidate -> fullLabel.equals(candidate.sourceNode())
				&& "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == QUEST_25324_REPORT_NPC
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.findFirst().orElseThrow();
		assertEquals(List.of(), report.conditions(), "the full chain node delivers ungated");

		// 顺序推进模拟：逐段打满 20 只后仍停在 START，满段 QUEST_SELECT 交付才进领奖。
		// Sequential walk: filling each stage's 20 kills stays START; only the full-node QUEST_SELECT
		// delivery enters reward.
		int stageOneSample = sampleTarget(raw, "a0b0c0");
		int stageTwoSample = sampleTarget(raw, "a20b0c0");
		int stageThreeSample = sampleTarget(raw, "a20b20c0");
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			// 真端接取流先落到链首节点（a0b0c0），击杀路由从 START 态才可达。
			// The retail accept flow must first land on the first chain node; kill routes are only
			// reachable from the START state.
			runtime.prepare(acceptRoute(definition));
			assertTrue(runtime.dispatchPrepared().handled());
			// 乱序不计：段 1 未满时段 2 样本不产生任何击杀计划。
			// Out-of-order kills never count: a stage-2 sample counts nothing before stage 1 fills.
			assertFalse(runtime.dispatchWorld(new QuestEvent.KillNpc(stageTwoSample)).handled(),
				"a later-stage target must not count early");
			for (int kill = 1; kill <= 20; kill++) {
				dispatchKill(runtime, stageOneSample);
			}
			for (int kill = 1; kill <= 20; kill++) {
				dispatchKill(runtime, stageTwoSample);
			}
			for (int kill = 1; kill <= 20; kill++) {
				dispatchKill(runtime, stageThreeSample);
			}
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 20, "var1", 20, "var2", 20), variables(definition, runtime));

			runtime.prepare(report);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 20, "var1", 20, "var2", 20), variables(definition, runtime));
		}
	}

	@Test
	void quest17541AdvancesOnlyTheFirstUnfinishedStage() throws Exception {
		CompiledQuestDefinition definition = productionLoad(17541);
		// 17541 已由真端三段顺序链驱动（1/1/1）：217195 属段 2，零态击杀不计数；217185 → 段 1、
		// 217195 → 段 2、217204 → 段 3 逐段推进，末杀仍停在 START，满链节点才可报告。
		// Retail three-stage sequential chain (1/1/1): 217195 is a stage-2 target and must not count
		// from the zero state; 217185/217195/217204 advance one stage each and the final kill stays
		// START — only the full chain node reports.
		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			// 真端接取流先落到链首节点（a0b0c0）；随后 217195（段 2 目标）在零态不得计数。
			// The retail accept flow lands on the first chain node first; 217195 (a stage-2 target)
			// must then not count from the zero state.
			runtime.prepare(acceptRoute(definition));
			assertTrue(runtime.dispatchPrepared().handled());
			assertFalse(runtime.dispatchWorld(new QuestEvent.KillNpc(217195)).handled(),
				"a later-stage target must not count from the zero state");
			dispatchKill(runtime, 217185);
			assertEquals(Map.of("var0", 1, "var1", 0, "var2", 0), variables(definition, runtime));
			dispatchKill(runtime, 217195);
			assertEquals(Map.of("var0", 1, "var1", 1, "var2", 0), variables(definition, runtime));
			dispatchKill(runtime, 217204);
			assertEquals(QuestStatus.START, runtime.state().status());
			assertEquals(Map.of("var0", 1, "var1", 1, "var2", 1), variables(definition, runtime));
		}
	}

	@Test
	void quest28504FinalKillEntersRewardWithoutOutOfRangeIncrement() throws Exception {
		CompiledQuestDefinition definition = load(28504);
		QuestTransition counting = killRoute(definition, "hunting");
		QuestTransition finishing = killRoute(definition, "reward");
		assertEquals(List.of(new QuestCondition.VariableBelow("var0", 65)), counting.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var0", 1)), counting.actions());
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 65)), finishing.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 65)), finishing.actions());

		try (QuestE2eRuntime runtime = new QuestE2eRuntime(definition)) {
			runtime.prepare(counting);
			assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(216897)).handled());
			assertEquals(QuestStatus.START, runtime.state().status());
			runtime.prepare(finishing);
			assertTrue(runtime.dispatchPrepared().handled());
			assertEquals(QuestStatus.REWARD, runtime.state().status());
			assertEquals(Map.of("var0", 65), variables(definition, runtime));
		}
	}

	@Test
	void quests15322And25322StageEdgesResetCountersWithoutOverIncrement() throws Exception {
		for (int questId : new int[] {15322, 25322}) {
			// QE-109 起 15322/25322 由真端多胞感官区链驱动（XML 已退役 ⇒ 走生产视图）；真端把每段猎杀集合
			// 逐 NPC 展开成 KillNpc 边（遗留壳是一条 KillNpcSet 边），因此按**源节点**聚合断言段数。
			// Since QE-109 quests 15322/25322 follow the retail multi-cell sensory-area chain (their XML is
			// retired, so the production view is used); retail expands each stage's hunt set into per-npc
			// KillNpc edges (the shell used one KillNpcSet edge), so stages are asserted per source node.
			CompiledQuestDefinition definition = productionLoad(questId);
			List<QuestTransition> stageEdges = definition.definition().transitions().stream()
				.filter(candidate -> candidate.actions().stream()
					.anyMatch(action -> action instanceof QuestAction.SetVariable(String field, int value)
						&& "var1".equals(field) && value == 0))
				.toList();
			// 末段（s9→reward）不进本集合：领奖投影携带末段计数，所以完成边只推进 var0。
			// The final stage (s9->reward) is excluded: the reward projection carries the final count, so its
			// completion edge only advances var0.
			assertEquals(java.util.Set.of("s1", "s3", "s5", "s7"),
				stageEdges.stream().map(QuestTransition::sourceNode).collect(java.util.stream.Collectors.toSet()),
				"quest " + questId + " resetting stage source nodes");
			for (QuestTransition edge : stageEdges) {
				assertTrue(edge.actions().stream()
						.noneMatch(action -> action instanceof QuestAction.IncrementVariable increment
							&& "var1".equals(increment.field())),
					"quest " + questId + " stage edge still increments var1 past its threshold");
			}
		}
	}

	private static void dispatchKill(QuestE2eRuntime runtime, int npcId) {
		assertTrue(runtime.dispatchWorld(new QuestEvent.KillNpc(npcId)).handled(),
			"kill " + npcId + " was not handled");
	}

	private static QuestTransition killRoute(CompiledQuestDefinition definition, String targetNode) {
		return definition.definition().transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet)
			.filter(candidate -> candidate.targetNode().equals(targetNode))
			.findFirst().orElseThrow();
	}

	private static Map<String, Integer> variables(CompiledQuestDefinition definition, QuestE2eRuntime runtime) {
		return definition.definition().progressLayout().unpack(runtime.state().packedVariables());
	}

	private static QuestNode node(CompiledQuestDefinition definition, String label) {
		return definition.definition().nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
	}

	/** 链式前缀状态：每步推进首个未满段。 / Chained prefix states; each step fills the first unfinished stage. */
	private static List<List<Integer>> chainedPrefixStates(List<Integer> counts) {
		List<List<Integer>> chain = new ArrayList<>();
		chain.add(new ArrayList<>(java.util.Collections.nCopies(counts.size(), 0)));
		while (true) {
			List<Integer> current = chain.get(chain.size() - 1);
			List<Integer> next = new ArrayList<>(current);
			boolean advanced = false;
			for (int slot = 0; slot < counts.size(); slot++) {
				if (next.get(slot) < counts.get(slot)) {
					next.set(slot, next.get(slot) + 1);
					advanced = true;
					break;
				}
			}
			if (!advanced) {
				return List.copyOf(chain);
			}
			chain.add(next);
		}
	}

	/** 链态标签（编译器 {@code label()} 同构）：a0b0c0。 / Chain-state label mirroring the compiler. */
	private static String chainLabel(List<Integer> state) {
		StringBuilder label = new StringBuilder();
		for (int index = 0; index < state.size(); index++) {
			label.append((char) ('a' + index)).append(state.get(index));
		}
		return label.toString();
	}

	/** 该链节点的任一击杀目标样本。 / One kill-target sample carried by the chain node. */
	private static int sampleTarget(QuestDefinition definition, String label) {
		return definition.transitions().stream()
			.filter(candidate -> label.equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc)
			.mapToInt(candidate -> ((QuestEvent.KillNpc) candidate.event()).npcId())
			.findFirst().orElseThrow(() -> new AssertionError("chain node " + label + " carries no kill edge"));
	}

	/** unaccepted → 链首的 QUEST_ACCEPT_SIMPLE 接取路由（真端 8 路接取展开中的一路）。
	 * The QUEST_ACCEPT_SIMPLE accept route from unaccepted to the chain head (one of the retail
	 * 8-route accept expansion). */
	private static QuestTransition acceptRoute(CompiledQuestDefinition definition) {
		return definition.definition().transitions().stream()
			.filter(candidate -> "unaccepted".equals(candidate.sourceNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_ACCEPT_SIMPLE.id())
			.findFirst().orElseThrow(() -> new AssertionError(
				"quest " + definition.id() + " keeps no QUEST_ACCEPT_SIMPLE route from unaccepted"));
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest/definitions/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
				QuestResidualCounterLocksTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/** 真端驱动任务的生产视图（退役 XML 只在 git 历史里）。 / Production view for retail-driven quests. */
	private static CompiledQuestDefinition productionLoad(int questId) {
		return ProductionQuestDefinitions.definition(questId);
	}
}
