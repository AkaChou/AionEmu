package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 18：镜像单侧投影落后族（11110/14201/16974/17160/17161/17526）。
 * <p>
 * 族级口径（批次 1-3 的镜像方法）：同形镜像对（q ↔ q±10000）客户端 quest_summary 行数相同、末行 NPC 都能在
 * 任务内对上，但只有一侧的 reward 投影等于领奖行——已对齐的那一侧就是另一侧的参照基线。本批 6 个任务都已具备
 * 完整的 0..N-1 行状态（只是 reward 节点仍停在行 0），因此修复 = 投影 → 镜像值 + `REWARD/var0=0 -> 镜像值`
 * 自愈边；14201 另有 legacy `changeQuestStep(2->2)` 独立确认领奖态保持 packed step 2。
 * <p>
 * 同族但锁定、本批不改：16837/16986/16988 被 {@link QuestPrematureRewardRouteExclusionTest} 断言
 * “完成报告后 packed var0 保持 0”（RewardCase.reward = {var0: 0}），属既有基线，需客户端观测才能重定。
 * <p>
 * <p>
 * P0c-58（2026-09-26）：17160/17161 及其镜像 27160/27161 已由原版挑战任务哨兵轴驱动，行锁对其不再适用
 * （成员与判据见 {@link #RETAIL_DRIVEN_RETAIL}）。
 * <p>
 * P0c-58: 17160/17161 and their mirrors 27160/27161 are retail-driven by the challenge-sentinel axis now, so
 * the row lock no longer applies to them (see {@link #RETAIL_DRIVEN_RETAIL}).
 * <p>
 * Locks batch 18: mirror pairs where only one shard had been migrated to the reward journal row. The
 * aligned shard is the reference; 16837/16986/16988 keep their gated packed step 0.
 */
class MirrorRewardProjectionLagContractTest {

	/** 落后一侧 / 镜像参照 / 镜像的领奖行。 / The lagging shard, its aligned mirror, and the reference row. */
	private record Contract(int questId, int mirrorId, int rewardRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(11110, 21110, 1),
		new Contract(16974, 26974, 1),
		new Contract(17160, 27160, 1),
		new Contract(17161, 27161, 1),
		new Contract(17526, 27526, 1)
	);

	/**
	 * P0c-6 起 24201 由原版 SimpleHunt 表驱动：它的 var0 是**击杀计数**（原版 count1=12），不是任务书行号——
	 * 行由客户端 {@code SECTION_0} 门控推导，因此镜像行锁（14201 的 XML 行 2）对它不再适用，
	 * 也不能再有任何无 source 的修复边（P3 既有裁定）。14201 仍是 XML，两者的 row/计数口径本就不同。
	 * Since P0c-6 24201 is retail-driven: var0 is the kill counter (count1=12), so the mirror-row lock does
	 * not apply and no source-less repair edge may remain.
	 */
	private static final List<Integer> RETAIL_DRIVEN_RETAIL = List.of(24201, 11110, 17526, 17160, 17161, 27160, 27161);
	/* P5-1（2026-09-25）追加 17526：DataDriven 击杀网格（a0/a1，var0 = 计数），其镜像 27526 同为网格——
	   两侧都是击杀计数口径，镜像行锁与无 source 修复边不再适用（与 11110/24201 同一裁定）。 */
	/* P5-1 adds 17526: grid-driven (a0/a1, var0 = kills) and its mirror 27526 is grid too — the
	   mirror-row lock and source-less heal edge no longer apply (same ruling as 11110/24201). */
	/* P0c-8c（2026-09-24）追加 11110：SimpleHunt 10 段网格原版驱动，reward 节点的 var0 = 饱和计数 10
	   （客户端行由 SECTION_0 门控推导），其镜像 21110 仍是 SimpleTalk 侧 XML（SEMANTIC_GAP:RETAIL_TALK_CHAIN），
	   故两侧 row/计数口径不再可比——与 24201/14201 同一裁定。 */
	/* P0c-58（2026-09-26）追加 17160/17161/27160/27161：挑战任务哨兵轴采纳（`_challengetask_` 回退到原版
	   reward 名 804699/804719，接取人 = 交付人；两侧都是原版单段击杀网格 a0..a10，var0 = 击杀计数，饱和段
	   自身即报告门控）⇒ 领奖投影不再是镜像行号 1，镜像行锁与无 source 修复边都不再适用（与
	   24201/11110/17526 同一裁定）。 */
	/* P0c-58 adds 17160/17161/27160/27161: the challenge sentinel axis landed (the `_challengetask_` value
	   falls back to the retail reward name 804699/804719, accept npc == hand-in npc; both sides are single-stage
	   retail kill grids a0..a10 with var0 = the kill counter and the saturated segment gating reporting), so
	   the reward projection is no longer the mirror row — neither the mirror row lock nor a source-less heal
	   edge applies (same ruling as 24201/11110/17526). */

	/** 原版驱动且其镜像仍是 XML 的任务：镜像行锁不适用（见 RETAIL_DRIVEN_RETAIL 注释）。 */
	/** Retail-driven shards whose mirror is still XML: the mirror row lock does not apply. */

	private static final int STALE_ROW = 0;

	/* 同族但被既有门禁锁定 packed step 的任务：不得按镜像推进。 */
	/* Same family but gated by QuestPrematureRewardRouteExclusionTest: keep packed step 0. */
	private static final List<Integer> LOCKED_MIRROR_SIBLINGS = List.of(16837, 16986, 16988);

	@Test
	void laggingShardsProjectTheAlignedMirrorRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (RETAIL_DRIVEN_RETAIL.contains(contract.questId())) {
				continue;  // 原版驱动的分片不再承担镜像行锁（见 RETAIL_DRIVEN_RETAIL 注释）。
			}
			int mirrorRow = node(definition(contract.mirrorId()).definition(), "reward")
				.projection().variables().get("var0");
			assertEquals(contract.rewardRow(), mirrorRow,
				() -> "mirror " + contract.mirrorId() + " reward row reference");
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(Map.of("var0", mirrorRow), node(definition, "reward").projection().variables(),
				() -> "quest " + contract.questId() + " must project its aligned mirror's reward row");
			assertEquals(QuestStatus.REWARD, node(definition, "reward").projection().status(),
				() -> "quest " + contract.questId() + " reward node status");
			assertEquals(Map.of("var0", STALE_ROW), node(definition, "started").projection().variables(),
				() -> "quest " + contract.questId() + " start state stays on row 0");
		}
	}

	@Test
	void staleRewardRowsAreHealedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (RETAIL_DRIVEN_RETAIL.contains(contract.questId())) {
				continue;  // 原版驱动的分片不得保留修复边（由 retailDrivenShardsProjectTheirSaturatedCounters 锁定）。
			}
			CompiledQuestDefinition compiled = definition(contract.questId());
			List<QuestTransition> matches = enterWorldRecoveries(compiled.definition()).stream()
				.filter(route -> route.conditions().equals(List.of(
					new QuestCondition.StatusIs(QuestStatus.REWARD),
					new QuestCondition.QuestVariableIs("var0", STALE_ROW))))
				.toList();
			assertEquals(1, matches.size(),
				() -> "quest " + contract.questId() + " heal route for stale row " + STALE_ROW);
			QuestTransition heal = matches.getFirst();
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())), heal.actions(),
				() -> "quest " + contract.questId() + " heal actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
					QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), heal.afterCommit(),
				() -> "quest " + contract.questId() + " heal after-commit");
			assertNull(heal.priority(), () -> "quest " + contract.questId() + " heal priority");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", STALE_ROW)), heal).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " healed status");
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " healed journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			List<QuestTransition> rewardRoutes = definition(contract.questId()).definition().transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(contract.rewardRow(), value, () -> "quest " + contract.questId()
							+ " reward route writes a non reward journal row");
					}
				}
			}
		}
	}

	@Test
	void lockedMirrorSiblingsKeepTheirPackedStep() throws Exception {
		/* P5-1：16837/16986/16988 已由原版击杀网格驱动（var0 = 击杀计数），领奖投影 = 饱和计数
		   （1/1/5），与 QuestPrematureRewardRouteExclusionTest 的 grid 合同 reward 字段一致锁定；
		   旧的 {var0:0} 行号锁随双变量形一并退役。
		   Grid since P5-1: the reward projection is the saturated count (1/1/5), locked in step with
		   the exclusion test's grid contracts; the legacy {var0:0} row lock retired with the two-var
		   shape. */
		Map<Integer, Integer> saturated = Map.of(16837, 1, 16986, 1, 16988, 5);
		for (int questId : LOCKED_MIRROR_SIBLINGS) {
			assertEquals(Map.of("var0", saturated.get(questId)),
				node(definition(questId).definition(), "reward").projection().variables(),
				() -> "quest " + questId + " keeps its grid saturated reward projection");
		}
	}

	@Test
	void bothShardsKeepTheSameRewardJournalRowPerPair() throws Exception {
		/* 只比较任务书行号（var0）：镜像两侧可以有不同的计数槽（例如 27160 另有 var1=10）。 */
		/* Compare the journal row only: mirrors may carry different counter slots (e.g. 27160 has var1=10). */
		for (Contract contract : CONTRACTS) {
			if (RETAIL_DRIVEN_RETAIL.contains(contract.questId())) {
				continue;
			}
			assertEquals(
				node(definition(contract.mirrorId()).definition(), "reward").projection().variables().get("var0"),
				node(definition(contract.questId()).definition(), "reward").projection().variables().get("var0"),
				() -> "mirror pair " + contract.questId() + "/" + contract.mirrorId()
					+ " must agree on the reward journal row");
		}
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
	}

	private static List<QuestTransition> enterWorldRecoveries(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
	}

	/**
	 * 原版驱动的一侧：领奖投影必须是饱和计数（客户端行由 SECTION 门控推导），且不得有修复边。
	 * The retail-driven side projects its saturated counters and carries no repair edge.
	 */
	@Test
	void retailDrivenShardsProjectTheirSaturatedCounters() throws Exception {
		for (int questId : RETAIL_DRIVEN_RETAIL) {
			QuestDefinition definition = definition(questId).definition();
			assertTrue(enterWorldRecoveries(definition).isEmpty(),
				() -> "quest " + questId + " is retail-driven and must not keep a reward heal edge");
			QuestNode reward = node(definition, "reward");
			assertEquals(QuestStatus.REWARD, reward.projection().status());
			assertTrue(reward.projection().variables().values().stream().anyMatch(value -> value > 0),
				() -> "quest " + questId + " reward must project the saturated kill counters");
		}
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
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
