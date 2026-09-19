package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 锁定任务 10501「被毁的遗迹 / Research the Ruins」上交龙族证物后的对话续接合同。
 * Locks the post hand-over dialog continuation contract of quest 10501.
 *
 * <p>Aion 5.8 客户端把 check_user_item_ok(10000) 页的唯一按钮渲染为 {@code HACTION_FINISH_DIALOG}（"递过证物"）；
 * 真实客户端点击后只关闭窗口并发送 {@code CM_CLOSE_DIALOG}，服务端收不到 FINISH_DIALOG(1008) 任务动作
 * （2026-09-19 实机 trace：10000 之后没有 CM_DIALOG_SELECT 1008，玩家必须重新对话才收到 10002）。因此该页
 * 不能作为服务端续接点：上交成功分支直接下发本任务的 select_success(10002) 报告页，由其
 * SELECT_QUEST_REWARD(1009) 打开奖励窗口，玩家不需要再对话一次。
 * The Aion 5.8 client renders the only check_user_item_ok(10000) button as HACTION_FINISH_DIALOG ("hand over the
 * evidence"); a real client only closes the window and sends CM_CLOSE_DIALOG, so the server never receives a
 * FINISH_DIALOG(1008) quest action (live trace 2026-09-19: no CM_DIALOG_SELECT 1008 after page 10000, and the
 * player had to re-open the dialogue to reach page 10002). That page therefore cannot carry a server continuation:
 * the hand-over success branch shows this quest's own select_success(10002) report page, whose
 * SELECT_QUEST_REWARD(1009) opens the reward window, so the player does not have to re-open the dialogue.</p>
 */
class Quest10501HandoverContinuationTest {
	private static final int HAND_OVER_NPC_ID = 804700;
	private static final int DRAGON_RELIC = 182215599;
	private static final int RELIC_SCANNER = 182215598;
	private static final int RELIC_DROP_NPC_ID = 236251;

	@Test
	void handOverSuccessShowsTheReportPageInsteadOfTheClientClosedConfirmationPage() throws Exception {
		QuestDefinition definition = definition();

		QuestTransition handOver = transition(definition, "s6", "reward",
			new QuestEvent.TalkToNpc(HAND_OVER_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 6),
			new QuestCondition.HasItem(DRAGON_RELIC, 1, true)), handOver.conditions());
		assertEquals(List.of(
			new QuestAction.RemoveItem(DRAGON_RELIC, 1),
			new QuestAction.SetVariable("var0", 7)), handOver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			handOver.afterCommit());

		// 客户端本地关闭页（10000）只关闭窗口、不回传任务动作，任何下发都会把对话停在死端。
		// Displaying the client-local confirmation page (10000) always ends the conversation on a page
		// whose only button is a local close, so it must not be emitted anywhere in this quest.
		assertFalse(displaysPage(definition, QuestDialogPage.CHECK_USER_ITEM_OK.id()),
			"quest 10501 must not display the client-local check_user_item_ok page");

		// 物品不足时仍然下发失败确认页（10001），该页的关闭按钮保留 SELECT_QUEST 落点。
		// When the relic is missing the fail confirmation page stays, and its close button keeps the
		// SELECT_QUEST landing.
		QuestTransition itemMissing = transition(definition, "s6", "s6",
			new QuestEvent.TalkToNpc(HAND_OVER_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()), 1);
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 6)), itemMissing.conditions());
		assertEquals(List.of(), itemMissing.actions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			itemMissing.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			talk(definition, "s6", "s6", HAND_OVER_NPC_ID, QuestDialogAction.FINISH_DIALOG.id()).afterCommit(),
			"the fail page close button keeps its SELECT_QUEST landing");

		// 领奖链：上交后落的页 = REWARD 态入口页，其 1009 打开第 1 档奖励窗。
		// Reward chain: the handed-over page equals the REWARD-state entry page, whose 1009 opens
		// reward window 1.
		QuestTransition rewardEntry = talk(definition, "reward", "reward", HAND_OVER_NPC_ID,
			QuestDialogAction.USE_OBJECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.DEFAULT_SUCCESS.id())),
			rewardEntry.afterCommit());
		assertEquals(showedPage(handOver), showedPage(rewardEntry),
			"hand-over continuation and reward entry must display the same report page");
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			talk(definition, "reward", "reward", HAND_OVER_NPC_ID,
				QuestDialogAction.SELECT_QUEST_REWARD.id()).afterCommit());

		// 六条可选奖励分支：三条固定奖励 + 本条可选奖励 + 完成标记，after-commit 固定三段。
		// Six selectable reward branches: three fixed rewards, this branch's selectable reward and the
		// completion marker, with a fixed three-step after-commit.
		List<QuestTransition> completions = definition.transitions().stream()
			.filter(candidate -> "reward".equals(candidate.sourceNode())
				&& "complete".equals(candidate.targetNode()))
			.toList();
		assertEquals(6, completions.size(), "quest 10501 must keep its six selectable reward branches");
		assertEquals(List.of(
			QuestDialogAction.SELECTED_QUEST_REWARD1.id(), QuestDialogAction.SELECTED_QUEST_REWARD2.id(),
			QuestDialogAction.SELECTED_QUEST_REWARD3.id(), QuestDialogAction.SELECTED_QUEST_REWARD4.id(),
			QuestDialogAction.SELECTED_QUEST_REWARD5.id(), QuestDialogAction.SELECTED_QUEST_REWARD6.id()),
			completions.stream().map(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).dialogId()).toList());
		List<QuestAction> fixedRewards = List.of(
			new QuestAction.GrantReward("EXP", 0, 22916836, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.GrantReward("ITEM", 186000231, 25, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 186000237, 60, QuestRewardAmountMode.EXACT));
		assertEquals(List.of(
			new QuestAction.GrantReward("ITEM", 113101245, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 113301247, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 113301492, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 113501232, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 113501621, 1, QuestRewardAmountMode.EXACT),
			new QuestAction.GrantReward("ITEM", 113601185, 1, QuestRewardAmountMode.EXACT)),
			completions.stream().map(completion -> completion.actions().get(3)).toList(),
			"every reward branch keeps its own selectable reward");
		for (QuestTransition completion : completions) {
			assertEquals(5, completion.actions().size(),
				"three fixed rewards, one selectable reward and the completion marker");
			assertEquals(fixedRewards, completion.actions().subList(0, 3));
			assertEquals(new QuestAction.CompleteQuest(0), completion.actions().get(4));
			assertEquals(List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				completion.afterCommit());
		}
	}

	@Test
	void relicHandOverKeepsItemWorkItemAndDropContract() throws Exception {
		QuestDefinition definition = definition();
		assertEquals(List.of(new QuestItemRequirement(DRAGON_RELIC, 1)),
			definition.metadata().itemRequirements());
		assertEquals(List.of(new QuestItemRequirement(RELIC_SCANNER, 1)),
			definition.metadata().questWorkItems());
		assertEquals(List.of(new QuestDrop(RELIC_DROP_NPC_ID, DRAGON_RELIC, 100, true, 6)),
			definition.metadata().drops());
		assertEquals("reward", transition(definition, "s6", "reward",
			new QuestEvent.TalkToNpc(HAND_OVER_NPC_ID, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()))
			.targetNode());
	}

	private static boolean displaysPage(QuestDefinition definition, int pageId) {
		return definition.transitions().stream()
			.flatMap(transition -> transition.afterCommit().stream())
			.anyMatch(action -> action instanceof AfterCommitAction.ShowQuestDialog show && show.dialogId() == pageId);
	}

	private static int showedPage(QuestTransition transition) {
		return transition.afterCommit().stream()
			.filter(action -> action instanceof AfterCommitAction.ShowQuestDialog)
			.map(action -> ((AfterCommitAction.ShowQuestDialog) action).dialogId())
			.findFirst().orElseThrow();
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& event.equals(candidate.event()))
			.findFirst().orElseThrow();
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event, int priority) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()) && target.equals(candidate.targetNode())
				&& event.equals(candidate.event()) && Integer.valueOf(priority).equals(candidate.priority()))
			.findFirst().orElseThrow();
	}

	private static QuestDefinition definition() throws Exception {
		try (InputStream input = Quest10501HandoverContinuationTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/10501.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition 10501.xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
