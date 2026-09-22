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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定第三批领奖行修复：203 个任务的客户端 quest_summary 末行是领奖行
 * （“和 X 对话 / 向 X 报告 / 把 X 交给 Y”，且该 NPC 必须是任务内 reward 路线的 NPC），
 * 而服务端 reward 投影停在更早的行号；修复把投影推到末行并补无 source 的 enter-world 自愈边。
 * Locks the third repair batch: 203 quests whose client quest_summary last row is the reward row
 * (a talk/report/hand-over line naming the reward NPC of the quest) while the server reward projection
 * stayed on an earlier row; the projection was rewritten to the last row and each quest gained a
 * source-less enter-world recovery edge.
 */
class JournalRewardRowRepairContractTest {

	private record Contract(int questId, int rewardRow, int staleRow) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(1218, 1, 0),
		new Contract(1319, 8, 0),
		new Contract(1322, 1, 0),
		new Contract(1324, 1, 0),
		new Contract(1361, 2, 1),
		new Contract(1363, 1, 0),
		new Contract(1430, 1, 0),
		new Contract(1452, 1, 0),
		new Contract(1464, 1, 0),
		new Contract(1469, 1, 0),
		new Contract(1472, 1, 0),
		new Contract(1483, 2, 0),
		new Contract(1484, 3, 0),
		new Contract(1540, 3, 0),
		new Contract(1553, 2, 0),
		new Contract(1574, 3, 0),
		new Contract(1604, 1, 0),
		new Contract(1614, 3, 2),
		new Contract(1626, 7, 6),
		new Contract(1636, 4, 3),
		new Contract(1647, 1, 0),
		new Contract(1900, 4, 0),
		new Contract(1918, 1, 0),
		new Contract(1921, 4, 3),
		new Contract(1937, 3, 0),
		new Contract(1988, 3, 2),
		new Contract(2122, 2, 1),
		new Contract(2208, 2, 1),
		new Contract(2232, 1, 0),
		new Contract(2271, 1, 0),
		new Contract(2278, 3, 0),
		new Contract(2279, 2, 0),
		new Contract(2284, 3, 2),
		new Contract(2333, 3, 2),
		new Contract(2394, 2, 1),
		new Contract(2423, 3, 0),
		new Contract(2428, 2, 0),
		new Contract(2436, 2, 1),
		new Contract(2443, 1, 0),
		new Contract(2449, 1, 0),
		new Contract(2458, 1, 0),
		new Contract(2480, 2, 0),
		new Contract(2493, 1, 0),
		new Contract(2505, 1, 0),
		new Contract(2512, 1, 0),
		new Contract(2513, 1, 0),
		new Contract(2515, 3, 0),
		new Contract(2523, 3, 0),
		new Contract(2538, 3, 0),
		new Contract(2542, 1, 0),
		new Contract(2564, 3, 0),
		new Contract(2620, 2, 1),
		new Contract(2634, 2, 1),
		new Contract(2663, 1, 0),
		new Contract(2664, 1, 0),
		new Contract(2669, 2, 1),
		new Contract(2912, 3, 0),
		new Contract(2913, 2, 0),
		new Contract(2914, 1, 0),
		new Contract(2917, 2, 0),
		new Contract(2928, 1, 0),
		new Contract(2946, 4, 3),
		new Contract(2954, 1, 0),
		new Contract(2988, 3, 2),
		new Contract(3036, 1, 0),
		new Contract(3037, 1, 0),
		new Contract(3041, 1, 0),
		new Contract(3056, 2, 1),
		new Contract(3057, 1, 0),
		new Contract(3082, 3, 2),
		new Contract(3085, 1, 0),
		new Contract(3086, 1, 0),
		new Contract(3093, 3, 0),
		new Contract(3200, 4, 3),
		new Contract(3211, 1, 0),
		new Contract(3319, 1, 0),
		new Contract(3547, 1, 0),
		new Contract(3721, 3, 2),
		new Contract(3920, 1, 0),
		new Contract(3965, 1, 0),
		new Contract(3967, 1, 0),
		new Contract(3969, 1, 0),
		new Contract(3970, 3, 0),
		new Contract(3973, 3, 0),
		new Contract(4004, 1, 0),
		new Contract(4012, 1, 0),
		new Contract(4015, 1, 0),
		new Contract(4038, 3, 0),
		new Contract(4052, 3, 0),
		new Contract(4077, 1, 0),
		new Contract(4210, 2, 1),
		new Contract(4502, 3, 2),
		new Contract(4721, 3, 2),
		new Contract(4905, 3, 0),
		new Contract(4906, 3, 0),
		new Contract(4920, 1, 0),
		new Contract(4937, 7, 0),
		new Contract(4938, 9, 0),
		new Contract(4939, 5, 0),
		new Contract(4940, 1, 0),
		new Contract(4941, 1, 0),
		new Contract(4943, 4, 0),
		new Contract(11001, 3, 0),
		new Contract(11006, 3, 2),
		new Contract(11008, 1, 0),
		new Contract(11009, 3, 0),
		new Contract(11026, 1, 0),
		new Contract(10530, 9, 8),
		new Contract(11068, 1, 0),
		new Contract(11069, 1, 0),
		new Contract(11070, 3, 0),
		new Contract(11076, 4, 3),
		new Contract(11103, 2, 0),
		new Contract(11105, 2, 0),
		new Contract(11106, 2, 0),
		new Contract(11107, 3, 0),
		new Contract(11116, 3, 0),
		new Contract(11117, 2, 0),
		new Contract(11460, 1, 0),
		new Contract(13809, 3, 0),
		new Contract(14046, 7, 6),
		new Contract(14051, 4, 3),
		new Contract(14121, 1, 0),
		new Contract(14122, 2, 0),
		new Contract(14153, 6, 5),
		new Contract(15011, 1, 0),
		new Contract(15012, 1, 0),
		new Contract(15021, 1, 0),
		new Contract(15022, 1, 0),
		new Contract(15043, 1, 0),
		new Contract(15044, 1, 0),
		new Contract(15053, 1, 0),
		new Contract(15066, 1, 0),
		new Contract(15071, 1, 0),
		new Contract(15072, 1, 0),
		new Contract(15102, 1, 0),
		new Contract(15103, 1, 0),
		new Contract(15230, 1, 0),
		new Contract(15231, 1, 0),
		new Contract(15232, 1, 0),
		new Contract(15613, 6, 5),
		new Contract(16900, 2, 1),
		new Contract(16901, 2, 1),
		new Contract(16902, 2, 1),
		new Contract(16903, 2, 1),
		new Contract(18809, 2, 0),
		new Contract(19002, 1, 0),
		new Contract(20530, 9, 8),
		new Contract(21033, 1, 0),
		new Contract(21036, 2, 0),
		new Contract(21070, 1, 0),
		new Contract(21073, 1, 0),
		new Contract(21105, 1, 0),
		new Contract(21114, 5, 4),
		new Contract(21296, 1, 0),
		new Contract(21460, 1, 0),
		new Contract(24021, 5, 4),
		new Contract(24022, 8, 7),
		new Contract(24023, 4, 3),
		new Contract(24024, 5, 4),
		new Contract(24025, 4, 3),
		new Contract(24030, 9, 8),
		new Contract(24046, 7, 6),
		new Contract(24051, 6, 5),
		new Contract(24121, 2, 0),
		new Contract(24201, 2, 1),
		new Contract(24202, 2, 1),
		new Contract(24242, 2, 0),
		new Contract(25000, 3, 2),
		new Contract(25023, 3, 2),
		new Contract(25606, 8, 7),
		new Contract(26977, 1, 0),
		new Contract(29000, 1, 0),
		new Contract(30007, 1, 0),
		new Contract(30055, 1, 0),
		new Contract(30111, 2, 1),
		new Contract(30202, 2, 0),
		new Contract(30210, 1, 0),
		new Contract(30217, 3, 2),
		new Contract(30227, 3, 2),
		new Contract(30302, 2, 0),
		new Contract(30308, 1, 0),
		new Contract(30310, 1, 0),
		new Contract(30317, 3, 2),
		new Contract(30327, 3, 2),
		new Contract(30503, 1, 0),
		new Contract(30553, 1, 0),
		new Contract(30604, 1, 0),
		new Contract(50126, 1, 0),
		new Contract(50127, 1, 0),
		new Contract(51126, 1, 0),
		new Contract(51127, 1, 0),
		new Contract(80020, 3, 2),
		new Contract(80021, 3, 2),
		new Contract(80257, 1, 0),
		new Contract(80258, 1, 0),
		new Contract(80259, 1, 0),
		new Contract(80260, 1, 0),
		new Contract(80261, 1, 0),
		new Contract(80262, 1, 0),
		new Contract(80263, 1, 0),
		new Contract(80264, 1, 0),
		new Contract(80265, 1, 0),
		new Contract(80266, 1, 0)
	);

	@Test
	void rewardRowEqualsTheClientJournalLastRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(contract.rewardRow(), rewardRow(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward journal row");
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
		try (InputStream input = JournalRewardRowRepairContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
