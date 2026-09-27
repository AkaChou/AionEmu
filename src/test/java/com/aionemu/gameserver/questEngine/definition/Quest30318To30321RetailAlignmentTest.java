package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest30318To30321RetailAlignmentTest {
	@Test
	void preservesTheRetailPrerequisiteNpcRouteAndRewardChain() throws Exception {
		assertQuest(30318, 30316, 799504,
			Set.of(216276, 216277, 216278, 216279), 25,
			Set.of(new QuestReward("GOLD", 0, 28900), new QuestReward("EXP", 0, 6517414)));
		assertQuest(30319, 30318, 799504, Set.of(216241), 1,
			Set.of(new QuestReward("GOLD", 0, 197550), new QuestReward("EXP", 0, 6517414),
				new QuestReward("ITEM", 182209718, 1)));
		assertQuest(30320, 30316, 799505,
			Set.of(216280, 216281, 216282, 216283, 216284), 25,
			Set.of(new QuestReward("GOLD", 0, 28900), new QuestReward("EXP", 0, 6517414)));
		assertQuest(30321, 30320, 799505,
			Set.of(216161, 216162, 216242, 216243), 1,
			Set.of(new QuestReward("GOLD", 0, 197550), new QuestReward("EXP", 0, 6517414),
				new QuestReward("ITEM", 182209719, 1)));
	}

	private static void assertQuest(int questId, int prerequisite, int npcId,
		Set<Integer> expectedKillNpcIds, int expectedKillCount, Set<QuestReward> expectedRewards) throws Exception {
		QuestDefinition definition = load(questId);
		/* 前置任务两种表达等价：XML 写 finished 启动条件，真端表 `finished_quest_cond` 无后缀且无其它条件族时
		   编译为 prerequisites（RetailQuestMetadataCompiler 的归属规则）。二者语义相同，断言接受两种表达。
		   The prerequisite is expressed either way: XML as a finished start condition, retail (plain
		   finished_quest_cond without other condition families) as a prerequisite entry. */
		assertTrue(definition.metadata().startConditions()
				.contains(new QuestStartCondition("finished", prerequisite, 0))
			|| definition.metadata().prerequisites().contains(prerequisite),
			() -> "quest " + questId + " must require finished " + prerequisite);
		assertEquals(expectedRewards, Set.copyOf(definition.metadata().rewards()));
		assertTrue(definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.allMatch(id -> id == npcId));

		Set<Integer> declaredKillNpcIds = new HashSet<>();
		for (int id : expectedKillNpcIds) {
			assertEquals(expectedKillCount, killRouteCount(definition, id),
				"unexpected kill count for quest " + questId + " NPC " + id);
			declaredKillNpcIds.add(id);
		}
		assertTrue(killNpcIds(definition).containsAll(declaredKillNpcIds));

		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(8).equals(talk.dialogId())));
	}

	private static QuestDefinition load(int questId) throws Exception {
		// P0c-8c（2026-09-24）：30319 已由真端 SimpleHunt 网格驱动（退役 XML），统一取生产视图。
		// Retired quests live in git history only: use the production view (XML dir + retail overlay).
		return ProductionQuestDefinitions.definition(questId).definition();
	}

	private static long killRouteCount(QuestDefinition definition, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc(int id)
				? id == npcId
				: transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)
					&& npcIds.contains(npcId))
			.count();
	}

	private static Set<Integer> killNpcIds(QuestDefinition definition) {
		Set<Integer> ids = new HashSet<>();
		definition.transitions().forEach(transition -> {
			if (transition.event() instanceof QuestEvent.KillNpc(int npcId)) {
				ids.add(npcId);
			} else if (transition.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)) {
				ids.addAll(npcIds);
			}
		});
		return ids;
	}
}
