package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest80787To80794RetailAlignmentTest {
	private static final Map<Integer, Integer> QUEST_NPCS = Map.of(
		80787, 833671,
		80788, 833672,
		80789, 833671,
		80790, 833672,
		80791, 833673,
		80792, 833674,
		80793, 833673,
		80794, 833674);

	@Test
	void opensGivingRotundAskWindowWhenUnacceptedQuestNpcIsTalkedTo() throws Exception {
		for (Map.Entry<Integer, Integer> entry : QUEST_NPCS.entrySet()) {
			QuestDefinition definition = load(entry.getKey());
			QuestTransition startDialog = dialogRoute(definition, "unaccepted", entry.getValue(), 31);
			// P0-2 DD 尾片：接取/交付切真端规范形（页 4 / 分档窗）。未接态 QUEST_SELECT 直发接取窗
			// （页 4，真端原生相位 A）；客户端 select_none(4762) 入口页不再由服务端下发。
			// The unaccepted QUEST_SELECT pops the ask window (page 4); the client select_none
			// entry page is no longer server-driven.
			assertEquals("unaccepted", startDialog.targetNode());
			assertEquals(java.util.List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), startDialog.afterCommit());
		}
	}

	@Test
	void opensGivingRotundRewardWindowWhenAcceptedQuestNpcIsTalkedToAgain() throws Exception {
		for (Map.Entry<Integer, Integer> entry : QUEST_NPCS.entrySet()) {
			QuestTransition delivery = dialogRoute(load(entry.getKey()), "started", entry.getValue(), 31);

			// P0-2 DD 尾片：接取/交付切真端规范形（页 4 / 分档窗）。交付 NPC 的 QUEST_SELECT 直翻
			// REWARD 并下发档位奖励窗（单奖励组 → 客户端第 1 档窗口）；客户端成功页(10002)与
			// SELECT_QUEST_REWARD(1009) 中转随页链删除。
			// The reward npc's QUEST_SELECT flips REWARD with the tiered window (one group → the
			// first client window); the client success page and the 1009 hop are gone.
			assertEquals("reward", delivery.targetNode());
			assertEquals(java.util.List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), delivery.afterCommit());
		}
	}

	/** 唯一对话路由（旧页链删除后同键不得再有第二条；P0-2 DD 尾片）。 /
	 * The single dialog route: after the page chain removal a key must not carry two routes. */
	private static QuestTransition dialogRoute(QuestDefinition definition, String source, int npcId, int dialogId) {
		java.util.List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		return routes.getFirst();
	}

	/** 生产驱动定义（已退役的 XML 只在 git 历史）。 / The production-driver definition (the retired XML lives only in git history). */
	private static QuestDefinition load(int questId) {
		return ProductionQuestDefinitions.definition(questId).definition();
	}
}
