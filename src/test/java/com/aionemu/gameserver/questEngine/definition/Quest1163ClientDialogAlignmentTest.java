package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest1163ClientDialogAlignmentTest {
	private static final int START_NPC = 203096;
	private static final int ILLOS = 203151;
	private static final int REWARD_NPC = 203155;
	private static final int WORK_ITEM = 182200564;

	/**
	 * 页梯退役重锚：1163 的阶段首屏由客户端 select2 页派生，行内翻页（select2_1）是客户端本地行为
	 * （服务端零路由），推进是 `SETPRO1` ≡ `SET_SUCCEED` 两条同义边；领奖 NPC 侧由规范交付
	 * （`QUEST_SELECT` 直翻领奖态 + 奖励窗）承担，奖励态重入页（`QUEST_SELECT` / 1009 / -1）重发同一奖励窗。
	 * <p>
	 * Ladder-retirement re-anchor: the derived select2 head page, the client-local select2_1 flip, the
	 * synonymous SETPRO1/SET_SUCCEED advance, the canonical QUEST_SELECT delivery into the reward window and
	 * the reward-state re-entry pages.
	 */
	@Test
	void followsTheRetailPotionHandoffAndRewardOwner() throws Exception {
		QuestDefinition definition = load();

		assertEquals(List.of(new QuestItemRequirement(WORK_ITEM, 1)),
			definition.metadata().questWorkItems());
		assertTrue(routes(definition, "unaccepted", ILLOS).isEmpty());
		assertTrue(routes(definition, "unaccepted", REWARD_NPC).isEmpty());

		QuestTransition accept = route(definition, "unaccepted", START_NPC, QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertTrue(accept.actions().contains(new QuestAction.GiveItem(WORK_ITEM, 1)));

		// 阶段首屏 = 客户端声明的 select2 页；行内翻页 select2_1 是客户端本地行为（服务端零路由）。
		// The derived select2 head page; the select2_1 flip stays client-local (no route).
		assertPage(definition, "started", ILLOS, QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertTrue(routes(definition, "started", ILLOS, QuestDialogAction.SELECT2_1).isEmpty(),
			"the retired in-stage flip must carry no route on the handoff npc");

		// 推进 = SETPRO1 ≡ SET_SUCCEED → step1（置 var0=1 并关窗）。
		// Advance: SETPRO1 == SET_SUCCEED into step1 (var0=1 + close dialog).
		QuestTransition handoff = route(definition, "started", ILLOS, QuestDialogAction.SETPRO1);
		QuestTransition succeeded = route(definition, "started", ILLOS, QuestDialogAction.SET_SUCCEED);
		assertEquals("step1", handoff.targetNode());
		assertEquals(handoff.actions(), succeeded.actions(), "SETPRO1 and SET_SUCCEED must share the actions");
		assertEquals(handoff.afterCommit(), succeeded.afterCommit(),
			"SETPRO1 and SET_SUCCEED must share the after-commit");
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), handoff.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.CloseDialog()),
			handoff.afterCommit());

		// 领奖 NPC：规范交付直翻领奖态 + 奖励窗；奖励态重入页（QUEST_SELECT / 1009）重发同一奖励窗；
		// 接取 NPC 在领奖态无任何入口。
		// Reward npc: the canonical delivery, the reward-state window carrier and the completion edge.
		List<QuestTransition> delivery = routes(definition, "step1", REWARD_NPC, QuestDialogAction.QUEST_SELECT);
		assertEquals(1, delivery.size(), "the reward-stage delivery must be unique");
		assertEquals("reward", delivery.getFirst().targetNode());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(
			QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH), new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), delivery.getFirst().afterCommit());
		assertEquals(1, routes(definition, "step1", REWARD_NPC, QuestDialogAction.FINISH_DIALOG).size(),
			"the close-dialog exit must stay on the reward npc");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", REWARD_NPC, QuestDialogAction.QUEST_SELECT).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECT_QUEST_REWARD).afterCommit());
		assertTrue(route(definition, "reward", REWARD_NPC, QuestDialogAction.SELECTED_QUEST_REWARD1)
			.actions().contains(new QuestAction.CompleteQuest(0)));
		assertTrue(routes(definition, "reward", ILLOS).isEmpty());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action, QuestDialogPage page) {
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(page.id())),
			route(definition, source, npcId, action).afterCommit());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		List<QuestTransition> routes = routes(definition, source, npcId, action);
		assertEquals(1, routes.size(), "quest 1163 " + source + " " + npcId + " " + action);
		return routes.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId,
		QuestDialogAction action) {
		return routes(definition, source, npcId).stream()
			.filter(transition -> Integer.valueOf(action.id()).equals(
				((QuestEvent.TalkToNpc) transition.event()).dialogId()))
			.toList();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static QuestDefinition load() throws Exception {
		return ProductionQuestDefinitions.definitionInOverlay(1163).definition();
	}
}
