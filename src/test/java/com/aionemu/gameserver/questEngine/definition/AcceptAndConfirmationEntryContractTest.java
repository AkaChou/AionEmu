package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定两类接取入口合同：
 * 1. 简报式 HTML（无 4/1003/1004 页）的区域/事件任务：QUEST_SELECT 显示 select_none 并带
 *    start-eligible（repeatable 别名复用），FINISH_DIALOG 关闭；接取由区域/事件入口完成。
 * 2. check 族确认页：上交成功分支显示 CHECK_USER_ITEM_OK，确认按钮关闭对话或打开奖励窗口。
 * Locks two accept-entry contracts:
 * 1. Briefing-only HTML (no pages 4/1003/1004) for zone/event-start quests: QUEST_SELECT shows
 *    select_none with start-eligible (repeat aliasing) and FINISH_DIALOG closes; the zone or
 *    event route performs the acceptance.
 * 2. Check-family confirmation pages: the hand-over success branch shows CHECK_USER_ITEM_OK and
 *    the confirmation button closes the dialog or opens the reward window.
 */
class AcceptAndConfirmationEntryContractTest {
	private static final Path QUEST_DIRECTORY = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests");

	@Test
	void briefingOnlySelectNoneStaysOnUnacceptedAndCloses() throws Exception {
		QuestDefinition definition = compile(1877);
		// 简报对：QUEST_SELECT 带 start-eligible 显示 select_none，不接取。
		// The briefing pair: QUEST_SELECT carries start-eligible and shows select_none without
		// accepting.
		QuestTransition briefing = talkRoute(definition, "unaccepted", 278503, 31);
		assertEquals(List.of(new QuestCondition.StartEligible()), briefing.conditions(),
			"quest 1877 briefing must stay start-eligible gated");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(4762)), briefing.afterCommit(),
			"quest 1877 briefing must show select_none");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 278503
					&& "started".equals(transition.targetNode())),
			"quest 1877 dialog must never accept; the zone route owns NONE -> START");
		// 履行委托按钮关闭对话。
		// The accept-commission button closes the dialog.
		QuestTransition finish = talkRoute(definition, "unaccepted", 278503, 1008);
		assertEquals(List.of(new QuestCondition.StartEligible()), finish.conditions(),
			"quest 1877 finish must stay start-eligible gated");
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), finish.afterCommit(),
			"quest 1877 finish must close");
	}

	@Test
	void checkHandOverShowsTheClientConfirmationPageThenClosesOrClaims() throws Exception {
		// 1636：39 成功分支 v1->v2 显示确认页，v2 的 FINISH_DIALOG 关闭，领奖经 REWARD 态入口。
		// 1636: the 39 success branch v1->v2 shows the confirmation page, v2 FINISH closes, and
		// the reward is claimed through the REWARD-state entry.
		QuestDefinition definition = compile(1636);
		QuestTransition success = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("v1")
				&& transition.targetNode().equals("v2")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203792 && talk.dialogId() == 39)
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.HasItem(182201786, 1)), success.conditions(),
			"quest 1636 hand-over conditions");
		assertEquals(List.of(new QuestAction.RemoveItem(182201786, 1)), success.actions(),
			"quest 1636 hand-over removes the spirit");
		assertTrue(success.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(10000)),
			"quest 1636 hand-over must show check_user_item_ok");
		QuestTransition confirm = talkRoute(definition, "v2", 203792, 1008);
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), confirm.afterCommit(),
			"quest 1636 confirmation button must close");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
				transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 203792
					&& talk.dialogId() == 1009 && "v2".equals(transition.sourceNode())),
			"quest 1636 reward claim belongs to the REWARD-state preview, not the ok page");

		// 15010：NPC_REPORT 检查对——1009 命中物品进 REWARD+奖励窗，未命中显示 fail 页。
		// 15010: the explicit 1009 check pair - with the items REWARD + reward window, without
		// them the fail page.
		QuestDefinition report = compile(15010);
		List<QuestTransition> claims = report.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 804700 && talk.dialogId() == 1009
				&& "started".equals(transition.sourceNode()))
			.toList();
		assertEquals(2, claims.size(), "quest 15010 check branch count");
		QuestTransition hit = claims.stream()
			.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
			.findFirst().orElseThrow();
		QuestTransition miss = claims.stream()
			.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
			.findFirst().orElseThrow();
		assertEquals("reward", hit.targetNode(), "quest 15010 hit target");
		assertEquals(List.of(new QuestCondition.HasItem(182215666, 7)), hit.conditions(),
			"quest 15010 hit conditions");
		assertEquals(List.of(new QuestAction.RemoveItem(182215666, 7)), hit.actions(),
			"quest 15010 hit removes the badges");
		assertTrue(hit.afterCommit().contains(
			new AfterCommitAction.ShowQuestDialog(5)), "quest 15010 hit opens reward window 1");
		assertEquals("started", miss.targetNode(), "quest 15010 miss target");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)), miss.afterCommit(),
			"quest 15010 miss shows check_user_item_fail");
		QuestTransition missClose = talkRoute(report, "started", 804700, 1008);
		assertEquals(List.of(new AfterCommitAction.CloseDialog()), missClose.afterCommit(),
			"quest 15010 fail-page button must close");
	}

	private static QuestDefinition compile(int questId) throws Exception {
		try (InputStream input = Files.newInputStream(QUEST_DIRECTORY.resolve(questId + ".xml"))) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static QuestTransition talkRoute(QuestDefinition definition, String source, int npcId,
			int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.findFirst().orElseThrow();
	}
}
