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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定“折叠报告行”批次（2026-10-09 实机修订版）：10529/20529 的客户端任务书共 12 行，第 10 行
 * “带上陷入沉睡的德扎波波/卫扎波波，向代理人报告”是独立 START 节点 s10（2026-09-24 实机截图
 * 证实高亮），同时接回 806075/806079 的报告对话链（QUEST_SELECT → SELECT11 → SELECT11_1 →
 * SELECT11_1_1 → SET_SUCCEED）。
 * <p>2026-10-09 修订：批次 4 原把 reward 投影推到第 11 行，实机裁决该形态让任务书步骤整块空白——
 * 第 11 行“和代理人对话”与第 0 行逐字重复，属 QE-054「末行 = 第 0 行复述行」例外（10528 勘误
 * 同型，10529/20529 在该勘误的“禁止按任一方向批量改”清单里）。reward 投影回滚到 legacy 落盘值
 * 10（SETPRO10 分支 changeQuestStep(9,10,false)，to 从不写盘，QE-051），SET_SUCCEED 不回写
 * var0，无 source enter-world 恢复边反转为 REWARD/var0=11 → 10（QE-051 反向带毒边反转）。
 * SETPRO10/SET_SUCCEED 交接同时清零 var1/var2（QE-044 计数纯净，2026-10-09 头顶标记修复）。</p>
 * 另外锁定 3090（皮皮任务）的位段布局：var1/var2 原先落在 SECTION_0（bit 2/3）内，会把客户端
 * 任务书行索引污染成 var0|var1&lt;&lt;2|var2&lt;&lt;3；修复把它们移到各自的 6 bit SECTION，并把领奖行从
 * 第 3 行推到第 4 行（“向 LF2A_NPC_Morati 报告”）。
 * Locks the “folded report row” batch (2026-10-09 revision) for 10529/20529: the report row keeps
 * its own START node s10 (highlighted per the 2026-09-24 live screenshot) with the restored
 * 806075/806079 report dialog chain, while the reward projection rolls back to the legacy persisted
 * 10 — the batch-4 "reward moves to the final row" shape was overturned live on 2026-10-09 (journal
 * rows went blank), because journal row 11 repeats row 0 verbatim (the QE-054 "last row repeats
 * row 0" exception, same as the 10528 erratum) and the legacy REWARD persists 10 (QE-051). The
 * enter-world repair edge is inverted to REWARD/var0=11 -> 10, and both hand-ins clear var1/var2
 * (the 2026-10-09 QE-044 counter-purity marker fix). Also locks 3090 (its counter fields no longer
 * sit inside SECTION_0, so the client journal row index stays the raw var0; the reward row moves
 * from 3 to 4).
 */
class JournalReportRowSplitContractTest {

	private record ReportContract(int questId, int carrierNpcId, int agentNpcId,
			Map<Integer, Integer> carrierInventory, int reportRow, int rewardRow) {
	}

	/** reportRow = s10 报告行；rewardRow = legacy 领奖投影（2026-10-09 实机修订：= 10，非末行 11）。 reportRow = the s10 report row; rewardRow = the legacy reward projection (2026-10-09 revision: 10, not the final row 11). */
	private static final List<ReportContract> REPORTS = List.of(
		new ReportContract(10529, 806294, 806075, Map.of(182216107, 1), 10, 10),
		new ReportContract(20529, 806299, 806079,
			Map.of(182216090, 1, 182216091, 1, 182216092, 1, 182216108, 1), 10, 10));

	@Test
	void reportRowKeepsItsOwnStartNodeAndTheRewardProjectionMatchesLegacy() throws Exception {
		for (ReportContract contract : REPORTS) {
			QuestDefinition definition = definition(contract.questId()).definition();
			QuestNode report = definition.nodes().stream()
				.filter(candidate -> "s10".equals(candidate.label())).findFirst().orElseThrow();
			assertEquals(QuestStatus.START, report.projection().status(),
				() -> "quest " + contract.questId() + " report row status");
			assertEquals(contract.reportRow(), report.projection().variables().get("var0"),
				() -> "quest " + contract.questId() + " report row");
			assertEquals(contract.rewardRow(), rewardRow(definition),
				() -> "quest " + contract.questId() + " reward journal row");
		}
	}

	@Test
	void fallenSageInitialTalkOpensTheQuestPageWithoutAClientQuestRow() throws Exception {
		for (ReportContract contract : REPORTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestEvent.TalkToNpc initialTalk = new QuestEvent.TalkToNpc(
				contract.carrierNpcId(), QuestDialogAction.USE_OBJECT.id());
			QuestTransition entry = route(compiled.definition(), "s9", "s9", initialTalk);
			assertEquals(1, compiled.definition().transitions().stream()
				.filter(transition -> transition.event().equals(initialTalk)).count(),
				() -> "quest " + contract.questId() + " must only open on the fallen sage at s9");
			assertEquals(List.of(), entry.conditions());
			assertEquals(List.of(), entry.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT10.id())),
				entry.afterCommit());
			assertNull(entry.priority());

			QuestSnapshot afterBossKill = snapshot(compiled, QuestStatus.START,
				Map.of("var0", 9, "var1", 0, "var2", 3), contract.carrierInventory());
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, afterBossKill,
				initialTalk, entry).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus());
			assertEquals(afterBossKill.packedVariables(), plan.nextPackedVariables(),
				() -> "quest " + contract.questId() + " initial talk must preserve the real kill counters");

			QuestTransition questRow = route(compiled.definition(), "s9", "s9",
				new QuestEvent.TalkToNpc(contract.carrierNpcId(), QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT10.id())),
				questRow.afterCommit());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT10_1.id())),
				route(compiled.definition(), "s9", "s9", new QuestEvent.TalkToNpc(
					contract.carrierNpcId(), QuestDialogAction.SELECT10_1.id())).afterCommit());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT10_1_1.id())),
				route(compiled.definition(), "s9", "s9", new QuestEvent.TalkToNpc(
					contract.carrierNpcId(), QuestDialogAction.SELECT10_1_1.id())).afterCommit());

			QuestTransition handover = route(compiled.definition(), "s9", "s10",
				new QuestEvent.TalkToNpc(contract.carrierNpcId(), QuestDialogAction.SETPRO10.id()));
			QuestMutationPlan handoverPlan = QuestMutationPlanner.plan(compiled, afterBossKill,
				handover.event(), handover).orElseThrow();
			assertEquals(QuestStatus.START, handoverPlan.nextStatus());
			// SETPRO10 交接同时清零 var1/var2（QE-044 计数纯净）：真实杀怪计数残值不得带进报告行。
			// The SETPRO10 hand-in also clears var1/var2 (QE-044 counter purity): real kill-counter
			// residue must not ride into the report row.
			assertEquals(Map.of("var0", contract.reportRow(), "var1", 0, "var2", 0),
				unpack(compiled, handoverPlan),
				() -> "quest " + contract.questId() + " must leave the fallen sage on the report row");
			assertTrue(handoverPlan.requiredActions().contains(new QuestAction.GiveItem(
				contract.questId() == 10529 ? 182216107 : 182216108, 1)));
			assertEquals(3, handoverPlan.afterCommit().size());
			assertInstanceOf(AfterCommitAction.TeleportPlayer.class, handoverPlan.afterCommit().get(0));
			assertEquals(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				handoverPlan.afterCommit().get(1));
			assertEquals(new AfterCommitAction.CloseDialog(), handoverPlan.afterCommit().get(2));
		}
	}

	@Test
	void carrierHandoverEntersTheReportRow() throws Exception {
		for (ReportContract contract : REPORTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition handover = route(compiled.definition(), "s9", "s10",
				new QuestEvent.TalkToNpc(contract.carrierNpcId(), QuestDialogAction.SETPRO10.id()));
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 9), contract.carrierInventory()),
				handover.event(), handover).orElseThrow();
			assertEquals(QuestStatus.START, plan.nextStatus(),
				() -> "quest " + contract.questId() + " carrier handover status");
			assertEquals(contract.reportRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " carrier handover journal row");
		}
	}

	@Test
	void agentInitialTalkOpensTheReportPageWithoutAClientQuestRow() throws Exception {
		for (ReportContract contract : REPORTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestEvent.TalkToNpc initialTalk = new QuestEvent.TalkToNpc(
				contract.agentNpcId(), QuestDialogAction.USE_OBJECT.id());
			QuestTransition entry = route(compiled.definition(), "s10", "s10", initialTalk);
			assertEquals(List.of(), entry.conditions());
			assertEquals(List.of(), entry.actions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT11.id())),
				entry.afterCommit());
			assertNull(entry.priority());

			QuestSnapshot report = snapshot(compiled, QuestStatus.START,
				Map.of("var0", contract.reportRow(), "var1", 0, "var2", 3), contract.carrierInventory());
			QuestMutationPlan entryPlan = QuestMutationPlanner.plan(compiled, report,
				initialTalk, entry).orElseThrow();
			assertEquals(QuestStatus.START, entryPlan.nextStatus());
			assertEquals(report.packedVariables(), entryPlan.nextPackedVariables(),
				() -> "quest " + contract.questId() + " agent talk must preserve the report stage");

			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT11.id())),
				route(compiled.definition(), "s10", "s10", new QuestEvent.TalkToNpc(
					contract.agentNpcId(), QuestDialogAction.QUEST_SELECT.id())).afterCommit());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT11_1.id())),
				route(compiled.definition(), "s10", "s10", new QuestEvent.TalkToNpc(
					contract.agentNpcId(), QuestDialogAction.SELECT11_1.id())).afterCommit());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT11_1_1.id())),
				route(compiled.definition(), "s10", "s10", new QuestEvent.TalkToNpc(
					contract.agentNpcId(), QuestDialogAction.SELECT11_1_1.id())).afterCommit());

			QuestTransition handover = route(compiled.definition(), "s10", "reward",
				new QuestEvent.TalkToNpc(contract.agentNpcId(), QuestDialogAction.SET_SUCCEED.id()));
			QuestMutationPlan rewardPlan = QuestMutationPlanner.plan(compiled, report,
				handover.event(), handover).orElseThrow();
			assertEquals(QuestStatus.REWARD, rewardPlan.nextStatus());
			// SET_SUCCEED 不回写 var0（reward 投影补 legacy 10），并清零 var1/var2（QE-044）。
			// SET_SUCCEED never writes var0 (the reward projection fills the legacy 10) and clears
			// var1/var2 (QE-044).
			assertEquals(Map.of("var0", contract.rewardRow(), "var1", 0, "var2", 0),
				unpack(compiled, rewardPlan));
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()), rewardPlan.afterCommit());
		}
	}

	@Test
	void reportingToTheAgentEntersTheRewardRow() throws Exception {
		for (ReportContract contract : REPORTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition handover = route(compiled.definition(), "s10", "reward",
				new QuestEvent.TalkToNpc(contract.agentNpcId(), QuestDialogAction.SET_SUCCEED.id()));
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", contract.reportRow()),
					contract.carrierInventory()),
				handover.event(), handover).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(),
				() -> "quest " + contract.questId() + " report handover status");
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " report handover journal row");
		}
	}

	@Test
	void persistedRewardRowElevenSavesAreRolledBackOnEnterWorld() throws Exception {
		for (ReportContract contract : REPORTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			// 反转边：批次 4 误推的 REWARD/11 档回滚到 legacy 落盘值 10（QE-051 反向带毒边反转）。
			// The inverted edge: batch-4's mistaken REWARD/11 saves roll back to the legacy 10
			// (the QE-051 reverse-poison edge inverted).
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", 11)), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(
				new QuestAction.SetVariable("var0", contract.rewardRow()),
				new QuestAction.SetVariable("var1", 0),
				new QuestAction.SetVariable("var2", 0)), recovery.actions(),
				() -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 11), Map.of()),
				recovery.event(), recovery).orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(contract.rewardRow(), unpack(compiled, plan).get("var0"),
				() -> "quest " + contract.questId() + " repaired journal row");
		}
	}

	@Test
	void noRewardRouteWritesAStaleJournalRow() throws Exception {
		for (int questId : List.of(10529, 20529, 10530, 3090)) {
			QuestDefinition definition = definition(questId).definition();
			int row = rewardRow(definition);
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode())).toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + questId + " reward routes");
			for (QuestTransition route : rewardRoutes) {
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(row, value, () -> "quest " + questId + " route "
							+ route.sourceNode() + " writes a non reward journal row");
					}
				}
			}
		}
	}

	@Test
	void theobomosContrabandQuestKeepsCountersOutOfTheJournalSection() throws Exception {
		QuestDefinition definition = definition(3090).definition();
		BitField row = definition.progressLayout().field("var0");
		assertEquals(0, row.offset(), "quest 3090 var0 must own SECTION_0");
		assertEquals(3, row.width(), "quest 3090 var0 must reach the fourth journal row");
		assertEquals(4, row.maxValue(), "quest 3090 var0 max");
		for (String field : List.of("var1", "var2")) {
			BitField counter = definition.progressLayout().field(field);
			assertEquals(Integer.parseInt(field.substring(3)) * 6, counter.offset(),
				() -> "quest 3090 " + field + " must not overlap the journal SECTION_0");
		}
		assertEquals(4, rewardRow(definition), "quest 3090 reward journal row");
	}

	@Test
	void theobomosContrabandHandoverEntersTheReportRow() throws Exception {
		CompiledQuestDefinition compiled = definition(3090);
		QuestTransition handover = route(compiled.definition(), "s3", "reward",
			new QuestEvent.TalkToNpc(700421, QuestDialogAction.SET_SUCCEED.id()));
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 3), Map.of(182208050, 1)),
			handover.event(), handover).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(4, unpack(compiled, plan).get("var0"), "quest 3090 handover journal row");

		QuestTransition recovery = recoveryRoute(compiled.definition());
		assertEquals(List.of(
			new QuestCondition.StatusIs(QuestStatus.REWARD),
			new QuestCondition.QuestVariableIs("var0", 3)), recovery.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 4)), recovery.actions());
		assertNull(recovery.priority());
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
		try (InputStream input = JournalReportRowSplitContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest/definitions/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
