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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定第二批领奖行修复：天/魔镜像“两侧都缺最后一行”的 118 个任务（59 对）；
 * 批次 5 追加 15550/25550 这一对（同一合同的“三段对话链”变体，领奖行从 1 推到 2）。
 * 客户端 quest_summary 的末行是领奖行（“和 X 对话 / 向 X 报告”，X 必须是任务内出场 NPC），
 * 旧投影停在倒数第二行；修复把 reward 投影推到末行，并补无 source 的 enter-world 自愈边。
 * Locks the second repair batch: 118 quests (59 Elyos/Asmodian pairs) whose journal was
 * missing its last row on both sides. The client quest_summary last row is the reward row (a talk/report
 * line naming an NPC of the quest); the projection was rewritten to it and each quest gained a
 * source-less enter-world recovery edge.
 */
class MirrorPairRewardRowContractTest {

	private record Contract(int questId, int rewardRow, int staleRow) {
	}

	/**
	 * P0c-6 起由真端 SimpleHunt 表驱动的镜像对（16960/26960）：var0 变成击杀计数（真端 count1=1），
	 * 任务书行由客户端 {@code SECTION_0} 门控推导，因此没有"存档行号"可漂移，也不得保留修复边
	 * （P3 既有裁定：修复边是 AionEmu 历史包袱）。
	 * Retail-driven mirror pair since P0c-6: var0 is a kill counter and the journal row is derived from
	 * the client SECTION gate, so no stored row can go stale and no repair edge may remain.
	 */
	private static final List<Integer> RETAIL_DRIVEN = List.of(16960, 26960);

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(10110, 6, 5),
		new Contract(11323, 5, 4),
		new Contract(11458, 1, 0),
		new Contract(13700, 1, 0),
		new Contract(13701, 1, 0),
		new Contract(13961, 1, 0),
		new Contract(13962, 1, 0),
		new Contract(13968, 1, 0),
		new Contract(14026, 5, 4),
		new Contract(14031, 12, 11),
		new Contract(14052, 5, 4),
		new Contract(15052, 1, 0),
		new Contract(15323, 1, 0),
		new Contract(15335, 1, 0),
		new Contract(15401, 1, 0),
		new Contract(15402, 3, 2),
		new Contract(15403, 1, 0),
		new Contract(15404, 1, 0),
		new Contract(15405, 1, 0),
		new Contract(15502, 1, 0),
		new Contract(15505, 1, 0),
		new Contract(15508, 1, 0),
		new Contract(15511, 1, 0),
		new Contract(15517, 1, 0),
		new Contract(15523, 1, 0),
		new Contract(15526, 1, 0),
		new Contract(15532, 1, 0),
		new Contract(15535, 1, 0),
		new Contract(15538, 1, 0),
		new Contract(15540, 1, 0),
		new Contract(15541, 1, 0),
		new Contract(15550, 2, 1),
		new Contract(25550, 2, 1),
		new Contract(15665, 1, 0),
		new Contract(15666, 1, 0),
		new Contract(15689, 1, 0),
		new Contract(15690, 1, 0),
		new Contract(15691, 1, 0),
		new Contract(16990, 1, 0),
		new Contract(17540, 5, 4),
		new Contract(18210, 1, 0),
		new Contract(18252, 1, 0),
		new Contract(18253, 1, 0),
		new Contract(18602, 4, 3),
		new Contract(18742, 1, 0),
		new Contract(18745, 1, 0),
		new Contract(18806, 1, 0),
		new Contract(18807, 1, 0),
		new Contract(18830, 2, 1),
		new Contract(18940, 1, 0),
		new Contract(18953, 1, 0),
		new Contract(18970, 1, 0),
		new Contract(18975, 1, 0),
		new Contract(18976, 1, 0),
		new Contract(18977, 1, 0),
		new Contract(18978, 1, 0),
		new Contract(19010, 1, 0),
		new Contract(19016, 1, 0),
		new Contract(19022, 1, 0),
		new Contract(19028, 1, 0),
		new Contract(19034, 1, 0),
		new Contract(20110, 6, 5),
		new Contract(21323, 5, 4),
		new Contract(21458, 1, 0),
		new Contract(23700, 1, 0),
		new Contract(23701, 1, 0),
		new Contract(23961, 1, 0),
		new Contract(23962, 1, 0),
		new Contract(23968, 1, 0),
		new Contract(24026, 5, 4),
		new Contract(24031, 12, 11),
		new Contract(24052, 5, 4),
		new Contract(25052, 1, 0),
		new Contract(25323, 1, 0),
		new Contract(25335, 1, 0),
		new Contract(25401, 1, 0),
		new Contract(25402, 3, 2),
		new Contract(25403, 1, 0),
		new Contract(25404, 1, 0),
		new Contract(25405, 1, 0),
		new Contract(25502, 1, 0),
		new Contract(25505, 1, 0),
		new Contract(25508, 1, 0),
		new Contract(25511, 1, 0),
		new Contract(25517, 1, 0),
		new Contract(25523, 1, 0),
		new Contract(25526, 1, 0),
		new Contract(25532, 1, 0),
		new Contract(25535, 1, 0),
		new Contract(25538, 1, 0),
		new Contract(25540, 1, 0),
		new Contract(25541, 1, 0),
		new Contract(25665, 1, 0),
		new Contract(25666, 1, 0),
		new Contract(25689, 1, 0),
		new Contract(25690, 1, 0),
		new Contract(25691, 1, 0),
		new Contract(26990, 1, 0),
		new Contract(27540, 5, 4),
		new Contract(28210, 1, 0),
		new Contract(28252, 1, 0),
		new Contract(28253, 1, 0),
		new Contract(28602, 4, 3),
		new Contract(28742, 1, 0),
		new Contract(28745, 1, 0),
		new Contract(28806, 1, 0),
		new Contract(28807, 1, 0),
		new Contract(28830, 2, 1),
		new Contract(28940, 1, 0),
		new Contract(28953, 1, 0),
		new Contract(28970, 1, 0),
		new Contract(28975, 1, 0),
		new Contract(28976, 1, 0),
		new Contract(28977, 1, 0),
		new Contract(28978, 1, 0),
		new Contract(29010, 1, 0),
		new Contract(29016, 1, 0),
		new Contract(29022, 1, 0),
		new Contract(29028, 1, 0),
		new Contract(29034, 1, 0)
	);

	@Test
	void rewardRowEqualsTheClientJournalLastRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(contract.rewardRow(), rewardRow(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward journal row");
		}
	}

	@Test
	void mirrorPairsAgreeOnTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (contract.questId() >= 20000) {
				continue;
			}
			int mirrorId = contract.questId() + 10000;
			assertEquals(contract.rewardRow(), rewardRow(definition(mirrorId).definition()),
				() -> "mirror " + mirrorId + " must share quest " + contract.questId() + " reward row");
		}
	}

	@Test
	void persistedRewardRowsAreRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", contract.staleRow())), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())),
				recovery.actions(), () -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, Map.of("var0", contract.staleRow()), Map.of()),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
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
						assertEquals(contract.rewardRow(), value, () -> "quest " + contract.questId()
							+ " route writes a non reward journal row");
					}
				}
			}
		}
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label())).findFirst().orElseThrow();
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

	/** 真端驱动的镜像对：不得保留修复边，领奖投影是饱和计数。 / Retail-driven pair: no repair edge. */
	@Test
	void retailDrivenMirrorPairsCarryNoRewardRepairEdge() throws Exception {
		for (int questId : RETAIL_DRIVEN) {
			QuestDefinition definition = definition(questId).definition();
			assertTrue(definition.transitions().stream().noneMatch(route ->
					route.sourceNode() == null && route.event() instanceof QuestEvent.EnterWorld),
				() -> "quest " + questId + " is retail-driven and must not keep a source-less enter-world edge");
			QuestNode reward = definition.nodes().stream()
				.filter(node -> node.projection().status() == QuestStatus.REWARD)
				.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " has no reward node"));
			assertTrue(reward.projection().variables().values().stream().anyMatch(value -> value > 0),
				() -> "quest " + questId + " reward must project the saturated kill counters");
		}
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.REWARD,
			definition.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId);
	}
}
