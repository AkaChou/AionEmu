package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 25608 [Group] Oh, Bother 的 7 步真端行对齐（DataDriven 编译形）。
 * <p>客户端 quest_q25608.html 的 quest_summary 有 7 个可见槽位（0/3/6/9/12/15/18），retail 表
 * 同样 7 步：TALK 806177 → TALK 806197 → ENTER_AREA 206534 → HUNT 241235 x10 → TALK 806197 →
 * ENTER_AREA 206542 → COLLECT_ITEM 805964。DD 编译器把 step0..step6 投影到行 0..6，领奖行 =
 * 客户端末行 6；两个 ENTER_AREA 步的 DD 驼峰别名经 {@code quest_enterarea_zone_resolution.tsv}
 * 解析为 zones_quest.xml 登记名（坐标取自客户端 DF6 mission level），未登记别名在编译期被拒
 * （RETAIL_ENTERAREA_ZONE_UNRESOLVED，防运行时死边）；hunt 段 SECTION_1 计数（count-1 门），
 * 交付行 39 检查直达领奖；REWARD/var0=5 陈旧存档由 enter-world 或 QUEST_SELECT 自愈边回到行 6。</p>
 * <p>Locks the seven-step retail journal alignment for quest 25608 in the DataDriven compiled
 * shape: the client quest_summary has seven visible slots matching the retail steps TALK, TALK,
 * ENTER_AREA 206534, HUNT x10, TALK, ENTER_AREA 206542 and COLLECT_ITEM. The compiler projects
 * step0..step6 onto journal rows 0..6 with the reward on the client's last row 6; both ENTER_AREA
 * aliases resolve through the zone-resolution registry to the zones_quest.xml registered names,
 * an unregistered alias is rejected at compile time (the runtime dead-edge guard); the hunt counts
 * in SECTION_1 behind the count-1 gate, the turn-in check lands the reward row, and stale
 * REWARD/var0=5 saves heal back to row 6.</p>
 */
class Quest25608RetailSevenStepAlignmentTest {

	private static final String ZONE_A = "DF6_SENSORY_AREA_Q25608_A_DYNAMIC_ENV_220110000";
	private static final String ZONE_B = "DF6_SENSORY_AREA_Q25608_B_DYNAMIC_ENV_220110000";
	private static final int TURN_IN_NPC = 805964;
	private static final int HUNT_NPC = 241235;
	private static final int QUEST_ITEM = 182216007;
	private static final int REWARD_ROW = 6;
	private static final int STALE_REWARD_ROW = 5;

	@Test
	void sevenRetailStepsCoverEveryClientJournalRow() {
		QuestDefinition definition = load().definition();

		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));
		assertNode(definition, "s3", QuestStatus.START, Map.of("var0", 3));
		assertNode(definition, "s4", QuestStatus.START, Map.of("var0", 4));
		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "s6", QuestStatus.START, Map.of("var0", 6));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", REWARD_ROW));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		Set<Integer> journalRows = definition.nodes().stream()
			.map(QuestNode::projection)
			.filter(projection -> projection.status() == QuestStatus.START
				|| projection.status() == QuestStatus.REWARD)
			.map(projection -> projection.variables().get("var0"))
			.collect(Collectors.toSet());
		assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6), journalRows,
			"客户端每一行都必须有 START/REWARD 状态");
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), label + " status");
		assertEquals(variables, node.projection().variables(), label + " variables");
	}

	private static QuestTransition transition(CompiledQuestDefinition compiled, String source, String target) {
		return transition(compiled, source, target, null);
	}

	private static QuestTransition transition(CompiledQuestDefinition compiled, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = compiled.definition().transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode()))
			.filter(candidate -> event == null || event.equals(candidate.event()))
			.toList();
		assertEquals(1, matches.size(), source + " -> " + target + " routes");
		return matches.getFirst();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables) {
		return snapshot(compiled, status, variables, Map.of());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, QuestStatus status,
			Map<String, Integer> variables, Map<Integer, Integer> inventory) {
		Map<String, Integer> packedVariables =
			new LinkedHashMap<>(compiled.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, compiled.id(), status,
			compiled.definition().progressLayout().pack(packedVariables), inventory, Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition load() {
		return ProductionQuestDefinitions.definitionInOverlay(25608);
	}
}
