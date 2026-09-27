package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务 28800 将客户端简易接取与报告、领奖路由限定在各自的正式 NPC owner。
 * Verifies quest 28800 confines its simple acceptance, report, and reward routes to their retail NPC owners.
 * P0c-20 裁定（真端对、XML 错）：真端表接取 Randiten(798459)/交付 Harretion(830532) 即形状权威
 * （模板索引同对）；单步族形里接取 NPC 的 started 态只有 FINISH_DIALOG 出口，进度对话与 1009
 * 上交只属于报告 NPC——遗留 XML 把两者额外挂在 798459 与第三 NPC 204191(Doman) 上，客户端
 * 登记（dic 链交付集/交付型/信件型）无背书；双生子 18800/18801/28801 已按同形采纳。
 * P0c-20 adjudication (retail-right, XML-wrong): the retail acquire/report NPC split is the
 * shape authority; in the single-step family shape the acquire NPC's started state has only
 * the FINISH_DIALOG exit — progress dialog and 1009 hand-in belong to the report NPC alone.
 * The legacy XML hung both on 798459 and the third NPC 204191 (Doman) without client registry
 * backing; twins 18800/18801/28801 already run this shape.
 */
class Quest28800ClientDialogAlignmentTest {
	private static final int START_NPC = 798459;
	private static final int REPORT_NPC = 830532;

	@Test
	void keepsTheRetailSimpleStartReportAndRewardOwnersExclusive() {
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

		QuestTransition accept = route(definition, "unaccepted", START_NPC,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE);
		assertEquals("started", accept.targetNode());
		assertEquals(List.of(new QuestCondition.StartEligible()), accept.conditions());
		assertEquals(List.of(), accept.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), accept.afterCommit());
		assertNull(accept.priority());

		// 真端形状（P0c-20 裁定，翻转态探针直证）：接取 NPC 的 started 态只有 FINISH_DIALOG
		// 出口（回任务选择页）；进度对话（QUEST_SELECT→select5）只属于报告 NPC。遗留 XML 的
		// started 进度对话（select5 挂接取 NPC）与 204191 SELECT2 辅助路径 = 手工形，随 XML 退役。
		// Retail shape (P0c-20 adjudication, probe-verified in the flipped state): the acquire
		// NPC's started state has only the FINISH_DIALOG exit; the progress dialog belongs to
		// the report NPC. The legacy XML's started progress dialog on the acquire NPC and the
		// 204191 SELECT2 helper path were hand-made and are retired with the XML.
		QuestTransition startedKeep = route(definition, "started", START_NPC,
			QuestDialogAction.FINISH_DIALOG);
		assertContract(startedKeep, "started", List.of(), List.of(
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		assertTrue(routes(definition, "started", START_NPC).stream().noneMatch(transition ->
			((QuestEvent.TalkToNpc) transition.event()).dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			"quest 28800 acquire NPC must not own a started progress dialog");
		assertTrue(routes(definition, "reward", START_NPC).stream().noneMatch(t ->
			((QuestEvent.TalkToNpc) t.event()).dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()));

		assertTrue(routes(definition, "unaccepted", REPORT_NPC).isEmpty());
		// P0-3 S1：交付切真端规范形——QUEST_SELECT 带门（本行无 item_check = 空门）直翻 REWARD
		// 并下发档位奖励窗；报告页 SELECT5(2375) 与 SELECT_QUEST_REWARD(1009) 中转随页链退场，
		// 未集齐时零路由（关窗兜底交 DialogService）。
		// P0-3 S1: the delivery switches to the retail canonical shape — the gated QUEST_SELECT
		// (no item_check on this row = empty gate) flips REWARD and shows the tiered reward
		// window; the SELECT5 report page and the SELECT_QUEST_REWARD(1009) hop retire with the
		// page chain, so an incomplete hand-in has no route at all.
		int deliveryWindow = QuestDialogPage.rewardWindowForTier(
			definition.metadata().rewardGroups().size() - 1)
			.orElse(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1).id();
		QuestTransition delivery = route(definition, "started", REPORT_NPC,
			QuestDialogAction.QUEST_SELECT);
		assertContract(delivery, "reward", List.of(), List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(deliveryWindow)));
		assertNoLegacyPageChainResidue(definition, REPORT_NPC);

		for (QuestDialogAction previewAction : List.of(
			QuestDialogAction.USE_OBJECT, QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestTransition preview = route(definition, "reward", REPORT_NPC, previewAction);
			assertContract(preview, "reward", List.of(), List.of(
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())));
		}

		List<QuestTransition> completionRoutes = routes(definition, "reward", REPORT_NPC).stream()
			.filter(transition -> {
				Integer dialogId = ((QuestEvent.TalkToNpc) transition.event()).dialogId();
				return dialogId != null && dialogId >= QuestDialogAction.SELECTED_QUEST_REWARD1.id()
					&& dialogId <= QuestDialogAction.SELECTED_QUEST_NOREWARD.id();
			})
			.toList();
		assertEquals(16, completionRoutes.size());
		for (QuestTransition completion : completionRoutes) {
			assertContract(completion, "complete", List.of(
				new QuestAction.GrantReward("EXP", 0, 12951, QuestRewardAmountMode.QUEST_BASE),
				new QuestAction.CompleteQuest(0)), List.of(
				new AfterCommitAction.RefreshPlayerStats(),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())));
		}

		// P0c-20 裁定的上交独占（真端形状方向）：started 态 1009 上交只在报告 NPC——遗留 XML
		// 在接取 NPC 与 204191(Doman) 上的额外 1009 路由无客户端登记背书，已随 XML 退役。
		// P0c-20 hand-in exclusivity (retail direction): the started 1009 hand-in belongs to the
		// report NPC alone — the legacy XML's extra routes at the acquire NPC and 204191 (Doman)
		// had no client registry backing and are retired with the XML.
		assertTrue(routes(definition, "started", START_NPC).stream().noneMatch(transition ->
			((QuestEvent.TalkToNpc) transition.event()).dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id()),
			"quest 28800 acquire NPC must not own a started hand-in");
		assertTrue(routes(definition, "started", 204191).isEmpty(),
			"quest 28800 third NPC 204191 must own no routes");
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
		assertEquals(1, matches.size(), "quest 28800 " + source + " " + npcId + " " + action);
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
		return ProductionQuestDefinitions.definition(28800);
	}
}
