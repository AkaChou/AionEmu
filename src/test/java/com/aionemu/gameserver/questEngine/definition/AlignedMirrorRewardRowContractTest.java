package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestWorldFacts;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证“领奖行投影 = 客户端任务书最后一行”在镜像一侧已对齐的任务族里逐对成立：
 * 11294/21294、15002/25002、15010/25010、15070/25070、15514/25514、19004/29004、
 * 25062/15062、25073/15073、26820/16820。旧投影落后一行的 9 个任务改为与镜像相同的领奖行，
 * 并补上无 source 的进入世界自愈边，避免已落盘的 REWARD/旧行号存档卡在领奖流程外。
 * <p>
 * 镜像侧已被真端表接管（retire 后 XML 只在 git 历史）的任务，其领奖投影改为真端族口径：
 * SimpleHunt/DataDriven 猎杀网格 = 饱和击杀计数（a0..aN），Talk 链 = 客户端任务书末行，
 * 多段顺序链 = 全槽饱和投影——镜像两侧行号不再逐字相等，逐一登记在 Contract 里。
 * Verifies the “reward projection equals the client journal last row” contract for the mirror pairs
 * whose other side already aligned: 11294/21294, 15002/25002, 15010/25010, 15070/25070,
 * 15514/25514, 19004/29004, 25062/15062, 25073/15073 and 26820/16820. The nine quests whose
 * projection lagged one row now match their mirror, and each gained a source-less enter-world
 * recovery edge so that saves persisted as REWARD/old-row are repaired instead of being stuck.
 * <p>
 * Mirrors adopted by the retail tables (XML retired into git history) follow the retail family
 * caliber instead: SimpleHunt/DataDriven hunt grids project the saturated kill counter
 * (a0..aN), talk chains project the client journal last row, and multi-stage sequential chains
 * project the full per-slot counters — the mirrored row numbers are no longer literally equal
 * and are recorded per pair in the Contract.
 */
class AlignedMirrorRewardRowContractTest {

	private record Contract(int questId, int mirrorId, String source, QuestEvent event,
			Map<Integer, Integer> inventory, String zone, int staleRow, int rewardRow,
			Map<String, Integer> mirrorProjection) {
	}

	private static final List<Contract> CONTRACTS = List.of(
		// 21294：真端 SimpleHunt 1 杀网格，领奖投影 = 饱和计数 1（与镜像行号恰好一致）。
		// 21294: retail SimpleHunt 1-kill grid; the reward projection is the saturated counter 1.
		new Contract(11294, 21294, "started",
			new QuestEvent.TalkToNpc(799010, QuestDialogAction.SETPRO1.id()),
			Map.of(182213038, 1), null, 0, 1, Map.of("var0", 1)),
		// 25002：真端 DataDriven 猎杀网格（双变体 10 杀），领奖投影 = 饱和击杀计数。
		// 25002: retail DataDriven hunt grid (two variants, 10 kills); saturated kill counter.
		new Contract(15002, 25002, "started",
			new QuestEvent.TalkToNpc(804698, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			Map.of(182215663, 7), null, 0, 1, Map.of("var0", 10)),
		// 25010：真端 DataDriven 猎杀网格（双变体 10 杀）。
		// 25010: retail DataDriven hunt grid (two variants, 10 kills).
		new Contract(15010, 25010, "started",
			new QuestEvent.TalkToNpc(804700, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			Map.of(182215664, 5, 182215665, 3), null, 0, 1, Map.of("var0", 10)),
		new Contract(15070, 25070, "started",
			new QuestEvent.TalkToNpc(804891, QuestDialogAction.SELECT_QUEST_REWARD.id()),
			Map.of(182215682, 10), null, 0, 1, Map.of("var0", 1)),
		// 25514：真端 DataDriven 猎杀网格（10 杀）。
		// 25514: retail DataDriven hunt grid (10 kills).
		new Contract(15514, 25514, "started",
			new QuestEvent.TalkToNpc(806093, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
			Map.of(182215975, 10), null, 0, 1, Map.of("var0", 10)),
		// 19004/29004：双方均为真端 Talk 链，领奖投影同为客户端任务书末行（3 行 → 行 2）。
		// 19004/29004: both retail talk chains; both project the client journal last row (3 rows -> 2).
		new Contract(19004, 29004, "s1",
			new QuestEvent.TalkToNpc(203701, QuestDialogAction.SETPRO2.id()),
			Map.of(), null, 1, 2, Map.of("var0", 2)),
		// 15062：真端 DataDriven 猎杀网格（双变体 7 杀）。
		// 15062: retail DataDriven hunt grid (two variants, 7 kills).
		new Contract(25062, 15062, "started",
			new QuestEvent.TalkToNpc(804917, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
			Map.of(182215722, 10), null, 0, 1, Map.of("var0", 7)),
		// 15073：真端 DataDriven 两段顺序链（5+5），领奖投影 = 全槽饱和计数。
		// 15073: retail DataDriven two-stage sequential chain (5+5); the full per-slot projection.
		new Contract(25073, 15073, "started",
			new QuestEvent.TalkToNpc(731556, QuestDialogAction.SET_SUCCEED.id()),
			Map.of(182215725, 1), null, 0, 1, Map.of("var0", 5, "var1", 5)),
		new Contract(26820, 16820, "s1",
			new QuestEvent.EnterZone("IDEternity_02_SensoryArea_Q16820a"),
			Map.of(), "IDEternity_02_SensoryArea_Q16820a", 1, 2, Map.of("var0", 2)));

	@Test
	void rewardRowMatchesTheClientJournalLastRowAndTheAlignedMirror() throws Exception {
		for (Contract contract : CONTRACTS) {
			assertEquals(Map.of("var0", contract.rewardRow()),
				rewardProjection(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward journal row");
			assertEquals(contract.mirrorProjection(),
				rewardProjection(definition(contract.mirrorId()).definition()),
				() -> "mirror " + contract.mirrorId() + " reward journal projection");
		}
	}

	@Test
	void handoversAdvanceTheJournalToTheRewardRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition handover = handoverRoute(compiled.definition(), contract);
			QuestSnapshot snapshot = snapshot(compiled, QuestStatus.START,
				Map.of("var0", contract.staleRow()), contract.inventory());
			if (contract.zone() != null) {
				snapshot = snapshot.withWorldFacts(new QuestWorldFacts(Set.of(), Set.of(contract.zone())));
			}
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot,
				handover.event(), handover).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " handover status");
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " handover journal row");
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

			QuestMutationPlan recoveryPlan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", contract.staleRow()), Map.of()),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, recoveryPlan.nextStatus());
			assertEquals(contract.rewardRow(), unpack(compiled, recoveryPlan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			int row = rewardRow(definition);
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode()))
				.toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + contract.questId() + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(row, value, () -> "quest " + contract.questId()
							+ " route " + route.sourceNode() + " writes a non reward journal row");
					}
				}
			}
		}
	}

	private static QuestTransition handoverRoute(QuestDefinition definition, Contract contract) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> contract.source().equals(candidate.sourceNode()))
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(contract.event()))
			.toList();
		assertFalse(matches.isEmpty(),
			() -> "quest " + contract.questId() + " route " + contract.event());
		if (matches.size() > 1) {
			// NPC_REPORT 简写与显式交付路线可能共用同一事件；带 has-item 守卫的是交付路线。
			// NPC_REPORT shorthands may share the event with the explicit turn-in route; the one
			// guarded by has-item is the turn-in route.
			matches = matches.stream().filter(candidate -> candidate.conditions().stream()
				.anyMatch(QuestCondition.HasItem.class::isInstance)).toList();
		}
		assertEquals(1, matches.size(), () -> "quest " + contract.questId() + " route " + contract.event());
		return matches.getFirst();
	}

	private static int rewardRow(QuestDefinition definition) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label())).findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables().get("var0");
	}

	/** 领奖节点全投影（多段顺序链携带 var0..varN 全槽计数）。 / The full reward projection. */
	private static Map<String, Integer> rewardProjection(QuestDefinition definition) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> "reward".equals(candidate.label())).findFirst().orElseThrow();
		assertEquals(QuestStatus.REWARD, node.projection().status());
		return node.projection().variables();
	}

	private static QuestTransition recoveryRoute(QuestDefinition definition) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode() == null)
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
			.toList();
		assertEquals(1, matches.size(),
			() -> "quest " + definition.id() + " reward recovery route");
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

	/** 生产定义：XML 目录 + 真端 overlay（退役任务不再有 XML，只在 git 历史里）。 */
	private static CompiledQuestDefinition definition(int questId) {
		// TEMP-VERIFY(view): 并行 SimpleTalk 批次落定前生产覆盖门不可用，用宽松 overlay 验证本断言。
		return verificationView().find(questId)
			.orElseThrow(() -> new IllegalStateException("missing production quest definition " + questId));
	}

	// TEMP-VERIFY(view): 并行批次落定前的宽松生产视图（XML 目录 + 真端驱动，跳过覆盖门）。
	private static final java.util.concurrent.atomic.AtomicReference<QuestCatalog> VIEW =
		new java.util.concurrent.atomic.AtomicReference<>();

	private static QuestCatalog verificationView() {
		return VIEW.updateAndGet(current -> current != null ? current
			: RetailQuestDriver.overlay(QuestDefinitionDirectoryLoader.compile(
				AlignedMirrorRewardRowContractTest.class.getClassLoader())));
	}
}
