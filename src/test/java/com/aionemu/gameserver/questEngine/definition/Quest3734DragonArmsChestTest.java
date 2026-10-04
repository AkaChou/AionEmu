package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.CameraRegistry;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;

/**
 * 3734（노흐사나_FOBJ 수집）龙之臂宝箱的原生采集合同。
 * <p>
 * 2026-10-04 修正（物品驱动）：真端 {@code Quest_SimpleCollectItem.xml} 行
 * （接取/交付同主 {@code LF2_Brando_E_LHM}，{@code object1=IDAB1_MiniCastle_DragonArms_Q3704}，
 * {@code quest.xml} {@code collect_item1=quest_3704a 4}）就是该任务的唯一事实；宝箱只在任务进行中可交互
 * （真端 ACTION_ITEM_USE 的 START 态判定），交互只认领（零状态写），收集由掉落列发放（上限=collect_item）。
 * <p>
 * Native collect contract of quest 3734's dragon-arms chest: the retail row is the single source of
 * truth, the chest is usable only while the quest is in progress, and interactions only claim.
 */
class Quest3734DragonArmsChestTest {

	private static final int QUEST_ID = 3734;
	/** 真端接取/交付同主。 / The retail accept and hand-in NPC. */
	private static final String QUEST_NPC = "LF2_Brando_E_LHM";
	/** 真端 object1 列（龙之臂宝箱）。 / The retail object column. */
	private static final String DRAGON_ARMS_CHEST = "IDAB1_MiniCastle_DragonArms_Q3704";
	/** 真端 collect_item1 列（4 件）。 / The retail hand-in column (four). */
	private static final String CHEST_ITEM = "quest_3704a";
	private static final int CHEST_COUNT = 4;

	@Test
	void dragonArmsChestIsTheNativeCollectTargetOfTheRetailRow() throws IOException {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		int questNpc = npc(QUEST_NPC);
		assertTrue(handler.routes(QUEST_ID), "3734 必须由 native 车道路由");
		assertEquals(questNpc, handler.acquireNpc(QUEST_ID), "真端接取 NPC");
		assertEquals(questNpc, handler.rewardNpc(QUEST_ID), "真端交付 NPC（与接取同主）");
		assertEquals(List.of(objectId()), handler.collectObjects(QUEST_ID), "宝箱必须解析为静态数据 id");
		assertEquals(List.of(itemId()), handler.handInItems(QUEST_ID), "交付物必须来自真端 collect_item1");
		// 采集族真端无相机（2026-10-04）：采集走掉落列（宝箱交互由掉落列表发放 quest_3704a）。
		int chestItem = itemId();
		assertTrue(handler.questDropsFor(objectId()).stream()
			.anyMatch(drop -> drop.questId() == QUEST_ID && drop.itemId() == chestItem),
			"宝箱必须携带 quest_3704a 的真端掉落条目");
		assertTrue(CameraRegistry.instance().find(QUEST_ID).isEmpty(),
			"采集族不得派生相机行（真端 262/262 无相机调用）");
	}

	@Test
	void chestIsUsableOnlyAfterAcceptanceAndClaimsWithoutWritingState() throws IOException {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		Player player = NativeTalkFixture.player();
		int chest = objectId();

		// 未接取：宝箱不可交互（真端无任务路由），点击零认领。
		assertFalse(handler.allowsItemUse(player, chest), "未接取时宝箱不得可交互");
		assertFalse(handler.onObjectUse(player, QUEST_ID, chest), "未接取不得认领");

		NativeTalkFixture.start(player, QUEST_ID);
		assertTrue(handler.allowsItemUse(player, chest), "进行中的宝箱必须可交互");
		// 交互认领（掉落链由交互物 AI 接手）：每次开箱 true、vars 恒零写（物品驱动）。
		for (int index = 0; index < CHEST_COUNT; index++) {
			assertTrue(handler.onObjectUse(player, QUEST_ID, chest), "第 " + (index + 1) + " 次开箱必须认领");
		}
		assertTrue(handler.onObjectUse(player, QUEST_ID, chest), "重复开箱同样认领（掉落上限在掉落链一侧）");
		assertEquals(0, vars(player), "物品驱动：交互零状态写（var0 保持）");
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"采集不改变状态，交付 NPC 处才翻 REWARD");
	}

	private static int vars(Player player) {
		return player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars();
	}

	private static int objectId() {
		List<Integer> ids = NativeNpcNameResolver.instance().resolveMonsterIds(DRAGON_ARMS_CHEST);
		assertEquals(1, ids.size(), () -> "真端对象名必须唯一解析: " + DRAGON_ARMS_CHEST);
		return ids.getFirst();
	}

	private static int npc(String retailName) {
		var match = NativeNpcNameResolver.instance().resolve(retailName);
		assertEquals(NativeNpcNameResolver.Resolution.UNIQUE, match.resolution(),
			() -> "真端 NPC 名必须唯一解析: " + retailName);
		return match.npcIds().getFirst();
	}

	private static int itemId() throws IOException {
		Integer id = RetailItemNameIndex.loadItemTemplates().resolve(CHEST_ITEM);
		assertNotNull(id, () -> "真端物品符号必须解析: " + CHEST_ITEM);
		return id;
	}
}
