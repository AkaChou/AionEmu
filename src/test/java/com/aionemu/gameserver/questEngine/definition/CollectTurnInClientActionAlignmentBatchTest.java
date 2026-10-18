package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定收集交付任务的客户端动作链对齐合同（P0-2 分族 / P0-3 S1 收口）：SimpleCollectItem、SimpleUseItem
 * 与 SimpleTalk 三族交付同形——{@code QUEST_SELECT}(started→reward) 带原版整组 HasItem 门控直翻
 * REWARD 并下发分档奖励窗；39/20002 检查对、SELECT5 报告页/入口页与 SELECT6 失败页随页链整体退场，
 * 未集齐时零路由（关窗兜底交 DialogService）。接取段走 S1 规范形：{@code QUEST_SELECT} 直发接取窗
 * （页 4）。reward owner 归属仍以 Aion 5.8 客户端与旧 handler 为准。
 * Locks the client action chain alignment contract for collect turn-in quests (P0-2 split, closed by
 * P0-3 S1): the SimpleCollectItem, SimpleUseItem and SimpleTalk families share one delivery shape —
 * a gated {@code QUEST_SELECT}(started->reward) flips REWARD and shows the tiered window; the 39/20002
 * check pairs, the SELECT5 report/entry pages and the SELECT6 failure page are gone, so an incomplete
 * hand-in has no route (DialogService closes the window). The accept segment uses the S1 canonical flow
 * ({@code QUEST_SELECT} opens the ask window, page 4). Reward ownership follows the Aion 5.8 client and
 * the legacy handlers.
 */
class CollectTurnInClientActionAlignmentBatchTest {
	private static final Set<Integer> RAKSANG_QUESTS = Set.of(18739, 18740);


	@Test
	void expertTapperQuestsCheckItemsOnTheSelect2PageAction() {
		QuestDefinition asmodians = definition(29000).definition();
		QuestTransition entry = talk(asmodians, "started", "started", 204096,
			QuestDialogAction.QUEST_SELECT.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			entry.afterCommit(), "29000 entry page");
		checkTurnInBranches(asmodians, 204096,
			List.of(
				new QuestCondition.HasItem(152003004, 1),
				new QuestCondition.HasItem(152003005, 1),
				new QuestCondition.HasItem(152003006, 1)),
			"CHECK_USER_ITEM_FAIL");

		QuestDefinition elyos = definition(29002).definition();
		for (int npcId : new int[] {204257, 204099}) {
			QuestTransition secondEntry = talk(elyos, "started", "started", npcId,
				QuestDialogAction.QUEST_SELECT.id());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
				secondEntry.afterCommit(), "29002 npc " + npcId + " entry page");
			checkTurnInBranches(elyos, npcId,
				List.of(
					new QuestCondition.HasItem(152003007, 1),
					new QuestCondition.HasItem(152003008, 1)),
				"CHECK_USER_ITEM_FAIL");
		}
	}

	@Test
	void stigmaScarChainSpawnsAndDeletesTheMidwayNpcBeforeTheTurnIn() {
		QuestDefinition definition = definition(30217).definition();
		assertNode(definition, "s1", QuestStatus.START, Map.of("var0", 1));
		assertNode(definition, "s2", QuestStatus.START, Map.of("var0", 2));

		QuestTransition spawn = transition(definition, "started", "s1",
			new QuestEvent.TalkToNpc(798941, QuestDialogAction.SETPRO1.id()));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), spawn.actions());
		assertTrue(spawn.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawnNpc
			&& spawnNpc.templateId() == 799506), "SETPRO1 must spawn 799506: " + spawn.afterCommit());

		QuestTransition despawn = transition(definition, "s1", "s2",
			new QuestEvent.TalkToNpc(799506, QuestDialogAction.SETPRO2.id()));
		assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), despawn.actions());
		assertEquals(List.of(
			new AfterCommitAction.DeleteInteractionNpc(false),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), despawn.afterCommit());

		// 最终判定挂点 NPC 对话（客户端在 s2 阶段打开对话即判定），集齐显示 SELECT3(1693)。
		QuestTransition ready = transition(definition, "s2", "reward",
			new QuestEvent.TalkToNpc(798909, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(Integer.valueOf(0), ready.priority());
		assertEquals(List.of(
			new QuestCondition.HasItem(182209618, 1),
			new QuestCondition.HasItem(182209619, 1)), ready.conditions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT3.id())), ready.afterCommit());
		QuestTransition notReady = transition(definition, "s2", "s2",
			new QuestEvent.TalkToNpc(798909, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(Integer.valueOf(1), notReady.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.CHECK_USER_ITEM_FAIL.id())),
			notReady.afterCommit());

		// 1693 页的确认按钮动作 39 在奖励状态打开奖励窗口。
		QuestTransition rewardWindow = talk(definition, "reward", "reward", 798909,
			QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), rewardWindow.afterCommit());
	}


	/**
	 * S1 规范形交付（P0-3）：{@code QUEST_SELECT}(started→reward) 带原版整组 HasItem 门控直翻 REWARD
	 * 并下发单档奖励窗 1；START 自环的报告入口页与 39/20002 检查对、SELECT5/SELECT6 报告页一律不得出现
	 * （未集齐零路由）。门物品 = 原版 quest.xml 的 collect_item1 整列（单组任务，窗 = 档位 0 即窗 1）。
	 * The S1 canonical delivery (P0-3): the gated QUEST_SELECT(started->reward) flips REWARD and shows the
	 * single-tier reward window 1; the START self-loop report entry, the 39/20002 check pair and the
	 * SELECT5/SELECT6 report pages must all be absent (an incomplete hand-in has no route). The gate is the
	 * whole retail collect_item1 group; a single reward tier maps to window 1.
	 */
	private static void checkCanonicalTurnIn(int questId, int npcId, int itemId, int count) {
		QuestDefinition definition = definition(questId).definition();
		QuestTransition deliver = delivery(definition, npcId, "quest " + questId + " delivery route");
		assertNull(deliver.priority(), "quest " + questId + " delivery route priority");
		assertEquals(List.of(new QuestCondition.HasItem(itemId, count)), deliver.conditions(),
			"quest " + questId + " delivery gate");
		assertEquals(List.of(new QuestAction.RemoveItem(itemId, count)), deliver.actions(),
			"quest " + questId + " delivery removals");
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit(), "quest " + questId + " delivery response");
		assertNoLegacyDeliveryPages(definition, npcId, "quest " + questId);
	}

	/**
	 * 旧交付页链零残留（S1 失败侧判据，单一真源）：交付 NPC 上不得再出现
	 * <ul>
	 * <li>{@code started→started} 的 {@code QUEST_SELECT} 报告入口页——**判据必须带 target 节点**：
	 * canonical 交付边本身就是同 NPC 的 {@code started} 源 {@code QUEST_SELECT}（started→reward），
	 * 只按 (npc, 事件, 源节点) 过滤会把它自己打成违规；</li>
	 * <li>39/20002 检查对（任意源节点）；</li>
	 * <li>定义内任何 SELECT5/SELECT6 页下发。</li>
	 * </ul>
	 * Zero residue of the legacy delivery page chain (the S1 failure-side criterion, one source of truth):
	 * no started->started QUEST_SELECT report entry at the turn-in npc — the target node must be part of the
	 * key because the canonical delivery edge is itself a started-source QUEST_SELECT (started->reward) at
	 * the same npc — no 39/20002 check pair anywhere, and no SELECT5/SELECT6 page push in the definition.
	 */
	private static void assertNoLegacyDeliveryPages(QuestDefinition definition, int npcId, String detail) {
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
					&& "started".equals(candidate.sourceNode()) && "started".equals(candidate.targetNode())
					&& talk.dialogId() != null
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			detail + " retired the SELECT5 report entry page at " + npcId);
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == npcId
					&& talk.dialogId() != null
					&& (talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()
						|| talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id())),
			detail + " retired the 39/20002 check pair at " + npcId);
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.afterCommit().stream().anyMatch(action ->
					action instanceof AfterCommitAction.ShowQuestDialog page
						&& (page.dialogId() == QuestDialogPage.SELECT5.id()
							|| page.dialogId() == QuestDialogPage.SELECT6.id()))),
			detail + " retired the SELECT5/SELECT6 report pages");
	}

	/** 交付边唯一取用：同一 (npc, QUEST_SELECT) 的 started→reward 路由必须恰有一条。 /
	 * Unique canonical delivery lookup: exactly one started->reward QUEST_SELECT route per npc. */
	private static QuestTransition delivery(QuestDefinition definition, int npcId, String detail) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode())
				&& "reward".equals(candidate.targetNode())
				&& candidate.event().equals(new QuestEvent.TalkToNpc(npcId,
					QuestDialogAction.QUEST_SELECT.id())))
			.toList();
		assertEquals(1, routes.size(), detail);
		return routes.getFirst();
	}

	private static void checkTurnInBranches(QuestDefinition definition, int npcId, int itemId, int count,
			String failPage) {
		checkTurnInBranches(definition, npcId, List.of(new QuestCondition.HasItem(itemId, count)), failPage);
	}

	private static void checkTurnInBranches(QuestDefinition definition, int npcId, int itemId, int count,
			String failPage, boolean confirmPage) {
		checkTurnInBranches(definition, npcId, List.of(new QuestCondition.HasItem(itemId, count)), failPage,
			confirmPage);
	}

	private static void checkTurnInBranches(QuestDefinition definition, int npcId, List<QuestCondition> conditions,
			String failPage) {
		checkTurnInBranches(definition, npcId, conditions, failPage, false);
	}

	private static void checkTurnInBranches(QuestDefinition definition, int npcId, List<QuestCondition> conditions,
			String failPage, boolean confirmPage) {
		QuestTransition success = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(0), success.priority());
		assertEquals(conditions, success.conditions());
		List<QuestAction> removals = conditions.stream()
			.map(condition -> (QuestCondition.HasItem) condition)
			.map(hasItem -> (QuestAction) new QuestAction.RemoveItem(hasItem.itemId(), hasItem.count()))
			.toList();
		assertEquals(removals, success.actions());
		// 客户端 check_user_item_ok 确认页由成功分支显示；奖励窗口由 REWARD 态 preview 的
		// SELECT_QUEST_REWARD 打开（ok 页按钮 1009）。
		// The client check_user_item_ok confirmation page is shown by the success branch; the
		// reward window opens through the REWARD-state preview's SELECT_QUEST_REWARD (ok 1009).
		if (confirmPage) {
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(10000)),
				success.afterCommit());
		} else {
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				success.afterCommit());
		}
		QuestTransition failure = transition(definition, "started", "started",
			new QuestEvent.TalkToNpc(npcId, QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()));
		assertEquals(Integer.valueOf(1), failure.priority());
		assertEquals(List.of(), failure.conditions());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageIdOf(failPage))),
			failure.afterCommit());
	}

	private static int pageIdOf(String pageName) {
		return switch (pageName) {
			case "SELECT1" -> QuestDialogPage.SELECT1.id();
			case "SELECT2" -> QuestDialogPage.SELECT2.id();
			case "SELECT5" -> QuestDialogPage.SELECT5.id();
			case "SELECT6" -> QuestDialogPage.SELECT6.id();
			case "CHECK_USER_ITEM_FAIL" -> QuestDialogPage.CHECK_USER_ITEM_FAIL.id();
			default -> throw new IllegalArgumentException("unknown page " + pageName);
		};
	}

	private static QuestTransition talk(QuestDefinition definition, String source, String target, int npcId,
			int action) {
		return transition(definition, source, target, new QuestEvent.TalkToNpc(npcId, action));
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		return definition.transitions().stream()
			.filter(candidate -> Objects.equals(candidate.sourceNode(), source)
				&& candidate.targetNode().equals(target) && candidate.event().equals(event))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static CompiledQuestDefinition definition(int questId) {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
