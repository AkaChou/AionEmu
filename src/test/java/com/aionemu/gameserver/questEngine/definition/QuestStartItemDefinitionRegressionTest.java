package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 任务对话/交互路由回归测试。
 * 遵循真端原则：1197(700004)、1198(700009)、1559(700513) 已按真端设计回归 NPC AI 层（QuestStartItemNpcAi2）
 * 独立发放道具，任务系统本身保持真端 SimpleUseItem 表驱动解耦，任务定义不再包含非标的 NPC USE_OBJECT 路由。
 * 1323 由真端 SimpleTalk 表声明 acquired_npc 为交互物 (730032)，1582 为标准对话任务。
 */
class QuestStartItemDefinitionRegressionTest {

	@Test
	void startItemNpcsExposeTheDialogRoutesUsedAfterReading() throws Exception {
		for (ExpectedRoute expected : List.of(
			new ExpectedRoute(1323, 730032, "unaccepted", QuestDialogAction.USE_OBJECT.id()),
			new ExpectedRoute(1582, 700196, "started", QuestDialogAction.QUEST_SELECT.id()))) {
			CompiledQuestDefinition definition = definition(expected.questId());
			assertTrue(definition.definition().transitions().stream()
				.filter(transition -> expected.source().equals(transition.sourceNode()))
				.anyMatch(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == expected.npcId() && talk.dialogId() == expected.dialogId()),
				"quest " + expected.questId() + " NPC " + expected.npcId());
		}
	}

	private CompiledQuestDefinition definition(int questId) throws Exception {
		return ProductionQuestDefinitions.definitionInOverlay(questId);
	}

	private record ExpectedRoute(int questId, int npcId, String source, int dialogId) {
	}
}
