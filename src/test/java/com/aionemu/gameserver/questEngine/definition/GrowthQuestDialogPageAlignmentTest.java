package com.aionemu.gameserver.questEngine.definition;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrowthQuestDialogPageAlignmentTest {
	private static final List<Integer> ABBEY_KILL_QUESTS = List.of(
		19673, 19674, 19675, 19676, 19680, 19681, 19682,
		29673, 29674, 29675, 29676, 29680, 29681, 29682);
	private static final List<ItemQuest> ABBEY_ITEM_QUESTS = List.of(
		new ItemQuest(19672, 806698, 186000476, 10),
		new ItemQuest(19677, 806698, 186000476, 50),
		new ItemQuest(19684, 806698, 186000477, 3),
		new ItemQuest(19685, 806698, 186000477, 5),
		new ItemQuest(19686, 806698, 186000477, 10),
		new ItemQuest(19687, 806698, 186000477, 15),
		new ItemQuest(19688, 806698, 186000477, 20),
		new ItemQuest(19689, 806698, 186000477, 50),
		new ItemQuest(19694, 806698, 186000477, 50),
		new ItemQuest(29672, 806700, 186000476, 10),
		new ItemQuest(29677, 806700, 186000476, 50),
		new ItemQuest(29684, 806700, 186000477, 3),
		new ItemQuest(29685, 806700, 186000477, 5),
		new ItemQuest(29686, 806700, 186000477, 10),
		new ItemQuest(29687, 806700, 186000477, 15),
		new ItemQuest(29688, 806700, 186000477, 20),
		new ItemQuest(29689, 806700, 186000477, 50),
		new ItemQuest(29694, 806700, 186000477, 50));
	private static final List<WelcomeQuest> ABBEY_WELCOME_QUESTS = List.of(
		new WelcomeQuest(19671, 806698, 806699),
		new WelcomeQuest(19683, 806698, 806708),
		new WelcomeQuest(29671, 806700, 806701));

	@Test
	void deliveryOnlyGrowthQuestsFollowTheRetailTalkRow() {
		// P3 重锚（计划 §8.9）：80369-80386 / 80487-80538 共 70 行自 SimpleTalk 切换批起由 native 车道
		// 直驱。真端行只有 acquired/reward 两列（零中继、零交付门、零发扣、零过场），所以页阶梯是
		// 「接取入口页（客户端任务页声明页）→ 1003 确认 → 交付 NPC 1009 直开奖励窗（页 5）」。
		// P3 re-anchor (plan §8.9): the seventy delivery-only rows run on the native lane; their retail
		// rows carry nothing but acquired/reward, so the dialogs are entry page → 1003 → reward window 5.
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (int questId = 80369; questId <= 80386; questId++) {
			assertDeliveryOnlyNativeRow(handler, questId);
		}
		for (int questId = 80487; questId <= 80538; questId++) {
			assertDeliveryOnlyNativeRow(handler, questId);
		}
	}

	/** 74 行成长任务的接取 NPC（真端表列 → 静态 npc_template 解析，冻结）。 /
	 * The acquire NPCs of the growth rows (retail column → static npc_template, frozen). */
	private static final Map<String, Integer> ACQUIRE_NPCS = Map.of(
		"event_Cherylin", 831833,
		"event_Asif", 831834,
		"event_Rylin", 831835,
		"event_Jaysif", 831836,
		"event_Nebrith", 831031,
		"event_Edandos", 831029);

	private static void assertDeliveryOnlyNativeRow(SimpleTalkHandler handler, int questId) {
		assertTrue(handler.routes(questId), "quest " + questId + " 必须由 native 车道路由");
		NativeQuestXmlTable.QuestRow meta = NativeQuestXmlTable.instance().find(questId).orElseThrow();
		String acquireName = handler.requireRow(questId).acquiredNpcName();
		Integer expectedAcquire = ACQUIRE_NPCS.get(acquireName);
		assertNotNull(expectedAcquire, "接取名未冻结 / unfrozen acquire name: " + acquireName);
		assertEquals(expectedAcquire, handler.acquireNpc(questId), "quest " + questId + " 接取 NPC");
		// 交付 NPC：真端 reward 列必须唯一解析；同名列（80487 族）接取/交付同主，异名列交付 owner 分离。
		// Reward NPC: the retail reward column must resolve; same-name rows share the owner, others split it.
		assertNotNull(handler.rewardNpc(questId), "quest " + questId + " 交付 NPC 未解析: "
			+ handler.requireRow(questId).rewardNpcName());
		if (acquireName.equals(handler.requireRow(questId).rewardNpcName())) {
			assertEquals(handler.acquireNpc(questId), handler.rewardNpc(questId),
				"quest " + questId + " 同名单步行");
		} else {
			assertNotEquals(handler.acquireNpc(questId), handler.rewardNpc(questId),
				"quest " + questId + " 接取与交付 owner 分离");
		}
		assertEquals(0, handler.relayCount(questId), "quest " + questId + " 是单步行（真端行无 talk_npc 列）");
		assertTrue(handler.workItems(questId).isEmpty(),
			"quest " + questId + " 无交付门（真端行无 item_check 列）");
		assertFalse(handler.unresolvedGate(questId), "quest " + questId + " 无门不得 fail-closed");
		assertNull(handler.acceptGiveItem(questId), "quest " + questId + " 接取侧无发放");
		assertNull(handler.stepGiveItem(questId, 1), "quest " + questId + " 无步进发放");
		assertNull(handler.stepRemoveItem(questId, 1), "quest " + questId + " 无步进扣除");
		assertNull(handler.cutscene(questId), "quest " + questId + " 无过场");

		Player player = NativeTalkFixture.player(acquireRace(meta), PlayerClass.WARRIOR, acceptLevel(meta));
		int acquire = handler.acquireNpc(questId);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquire, questId, 31)), "接取问询");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(questId));

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, acquire, questId, 1002)), "接取确认");
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(questId).getStatus(), "接取必须建档到 START");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, handler.rewardNpc(questId), questId, 1009)),
			"交付报告");
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(questId).getStatus(), "无门交付必须直开领奖态");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
	}

	/** 真端行的种族轴（{@code race_permitted}）。 / The retail race axis. */
	private static Race acquireRace(NativeQuestXmlTable.QuestRow meta) {
		return "pc_dark".equals(meta.text("race_permitted")) ? Race.ASMODIANS : Race.ELYOS;
	}

	/** 真端行的等级下限（等级上限同样受 {@link com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort} 约束）。 /
	 * The retail minimum level (the ceiling is adjudicated by the native start port as well). */
	private static int acceptLevel(NativeQuestXmlTable.QuestRow meta) {
		Integer min = meta.integer("minlevel_permitted");
		return min == null || min <= 0 ? 1 : min;
	}

	@Test
	void abbeyGrowthQuestsUseTheRetailAskWindowExceptHandinRows() throws Exception {
		/* P0-2 DD 尾片：接取/交付切真端规范形（页 4 / 分档窗）。Abbey 击杀行与 welcome 链行的未接态
		   QUEST_SELECT 直发接取窗（页 4），客户端 select_none 入口页不再由服务端下发；item 行走交付型
		   词汇合成器（select_none/select1/check_* 是它的原生按钮面），入口页仍取客户端首屏。
		   Since the P0-2 DD tail slice the kill and welcome rows are canonical — the unaccepted
		   QUEST_SELECT emits the ask window (page 4) and the client select-none page is no longer
		   server-driven; the item rows keep the client entry page of the hand-in vocabulary compiler. */
		for (int questId : ABBEY_KILL_QUESTS) {
			int npcId = questId < 20000 ? 806698 : 806700;
			assertDialogPage(compile(questId), "unaccepted", npcId, 31,
				ClientAcceptEntryPageAssertions.expectedEntryPage(questId));
		}
		for (ItemQuest quest : ABBEY_ITEM_QUESTS) {
			assertDialogPage(compile(quest.id()), "unaccepted", quest.npcId(), 31, 4762);
		}
		for (WelcomeQuest quest : ABBEY_WELCOME_QUESTS) {
			// 接取入口页 = 客户端任务页声明的页（真端接取窗 4 只在客户端声明 ask_quest_accept 时可用）。
			// The accept entry page is the client task page's declared page (page 4 only where the
			// client declares ask_quest_accept).
			assertDialogPage(compile(quest.id()), "unaccepted", quest.instructorNpcId(), 31,
				ClientAcceptEntryPageAssertions.expectedEntryPage(quest.id()));
		}
	}

	@Test
	void abbeyKillGrowthQuestsBecomeRewardReadyOnTheFirstKill() throws Exception {
		for (int questId : ABBEY_KILL_QUESTS) {
			/* P5-1（2026-09-25）：Abbey 击杀成长任务已是真端网格（a0/a1，var0 = 计数）：每目标一条
			   击杀边（PACKET_ONLY），满段 a1 的 QUEST_SELECT/1009 报告进领奖；首杀即"报告就绪"。
			   Grid since P5-1: per-target kill edges step a0 to a1 (PACKET_ONLY) and the saturated a1
			   reports into reward — the first kill makes the quest report-ready. */
			QuestDefinition definition = compile(questId);
			List<QuestTransition> killRoutes = definition.transitions().stream()
				.filter(transition -> "a0".equals(transition.sourceNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpc
					|| transition.event() instanceof QuestEvent.KillNpcSet)
				.toList();
			assertFalse(killRoutes.isEmpty(), "quest " + questId + " kill route");
			for (QuestTransition killRoute : killRoutes) {
				assertEquals("a1", killRoute.targetNode(), "quest " + questId + " kill target");
				assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
					killRoute.afterCommit(), "quest " + questId + " kill sync");
			}
			QuestNode rewardNode = definition.nodes().stream()
				.filter(node -> "reward".equals(node.label()))
				.findFirst().orElseThrow();
			assertEquals(1, rewardNode.projection().variables().get("var0"),
				"quest " + questId + " reward projection");
			// 满段交付：a1 的 QUEST_SELECT 直翻 REWARD 并按档位查表下发奖励窗（P0-2 规范形，
			// 1009 中转删除）。
			// Full-node delivery: a1's QUEST_SELECT flips REWARD with the tiered window (canonical
			// since P0-2, no 1009 hop).
			QuestTransition deliver = singleTalkRoute(definition, "a1", reportNpc(definition),
				QuestDialogAction.QUEST_SELECT.id());
			assertEquals("reward", deliver.targetNode(), "quest " + questId + " delivery target");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
					definition.metadata().rewardGroups().size() - 1).orElseThrow().id())),
				deliver.afterCommit(), "quest " + questId + " delivery window");

			// 真端网格行在 REWARD 态无 QUEST_SELECT 重开路由（P0c-8c transXmlOnly 判例）；
			// 满段 1009/USE_OBJECT 预览再开领奖窗。
			// Grid rows keep no reward-state QUEST_SELECT re-open (P0c-8c transXmlOnly); the
			// saturated-segment 1009/USE_OBJECT previews re-open the window.
			assertTrue(definition.transitions().stream().noneMatch(candidate ->
				"reward".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode())
					&& candidate.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
				() -> "quest " + questId + " must not keep the legacy reward-state re-open route");
		}
	}

	@Test
	void abbeyItemGrowthQuestsUseTheLegacyItemCheckProtocol() throws Exception {
		for (ItemQuest quest : ABBEY_ITEM_QUESTS) {
			QuestDefinition definition = compile(quest.id());
			assertDialogPage(definition, "started", quest.npcId(), 31, 1011);
			assertDialogPage(definition, "reward", quest.npcId(), 31, 10002);

			List<QuestTransition> checks = talkRoutes(definition, "started", quest.npcId(), 39);
			assertEquals(2, checks.size(), "quest " + quest.id() + " item check branches");
			QuestTransition success = checks.stream()
				.filter(transition -> Integer.valueOf(0).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals("reward", success.targetNode(), "quest " + quest.id() + " item check target");
			assertEquals(List.of(new QuestCondition.HasItem(quest.itemId(), quest.count(), true)),
				success.conditions(), "quest " + quest.id() + " item check condition");
			assertEquals(List.of(new QuestAction.RemoveItem(quest.itemId(), quest.count())),
				success.actions(), "quest " + quest.id() + " item removal");
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(10000)),
				success.afterCommit(), "quest " + quest.id() + " successful check page");

			QuestTransition failure = checks.stream()
				.filter(transition -> Integer.valueOf(1).equals(transition.priority()))
				.findFirst().orElseThrow();
			assertEquals("started", failure.targetNode(), "quest " + quest.id() + " failed check target");
			assertTrue(failure.conditions().isEmpty(), "quest " + quest.id() + " failed check fallback");
			assertTrue(failure.actions().isEmpty(), "quest " + quest.id() + " failed check actions");
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)),
				failure.afterCommit(), "quest " + quest.id() + " failed check page");
			assertTrue(definition.transitions().stream()
				.filter(transition -> "reward".equals(transition.sourceNode()))
				.flatMap(transition -> transition.actions().stream())
				.noneMatch(QuestAction.RemoveItem.class::isInstance),
				"quest " + quest.id() + " must not remove the checked item twice");
		}
	}

	@Test
	void welcomeQuestsUseTheSellerThenInstructorProtocol() throws Exception {
		for (WelcomeQuest quest : ABBEY_WELCOME_QUESTS) {
			/* P5-1：真端信件 seller 对话无 select1_1(1012) 续页路由（1011 页直接 SET_SUCCEED 交付）；
			   SET_SUCCEED 进领奖带 LEVEL 刷新 + 全局任务簿页(10)，与 1919 期 DD talk 形状一致。
			   The retail letter has no select1_1 continuation: 1011 hands in via SET_SUCCEED straight
			   into reward with the book page, matching the DD talk shape. */
			QuestDefinition definition = compile(quest.id());
			assertDialogPage(definition, "started", quest.sellerNpcId(), 31, 1011);
			// P0-2 DD 尾片：接取/交付切真端规范形（页 4 / 分档窗）。领奖态的 QUEST_SELECT 直接开档位
			// 奖励窗（单奖励组 → 客户端第 1 档窗口），客户端 select_success(10002) 收尾页不再下发。
			// The reward-state QUEST_SELECT opens the tiered reward window (one group → the first client
			// window); the client select_success page is no longer server-driven.
			assertDialogPage(definition, "reward", quest.instructorNpcId(), 31,
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id());

			QuestTransition reward = singleTalkRoute(definition, "started", quest.sellerNpcId(), 10255);
			assertEquals("reward", reward.targetNode());
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
				reward.afterCommit());
			assertTrue(talkRoutes(definition, "unaccepted", quest.sellerNpcId(), 31).isEmpty());
		}
	}

	/** 满段交付 NPC：从满段 QUEST_SELECT 交付边提取（P0-2 规范形）。 / Delivery npc from the full-node QUEST_SELECT delivery edge. */
	private static int reportNpc(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> "a1".equals(transition.sourceNode())
				&& "reward".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.mapToInt(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.findFirst().orElseThrow();
	}

	private static QuestDefinition compile(int questId) throws Exception {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		return ProductionQuestDefinitions.definition(questId).definition();
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int dialogId, int pageId) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), routes.getFirst().afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static void assertDialogPage(QuestDefinition definition, String source, int npcId, int dialogId,
			int pageId) {
		QuestTransition route = singleTalkRoute(definition, source, npcId, dialogId);
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(pageId)), route.afterCommit(),
			"quest " + definition.id() + " " + source + " page");
	}

	private static QuestTransition singleTalkRoute(QuestDefinition definition, String source, int npcId, int dialogId) {
		List<QuestTransition> routes = talkRoutes(definition, source, npcId, dialogId);
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " dialog " + dialogId);
		return routes.getFirst();
	}

	private static List<QuestTransition> talkRoutes(QuestDefinition definition, String source, int npcId, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(dialogId).equals(talk.dialogId()))
			.toList();
	}

	private record ItemQuest(int id, int npcId, int itemId, int count) {
	}

	private record WelcomeQuest(int id, int instructorNpcId, int sellerNpcId) {
	}
}
