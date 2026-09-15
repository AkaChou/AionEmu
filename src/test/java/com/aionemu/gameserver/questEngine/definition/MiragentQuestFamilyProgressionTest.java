package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
		3938, List.of("started", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "reward"),
		3939, List.of("started", "s1", "s2", "s3", "reward"),
		3940, List.of("started", "hunt", "hunt-done", "s3", "s4", "s5", "reward"));
	private static final Map<Integer, String> FINAL_PROGRESS_NODES = Map.of(
		3933, "s6",
		3934, "s8",
		3935, "s4",
		3936, "s1",
		3937, "s1",
		3938, "s8",
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

			assertTrue(definition.definition().transitions().stream()
				.anyMatch(transition -> "reward".equals(transition.targetNode())
					&& FINAL_PROGRESS_NODES.get(questId).equals(transition.sourceNode())),
				"quest " + questId + " missing final progress route");
		}
	}

	@Test
	void noQuestUsesSetproToSkipFromAnIntermediateStageIntoReward() throws Exception {
		for (int questId : EXPECTED_PROGRESS_NODES.keySet()) {
			CompiledQuestDefinition definition = load(questId);
			assertTrue(definition.definition().transitions().stream()
				.noneMatch(MiragentQuestFamilyProgressionTest::isSetproRewardRoute),
				"quest " + questId + " still has a SETPRO -> REWARD route");
		}
	}

	private static boolean isSetproRewardRoute(QuestTransition transition) {
		if (!"reward".equals(transition.targetNode())
			|| !(transition.event() instanceof QuestEvent.TalkToNpc talk)) {
			return false;
		}
		return Arrays.stream(QuestDialogAction.values())
			.anyMatch(action -> action.name().startsWith("SETPRO") && action.id() == talk.dialogId());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
