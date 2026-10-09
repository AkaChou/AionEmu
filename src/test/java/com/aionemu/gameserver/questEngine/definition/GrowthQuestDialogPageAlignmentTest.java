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
		// 直驱。原版行只有 acquired/reward 两列（零中继、零交付门、零发扣、零过场），所以页阶梯是
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

	/** 74 行成长任务的接取 NPC（原版表列 → 静态 npc_template 解析，冻结）。 /
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
		// 交付 NPC：原版 reward 列必须唯一解析；同名列（80487 族）接取/交付同主，异名列交付 owner 分离。
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
		assertEquals(0, handler.relayCount(questId), "quest " + questId + " 是单步行（原版行无 talk_npc 列）");
		assertTrue(handler.workItems(questId).isEmpty(),
			"quest " + questId + " 无交付门（原版行无 item_check 列）");
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

	/** 原版行的种族轴（{@code race_permitted}）。 / The retail race axis. */
	private static Race acquireRace(NativeQuestXmlTable.QuestRow meta) {
		return "pc_dark".equals(meta.text("race_permitted")) ? Race.ASMODIANS : Race.ELYOS;
	}

	/** 原版行的等级下限（等级上限同样受 {@link com.aionemu.gameserver.questEngine.tablelane.NativeQuestStartPort} 约束）。 /
	 * The retail minimum level (the ceiling is adjudicated by the native start port as well). */
	private static int acceptLevel(NativeQuestXmlTable.QuestRow meta) {
		Integer min = meta.integer("minlevel_permitted");
		return min == null || min <= 0 ? 1 : min;
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
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 原版 overlay）。
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
