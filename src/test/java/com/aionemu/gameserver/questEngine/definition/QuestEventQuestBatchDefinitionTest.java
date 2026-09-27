package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 80008/80009（Cake 系, 收集 1 扣物完成）与 80028/80031/80032（Fayrefolk 系, 对话即完成）:
 * 事件激活自动弃任（level-up + event-active(false) + abandon-quest）、1009/23 进 REWARD。
 * 依据真端 quest.xml / Quest_SimpleUseItem、quest_data.xml 工作物品与客户端 1009 按钮。
 */
class QuestEventQuestBatchDefinitionTest {


	@Test
	void cakeQuestsMatchQuestDataAndRetail() throws Exception {
		assertCake(80008, "Q80008", 1180008, "ELYOS", 182214006, 798415);
		assertCake(80009, "Q80009", 1180009, "ASMODIANS", 182214007, 798417);
	}

	@Test
	void cakeQuestsRemoveWorkItemOnQuestSelectDelivery() throws Exception {
		for (int questId : new int[] {80008, 80009}) {
			int itemId = questId == 80008 ? 182214006 : 182214007;
			QuestDefinition definition = load(questId);
			// P0-2 规范形交付：QUEST_SELECT 带工作物品门控直翻 REWARD（1009 中转与 SELECT5
			// 失败页随页链删除，未集齐零路由，关窗兜底）。
			// P0-2 canonical delivery: QUEST_SELECT gated on the work item flips REWARD (the 1009
			// hop and SELECT5 failure page are gone; an incomplete hand-in has no route).
			QuestTransition deliver = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode())
					&& "reward".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() != 0 && QuestDialogAction.QUEST_SELECT.id() == talk.dialogId())
				.findFirst().orElseThrow();
			assertEquals(List.of(new QuestCondition.HasItem(itemId, 1)), deliver.conditions(),
				"quest " + questId + " delivery conditions");
			assertEquals(List.of(
					new QuestAction.RemoveItem(itemId, 1), new QuestAction.SetVariable("var0", 1)),
				deliver.actions(), "quest " + questId + " delivery removals");
			assertTrue(definition.transitions().stream().noneMatch(transition ->
					"started".equals(transition.sourceNode()) && "started".equals(transition.targetNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc talk
						&& QuestDialogAction.SELECT_QUEST_REWARD.id() == talk.dialogId()),
				"quest " + questId + " canonical removed the 1009 failure self-loop");
		}
	}

	@Test
	void fayrefolkQuestsEnterRewardWithoutCollecting() throws Exception {
		for (int questId : new int[] {80028, 80031, 80032}) {
			QuestDefinition definition = load(questId);
			for (int dialogId : new int[] {1009, 23}) {
				assertTrue(definition.transitions().stream().anyMatch(transition ->
					"started".equals(transition.sourceNode())
						&& "reward".equals(transition.targetNode())
						&& transition.event() instanceof QuestEvent.TalkToNpc talk
						&& Integer.valueOf(dialogId).equals(talk.dialogId())
						&& transition.actions().isEmpty()),
					"quest " + questId + " dialog " + dialogId + " must enter REWARD without actions (no collectibles)");
			}
		}
	}

	@Test
	void levelUpAbandonsWhenEventInactive() throws Exception {
		for (int questId : new int[] {80008, 80009, 80028, 80031, 80032}) {
			QuestDefinition definition = load(questId);
			assertTrue(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& "unaccepted".equals(transition.targetNode())
					&& transition.event() instanceof QuestEvent.LevelUp
					&& transition.conditions().contains(new QuestCondition.EventActive(false))
					&& transition.actions().contains(new QuestAction.AbandonQuest())),
				"quest " + questId + " must abandon on level-up when the event is inactive");
		}
	}

	@Test
	void fayrefolkMetadataMatchesQuestDataAndRetail() throws Exception {
		assertFayrefolk(80028, "[Event] Meet The Fayrefolk", 1180028, "ELYOS", 10, 799766, 169610036, 1);
		assertFayrefolk(80031, "[Event] The Fayrefolk", 1180031, "ASMODIANS", 10, 799781, 169610036, 1);
		assertFayrefolk(80032, "[Event] A Charmed Existence", 1180032, "ASMODIANS", 15, 799781, 188051133, 10);
	}

	private static void assertCake(int questId, String name, int displayNameId, String race,
			int workItem, int npcId) throws Exception {
		QuestDefinition definition = load(questId);
		QuestMetadata metadata = definition.metadata();
		assertEquals(questId, definition.id());
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(10, metadata.minLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertEquals(10, metadata.repeatPolicy().maxRepeatCount());
		assertTrue(metadata.cannotShare());
		assertEquals(List.of(new QuestItemRequirement(workItem, 1)), metadata.questWorkItems());
		assertEquals(List.of(new QuestReward("GOLD", 0, 20000L),
			new QuestReward("EXP", 0, 10000L),
			new QuestReward("ITEM", 160010100, 5L),
			new QuestReward("ITEM", 164002019, 3L)), metadata.rewards());
		assertEquals(Set.of(npcId), definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}

	private static void assertFayrefolk(int questId, String name, int displayNameId, String race,
			int minLevel, int npcId, int rewardItem, int rewardCount) throws Exception {
		QuestDefinition definition = load(questId);
		QuestMetadata metadata = definition.metadata();
		assertEquals(questId, definition.id());
		assertEquals(name, metadata.name());
		assertEquals(displayNameId, metadata.displayNameId());
		assertEquals(minLevel, metadata.minLevel());
		assertEquals("EVENT", metadata.category());
		assertEquals(Set.of(race), metadata.permittedRaces());
		assertEquals(1, metadata.repeatPolicy().maxRepeatCount());
		assertTrue(metadata.cannotShare());
		assertTrue(metadata.itemRequirements().isEmpty(), "Fayrefolk quests have no collectibles");
		assertEquals(List.of(new QuestReward("ITEM", rewardItem, rewardCount)), metadata.rewards());
		assertEquals(Set.of(npcId), definition.transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc)
			.map(t -> ((QuestEvent.TalkToNpc) t.event()).npcId())
			.collect(java.util.stream.Collectors.toSet()));
	}

	private static QuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definitionInOverlay(questId).definition();
	}
}
