package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 50009 将客户端标准接取与报告、领奖路由限定在正式 NPC owner，修复迁移双重 owner。
 * Verifies quest 50009 confines its standard acceptance, report, and reward routes to the retail NPC owner.
 */
class Quest50009ClientDialogAlignmentTest {
	private static final int START_NPC = 831032;

	@Test
	void keepsTheRetailStandardStartReportAndRewardOwnersExclusive() {
		QuestDefinition definition = load().definition();

		assertTrue(definition.metadata().prerequisites().isEmpty());
		assertTrue(definition.metadata().startConditions().isEmpty());
		assertNode(definition, "unaccepted", QuestStatus.NONE, Map.of("var0", 0));
		assertNode(definition, "started", QuestStatus.START, Map.of("var0", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 0));
		assertNode(definition, "complete", QuestStatus.COMPLETE, Map.of("var0", 0));

		// P0-3 S1：SimpleTalk 接取切真端规范形——QUEST_SELECT 直发询问窗（页 4）；
		// select1 简报页（1011）与 ASK_QUEST_ACCEPT(1007) 中转随页链退场。
		// P0-3 S1: the SimpleTalk accept switches to the retail canonical shape — QUEST_SELECT
		// emits the ask-accept window (page 4); the select1 letter page (1011) and the
		// ASK_QUEST_ACCEPT(1007) hop retire with the page chain.
		QuestTransition offer = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(offer, "unaccepted", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())));

		// P0-3 S1：1007 中转不再由服务端驱动（canonical 接取流直发页 4），零残留负控。
		// P0-3 S1: the ASK_QUEST_ACCEPT(1007) hop is no longer server-driven (the canonical
		// accept flow emits page 4 directly); zero-residue control.
		assertTrue(routes(definition, "unaccepted", START_NPC).stream()
			.noneMatch(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(START_NPC, QuestDialogAction.ASK_QUEST_ACCEPT.id()))));

		QuestTransition accept = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_1);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())), accept.afterCommit());
		assertNull(accept.priority());

		QuestTransition refuse = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_REFUSE_1);
		assertContract(refuse, "unaccepted", List.of(), List.of(
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_REFUSE_1.id())));

		// P0-3 S1：交付切真端规范形——QUEST_SELECT 带门（本行无 item_check = 空门）直翻 REWARD
		// 并下发档位奖励窗；报告页 SELECT5(2375) 与 SELECT_QUEST_REWARD(1009) 中转随页链退场，
		// 未集齐时零路由（关窗兜底交 DialogService）。本行接取 NPC 与交付 NPC 同为 Santa。
		// P0-3 S1: the delivery switches to the retail canonical shape — the gated QUEST_SELECT
		// (no item_check on this row = empty gate) flips REWARD and shows the tiered reward
		// window; the SELECT5 report page and the SELECT_QUEST_REWARD(1009) hop retire with the
		// page chain, so an incomplete hand-in has no route at all.
		int deliveryWindow = QuestDialogPage.rewardWindowForTier(
			definition.metadata().rewardGroups().size() - 1)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1).id();
		QuestTransition delivery = route(definition, "started", START_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(delivery, "reward", List.of(), List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow)));
		assertNoLegacyPageChainResidue(definition, START_NPC);

		for (QuestDialogAction previewAction : List.of(
			QuestDialogAction.USE_OBJECT, QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = route(definition, "reward", START_NPC, previewAction);
			assertContract(preview, "reward", List.of(), List.of(
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		}

		List<QuestTransition> completionRoutes = routes(definition, "reward", START_NPC).stream()
			.filter(transition -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) transition.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertContract(completion, "complete", List.of(
				new QuestAction.GrantReward("ITEM", 160010201, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 160010202, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.GrantReward("ITEM", 186000177, 1, QuestRewardAmountMode.EXACT),
				new QuestAction.CompleteQuest(0)), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		}

		// no stray owner 831038
		assertTrue(routes(definition, "unaccepted", 831038).isEmpty());
		assertTrue(routes(definition, "started", 831038).isEmpty());
		assertTrue(routes(definition, "reward", 831038).isEmpty());
	}

	private static void assertContract(QuestTransition transition, String target,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		assertEquals(target, transition.targetNode());
		assertEquals(List.of(), transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
		assertNull(transition.priority());
	}

	private static QuestTransition route(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action) {
		List<QuestTransition> matches = routes(definition, source, npcId).stream()
			.filter(transition -> transition.event().equals(
				new QuestEvent.TalkToNpc(npcId, action.id())))
			.toList();
		assertEquals(1, matches.size(), "quest 50009 " + source + " " + npcId + " " + action);
		return matches.getFirst();
	}

	private static List<QuestTransition> routes(QuestDefinition definition, String source, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId)
			.toList();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	/**
	 * 旧页链零残留（P0-3 S1）：交付段（started→reward）不得有带优先级的路由，定义内不得再下发
	 * SELECT5/SELECT6 页，started 态不得残留 1009/39/20002 交付路由（完成流的 reward→complete
	 * 类/槽位路由本来就带优先级，不是残留）。
	 * Zero legacy page-chain residue (P0-3 S1): no priority-carrying delivery route, no SELECT5/SELECT6
	 * page push, and no started-state 1009/39/20002 hand-in route (the completion flow's
	 * reward→complete class/slot routes legitimately carry priorities and are not residue).
	 */
	private static void assertNoLegacyPageChainResidue(QuestDefinition definition, int rewardNpc) {
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			"started".equals(transition.sourceNode()) && "reward".equals(transition.targetNode())
				&& transition.priority() != null),
			"the started->reward delivery segment must be priority-free");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			transition.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.ShowQuestDialog page
					&& (page.dialogId() == QuestDialogPage.SELECT5.id()
						|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
			"the retired SELECT5/SELECT6 pages must not be pushed");
		assertTrue(definition.transitions().stream().noneMatch(transition ->
			"started".equals(transition.sourceNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == rewardNpc
				&& talk.dialogId() != null
				&& (talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()
					|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
					|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
			"the started-state 1009/39/20002 hand-in routes must not survive");
	}

	private static CompiledQuestDefinition load() {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(50009);
	}
}
