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
 * 18501（상인 슈고의 요청_천）两个哈拉梅尔交互物的原生采集合同。
 * <p>
 * 2026-10-04 修正（物品驱动）：真端 {@code Quest_SimpleCollectItem.xml} 的两列
 * （{@code object1}=IDNovice_FOBJ_ODBox / {@code object2}=IDNovice_Rough_Odum）与 {@code quest.xml} 的
 * {@code collect_item1/2}（各 5 件）在生产侧成线——对象交互只认领（零状态写），收集由掉落列
 * （掉落列表经 {@code isQuestDrop} 的 collect_item 上限）发放；采集族真端无相机（262/262 无调用）。
 * <p>
 * Native collect contract of 18501's two Haramel interaction objects: interactions only claim (no
 * state write) and the drop chain grants the items, capped by the {@code collect_item} requirement;
 * the collect family carries no retail camera. Positive end-to-end coverage lives in
 * {@code SimpleCollectItemNativeFamilyGateTest}; this class asserts the production wiring.
 */
class Quest18501InteractionObjectTest {

	private static final int QUEST_ID = 18501;
	/** Shugo_IDNovice_1 / Shugo_IDNovice_2（真端 acquired/reward 列 → 静态 npc_template）。 */
	private static final int ACCEPT_NPC = 799522;
	private static final int REWARD_NPC = 799523;
	/** 真端 object1 / object2 列名。 / The retail object columns. */
	private static final String OD_BOX = "IDNovice_FOBJ_ODBox";
	private static final String ODUM = "IDNovice_Rough_Odum";
	/** 真端 collect_item1 / collect_item2 列（各 5 件）。 / The retail hand-in columns (five each). */
	private static final String OD_BOX_ITEM = "quest_18501a";
	private static final String ODUM_ITEM = "quest_18501b";
	private static final int OD_BOX_COUNT = 5;
	private static final int ODUM_COUNT = 5;

	@Test
	void bothOdiumObjectsAndColumnsResolveFromTheRetailTable() throws IOException {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		assertTrue(handler.routes(QUEST_ID), "18501 必须由 native 车道路由");
		assertEquals(ACCEPT_NPC, handler.acquireNpc(QUEST_ID), "真端接取 NPC");
		assertEquals(REWARD_NPC, handler.rewardNpc(QUEST_ID), "真端交付 NPC");
		assertEquals(List.of(objectId(OD_BOX), objectId(ODUM)), handler.collectObjects(QUEST_ID),
			"两列对象必须逐列解析为静态数据 id");
		assertEquals(List.of(itemId(OD_BOX_ITEM), itemId(ODUM_ITEM)), handler.handInItems(QUEST_ID),
			"两列交付物必须逐列来自真端 collect_item1/2");
		// 采集族真端无相机（2026-10-04）：采集走掉落列（对象交互由 QuestItemNpcAI2 掉落列表发放），
		// 两对象各自携带自己的掉落条目（物品与交付列同源）。
		int odBoxItem = itemId(OD_BOX_ITEM);
		int odumItem = itemId(ODUM_ITEM);
		assertTrue(handler.questDropsFor(objectId(OD_BOX)).stream()
			.anyMatch(drop -> drop.questId() == QUEST_ID && drop.itemId() == odBoxItem),
			"object1 必须携带 quest_18501a 的真端掉落条目");
		assertTrue(handler.questDropsFor(objectId(ODUM)).stream()
			.anyMatch(drop -> drop.questId() == QUEST_ID && drop.itemId() == odumItem),
			"object2 必须携带 quest_18501b 的真端掉落条目");
		assertTrue(CameraRegistry.instance().find(QUEST_ID).isEmpty(),
			"采集族不得派生相机行（真端 262/262 无相机调用）");
	}

	@Test
	void eachObjectClaimsWithoutWritingState() throws IOException {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		Player player = NativeTalkFixture.player();
		int odBox = objectId(OD_BOX);
		int odum = objectId(ODUM);

		// 未接取：零认领（真端该对象只在任务进行中响应）。
		assertFalse(handler.onObjectUse(player, QUEST_ID, odBox), "未接取不得认领");
		assertFalse(handler.onObjectUse(player, QUEST_ID, odum), "未接取不得认领");

		NativeTalkFixture.start(player, QUEST_ID);
		// 交互认领（掉落链由交互物 AI 接手）：每次点击 true、vars 恒零写（物品驱动）。
		// Claim-only interactions: every click returns true and writes no state.
		for (int index = 0; index < OD_BOX_COUNT; index++) {
			assertTrue(handler.onObjectUse(player, QUEST_ID, odBox),
				"槽 1（object1）第 " + (index + 1) + " 次点击必须认领");
		}
		assertEquals(0, vars(player), "物品驱动：交互零状态写（var0 保持）");
		assertTrue(handler.onObjectUse(player, QUEST_ID, odum), "object2 同样认领");
		assertEquals(0, vars(player), "object2 同样零写");
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"采集不改变状态（交付 NPC 处才翻 REWARD）");
	}

	private static int vars(Player player) {
		return player.getQuestStateList().getQuestState(QUEST_ID).getQuestVars().getQuestVars();
	}

	private static int objectId(String retailName) {
		List<Integer> ids = NativeNpcNameResolver.instance().resolveMonsterIds(retailName);
		assertEquals(1, ids.size(), () -> "真端对象名必须唯一解析: " + retailName);
		assertNotNull(ids.getFirst());
		return ids.getFirst();
	}

	private static int itemId(String symbol) throws IOException {
		Integer id = RetailItemNameIndex.loadItemTemplates().resolve(symbol);
		assertNotNull(id, () -> "真端物品符号必须解析: " + symbol);
		return id;
	}
}
