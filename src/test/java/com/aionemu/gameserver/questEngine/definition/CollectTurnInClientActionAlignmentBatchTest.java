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
 * 与 SimpleTalk 三族交付同形——{@code QUEST_SELECT}(started→reward) 带真端整组 HasItem 门控直翻
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
	void simpleSelect5QuestsCheckItemsOnClientAction39() throws Exception {
		// 80482/80486 自 P0-2 起走 SimpleUseItem 规范形，分拣到 canonicalUseItemQuestDeliversOnQuestSelect；
		// 1103 自 P0-2 起走 SimpleCollectItem 规范形，分拣到 canonicalCollectQuestDeliversOnQuestSelect；
		// 其余（SimpleTalk 族）自 P0-3 S1 起同形收口：SELECT5 入口页 + 39 双分支退场，走
		// canonicalAcceptFlow（接取窗页 4）+ canonicalDelivery（分档奖励窗）。
		// P0-3 S1: the SimpleTalk family takes the same canonical shape — the SELECT5 entry page and
		// the 39 double branch are retired in favour of canonicalAcceptFlow (ask window, page 4) plus
		// canonicalDelivery (tiered reward window).
		checkCanonicalTurnIn(30312, 799322, 182209715, 20);
		checkCanonicalTurnIn(30314, 799226, 186000098, 100);
		checkCanonicalTurnIn(30315, 799226, 186000098, 200);
		checkCanonicalTurnIn(1124, 790001, 182200210, 3);
	}

	/**
	 * P0-2 规范形 UseItem 交付（SimpleUseItem 族 CHECK 形）：QUEST_SELECT 带交付物 HasItem 门控
	 * 直翻 REWARD 并下发档位奖励窗；SELECT5 报告页与 39 检查对随页链删除，未集齐零路由。
	 * Canonical UseItem turn-in since P0-2 (the SimpleUseItem CHECK form): QUEST_SELECT gated on
	 * the hand-in flips REWARD and shows the tiered window; the SELECT5 page and the 39 check pair
	 * are gone, and an incomplete hand-in has no route.
	 */
	@Test
	void canonicalUseItemQuestDeliversOnQuestSelect() throws Exception {
		for (int[] quest : new int[][] {{80482, 831959, 182215419}, {80486, 831961, 182215421}}) {
			QuestDefinition definition = definition(quest[0]).definition();
			int rewardWindow = QuestDialogPage.rewardWindowForTier(
				definition.metadata().rewardGroups().size() - 1).orElseThrow().id();
			QuestTransition deliver = transition(definition, "started", "reward",
				new QuestEvent.TalkToNpc(quest[1], QuestDialogAction.QUEST_SELECT.id()));
			assertEquals(List.of(new QuestCondition.HasItem(quest[2], 1)), deliver.conditions(),
				"quest " + quest[0] + " delivery conditions");
			assertEquals(List.of(new QuestAction.RemoveItem(quest[2], 1)), deliver.actions(),
				"quest " + quest[0] + " delivery removals");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardWindow)), deliver.afterCommit(),
				"quest " + quest[0] + " delivery response");
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
					candidate.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == quest[1]
						&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				"quest " + quest[0] + " canonical removed the 39 check pair");
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
					candidate.sourceNode().equals("started") && candidate.targetNode().equals("started")
						&& candidate.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.npcId() == quest[1]
						&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
				"quest " + quest[0] + " canonical removed the SELECT5 report page self-loop");
		}
	}

	/**
	 * P0-2 规范形收集交付（SimpleCollectItem 族）：QUEST_SELECT 带整组 HasItem 门控直翻 REWARD 并
	 * 下发档位奖励窗；SELECT5 报告页与 39 检查对随页链删除，未集齐零路由（关窗兜底）。
	 * Canonical collect turn-in since P0-2 (the SimpleCollectItem family): QUEST_SELECT gated by the
	 * whole hand-in set flips REWARD and shows the tiered window; the SELECT5 page and the 39 check
	 * pair are gone, and an incomplete hand-in has no route.
	 */
	@Test
	void canonicalCollectQuestDeliversOnQuestSelect() throws Exception {
		QuestDefinition definition = definition(1103).definition();
		QuestTransition deliver = transition(definition, "started", "reward",
			new QuestEvent.TalkToNpc(203057, QuestDialogAction.QUEST_SELECT.id()));
		assertEquals(List.of(new QuestCondition.HasItem(182200201, 3)), deliver.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182200201, 3)), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 203057
					&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
			"canonical removed the 39 check pair at the turn-in npc");
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
				candidate.sourceNode().equals("started") && candidate.targetNode().equals("started")
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 203057
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			"canonical removed the SELECT5 report page self-loop");
	}

	@Test
	void quest1137ChecksOnlyTheCollectedFossilNotTheWorkItem() throws Exception {
		QuestDefinition definition = definition(1137).definition();
		checkTurnInBranches(definition, 203111, List.of(new QuestCondition.HasItem(182200513, 1)), "SELECT6");
		// 工具物品 182200512 是 work-item，由完成清理回收，不得作为交付条件或被交付移除。
		assertTrue(definition.transitions().stream()
			.flatMap(candidate -> candidate.conditions().stream())
			.noneMatch(condition -> condition instanceof QuestCondition.HasItem hasItem
				&& hasItem.itemId() == 182200512), "work item must not gate turn-in");
		assertTrue(definition.transitions().stream()
			.flatMap(candidate -> candidate.actions().stream())
			.noneMatch(action -> action instanceof QuestAction.RemoveItem remove
				&& remove.itemId() == 182200512), "work item must not be removed at turn-in");
	}

	@Test
	void dualNpcQuest1351KeepsRewardOwnershipOnTheTurnInNpc() throws Exception {
		QuestDefinition definition = definition(1351).definition();

		// P0c-27 裁定（真端对、XML 错）：族形 started 态在接取 NPC 上只有 FINISH_DIALOG(1008)
		// 出口，推进后展示任务接取页（SELECT_QUEST=10）；遗留 XML 的 QUEST_SELECT→SELECT1
		// "任务描述页"是手工形（P0c-19/20 NPC 角色词汇；翻转前 overlay 探针直证真端编译形状）。
		// P0c-27 adjudication (retail-right, XML-wrong): the acquire NPC's started state carries
		// only the FINISH_DIALOG(1008) family exit, which then shows the quest-selection page
		// (SELECT_QUEST=10); the legacy QUEST_SELECT->SELECT1 description page was hand-made.
		assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 203965 && "started".equals(candidate.sourceNode())
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			"203965 must not open a QUEST_SELECT description page in the started state");
		QuestTransition exit = talk(definition, "started", "started", 203965,
			QuestDialogAction.FINISH_DIALOG.id());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(
			QuestDialogPage.SELECT_QUEST.id())), exit.afterCommit());
		assertTrue(definition.transitions().stream()
			.noneMatch(candidate -> candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203965 && "reward".equals(candidate.targetNode())),
			"203965 must not own reward routes");

		// P0-3 S1：SimpleTalk 接取/交付切真端规范形（页 4 / 分档窗）。
		// 交付 NPC 203983：START 自环的 SELECT5 报告入口页与 39/20002 检查对退场；交付 =
		// canonicalDelivery——QUEST_SELECT(started→reward) 带真端 collect_item1 整组门
		// （quest_1351a×10）直翻领奖并下发单档奖励窗 1（单奖励组 → rewardWindowForTier(0)），
		// 未集齐时零路由（关窗兜底交 DialogService）。
		// P0-3 S1: the SimpleTalk accept/delivery segments take the retail canonical shape (page 4 /
		// tiered window). The turn-in npc 203983 loses the START self-loop SELECT5 report entry and the
		// 39/20002 check pair; the delivery is canonicalDelivery — a gated QUEST_SELECT(started->reward)
		// carrying the whole retail collect_item1 group (quest_1351a x10) that flips REWARD and shows the
		// single-tier reward window 1 (one reward group -> rewardWindowForTier(0)); an incomplete hand-in
		// has no route and DialogService closes the window.
		QuestTransition deliver = delivery(definition, 203983, "1351 delivery route");
		assertNull(deliver.priority(), "1351 delivery route priority");
		assertEquals(List.of(new QuestCondition.HasItem(182201321, 10)), deliver.conditions());
		assertEquals(List.of(new QuestAction.RemoveItem(182201321, 10)), deliver.actions());
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
			deliver.afterCommit());
		assertNoLegacyDeliveryPages(definition, 203983, "1351");
	}

	/**
	 * RAKSANG 族（QE-108 起由真端 quest_area 驱动）：接取 = 进区域 {@code SystemGrant} 发放（无接取 NPC），
	 * 交付 = {@code QUEST_SELECT}(started→reward) 带真端整组 HasItem 门控直翻 REWARD 并下发单档奖励窗；
	 * 旧 SELECT1 入口页与 39 检查失败页随页链整体退场。
	 * The RAKSANG family is area-driven since QE-108: the acquire is the {@code SystemGrant} edge fired on
	 * area entry (no acquire npc), and the delivery is a gated {@code QUEST_SELECT}(started->reward) that
	 * flips REWARD and shows the single-tier reward window; the legacy SELECT1 entry page and the 39
	 * check-failure page are gone.
	 */
	@Test
	void raksangQuestsGrantOnAreaEntryAndDeliverOnQuestSelect() throws Exception {
		for (int questId : RAKSANG_QUESTS) {
			int itemId = questId == 18739 ? 182215692 : 182215693;
			int count = questId == 18739 ? 5 : 8;
			QuestDefinition definition = definition(questId).definition();

			QuestTransition grant = transition(definition, "unaccepted", "started",
				new QuestEvent.SystemGrant());
			assertNull(grant.priority(), "quest " + questId + " grant priority");
			assertEquals(List.of(new QuestCondition.StartEligible()), grant.conditions(),
				"quest " + questId + " grant conditions");
			assertEquals(List.of(), grant.actions(), "quest " + questId + " grant actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH)),
				grant.afterCommit(), "quest " + questId + " grant response");

			QuestTransition deliver = delivery(definition, 804707, "quest " + questId + " delivery route");
			assertNull(deliver.priority(), "quest " + questId + " delivery priority");
			assertEquals(List.of(new QuestCondition.HasItem(itemId, count)), deliver.conditions(),
				"quest " + questId + " delivery gate");
			assertEquals(List.of(new QuestAction.RemoveItem(itemId, count)), deliver.actions(),
				"quest " + questId + " delivery removals");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				deliver.afterCommit(), "quest " + questId + " delivery response");
			assertNoLegacyDeliveryPages(definition, 804707, "quest " + questId);
		}
	}

	@Test
	void expertTapperQuestsCheckItemsOnTheSelect2PageAction() throws Exception {
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
	void stigmaScarChainSpawnsAndDeletesTheMidwayNpcBeforeTheTurnIn() throws Exception {
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

	@Test
	void quest18745KeepsTheRewardOwnerExclusive() throws Exception {
		QuestDefinition definition = definition(18745).definition();
		checkTurnInBranches(definition, 804707, 182215943, 1, "CHECK_USER_ITEM_FAIL", true);
		for (int npcId : new int[] {206378, 206379, 206380, 702958}) {
			assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId && "started".equals(candidate.sourceNode())
					&& talk.dialogId() == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id()),
				"npc " + npcId + " must not own started dialog routes");
			assertFalse(definition.transitions().stream().anyMatch(candidate ->
				candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == npcId && "reward".equals(candidate.sourceNode())),
				"npc " + npcId + " must not own reward routes");
		}
	}

	/**
	 * S1 规范形交付（P0-3）：{@code QUEST_SELECT}(started→reward) 带真端整组 HasItem 门控直翻 REWARD
	 * 并下发单档奖励窗 1；START 自环的报告入口页与 39/20002 检查对、SELECT5/SELECT6 报告页一律不得出现
	 * （未集齐零路由）。门物品 = 真端 quest.xml 的 collect_item1 整列（单组任务，窗 = 档位 0 即窗 1）。
	 * The S1 canonical delivery (P0-3): the gated QUEST_SELECT(started->reward) flips REWARD and shows the
	 * single-tier reward window 1; the START self-loop report entry, the 39/20002 check pair and the
	 * SELECT5/SELECT6 report pages must all be absent (an incomplete hand-in has no route). The gate is the
	 * whole retail collect_item1 group; a single reward tier maps to window 1.
	 */
	private static void checkCanonicalTurnIn(int questId, int npcId, int itemId, int count) throws Exception {
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

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId);
	}
}
