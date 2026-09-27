package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30314RetailAlignmentTest {
	@Test
	void setRewardRequiresAndConsumesTheCertificationItems() throws Exception {
		{
			// 退役任务的生产 XML 只在 git 历史里：取生产视图。
			QuestDefinition definition = ProductionQuestDefinitions.definition(30314).definition();
			QuestTransition setReward = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(10255).equals(talk.dialogId()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new QuestCondition.HasItem(186000098, 100)), setReward.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(186000098, 100)),
				setReward.actions());
		}
	}
}
