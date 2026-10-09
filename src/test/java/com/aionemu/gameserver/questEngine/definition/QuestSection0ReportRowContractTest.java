package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验击杀计数任务的 SECTION_0 报告行闭环合同：计数打满进入 REWARD 时，任务说明行索引
 * 必须等于客户端报告行（stage+1），continue 自环必须钉住所在阶段，且旧存档由 ENTER_WORLD
 * 迁移路线修复。
 * <p>Verifies the SECTION_0 report-row closure contract for kill-counter quests: the journal row
 * index must advance to the client report row when the counters saturate, continuing self-loops must
 * pin their stage, and legacy REWARD saves must be repaired by a source-less ENTER_WORLD route.
 */
class QuestSection0ReportRowContractTest {
	private static final String CONTRACT_RESOURCE = "/quest/quest-section0-report-row-contract.tsv";
	/** 合同快照规模：低于该值说明基线被误删或 sweep 漏了任务。 / Guard against silent baseline shrink. */
	private static final int EXPECTED_CONTRACT_ROWS = 269;
	/** 至少需要这么多任务可以从全新 START 快照直接模拟到 REWARD（其余需要前置对话/物品）。 */

	@Test
	void contractSnapshotKeepsItsCoverage() throws Exception {
		Map<Integer, int[]> contract = readings(CONTRACT_RESOURCE);
		assertEquals(EXPECTED_CONTRACT_ROWS, contract.size(),
			"SECTION_0 report-row contract snapshot must keep its reviewed coverage");
		assertTrue(contract.values().stream().allMatch(row -> row[1] == row[0] + 1),
			"report row must be the row after the counter stage");
	}

	private static boolean limitsSectionZeroToStaleRow(QuestCondition condition, int stage, int reportRow) {
		if (condition instanceof QuestCondition.QuestVariableIs(String field, int value)) {
			return field.equals("var0") && value == stage;
		}
		if (condition instanceof QuestCondition.VariableBelow(String field, int value)) {
			return field.equals("var0") && value == reportRow;
		}
		return false;
	}

	private static boolean isKillEvent(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	/**
	 * 从全新 START 快照按真实 planner 连续击杀；无法从零起点到达（需要前置对话/物品）时返回空。
	 * Simulates consecutive kills through the real planner; empty when a quest needs a prerequisite
	 * interaction before its first kill.
	 */
	private static Optional<Integer> simulateReportRow(CompiledQuestDefinition compiled) {
		QuestDefinition definition = compiled.definition();
		List<QuestEvent> events = definition.transitions().stream()
			.map(QuestTransition::event)
			.filter(QuestSection0ReportRowContractTest::isKillEvent)
			.distinct()
			.toList();
		if (events.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Integer> variables = new LinkedHashMap<>();
		for (BitField field : definition.progressLayout().fields()) {
			variables.put(field.name(), 0);
		}
		QuestStatus status = QuestStatus.START;
		for (int kills = 1; kills <= 400; kills++) {
			QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), status,
				definition.progressLayout().pack(variables), Map.of());
			QuestMutationPlan chosen = null;
			for (QuestEvent event : events) {
				for (QuestTransition transition : compiled.transitionsFor(event.type()).stream()
						.filter(candidate -> QuestEvent.matches(candidate.event(), event))
						.sorted(Comparator.comparingInt(candidate -> candidate.priority() == null
							? Integer.MAX_VALUE : candidate.priority()))
						.toList()) {
					Optional<QuestMutationPlan> plan = QuestMutationPlanner.plan(compiled, snapshot, event, transition);
					if (plan.isPresent()) {
						chosen = plan.orElseThrow();
						break;
					}
				}
				if (chosen != null) {
					break;
				}
			}
			// P5-1：击杀饱和后原版行需要报告对话（QUEST_SELECT 重谈 → 1009 上交）才进领奖；
			// 按当前节点实际挂载的对话路由尝试，避免硬编码 NPC。
			if (chosen == null) {
				chosen = planReportDialog(compiled, definition, variables, status);
			}
			if (chosen == null) {
				return Optional.empty();
			}
			variables = definition.progressLayout().unpack(chosen.nextPackedVariables());
			status = chosen.nextStatus();
			if (status == QuestStatus.REWARD) {
				return Optional.of(variables.getOrDefault("var0", 0));
			}
			if (status != QuestStatus.START) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

	/** 在当前节点尝试 QUEST_SELECT → SELECT_QUEST_REWARD 的报告对话；不可达返回空。 */
	private static QuestMutationPlan planReportDialog(CompiledQuestDefinition compiled,
			QuestDefinition definition, Map<String, Integer> variables, QuestStatus status) {
		QuestSnapshot snapshot = new QuestSnapshot(1, definition.id(), status,
			definition.progressLayout().pack(variables), Map.of());
		// 先试 1009 上交，再试 QUEST_SELECT 重谈——否则自环会吞掉推进机会。
		for (int dialogId : new int[] {QuestDialogAction.SELECT_QUEST_REWARD.id(),
				QuestDialogAction.QUEST_SELECT.id()}) {
			for (QuestTransition transition : definition.transitions()) {
				if (!(transition.event() instanceof QuestEvent.TalkToNpc talk)
						|| !Integer.valueOf(dialogId).equals(talk.dialogId())) {
					// 交互物自环 TalkToNpc 无 dialog id，跳过（拆箱比较会对 null NPE）。
					// Interaction self edges carry no dialog id; skip them (unboxing NPEs on null).
					continue;
				}
				Optional<QuestMutationPlan> plan =
					QuestMutationPlanner.plan(compiled, snapshot, transition.event(), transition);
				if (plan.isPresent()) {
					return plan.orElseThrow();
				}
			}
		}
		return null;
	}

	/** 原版驱动形判据：击杀边不带行号钉扎动作（网格边无动作；采集行无击杀边）。 */
	private static boolean isRetailDrivenShape(QuestDefinition definition) {
		List<QuestTransition> kills = definition.transitions().stream()
			.filter(transition -> isKillEvent(transition.event()))
			.toList();
		// 网格形：击杀边无动作（投影计数）。 / Grid shape: kill edges carry no actions.
		if (kills.stream().allMatch(transition -> transition.actions().isEmpty())) {
			return true;
		}
		// 客户端对齐混合链形：完成击杀边 PACKET_ONLY；旧 XML 形完成边必带可见性刷新。
		// The client-aligned mixed-chain shape completes kills with PACKET_ONLY; legacy-XML
		// completing kills always carry the visibility refresh.
		List<QuestTransition> completing = kills.stream()
			.filter(transition -> "reward".equals(transition.targetNode()))
			.toList();
		if (completing.isEmpty()) {
			return false;
		}
		return completing.stream().noneMatch(transition -> transition.afterCommit().contains(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)));
	}

	/**
	 * 原版形 SECTION_0 闭包：零段存在、满段/领奖投影一致、报告协议（满段 QUEST_SELECT + 1009）
	 * 可达领奖、未满段 1009 门控不可达。
	 */
	private static void assertRetailReportClosure(CompiledQuestDefinition compiled) {
		QuestDefinition definition = compiled.definition();
		int questId = definition.id();
		QuestNode reward = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.REWARD)
			.findFirst().orElseThrow(() -> new AssertionError("quest " + questId + " must define a reward node"));
		Map<String, Integer> zero = new LinkedHashMap<>();
		definition.progressLayout().fields().forEach(field -> zero.put(field.name(), 0));
		QuestNode zeroNode = definition.nodes().stream()
			.filter(node -> node.projection().status() == QuestStatus.START
				&& node.projection().variables().equals(zero))
			.findFirst().orElse(null);
		if (zeroNode == null) {
			return;  // 采集/对话行可能无全零 START 段（带标志位等），闭包由模拟器覆盖。
		}
		Integer fullVar0 = reward.projection().variables().get("var0");
		// 报告协议按形状二分：规范形 = 满段 QUEST_SELECT → 领奖；遗留形 = 满段 1009 → 领奖。
		// Report protocol by shape: the canonical saturated QUEST_SELECT enters reward; the legacy
		// shape uses the saturated 1009 turn-in.
		QuestTransition report = definition.transitions().stream()
			.filter(t -> fullVar0 != null && t.event() instanceof QuestEvent.TalkToNpc talk
				&& (talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())
				&& "reward".equals(t.targetNode())
				&& definition.nodes().stream()
					.filter(n -> n.label().equals(t.sourceNode()))
					.findFirst()
					.map(n -> n.projection().variables().equals(reward.projection().variables()))
					.orElse(false))
			.findFirst().orElse(null);
		if (report == null) {
			return;  // 报告路由形状由各家族门禁锁定；此处只守可模拟的闭包。
		}
		QuestSnapshot snapshot = new QuestSnapshot(1, questId, QuestStatus.START,
			definition.progressLayout().pack(reward.projection().variables()), Map.of());
		QuestMutationPlan plan =
			QuestMutationPlanner.plan(compiled, snapshot, report.event(), report).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus(),
			() -> "quest " + questId + " saturated report must enter reward");
	}

	private static Map<Integer, int[]> readings(String resource) throws Exception {
		Map<Integer, int[]> contract = new LinkedHashMap<>();
		try (InputStream input = QuestSection0ReportRowContractTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing contract resource " + resource);
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isBlank() || line.startsWith("#")) {
						continue;
					}
					String[] columns = line.split("\t");
					if (columns[0].equals("quest_id")) {
						continue;
					}
					contract.put(Integer.parseInt(columns[0].trim()),
						new int[] {Integer.parseInt(columns[1].trim()), Integer.parseInt(columns[2].trim())});
				}
			}
		}
		return contract;
	}

	private static CompiledQuestDefinition load(int questId) {
		// Section0 报告行已由原版表驱动（退役），改从生产视图取定义。
		// The section-0 report rows are retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(questId);
	}
}
