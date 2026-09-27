package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class Quest30312RetailAlignmentTest {
	@Test
	void dropsAndEveryTurnInMatchRetailItemRequirements() throws Exception {
		{
			// 退役任务的生产 XML 只在 git 历史里：取生产视图。
			QuestDefinition definition = ProductionQuestDefinitions.definition(30312).definition();
			// P0c-25 裁定（真端对、XML 错）：真端 drop 族未声明 drop_each_member → mapper 生产
			// 约定缺省 true（GROUP 掉落，既定通道/全族先例）；遗留 XML 的 each-member="false"
			// 是手工值，随 XML 退役。
			// P0c-25 adjudication (retail-right, XML-wrong): the retail drop family omits
			// drop_each_member, so the mapper's production default applies (GROUP drops); the
			// legacy XML's each-member="false" was hand-made and is retired with the XML.
			assertEquals(Set.of(
				new QuestDrop(216007, 182209715, 50, true, 0),
				new QuestDrop(216008, 182209715, 50, true, 0)
			), Set.copyOf(definition.metadata().drops()));

			// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）——交付由 canonicalDelivery 承担：
			// QUEST_SELECT(started→reward) 带真端 collect_item1 整组门（quest_30312a×20）直翻领奖并下发
			// 单档奖励窗 1；P0c-25 锁定的 select5(2375) 报告页与 39/20002 双变体检查对随页链整体退场
			// （未集齐零路由，关窗兜底交 DialogService）。
			// P0-3 S1: the delivery is canonicalDelivery — a gated QUEST_SELECT(started->reward) carrying the
			// whole retail collect_item1 group (quest_30312a x20) that flips REWARD and shows the single-tier
			// reward window 1; the select5(2375) report page and the 39/20002 alias pair locked by P0c-25 are
			// retired with the page chain (an incomplete hand-in has no route; DialogService closes).
			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.toList();
			assertEquals(1, turnIns.size());
			QuestTransition turnIn = turnIns.getFirst();
			assertEquals(QuestDialogAction.QUEST_SELECT.id(),
				((QuestEvent.TalkToNpc) turnIn.event()).dialogId());
			assertNull(turnIn.priority());
			assertEquals(List.of(new QuestCondition.HasItem(182209715, 20)), turnIn.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(182209715, 20)), turnIn.actions());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				turnIn.afterCommit());
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
				"30312 retired the 39/20002 check pair");
			assertFalse(definition.transitions().stream().anyMatch(transition ->
				transition.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT5.id()
							|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
				"30312 retired the SELECT5/SELECT6 report pages");
		}
	}
}
