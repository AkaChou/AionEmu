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
 * 80030/80033 春节事件任务（瑞雪爷爷 3 段）：真端表行 = 单步行 NPC 接取 + {@code item_check}
 * 交付门（神符 1 件）+ 称号道具奖励。
 * <p>
 * P3 重锚（计划 §8.9）：断言面只保留真端表行、真端 {@code quest.xml} 与客户端页契约。
 * 旧 IR 断言的 {@code EventQuestRefresh} 排程（10s 复活/子任务重启）与「活动失效时 UseItem 阻断」
 * 只存在于本地 XML 与旧 handler；真端 codegen 为这两行注册的是普通 SimpleTalk 槽位
 * （{@code L"event_Lotus"/L"event_Metrano"} → {@code 0x1389e/0x138a1} 注册块 + cab520/cabb10 thunk），
 * 活动子系表 {@code quest/event_quest.xml} 全域缺失（计划 §10.3-#3）⇒ 登记为不可实证假设，不再断言。
 * <p>
 * Event quests 80030/80033: the retail row is a single-step NPC talk row with a one-item hand-in gate
 * and a title-item reward. The old IR-only {@code EventQuestRefresh} scheduling and inactive-event item
 * blocking were local inventions; they are now registered as unverifiable assumptions.
 */
class QuestCharmedEventDefinitionTest {

	private static final int ELYOS_QUEST = 80030;
	private static final int ASMODIAN_QUEST = 80033;
	/** event_Lotus / event_Metrano（真端 acquired/reward 列 → 静态 npc_template）。 */
	private static final int ELYOS_NPC = 799766;
	private static final int ASMODIAN_NPC = 799781;
	/** world_event_lunar_scroll_shield_all_20a / world_event_add_title_153_14。 */
	private static final int GATE_ITEM = 164002015;
	private static final int REWARD_ITEM = 169610037;

	@Test
	void retailRowsCarryTheSingleStepNpcHandIn() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (int questId : quests()) {
			assertTrue(handler.routes(questId), "quest " + questId + " 必须由 native 车道路由");
			assertEquals(RetailGrantKind.NPC, handler.grantKind(questId), "NPC 接取行");
			assertEquals(npcOf(questId), handler.acquireNpc(questId), "接取 NPC");
			assertEquals(npcOf(questId), handler.rewardNpc(questId), "交付 NPC（真端同主）");
			assertEquals(0, handler.relayCount(questId), "无中继步");
			assertEquals(List.of(new SimpleTalkHandler.ItemStack(GATE_ITEM, 1)), handler.workItems(questId),
				"交付门 = quest.xml collect_item1 ×1");
			assertFalse(handler.unresolvedGate(questId), "交付门必须可解");
			assertNull(handler.acceptGiveItem(questId), "无接取发放");
			assertNull(handler.stepGiveItem(questId, 1), "无步进发放");
			assertNull(handler.stepRemoveItem(questId, 1), "无步进扣除");
			assertNull(handler.cutscene(questId), "无过场");
		}
	}

	@Test
	void questXmlDeclaresTheEventMetadata() {
		assertEventRow(ELYOS_QUEST, "pc_light", "STR_QUEST_ZONE19");
		assertEventRow(ASMODIAN_QUEST, "pc_dark", "STR_QUEST_ZONE20");
	}

	@Test
	void acceptAndHandInFollowTheRetailTalkLane() {
		for (int questId : quests()) {
			int npcId = npcOf(questId);
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

			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1009)), "报告被受理");
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus(),
				"未持有神符必须保持 START");
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_IN_PROGRESS);

			inventory.hold(GATE_ITEM, 1);
			NativeTalkFixture.clearPackets(player);
			assertTrue(itemHandler.onDialog(NativeTalkFixture.dialog(player, npcId, questId, 1009)), "交付报告");
			assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus());
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
			assertEquals(List.of("remove:" + GATE_ITEM + ":1"), inventory.calls(), "交付门按真端扣除");
		}
	}

	@Test
	void theRetailRowHasNoSystemGrantOrRefreshAxis() {
		SimpleTalkHandler handler = NativeTalkFixture.handler();
		for (int questId : quests()) {
			assertFalse(handler.isSystemGranted(questId), "quest " + questId + " 只能由 NPC 接取");
			assertEquals(0, handler.factionId(questId), "quest " + questId + " 不是阵营日常行");
			assertNull(handler.conQuest(questId), "quest " + questId + " 无链式接取窗");
			NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElseThrow();
			assertTrue(row.fields().keySet().stream().noneMatch(tag -> tag.contains("refresh")),
				"真端行不得声明刷新排程（活动子系表缺失，计划 §10.3-#3）: " + row.fields().keySet());
		}
	}

	private static void assertEventRow(int questId, String race, String zone) {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElseThrow();
		assertEquals("event", row.text("category1"), "真端类别");
		assertEquals(zone, row.text("category2"), "真端区域");
		assertEquals(10, row.integer("minlevel_permitted"), "真端等级下限");
		assertEquals(race, row.text("race_permitted"), "真端种族轴");
		assertEquals("1", row.text("max_repeat_count"), "一次性任务");
		assertEquals("world_event_lunar_scroll_shield_all_20a 1", row.text("collect_item1"), "真端收集物通道");
		assertEquals("world_event_lunar_scroll_shield_all_20a", row.text("inventory_item_name1"),
			"真端背包物通道");
		assertEquals("world_event_lunar_scroll_shield_all_20a 1", row.text("check_item1_1"), "真端交付门通道");
		assertEquals("world_event_add_title_153_14 1", row.text("reward_item1_1"), "真端称号道具奖励");
		assertEquals("0", row.text("reward_exp1"), "真端经验奖励为 0");
		assertEquals("0", row.text("reward_gold1"), "真端金币奖励为 0");
	}

	private static int[] quests() {
		return new int[] {ELYOS_QUEST, ASMODIAN_QUEST};
	}

	private static int npcOf(int questId) {
		return questId == ELYOS_QUEST ? ELYOS_NPC : ASMODIAN_NPC;
	}

	private static Race raceOf(int questId) {
		return questId == ELYOS_QUEST ? Race.ELYOS : Race.ASMODIANS;
	}
}
