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
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 锁定批次 5 的“感应区坐骑行推进”合同：15551-15554（天族）与 25551-25554（魔族）的客户端
 * quest_summary 都是 3 行——“使用 A→X 吸引物”“使用 X→A 吸引物”“和领奖 NPC 对话”。
 * 旧 handler（`_15551Giddyup_Starturtle`、`_25551Springleaf_Shortcut` 一类）用两条感应区路线推进行号：
 * A_TO_X 感应区把 var0 0 -&gt; 1，X_TO_A 感应区把 var0 1 -&gt; 2 并置 REWARD。迁移提交 79bc5d3a6 只保留了
 * “进入感应区自动接取（unaccepted -&gt; started）”，把行推进整条丢掉，于是第 2 行没有任何 START 状态、
 * reward 投影停在倒数第二行（审计 verdict：MISSING_LAST_ROW + ROW_WITHOUT_STATE）。
 * 本测试锁定：每行都有状态且位段容得下、两条自动接取仍在、两条感应区推进的条件/动作/after-commit、
 * 进入 REWARD 的路线只写领奖行、旧存档 enter-world 自愈边，以及天/魔镜像同形。
 * Locks the batch-5 sensory-area ride row contract for 15551-15554 and their Asmodian mirrors
 * 25551-25554: the client journal has three rows (ride A→X, ride X→A, talk to the reward NPC) while the
 * legacy handlers advanced var0 0 -&gt; 1 on the A_TO_X sensor and 1 -&gt; 2 with REWARD on the X_TO_A sensor.
 * The migration kept only the zone auto-accept, so row 2 had no state and the reward projection stayed on
 * the second-to-last row. The test pins every row state and its field capacity, both auto-accept routes,
 * both sensor advances (conditions, actions, after-commit), the reward-row writes, the source-less
 * enter-world repair edge, and Elyos/Asmodian mirror parity.
 */
class SensoryAreaRideRowContractTest {

	private record Contract(int questId, int rewardNpcId, String zoneIn, String zoneBack) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(15551, 806089,
			"LF6_SENSORY_AREA_Q15551_A_TO_B_210100000", "LF6_SENSORY_AREA_Q15551_B_TO_A_210100000"),
		new Contract(15552, 806089,
			"LF6_SENSORY_AREA_Q15552_A_TO_D_210100000", "LF6_SENSORY_AREA_Q15552_D_TO_A_210100000"),
		new Contract(15553, 806089,
			"LF6_SENSORY_AREA_Q15553_A_TO_F_210100000", "LF6_SENSORY_AREA_Q15553_F_TO_A_210100000"),
		new Contract(15554, 806089,
			"LF6_SENSORY_AREA_Q15554_A_TO_H_210100000", "LF6_SENSORY_AREA_Q15554_H_TO_A_210100000"),
		new Contract(25551, 806101,
			"DF6_SENSORY_AREA_Q25551_A_TO_B_220110000", "DF6_SENSORY_AREA_Q25551_B_TO_A_220110000"),
		new Contract(25552, 806101,
			"DF6_SENSORY_AREA_Q25552_A_TO_D_220110000", "DF6_SENSORY_AREA_Q25552_D_TO_A_220110000"),
		new Contract(25553, 806101,
			"DF6_SENSORY_AREA_Q25553_A_TO_F_220110000", "DF6_SENSORY_AREA_Q25553_F_TO_A_220110000"),
		new Contract(25554, 806101,
			"DF6_SENSORY_AREA_Q25554_A_TO_H_220110000", "DF6_SENSORY_AREA_Q25554_H_TO_A_220110000"));

	@Test
	void everyJournalRowHasItsOwnStateAndFieldRoom() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			assertEquals(0, row(definition, "started"),
				() -> "quest " + contract.questId() + " first ride row");
			assertEquals(1, row(definition, "s1"),
				() -> "quest " + contract.questId() + " second ride row");
			assertEquals(2, row(definition, "reward"),
				() -> "quest " + contract.questId() + " reward row");
			BitField field = definition.progressLayout().field("var0");
			assertEquals(0, field.offset(), () -> "quest " + contract.questId() + " var0 SECTION_0");
			assertEquals(2, field.width(),
				() -> "quest " + contract.questId() + " var0 must hold the third journal row");
			assertEquals(2, field.maxValue(), () -> "quest " + contract.questId() + " var0 max");
		}
	}

	@Test
	void bothSensorsStillAutoAcceptTheQuest() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			for (String zone : List.of(contract.zoneIn(), contract.zoneBack())) {
				QuestTransition accept = route(definition, "unaccepted", "started", new QuestEvent.EnterZone(zone));
				assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions(),
					() -> "quest " + contract.questId() + " auto-accept " + zone);
			}
		}
	}

	@Test
	void sensorRowsAdvanceTheJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			QuestTransition firstRide = route(definition, "started", "s1",
				new QuestEvent.EnterZone(contract.zoneIn()));
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), firstRide.conditions(),
				() -> "quest " + contract.questId() + " A_TO_X conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), firstRide.actions(),
				() -> "quest " + contract.questId() + " A_TO_X actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				firstRide.afterCommit(),
				() -> "quest " + contract.questId() + " A_TO_X after-commit");

			QuestTransition secondRide = route(definition, "s1", "reward",
				new QuestEvent.EnterZone(contract.zoneBack()));
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), secondRide.conditions(),
				() -> "quest " + contract.questId() + " X_TO_A conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), secondRide.actions(),
				() -> "quest " + contract.questId() + " X_TO_A actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), secondRide.afterCommit(),
				() -> "quest " + contract.questId() + " X_TO_A after-commit");
		}
	}

	@Test
	void theTwoRidesAdvanceAndThenCommitTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition firstRide = route(compiled.definition(), "started", "s1",
				new QuestEvent.EnterZone(contract.zoneIn()));
			QuestMutationPlan started = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 0), Map.of()),
				firstRide.event(), firstRide).orElseThrow();
			assertEquals(QuestStatus.START, started.nextStatus(),
				() -> "quest " + contract.questId() + " first ride status");
			assertEquals(1, unpack(compiled, started).get("var0"),
				() -> "quest " + contract.questId() + " first ride row");

			QuestTransition secondRide = route(compiled.definition(), "s1", "reward",
				new QuestEvent.EnterZone(contract.zoneBack()));
			QuestMutationPlan rewarded = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 1), Map.of()),
				secondRide.event(), secondRide).orElseThrow();
			assertEquals(QuestStatus.REWARD, rewarded.nextStatus(),
				() -> "quest " + contract.questId() + " second ride status");
			assertEquals(2, unpack(compiled, rewarded).get("var0"),
				() -> "quest " + contract.questId() + " second ride row");
		}
	}

	@Test
	void persistedRewardRowsAreRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 1)), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), recovery.actions(),
				() -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 1), Map.of()),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(2, unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode())).toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(2, value, () -> "quest " + contract.questId() + " route "
							+ route.sourceNode() + " writes a non reward journal row");
					}
				}
			}
		}
	}

	@Test
	void rewardNpcRoutesStayBoundToTheRewardWindow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			QuestTransition handover = route(definition, "started", "reward",
				new QuestEvent.TalkToNpc(contract.rewardNpcId(), QuestDialogAction.SELECT_QUEST_REWARD.id()));
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), handover.actions(),
				() -> "quest " + contract.questId() + " report hand-over");
			assertEquals(
				List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				handover.afterCommit(),
				() -> "quest " + contract.questId() + " report hand-over after-commit");
		}
	}

	@Test
	void mirrorPairsShareTheSameRowModel() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (contract.questId() >= 20000) {
				continue;
			}
			QuestDefinition elyos = definition(contract.questId()).definition();
			QuestDefinition asmodian = definition(contract.questId() + 10000).definition();
			assertEquals(projections(elyos), projections(asmodian),
				() -> "mirror " + (contract.questId() + 10000) + " node projections");
			assertEquals(zoneRouteShapes(elyos), zoneRouteShapes(asmodian),
				() -> "mirror " + (contract.questId() + 10000) + " enter-zone route shapes");
		}
	}

	private static Map<String, String> projections(QuestDefinition definition) {
		Map<String, String> projections = new TreeMap<>();
		for (QuestNode node : definition.nodes()) {
			projections.put(node.label(),
				node.projection().status() + "@" + node.projection().variables().get("var0"));
		}
		return projections;
	}

	private static List<String> zoneRouteShapes(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.EnterZone)
			.map(candidate -> candidate.sourceNode() + "->" + candidate.targetNode())
			.sorted()
			.toList();
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

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = SensoryAreaRideRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
