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
 * 锁定第三批领奖行修复：203 个任务的客户端 quest_summary 末行是领奖行
 * （“和 X 对话 / 向 X 报告 / 把 X 交给 Y”，且该 NPC 必须是任务内 reward 路线的 NPC），
 * 而服务端 reward 投影停在更早的行号；修复把投影推到末行并补无 source 的 enter-world 自愈边。
 * Locks the third repair batch: 203 quests whose client quest_summary last row is the reward row
 * (a talk/report/hand-over line naming the reward NPC of the quest) while the server reward projection
 * stayed on an earlier row; the projection was rewritten to the last row and each quest gained a
 * source-less enter-world recovery edge.
 * <p>
 * P0c-6：其中 5 行（16900–16903 / 24201）已改由真端 SimpleHunt 表驱动，var0 变成**击杀计数**而不是
 * 任务书行号——任务书行由客户端 {@code SECTION_n} 门控推导，服务端没有可漂移的行号，
 * 因此真端形状下不存在也不需要 repair 边（P3 既有裁定：修复边是 AionEmu 历史包袱）。
 * 后续 DD 批次把 50126/50127/51126/51127 同批划入真端 DataDriven 猎杀网格（击杀 1 名首领，
 * var0 = 饱和计数 1），与 P0c-6 同口径：无 repair 边、领奖投影即饱和计数。
 * <p>
 * Since P0c-6 five rows are retail-driven: var0 is a kill counter, the journal row comes from the
 * client's SECTION gates, and no source-less repair edge may remain. The later DD batch moved
 * 50126/50127/51126/51127 into the retail DataDriven hunt grid the same way (single-boss kills,
 * var0 = the saturated counter 1): no repair edge, the reward projection is the saturated counter.
 */
class JournalRewardRowRepairContractTest {

	private record Contract(int questId, int rewardRow, int staleRow) {
	}

	/**
	 * 已由真端表驱动（P0c-6 SimpleHunt + DD 批次 DataDriven 猎杀网格）：不得保留任何无 source 的
	 * enter-world 修复边。
	 * Retail-driven since P0c-6 (SimpleHunt) and the DD batch (DataDriven hunt grids): no
	 * source-less enter-world repair edge may remain.
	 */
	private static final List<Integer> RETAIL_DRIVEN = List.of(16900, 16901, 16902, 16903, 24201,
		50126, 50127, 51126, 51127);

	private static final List<Contract> CONTRACTS = List.of(
		new Contract(1218, 1, 0),
		new Contract(1319, 8, 0),
		new Contract(1322, 1, 0),
		new Contract(1324, 1, 0),
		/* 1361 移出本批（2026-10-07 实机报障 + 真端/legacy 取证）：领奖态权威值是 legacy/真端的
		 * packed step 1（QE-054/QE-045），不是末行索引 2；误抬为 2 会让客户端任务书步骤整块空白。
		 * 基线由 Quest1361ClientDialogAlignmentTest 锁定（reward 投影 1、自愈边 REWARD/2 -> 1）。
		 * 1361 left this batch (live report 2026-10-07 + retail/legacy evidence): the authoritative
		 * reward value is the legacy/retail packed step 1 (QE-054/QE-045), not the last-row index 2,
		 * which blanks the client journal steps. Its baseline is owned by
		 * Quest1361ClientDialogAlignmentTest (reward projection 1, healing edge REWARD/2 -> 1). */
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
		/* 11006 移出本批（2026-10-07 全族审计 + 真端/legacy 取证）：装第二瓶水后的权威 packed step 是
		 * legacy/真端的 2（useQuestItem(env, item, 2, 2, true, ...) 的 reward 分支不写 nextStep；
		 * 真端 FUN_180f03f20 在 START/step==2 时以 0x100(0x2afe, 0, 0) 推进），不是末行索引 3；
		 * 误抬为 3 会让客户端任务书步骤整块空白（与 1361 同型）。基线与反向自愈边由
		 * Quest11006ClientDialogAlignmentTest 锁定（reward 投影 2、REWARD/3 -> 2）。
		 * 11006 left this batch (2026-10-07 family audit + retail/legacy evidence): the authoritative
		 * packed step after the second fill is the legacy/retail 2 (the reward branch of
		 * useQuestItem(item, 2, 2, true) does not write nextStep; retail FUN_180f03f20 advances via
		 * 0x100(0x2afe, 0, 0) on START/step==2), not the last-row index 3, which blanks the client
		 * journal steps (same shape as 1361). Its baseline is owned by
		 * Quest11006ClientDialogAlignmentTest (reward projection 2, healing edge REWARD/3 -> 2). */
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
		/* 24021 移出本批（2026-10-07 全族审计 + 真端/legacy 取证）：撒 24021c（use-item）后的权威
		 * packed step 是 legacy/真端的 4（useQuestItem(env, item, 4, 4, true, 88) 的 reward 分支不写
		 * nextStep；真端选中 10003=SETPRO4 的 FUN_180faea90 显式 SetProgress(0x5dd5, 4)，随后
		 * FUN_180f00500 以 0x100(0x5dd5, 0, 0) 推进），不是末行索引 5；误抬为 5 会让客户端任务书
		 * 步骤整块空白（与 1361 同型）。基线与反向自愈边由 Quest24021ClientDialogAlignmentTest 锁定
		 * （reward 投影 4、REWARD/5 -> 4）。
		 * 24021 left this batch (2026-10-07 family audit + retail/legacy evidence): the authoritative
		 * packed step after spreading 24021c is the legacy/retail 4 (the reward branch of
		 * useQuestItem(item, 4, 4, true, 88) does not write nextStep; retail FUN_180faea90 writes
		 * SetProgress(0x5dd5, 4) explicitly for option 10003=SETPRO4, then FUN_180f00500 advances via
		 * 0x100(0x5dd5, 0, 0)), not the last-row index 5. Its baseline is owned by
		 * Quest24021ClientDialogAlignmentTest (reward projection 4, healing edge REWARD/5 -> 4). */
		new Contract(24022, 8, 7),
		new Contract(24023, 4, 3),
		new Contract(24024, 5, 4),
		new Contract(24025, 4, 3),
		new Contract(24030, 9, 8),
		new Contract(24046, 7, 6),
		new Contract(24051, 6, 5),
		new Contract(24121, 2, 0),
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
		new Contract(39713, 2, 0),
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
			if (retiredOnly(contract.questId())) {
				continue;
			}
			assertEquals(contract.rewardRow(), rewardRow(definition(contract.questId()).definition()),
				() -> "quest " + contract.questId() + " reward journal row");
		}
	}

	@Test
	void persistedRewardRowsAreRepairedOnEnterWorld() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (retiredOnly(contract.questId())) {
				continue;
			}
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

	/**
	 * 真端驱动的同一批行（2026-10-05 重锚；原 lax overlay 视图随 P7 步 f 退场）：退役行无 IR，
	 * 无 source 的 enter-world 修复边在结构上不可能；守卫 = 每一行必须确属退役行（防名单陈旧
	 * 静默缩水）。领奖投影口径由 native 车道门承担。
	 * The same rows under retail driving (re-anchored 2026-10-05): with no IR left a source-less
	 * repair edge is structurally impossible; the guard keeps the list honest (every listed row must
	 * be retired). The reward-projection caliber is owned by the native lane gates.
	 */
	@Test
	void retailDrivenRowsCarryNoJournalRepairEdge() {
		for (int questId : RETAIL_DRIVEN) {
			assertTrue(RetiredQuestIds.contains(questId),
				() -> "quest " + questId + " is retail-driven and must be a retired row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (Contract contract : CONTRACTS) {
			if (retiredOnly(contract.questId())) {
				continue;
			}
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

	/**
	 * 定义只来自 XML 目录：退役行（如 1218/16900）无 IR——调用方先经 retiredOnly 跳过。
	 * Definitions come from the XML directory only: retired rows (e.g. 1218/16900) have no IR and
	 * are skipped by retiredOnly before the lookup.
	 */
	private static CompiledQuestDefinition definition(int questId) throws Exception {
		return catalog().findExecutable(questId)
			.orElseThrow(() -> new IllegalStateException("missing production quest definition " + questId));
	}

	/**
	 * 退役行（目录缺行）跳过并要求确属退役（防名单陈旧静默缩水）；XML 保留行仍逐条断言。
	 * Retired rows (absent from the catalog) are skipped but must be really retired; XML rows still assert.
	 */
	private static boolean retiredOnly(int questId) throws Exception {
		if (catalog().findExecutable(questId).isPresent()) {
			return false;
		}
		assertTrue(RetiredQuestIds.contains(questId), () -> "quest " + questId + " missing and not retired");
		return true;
	}

	private static final java.util.concurrent.atomic.AtomicReference<QuestCatalog> VIEW =
		new java.util.concurrent.atomic.AtomicReference<>();

	private static QuestCatalog catalog() {
		return VIEW.updateAndGet(current -> current != null ? current
			: QuestDefinitionDirectoryLoader.compile(
				JournalRewardRowRepairContractTest.class.getClassLoader()));
	}
}
