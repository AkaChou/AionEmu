package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证多步与猎怪任务在初始或进行中状态（如 started、a0b0、s1 等）严禁提前开放领奖路由。
 * Verifies multi-step and monster-hunt quests forbid premature reward routes from initial or progress states.
 */
class QuestPrematureRewardRouteExclusionTest {
	private static final Set<Integer> CLEANED_QUESTS = Set.of(
		1347, 16837, 16986, 16988, 19631, 19633, 19638, 19642,
		2569, 28208, 28743, 28932, 28951, 28952, 28972, 28973, 28974
	);

	@ParameterizedTest
	@ValueSource(ints = {
		1347, 16837, 16986, 16988, 19631, 19633, 19638, 19642,
		2569, 28208, 28743, 28932, 28951, 28952, 28972, 28973, 28974
	})
	void noPrematureRewardRouteFromInitialOrProgressState(int questId) {
		QuestDefinition definition = load(questId).definition();

		// 严禁从 started 态直接跳转至 reward 态
		// Forbid any direct transition from started to reward
		assertFalse(definition.transitions().stream().anyMatch(t ->
			"started".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& hasRewardAction(t)),
			() -> "quest " + questId + " must not have started -> reward turn-in route");

		// 1347 初始节点为 a0b0，严禁 a0b0 直接领奖
		// Quest 1347 initial node is a0b0, forbid a0b0 direct reward
		if (questId == 1347) {
			assertFalse(definition.transitions().stream().anyMatch(t ->
				"a0b0".equals(t.sourceNode()) && "reward".equals(t.targetNode())),
				"quest 1347 must not have a0b0 -> reward turn-in route");
		}

		// 必须存在合法的终态交付路由（如 k<N>、a7b3、s1）
		// Must have a legitimate terminal completion route
		assertTrue(definition.transitions().stream().anyMatch(t ->
			!"started".equals(t.sourceNode()) && !"a0b0".equals(t.sourceNode())
				&& "reward".equals(t.targetNode()) && hasRewardAction(t)),
			() -> "quest " + questId + " must retain valid terminal reward routes");
	}

	private static boolean hasRewardAction(QuestTransition transition) {
		if (transition.event() instanceof QuestEvent.TalkToNpc talk) {
			return talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id();
		}
		return false;
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
			QuestPrematureRewardRouteExclusionTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
