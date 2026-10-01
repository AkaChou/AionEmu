package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.CameraRegistry;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.RawQuestVarsCodec;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;

/**
 * 18501（상인 슈고의 요청_천）两个哈拉梅尔交互物的原生采集合同。
 * <p>
 * P4 重锚（计划 §8.9）：真端 {@code Quest_SimpleCollectItem.xml} 的两列
 * （{@code object1}=IDNovice_FOBJ_ODBox / {@code object2}=IDNovice_Rough_Odum）与 {@code quest.xml} 的
 * {@code collect_item1/2}（各 5 件）在同一下标上成线，故对象点击只推进自己那一槽；旧 IR 断言
 * （typed 节点 / 掉落 {@code collectingStep} 形状 / SELECT 页链）随本族切换批退场。
 * <p>
 * Native collect contract of 18501's two Haramel interaction objects: the retail table's object columns
 * and the {@code quest.xml} hand-in columns line up on the same index, so each object advances only its
 * own camera slot. Positive end-to-end coverage (with an injected inventory port) lives in
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
	void bothOdiumObjectsCarryTheirOwnSlotFromTheRetailColumns() throws IOException {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		assertTrue(handler.routes(QUEST_ID), "18501 必须由 native 车道路由");
		assertEquals(ACCEPT_NPC, handler.acquireNpc(QUEST_ID), "真端接取 NPC");
		assertEquals(REWARD_NPC, handler.rewardNpc(QUEST_ID), "真端交付 NPC");
		assertEquals(List.of(objectId(OD_BOX), objectId(ODUM)), handler.collectObjects(QUEST_ID),
			"两列对象必须逐列解析为静态数据 id");
		assertEquals(List.of(itemId(OD_BOX_ITEM), itemId(ODUM_ITEM)), handler.handInItems(QUEST_ID),
			"两列交付物必须逐列来自真端 collect_item1/2");
		assertEquals(Map.of(1, OD_BOX_COUNT, 2, ODUM_COUNT),
			CameraRegistry.instance().require(QUEST_ID).slotRequires(),
			"相机槽 1/2 的 required 必须等于真端 collect_item1/2 的计数");
	}

	@Test
	void eachObjectAdvancesOnlyItsOwnColumnAndSaturatesThere() throws IOException {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		Player player = NativeTalkFixture.player();
		int odBox = objectId(OD_BOX);
		int odum = objectId(ODUM);

		// 未接取：零推进（真端该对象只在任务进行中响应）。
		assertFalse(handler.onObjectUse(player, QUEST_ID, odBox), "未接取不得推进相机");
		assertFalse(handler.onObjectUse(player, QUEST_ID, odum), "未接取不得推进相机");

		NativeTalkFixture.start(player, QUEST_ID);
		CameraRegistry.CameraRow camera = CameraRegistry.instance().require(QUEST_ID);
		for (int index = 0; index < OD_BOX_COUNT; index++) {
			assertTrue(handler.onObjectUse(player, QUEST_ID, odBox),
				"槽 1 第 " + (index + 1) + " 次点击必须推进");
		}
		assertFalse(handler.onObjectUse(player, QUEST_ID, odBox), "槽 1 满值后不得超发");
		int afterSlotOne = vars(player);
		assertEquals(OD_BOX_COUNT, RawQuestVarsCodec.slotValue(camera.width(), afterSlotOne, 1),
			"object1 只能推进槽 1");
		assertEquals(0, RawQuestVarsCodec.slotValue(camera.width(), afterSlotOne, 2),
			"object1 不得推进槽 2");

		for (int index = 0; index < ODUM_COUNT; index++) {
			assertTrue(handler.onObjectUse(player, QUEST_ID, odum),
				"槽 2 第 " + (index + 1) + " 次点击必须推进");
		}
		assertFalse(handler.onObjectUse(player, QUEST_ID, odum), "槽 2 满值后不得超发");
		int bothFull = vars(player);
		assertEquals(OD_BOX_COUNT, RawQuestVarsCodec.slotValue(camera.width(), bothFull, 1));
		assertEquals(ODUM_COUNT, RawQuestVarsCodec.slotValue(camera.width(), bothFull, 2),
			"object2 只能推进槽 2");
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(QUEST_ID).getStatus(),
			"采集族满值仍留在 START（交付 NPC 处才翻 REWARD）");
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
