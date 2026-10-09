package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestXmlTable;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;

/**
 * 80016/80018 圣诞事件任务（Sock Hop / Sock It To 'Em）：原版表行 = 单步行 NPC 接取 +
 * {@code item_check} 交付门（各 15 件收集物）+ 交付领奖。
 * <p>
 * P3 重锚（计划 §8.9）：本类只断言原版表行、原版 {@code quest.xml} 与客户端页契约可实证的事实。
 * 旧 IR 断言的「onLvlUp 自动接取 / 活动失效弃任 / MOVIE bonus 随机播放」只存在于本地 XML 与旧 handler：
 * 原版 codegen 对这两行注册的是普通 SimpleTalk 槽位
 * （{@code FUN_180cb5920(&DAT_…,L"event_Sonaran",0x13890)} 注册块 + cab520 接取 thunk + cabb10 对话 thunk，
 * {@code server58/MainServer_ScriptDLL64/ScriptDLL64.c:1462204}、{@code :2341547}、{@code :2379305}），
 * 活动子系表 {@code quest/event_quest.xml} 全域缺失（计划 §10.3-#3）⇒ 那些轴登记为**不可实证假设**，
 * 不再作为断言面，也不在 native 车道发明。
 * <p>
 * Event quests 80016/80018: the retail row is a single-step NPC-acquired talk row with a 15-item
 * hand-in gate. P3 re-anchor (plan §8.9) keeps only retail-table, quest.xml and client-contract facts;
 * the old IR-only level-up auto-accept / MOVIE-bonus axes were local inventions and are now registered
 * as unverifiable assumptions (the retail event subsystem table is missing).
 */
class QuestMovieEventDefinitionTest {

	private static final int ELYOS_QUEST = 80016;
	private static final int ASMODIAN_QUEST = 80018;
	/** event_Sonaran / event_Mayer（原版 acquired/reward 列 → 静态 npc_template）。 */
	private static final int ELYOS_NPC = 799763;
	private static final int ASMODIAN_NPC = 799778;
	/** quest_80016a / quest_80018a（quest.xml collect_item1 → 静态物品 name_desc）。 */
	private static final int ELYOS_ITEM = 182214008;
	private static final int ASMODIAN_ITEM = 182214010;

	@Test
	void retailRowsCarryTheSingleStepNpcHandIn() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (int questId : new int[] {ELYOS_QUEST, ASMODIAN_QUEST}) {
			int npcId = npcOf(questId);
			int itemId = itemOf(questId);
			assertTrue(handler.routes(questId), "quest " + questId + " 必须由 native 车道路由");
			assertEquals(RetailGrantKind.NPC, handler.grantKind(questId), "NPC 接取行");
			assertEquals(npcId, handler.acquireNpc(questId), "接取 NPC");
			assertEquals(npcId, handler.rewardNpc(questId), "交付 NPC（原版同主）");
			assertEquals(0, handler.relayCount(questId), "无中继步");
			assertEquals(List.of(new SimpleTalkHandler.ItemStack(itemId, 15)), handler.workItems(questId),
				"交付门 = quest.xml collect_item1 ×15");
			assertFalse(handler.unresolvedGate(questId), "交付门必须可解");
			assertNull(handler.acceptGiveItem(questId), "无接取发放");
			assertNull(handler.stepGiveItem(questId, 1), "无步进发放");
			assertNull(handler.stepRemoveItem(questId, 1), "无步进扣除");
			assertNull(handler.cutscene(questId), "无过场");
		}
	}

	@Test
	void questXmlDeclaresTheEventMetadata() {
		assertEventRow(ELYOS_QUEST, "pc_light", ELYOS_ITEM, "Branch_Event_XmasEvent_ShugoSantaD_99_n");
		assertEventRow(ASMODIAN_QUEST, "pc_dark", ASMODIAN_ITEM, "Branch_Event_XmasEvent_Rudolph_99_n");
	}

	@Test
	void acceptAndHandInFollowTheRetailTalkLane() {
		for (int questId : new int[] {ELYOS_QUEST, ASMODIAN_QUEST}) {
			int npcId = npcOf(questId);
			int itemId = itemOf(questId);
			Player player = NativeTalkFixture.player(raceOf(questId), PlayerClass.WARRIOR, 10);
			SimpleTalkHandler handler = NativeTalkFixture.handler();
			NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
			SimpleTalkHandler itemHandler = NativeTalkFixture.handler(inventory);

			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 31)), "接取问询");
			assertNull(player.getQuestStateList().getQuestState(questId), "问询页不得落库");
			NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(questId));

			NativeTalkFixture.clearPackets(player);
			assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1002)), "接取确认");
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_ACCEPTED);

			// 未持有交付物：报告门保持 START（进行中页）。 / Without the items the report gate holds.
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1009)), "报告被受理");
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus(),
				"交付门未持有必须保持 START");
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_IN_PROGRESS);

			// 持有 15 件：报告 → 领奖态并扣除整组。 / With the items the report pays into REWARD.
			inventory.hold(itemId, 15);
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1009)), "交付报告");
			assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
			assertEquals(List.of("remove:" + itemId + ":15"), inventory.calls(), "交付门按原版扣除");
		}
	}

	@Test
	void theRetailRowHasNoSystemGrantAxis() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (int questId : new int[] {ELYOS_QUEST, ASMODIAN_QUEST}) {
			// 原版 codegen 注册的是 NPC 节点（b5920），事件子系默认槽 0x37/0x38 不在 retail codegen 面内。
			assertFalse(handler.isSystemGranted(questId), "quest " + questId + " 只能由 NPC 接取");
			assertEquals(0, handler.factionId(questId), "quest " + questId + " 不是阵营日常行");
			assertNull(handler.conQuest(questId), "quest " + questId + " 无链式接取窗");
			assertTrue(NativeTalkFixture.clientDeclares(questId, SimpleTalkHandler.PAGE_ASK_ACCEPT),
				"quest " + questId + " 客户端任务页声明原版接取窗页 4");
		}
	}

	private static void assertEventRow(int questId, String race, int itemId, String dropMonster) {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElseThrow();
		assertEquals("event", row.text("category1"), "原版类别");
		assertEquals(10, row.integer("minlevel_permitted"), "原版等级下限");
		assertEquals(race, row.text("race_permitted"), "原版种族轴");
		assertEquals("1", row.text("max_repeat_count"), "一次性任务");
		assertEquals("quest_" + questId + "a 15", row.text("collect_item1"), "原版收集物通道");
		assertEquals("quest_" + questId + "a 15", row.text("check_item1_1"), "原版交付门通道");
		assertEquals("50000", row.text("reward_exp1"), "原版经验奖励");
		assertEquals("100000", row.text("reward_gold1"), "原版金币奖励");
		assertEquals("world_wrap_event_winter_02a 1", row.text("reward_item1_1"), "原版奖励道具 1");
		assertEquals("world_event_head_winter_01 1", row.text("reward_item1_2"), "原版奖励道具 2");
		assertEquals(dropMonster, row.text("drop_monster_1"), "原版掉落怪");
		assertEquals("100", row.text("drop_prob_1"), "原版掉落率");
		assertEquals(15, collectCount(row), "交付数量");
		assertEquals(itemId, itemOf(questId), "收集物 id 冻结（静态物品 name_desc）");
	}

	private static int collectCount(NativeQuestXmlTable.QuestRow row) {
		return Integer.parseInt(row.text("collect_item1").trim().split("\\s+")[1]);
	}

	private static int npcOf(int questId) {
		return questId == ELYOS_QUEST ? ELYOS_NPC : ASMODIAN_NPC;
	}

	private static int itemOf(int questId) {
		return questId == ELYOS_QUEST ? ELYOS_ITEM : ASMODIAN_ITEM;
	}

	private static Race raceOf(int questId) {
		return questId == ELYOS_QUEST ? Race.ELYOS : Race.ASMODIANS;
	}
}
