package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 5 的“感应区坐骑行推进”合同（DataDriven 编译形）：15551-15554（天族）与 25551-25554
 * （魔族）由 Aquaris/Springleaf 系 NPC 对话接取（真端 acquire=Talk 轴权威，遗留的双感应区
 * 自动接取按真端优先退役），两段坐骑感应区边（区名经解析表落登记名）推进行号 0→1→领奖行 2，
 * 末行 REWARD 投影 + enter-world/重谈双自愈边；天魔镜像同构。
 * <p>Locks the batch-5 sensory-area ride row contract in the DataDriven compiled shape: the
 * quests are accepted through the Aquaris/Springleaf NPC dialog (the retail acquire=Talk axis is
 * authoritative — the legacy dual-sensor auto-accept retired retail-first), the two ride zones
 * (names resolved through the registry) advance the journal 0→1→reward row 2 with the REWARD
 * projection and the enter-world / re-talk heal pair; elyos and asmodian mirrors share the model.
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
			// DD 布局：var0 行阶梯独占整 6 位 SECTION_0 段（非最小位宽）。
			// DD layout: the var0 row ladder owns the whole 6-bit SECTION_0 slot (not minimal
			// width).
			assertEquals(0, field.offset(), () -> "quest " + contract.questId() + " var0 SECTION_0");
			assertEquals(6, field.width(),
				() -> "quest " + contract.questId() + " var0 must hold the third journal row");
			assertEquals(63, field.maxValue(), () -> "quest " + contract.questId() + " var0 max");
		}
	}

	@Test
	void theAcquireNpcOwnsTheAcceptRoute() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			// 真端 acquire=Talk 轴权威： Aquaris 系 NPC 对话接取，感应区不再自动接取。
			// The retail acquire=Talk axis is authoritative: the Aquaris dialog accepts; the zones
			// no longer auto-accept.
			QuestTransition accept = definition.transitions().stream()
				.filter(candidate -> "unaccepted".equals(candidate.sourceNode())
					&& "started".equals(candidate.targetNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc)
				.findFirst().orElseThrow();
			assertTrue(accept.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == contract.rewardNpcId(),
				() -> "quest " + contract.questId() + " accept npc");
			for (String zone : List.of(contract.zoneIn(), contract.zoneBack())) {
				assertTrue(definition.transitions().stream()
					.noneMatch(candidate -> "unaccepted".equals(candidate.sourceNode())
						&& candidate.event().equals(new QuestEvent.EnterZone(zone))),
					() -> "quest " + contract.questId() + " must not auto-accept on " + zone);
			}
		}
	}

	@Test
	void sensorRowsAdvanceTheJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			// DD EA 边：无条件、SetVariable 推进、PACKET_ONLY 同步（区名经解析表落登记名）。
			// DD EA edges: unconditional, SetVariable advance, PACKET_ONLY sync (zone names
			// resolved through the registry).
			QuestTransition firstRide = route(definition, "started", "s1",
				new QuestEvent.EnterZone(contract.zoneIn()));
			assertEquals(List.of(), firstRide.conditions(),
				() -> "quest " + contract.questId() + " A_TO_X conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), firstRide.actions(),
				() -> "quest " + contract.questId() + " A_TO_X actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				firstRide.afterCommit(),
				() -> "quest " + contract.questId() + " A_TO_X after-commit");

			QuestTransition secondRide = route(definition, "s1", "reward",
				new QuestEvent.EnterZone(contract.zoneBack()));
			assertEquals(List.of(), secondRide.conditions(),
				() -> "quest " + contract.questId() + " X_TO_A conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), secondRide.actions(),
				() -> "quest " + contract.questId() + " X_TO_A actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				secondRide.afterCommit(),
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
			// DD 期刊行自愈：REWARD 且 var0<2 的存档在进世界或重谈时回到领奖行 2。
			// The DD journal-row heal: saves at REWARD with var0<2 return to reward row 2 on
			// enter-world or on the reward-npc re-talk.
			QuestTransition recovery = recoveryRoute(compiled.definition());
			// EA 末链实况形：末步非 talk/collect → 单边 EnterWorld repair，仅修 var0=0 的陈旧
			// REWARD 存档（行投影之前的打包形）。
			// EA final-chain live shape: the last step is neither talk nor collect, so a single
			// EnterWorld repair edge restores only the var0=0 stale REWARD save (the pre-projection
			// packed shape).
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 0)), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), recovery.actions(),
				() -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 0), Map.of()),
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
			// 领奖态 QUEST_SELECT 重谈开奖励窗（DD 标准形；坐骑末段直达领奖后由 1009/确认完成）。
			// The reward-state re-talk opens the reward window (the DD standard shape).
			QuestTransition select = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.sourceNode())
					&& "reward".equals(candidate.targetNode()))
				.filter(candidate -> candidate.event().equals(
					new QuestEvent.TalkToNpc(contract.rewardNpcId(),
						QuestDialogAction.QUEST_SELECT.id())))
				.findFirst().orElseThrow();
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.DEFAULT_SUCCESS.id())), select.afterCommit(),
				() -> "quest " + contract.questId() + " reward re-talk window");
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
		// 退役任务统一走生产视图（真端 overlay 合成；旧 XML 只在 git 历史里）。
		// Retired quests resolve through the production view (retail overlay; the old XML lives in
		// git history only).
		return ProductionQuestDefinitions.definition(questId);
	}
}
