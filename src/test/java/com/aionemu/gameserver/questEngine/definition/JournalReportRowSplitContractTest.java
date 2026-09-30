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
 * 锁定“折叠报告行”收尾批次：10529/20529 的客户端任务书共 12 行，第 10 行“带上陷入沉睡的
 * 德扎波波/卫扎波波，向代理人报告”此前直接压在 REWARD 上，第 11 行“和代理人对话”反而没有任何
 * 状态；修复把第 10 行拆成独立 START 节点 s10，reward 投影推进到第 11 行并补无 source 的进入
 * 世界自愈边，同时接回 806075/806079 的报告对话链（QUEST_SELECT → SELECT11 → SELECT11_1 →
 * SELECT11_1_1 → SET_SUCCEED）。
 * 另外锁定 3090（皮皮任务）的位段布局：var1/var2 原先落在 SECTION_0（bit 2/3）内，会把客户端
 * 任务书行索引污染成 var0|var1&lt;&lt;2|var2&lt;&lt;3；修复把它们移到各自的 6 bit SECTION，并把领奖行从
 * 第 3 行推到第 4 行（“向 LF2A_NPC_Morati 报告”）。
 * Locks the “folded report row” follow-up batch (10529/20529: the report row becomes its own START
 * node and the reward row moves to the final journal row, with a source-less enter-world repair edge
 * and the restored 806075/806079 report dialog chain) and 3090 (its counter fields no longer sit
 * inside SECTION_0, so the client journal row index stays the raw var0; the reward row moves from 3
 * to 4).
 */
class JournalReportRowSplitContractTest {

	private record ReportContract(int questId, int carrierNpcId, int agentNpcId,
			Map<Integer, Integer> carrierInventory, int reportRow, int rewardRow) {
	}

	private static final List<ReportContract> REPORTS = List.of(
		new ReportContract(10529, 806294, 806075, Map.of(182216107, 1), 10, 11),
		new ReportContract(20529, 806299, 806079,
			Map.of(182216090, 1, 182216091, 1, 182216092, 1, 182216108, 1), 10, 11));

	@Test
	void reportRowGetsItsOwnStartNodeAndTheRewardMovesToTheFinalRow() throws Exception {
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
			assertEquals(Map.of("var0", contract.reportRow(), "var1", 0, "var2", 3),
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
			assertEquals(Map.of("var0", contract.rewardRow(), "var1", 0, "var2", 3),
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
	void persistedReportRowsAreRepairedOnEnterWorld() throws Exception {
		for (ReportContract contract : REPORTS) {
			CompiledQuestDefinition compiled = definition(contract.questId());
			QuestTransition recovery = recoveryRoute(compiled.definition());
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.QuestVariableIs("var0", contract.reportRow())), recovery.conditions(),
				() -> "quest " + contract.questId() + " recovery conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", contract.rewardRow())),
				recovery.actions(), () -> "quest " + contract.questId() + " recovery actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), recovery.afterCommit(),
				() -> "quest " + contract.questId() + " recovery after-commit");
			assertNull(recovery.priority());

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", contract.reportRow()), Map.of()),
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
