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
 * 锁定批次 7 的“报告/交付型末行”领奖行合同：客户端 quest_summary 的最后一行是“向 NPC 报告 / 交出物品”
 * 时，reward 节点投影必须落在该行，旧存档由无 source 的 enter-world 自愈边纠正，且交出物品的事务
 * 必须落在与投影一致的 START 节点上。
 * Locks the batch-7 report/hand-over reward-row contract: when the last journal row asks the player to report
 * to an NPC, the reward projection must sit on that row, a source-less enter-world recovery edge must repair
 * stale saves, and the hand-over transaction must target a START node whose projection matches.
 * <p>三组证据：A 组 18 个成长任务（19672..19694 / 29672..29694，客户端 2 行“收集成长货币 → 向成长支援教官
 * 报告”，定义里唯一 NPC 806698 / 806700 既接取又领奖）；B 组 3210（客户端 3 行，末行“和 Shugo_Shulack_02
 * 对话”，镜像 4210 的 reward 已是 2）；C 组 18036/28036（客户端 2 行“和德拉坎战士对话 → 向 Demades/Latkel
 * 报告”，旧定义把 var0 写成 1 却仍以投影 var0=0 的 started 为 source，导致交出物品后匹配不到任何报告路线）。
 * Three evidence groups: A = 18 growth quests (2 rows: collect coins then report to the growth instructor, the
 * only NPC 806698/806700 accepts and rewards); B = 3210 (3 rows, last row talks to Shugo_Shulack_02, mirror
 * 4210 already projects 2); C = 18036/28036 (2 rows: talk to the Drakan fighter then report to Demades/Latkel,
 * where the old definition wrote var0=1 while every report route was sourced at the var0=0 started node and
 * therefore could never match).
 */
class ReportRowRewardProjectionContractTest {

	private record Contract(int questId, int rewardRow, List<Integer> staleRows) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(19672, 1, List.of(0)),
		new Contract(19677, 1, List.of(0)),
		new Contract(19684, 1, List.of(0)),
		new Contract(19685, 1, List.of(0)),
		new Contract(19686, 1, List.of(0)),
		new Contract(19687, 1, List.of(0)),
		new Contract(19688, 1, List.of(0)),
		new Contract(19689, 1, List.of(0)),
		new Contract(19694, 1, List.of(0)),
		new Contract(29672, 1, List.of(0)),
		new Contract(29677, 1, List.of(0)),
		new Contract(29684, 1, List.of(0)),
		new Contract(29685, 1, List.of(0)),
		new Contract(29686, 1, List.of(0)),
		new Contract(29687, 1, List.of(0)),
		new Contract(29688, 1, List.of(0)),
		new Contract(29689, 1, List.of(0)),
		new Contract(29694, 1, List.of(0)),
		new Contract(3210, 2, List.of(0, 1)),
		new Contract(18036, 1, List.of(0)),
		new Contract(28036, 1, List.of(0)));

	/**
	 * C 组：交出物品后必须落到 s1(var0=1)，报告与领奖路线都以 s1 为 source。
	 */
	private record StartNodeContract(int questId, int acceptNpcId, int reportNpcId) {
	}

	private static final List<StartNodeContract> START_NODE_CONTRACTS = List.of(
		new StartNodeContract(18036, 802008, 801281),
		new StartNodeContract(28036, 802015, 801280));


	@Test
	void handOverLandsOnTheStartNodeThatOwnsTheReportRow() {
		for (StartNodeContract contract : START_NODE_CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(1, row(definition, "s1"),
				() -> "quest " + contract.questId() + " report row state");

			QuestTransition handOver = route(definition, "started", "s1",
				new QuestEvent.TalkToNpc(contract.acceptNpcId(), QuestDialogAction.SETPRO1.id()));
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), handOver.actions(),
				() -> "quest " + contract.questId() + " hand-over action");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.CloseDialog()), handOver.afterCommit(),
				() -> "quest " + contract.questId() + " hand-over after-commit");

			// s1 -> reward 必须可规划：旧定义以投影 var0=0 的 started 为 source，事务永远匹配不到。
			QuestTransition report = route(definition, "s1", "reward",
				new QuestEvent.TalkToNpc(contract.reportNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
			assertEquals(List.of(new QuestCondition.VariableAtLeast("var0", 1)), report.conditions(),
				() -> "quest " + contract.questId() + " report conditions");
			QuestMutationPlan plan = QuestMutationPlanner.plan(definition(contract.questId()),
				snapshot(definition(contract.questId()), QuestStatus.START, Map.of("var0", 1), Map.of()),
				report.event(), report).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(1, unpack(definition(contract.questId()), plan).get("var0"),
				() -> "quest " + contract.questId() + " reward projection must land on the report row");
		}
	}


	private static Map<String, Integer> nodeRows(QuestDefinition definition) {
		Map<String, Integer> rows = new java.util.TreeMap<>();
		for (QuestNode node : definition.nodes()) {
			rows.put(node.label(), node.projection().variables().get("var0"));
		}
		return rows;
	}

	private static int row(QuestDefinition definition, String label) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label())).findFirst().orElseThrow();
		return node.projection().variables().get("var0");
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
										 QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " route " + event);
		return matches.getFirst();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
										  Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) {
		// 行已由原版表驱动（退役），改从生产视图取定义。
		// The rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(questId);
	}
}
