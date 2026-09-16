package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证怪物击杀任务与 Aion 5.8 客户端进度契约（SECTION_0 与 SECTION_1）的对齐性。
 * Verifies that monster hunt quests align with the Aion 5.8 client progress contract (SECTION_0 step vs SECTION_1 kill counter).
 */
class QuestMonsterProgressContractAuditTest {
	private static final Path CONTRACTS_CSV = Path.of("docs/quest/client-dialog-mapping/client-monster-progress-contracts.csv");
	private static final List<Integer> SPECIAL_MISSIONS_1_TO_24 = List.of(
		19631, 19632, 19633, 19634, 19635, 19636, 19637, 19638, 19639, 19640, 19641, 19642,
		29631, 29632, 29633, 29634, 29635, 29636, 29637, 29638, 29639, 29640, 29641, 29642);

	@Test
	void specialMissionsElyosAndAsmodiansStrictlyAlignWithClientStepAndKillCounterSeparation() throws Exception {
		for (int questId : SPECIAL_MISSIONS_1_TO_24) {
			CompiledQuestDefinition compiled = load(questId);
			QuestDefinition definition = compiled.definition();
			ProgressLayout layout = definition.progressLayout();

			// 1. 变量契约：必须具有 var0（步骤，width=6）与 var1（击杀计数，width=6）
			BitField var0 = layout.field("var0");
			BitField var1 = layout.field("var1");
			assertNotNull(var0, () -> "quest " + questId + " must declare var0");
			assertNotNull(var1, () -> "quest " + questId + " must declare var1 for kill counter");
			assertEquals(0, var0.offset());
			assertEquals(6, var1.offset());

			// 2. 节点契约：started 节点只投影 START，不得锁死实时计数字段；reward 节点投影 var0=1, var1=10
			assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0, "var1", 0));
			assertNode(definition, "started", QuestStatus.START, Map.of());
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 1, "var1", 10));
			assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0, "var1", 0));

			// 3. 击杀路线契约：存在 priority 1 的 var1 递增自环（PACKET_ONLY），且绝不递增 var0
			List<QuestTransition> continuingKills = definition.transitions().stream()
				.filter(t -> isKillEvent(t.event()))
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("started"))
				.filter(t -> Integer.valueOf(1).equals(t.priority()))
				.toList();
			assertFalse(continuingKills.isEmpty(), () -> "quest " + questId + " must have continuing kill route");
			for (QuestTransition t : continuingKills) {
				assertTrue(t.actions().stream().anyMatch(a -> a instanceof QuestAction.IncrementVariable(String field, int delta) && field.equals("var1")),
					() -> "quest " + questId + " continuing kill route must increment var1");
				assertFalse(t.actions().stream().anyMatch(a -> a instanceof QuestAction.IncrementVariable(String field, int delta) && field.equals("var0")),
					() -> "quest " + questId + " continuing kill route must not increment var0");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), t.afterCommit());
			}

			// 4. 终结路线契约：存在 priority 0 的终击直达 reward 路线（LEVEL_AND_VISIBILITY_REFRESH），并显式设置 var0=1 与 var1=10
			List<QuestTransition> completingKills = definition.transitions().stream()
				.filter(t -> isKillEvent(t.event()))
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward"))
				.filter(t -> Integer.valueOf(0).equals(t.priority()))
				.toList();
			assertFalse(completingKills.isEmpty(), () -> "quest " + questId + " must have priority 0 completing kill route");
			for (QuestTransition t : completingKills) {
				assertTrue(t.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable(String field, int value) && field.equals("var0") && value == 1),
					() -> "quest " + questId + " completing route must set var0=1");
				assertTrue(t.actions().stream().anyMatch(a -> a instanceof QuestAction.SetVariable(String field, int value) && field.equals("var1") && value == 10),
					() -> "quest " + questId + " completing route must set var1=10");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), t.afterCommit());
			}

			// 5. 满计数恢复路由：在 started 状态下且 var1>=10 时，必须能向报告 NPC 提交报告
			boolean hasRecoveredReport = definition.transitions().stream()
				.filter(t -> t.sourceNode().equals("started") && t.targetNode().equals("reward"))
				.anyMatch(t -> t.event() instanceof QuestEvent.TalkToNpc talk && (talk.dialogId() == null || talk.dialogId() == 31 || talk.dialogId() == 1009)
					&& t.conditions().stream().anyMatch(c -> c instanceof QuestCondition.VariableAtLeast(String field, int value) && field.equals("var1") && value == 10));
			assertTrue(hasRecoveredReport, () -> "quest " + questId + " must provide recovered report route for full counters");
		}
	}

	@Test
	void clientMonsterProgressContractsCsvExistsAndHasExpectedShape() throws Exception {
		assertTrue(Files.exists(CONTRACTS_CSV), "contracts CSV must exist in docs/quest/client-dialog-mapping/");
		try (BufferedReader reader = Files.newBufferedReader(CONTRACTS_CSV, StandardCharsets.UTF_8)) {
			String header = reader.readLine();
			assertNotNull(header);
			assertTrue(header.contains("quest_id"));
			assertTrue(header.contains("step_section"));
			assertTrue(header.contains("counter_section"));
			assertTrue(header.contains("required"));

			int count = 0;
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					count++;
				}
			}
			final int totalCount = count;
			assertTrue(totalCount >= 800, () -> "expected at least 800 client monster progress contracts, found " + totalCount);
		}
	}

	private static boolean isKillEvent(QuestEvent event) {
		return event instanceof QuestEvent.KillNpc || event instanceof QuestEvent.KillNpcSet;
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow(() -> new AssertionError("missing node " + label));
		assertEquals(status, node.projection().status(), () -> "status mismatch on node " + label);
		assertEquals(variables, node.projection().variables(), () -> "variables mismatch on node " + label);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestMonsterProgressContractAuditTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
