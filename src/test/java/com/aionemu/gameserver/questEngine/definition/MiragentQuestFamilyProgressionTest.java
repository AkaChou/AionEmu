package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证米拉詹特任务链的中间阶段没有被压平成直接奖励路由。
 * Verifies that intermediate stages in the Miragent quest family are not flattened into direct reward routes.
 */
class MiragentQuestFamilyProgressionTest {
	private static final Map<Integer, List<String>> EXPECTED_PROGRESS_NODES = Map.of(
		3933, List.of("started", "s1", "s2", "s3", "s4", "s5", "s6", "reward"),
		3934, List.of("started", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "reward"),
		3935, List.of("started", "s1", "s2", "s3", "s4", "reward"),
		3936, List.of("started", "s1", "reward"),
		3937, List.of("started", "s1", "reward"),
		// 3938：QE-051 批次 51 补出客户端任务书的末两行（行 9 仪式行、行 10 领奖行），
		// 领奖节点沿用该批次的 s10 命名。
		// 3938 owns two extra client journal rows (row 9 ritual, row 10 reward), kept as s10 by batch 51.
		3938, List.of("started", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10"),
		3939, List.of("started", "s1", "s2", "s3", "reward"),
		3940, List.of("started", "hunt", "hunt-done", "s3", "s4", "s5", "reward"));
	private static final Map<Integer, String> FINAL_PROGRESS_NODES = Map.of(
		3933, "s6",
		3934, "s8",
		3935, "s4",
		3936, "s1",
		3937, "s1",
		3938, "s9",
		3939, "s3",
		3940, "s5");

	@Test
	void familyDefinitionsKeepExplicitProgressNodesAndFinalRoutes() throws Exception {
		for (Map.Entry<Integer, List<String>> entry : EXPECTED_PROGRESS_NODES.entrySet()) {
			int questId = entry.getKey();
			CompiledQuestDefinition definition = load(questId);

			for (String label : entry.getValue()) {
				assertTrue(definition.definition().nodes().stream()
					.anyMatch(node -> label.equals(node.label())),
					"quest " + questId + " missing progress node " + label);
			}
			// 末节点必须是 REWARD 态：家族允许它叫 reward 或 sN（3938 用 s10），但不得压平领奖态。
			// The last node must project REWARD; the label may be reward or sN (3938 uses s10).
			String rewardNode = entry.getValue().getLast();
			assertTrue(definition.definition().nodes().stream()
				.anyMatch(node -> rewardNode.equals(node.label())
					&& node.projection().status() == QuestStatus.REWARD),
				"quest " + questId + " final node " + rewardNode + " must project REWARD");

			assertTrue(definition.definition().transitions().stream()
				.anyMatch(transition -> rewardNode.equals(transition.targetNode())
					&& FINAL_PROGRESS_NODES.get(questId).equals(transition.sourceNode())),
				"quest " + questId + " missing final progress route");
		}
	}

	@Test
	void noQuestUsesSetproToSkipFromAnIntermediateStageIntoReward() throws Exception {
		for (int questId : EXPECTED_PROGRESS_NODES.keySet()) {
			CompiledQuestDefinition definition = load(questId);
			String rewardNode = EXPECTED_PROGRESS_NODES.get(questId).getLast();
			assertTrue(definition.definition().transitions().stream()
				.noneMatch(transition -> isSetproRewardRoute(transition, rewardNode)),
				"quest " + questId + " still has a SETPRO -> REWARD route");
		}
	}

	private static boolean isSetproRewardRoute(QuestTransition transition, String rewardNode) {
		if (!rewardNode.equals(transition.targetNode())
			|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
			return false;
		}
		return Arrays.stream(QuestDialogAction.values())
			.anyMatch(action -> action.name().startsWith("SETPRO") && action.id() == talk.dialogId());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
