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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定批次 10：retail“单步任务”（start + 1 step）族的领奖行合同。
 * retail 源 `definitions/compact/quests/scripts/zz_retail_simple_quests.xml` 里这批任务是
 * “拿任务 → 完成一个交付/收集动作 → 领奖”的两段式，客户端 quest_summary 正好 2 行，
 * 末行是领奖/交付行（交给/送给/报告/再次对话）。同族 249 个 retail 单步任务中 184 个早已是
 * `reward var0=1`，本批 17 个的投影停在 0：其中 12 个末行完全没有 START/REWARD 状态
 * （审计 MISSING_LAST_ROW），5 个末行有中间行状态但领奖态仍显示上一行（审计 ROW_BEHIND）。
 * 两组都补一条无 source 的 enter-world 自愈边（REWARD/var0=0 → 1），否则旧存档会因
 * `QuestMutationPlanner#matchesSourceNode` 的“投影变量必须全等”语义匹配不到任何领奖路由。
 * Locks the tenth repair batch: the retail "single-step" family (start + one step) has a two-row
 * quest_summary whose last row is the reward row, and 184 of the 249 sibling retail single-step quests
 * already projected `reward var0=1`. The 17 quests here still projected 0 — 12 of them left the last
 * journal row without any START/REWARD state, 5 kept showing the previous row in the reward state. Both
 * groups gained a source-less enter-world recovery edge (REWARD/var0=0 -&gt; 1) because the planner only
 * matches source nodes whose declared variables are equal, so persisted reward saves would otherwise
 * match no reward route at all.
 */
class RetailSingleStepRewardRowContractTest {

	private record Contract(int questId, String group) {
	}

	private static final int REWARD_ROW = 1;
	private static final int STALE_ROW = 0;

	/* A 组：末行完全没有 START/REWARD 状态（审计 MISSING_LAST_ROW）。 */
	/* Group A: the last journal row had no START/REWARD state at all (audit: MISSING_LAST_ROW). */
	private static final List<Contract> GROUP_A = List.of(
		new Contract(1527, "A"),
		new Contract(1528, "A"),
		new Contract(1725, "A"),
		new Contract(2135, "A"),
		new Contract(2247, "A"),
		new Contract(2266, "A"),
		new Contract(3087, "A"),
		new Contract(4020, "A"),
		new Contract(21455, "A"),
		new Contract(26838, "A"),
		new Contract(80735, "A"),
		new Contract(80736, "A")
	);

	/* B 组：末行已有中间行状态（var0=1），但 reward 投影仍停在行 0。 */
	/* Group B: the last row already had an intermediate state (var0=1) while reward still projected row 0. */
	private static final List<Contract> GROUP_B = List.of(
		new Contract(1963, "B"),
		new Contract(1964, "B"),
		new Contract(16838, "B"),
		new Contract(16977, "B"),
		new Contract(18035, "B")
	);

	/* C 组：镜像 19002 已对齐的单侧缺陷，旧 handler 直接给出“领奖时 var0=1”的语义证据。 */
	/* Group C: single-sided defect whose mirror 19002 is aligned, evidenced by the legacy handler. */
	private static final List<Contract> GROUP_C = List.of(
		new Contract(29002, "C")
	);

	private static final List<Contract> CONTRACTS = Stream.of(GROUP_A, GROUP_B, GROUP_C)
		.flatMap(List::stream)
		.toList();

	/* 已对齐的同族参照（retail 单步任务，修复前就已是 reward var0=1）。 */
	/* Aligned siblings of the same retail single-step family (already var0=1 before this batch). */
	private static final List<Integer> ALIGNED_SIBLINGS = List.of(1526, 21458, 11455);

	@Test
	void rewardRowEqualsTheClientJournalLastRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(REWARD_ROW, rewardRow(definition),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(Map.of("var0", STALE_ROW), node(definition, "started").projection().variables(),
				() -> "quest " + contract.questId() + " start state keeps journal row 0");
		}
	}

	@Test
	void alignedSiblingsOfTheSameFamilyShareTheRewardRow() throws Exception {
		for (int questId : ALIGNED_SIBLINGS) {
			assertEquals(REWARD_ROW, rewardRow(definition(questId).definition()),
				() -> "aligned sibling " + questId + " reward journal row");
		}
		/* C 组的镜像必须与本侧同值。 / The mirror of the group C quest must share its reward row. */
		assertEquals(REWARD_ROW, rewardRow(definition(19002).definition()),
			"mirror 19002 reward journal row");
	}

	@Test
	void persistedRewardRowsAreRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", STALE_ROW)), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", REWARD_ROW)),
				recovery.actions(), () -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", STALE_ROW)), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(REWARD_ROW, unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(REWARD_ROW, value, () -> "quest " + contract.questId()
							+ " route writes a non reward journal row");
					}
				}
			}
		}
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = node(definition, "reward");
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables().get("var0");
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " reward recovery route");
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = RetailSingleStepRewardRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
