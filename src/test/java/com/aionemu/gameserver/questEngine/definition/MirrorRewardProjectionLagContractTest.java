package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
 * Locks batch 18: mirror pairs where only one shard had been migrated to the reward journal row. The
 * aligned shard is the reference; 16837/16986/16988 keep their gated packed step 0.
 */
class MirrorRewardProjectionLagContractTest {

	/** 落后一侧 / 镜像参照 / 镜像的领奖行。 / The lagging shard, its aligned mirror, and the reference row. */
	private record Contract(int questId, int mirrorId, int rewardRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(11110, 21110, 1),
		new Contract(14201, 24201, 2),
		new Contract(16974, 26974, 1),
		new Contract(17160, 27160, 1),
		new Contract(17161, 27161, 1),
		new Contract(17526, 27526, 1)
	);

	private static final int STALE_ROW = 0;

	/* 同族但被既有门禁锁定 packed step 的任务：不得按镜像推进。 */
	/* Same family but gated by QuestPrematureRewardRouteExclusionTest: keep packed step 0. */
	private static final List<Integer> LOCKED_MIRROR_SIBLINGS = List.of(16837, 16986, 16988);

	@Test
	void laggingShardsProjectTheAlignedMirrorRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
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
		/* QuestPrematureRewardRouteExclusionTest 的 RewardCase.reward = {var0: 0}，没有客户端观测不得改。 */
		/* Gated by QuestPrematureRewardRouteExclusionTest (RewardCase.reward = {var0: 0}). */
		for (int questId : LOCKED_MIRROR_SIBLINGS) {
			assertEquals(Map.of("var0", STALE_ROW),
				node(definition(questId).definition(), "reward").projection().variables(),
				() -> "quest " + questId + " keeps its gated packed step");
		}
	}

	@Test
	void bothShardsKeepTheSameRewardJournalRowPerPair() throws Exception {
		/* 只比较任务书行号（var0）：镜像两侧可以有不同的计数槽（例如 27160 另有 var1=10）。 */
		/* Compare the journal row only: mirrors may carry different counter slots (e.g. 27160 has var1=10). */
		for (Contract contract : CONTRACTS) {
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

	private static CompiledQuestDefinition definition(int questId) throws IOException {
		try (InputStream input = MirrorRewardProjectionLagContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input, () -> "missing quest definition " + questId + ".xml");
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
