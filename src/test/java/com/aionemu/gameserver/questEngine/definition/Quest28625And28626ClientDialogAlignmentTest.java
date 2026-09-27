package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 28625/28626 在卡里加陈列柜上使用钥匙的客户端对话合同。
 * Verifies the client dialog contract for using the key at the Kaliga display cabinets in quests 28625/28626.
 */
class Quest28625And28626ClientDialogAlignmentTest {
	private static final int KALIGA_KEY_ID = 185000102;
	private static final int KALIGA_BOSS_ID = 217006;
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(28625, 730333),
		new QuestCase(28626, 730334));

	@Test
	void reportsTheKaligaCollectionAtTheDisplayCabinet() throws Exception {
		for (QuestCase questCase : CASES) {
			QuestDefinition definition = load(questCase.questId()).definition();
			assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
			assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));

			// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——接取走 canonicalAcceptFlow
			// （QUEST_SELECT 直发接取窗页 4，无 1007 中转与页梯），交付走 canonicalDelivery
			// （QUEST_SELECT(started→reward) 带陈列柜钥匙整组门直翻领奖并下发单档奖励窗 1）；START 自环的
			// SELECT5 报告入口页、39/20002 检查对与 SELECT6 失败页整体退场，未集齐零路由。
			// P0-3 S1: the SimpleTalk accept/delivery segments take the retail canonical shape (page 4 /
			// tiered window) — the accept is canonicalAcceptFlow (QUEST_SELECT opens the ask window, page 4)
			// and the delivery is canonicalDelivery (a gated QUEST_SELECT(started->reward) carrying the whole
			// cabinet-key group, showing the single-tier reward window 1); the START self-loop SELECT5 report
			// entry, the 39/20002 check pair and the SELECT6 failure page are gone.
			QuestTransition accept = route(definition, "unaccepted", "unaccepted",
				new QuestEvent.TalkToNpc(questCase.cabinetId(), QuestDialogAction.QUEST_SELECT.id()), null);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), accept.afterCommit());

			QuestTransition deliver = route(definition, "started", "reward",
				new QuestEvent.TalkToNpc(questCase.cabinetId(), QuestDialogAction.QUEST_SELECT.id()), null);
			assertNull(deliver.priority(), "quest " + questCase.questId() + " delivery route priority");
			assertEquals(List.of(new QuestCondition.HasItem(KALIGA_KEY_ID, 1)), deliver.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(KALIGA_KEY_ID, 1)), deliver.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				deliver.afterCommit());

			assertFalse(definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
				"quest " + questCase.questId() + " retired the 39/20002 check pair");
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT5.id()
							|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
				"quest " + questCase.questId() + " retired the SELECT5/SELECT6 report pages");
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				"started".equals(transition.sourceNode())
					&& transition.event().equals(new QuestEvent.TalkToNpc(KALIGA_BOSS_ID,
						QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()))),
				"quest " + questCase.questId() + " must not check the key at Kaliga");
		}
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
		QuestEvent event, Integer priority) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.filter(candidate -> priority == null || priority.equals(candidate.priority()))
			.toList();
		assertEquals(1, routes.size(), source + " -> " + target + " " + event);
		return routes.getFirst();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
		Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label()))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}

	private record QuestCase(int questId, int cabinetId) {
	}
}
