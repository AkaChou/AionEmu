package com.aionemu.gameserver.questEngine.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.tablelane.CameraRegistry;
import com.aionemu.gameserver.questEngine.tablelane.NativeNpcNameResolver;
import com.aionemu.gameserver.questEngine.tablelane.NativeTalkFixture;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;

/**
 * 哈拉梅尔（Haramel）四件采集任务的交互物门控与交付主。
 * <p>
 * P4 重锚（计划 §8.9）：四行的真端列（接取/交付 NPC、{@code objectN}、{@code collect_itemN}）就是合同——
 * 交付门 = 整组真端交付物 + 相机逐列满值，交互物只在任务进行中可点击；旧 IR 断言（typed 节点、
 * SELECT5/SELECT6 页、39/20002 检查对、掉落 {@code collectingStep}）随本族切换批退场。
 * <p>
 * Native contract of the four Haramel collecting quests: the retail columns are the contract — the
 * hand-in gate is the whole retail collect group plus a full per-column camera, and the interaction
 * objects are clickable only while the quest runs.
 */
class QuestHaramelItemCollectingRegressionTest {

	/**
	 * 真端行（{@code Quest_SimpleCollectItem.xml} + {@code quest.xml}）：
	 * 接取/交付 NPC 名 + object 列名 + collect_item 列名（静态 id 一并冻结）。
	 * Retail rows: accept/hand-in npc names, object columns and collect_item columns.
	 */
	private static final List<QuestCase> CASES = List.of(
		new QuestCase(18501, 799522, "Shugo_IDNovice_1", 799523, "Shugo_IDNovice_2",
			List.of("IDNovice_FOBJ_ODBox", "IDNovice_Rough_Odum"),
			List.of("quest_18501a", "quest_18501b")),
		new QuestCase(28501, 799522, "Shugo_IDNovice_1", 799523, "Shugo_IDNovice_2",
			List.of("IDNovice_FOBJ_ODBox", "IDNovice_Rough_Odum"),
			List.of("quest_28501a", "quest_28501b")),
		new QuestCase(18503, 799523, "Shugo_IDNovice_2", 203166, "Zephyros",
			List.of("IDNovice_FOBJ_ODGrass"), List.of("quest_18503b")),
		new QuestCase(28503, 799523, "Shugo_IDNovice_2", 804605, "DF1a_Schwanz_E",
			List.of("IDNovice_FOBJ_ODGrass"), List.of("quest_28503b")));

	/** 真端四行的 collect_item1 计数都是 5。 / Every retail hand-in column of these rows counts five. */
	private static final int HAND_IN_COUNT = 5;

	@Test
	void haramelRowsCarryTheirOwnAcceptAndHandInNpcFromTheRetailColumns() {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		for (QuestCase questCase : CASES) {
			int questId = questCase.questId();
			assertTrue(handler.routes(questId), "任务必须由 native 车道路由: " + questId);
			assertEquals(questCase.acceptNpcId(), handler.acquireNpc(questId),
				"接取 NPC（真端 acquired_npc_name）: " + questId);
			assertEquals(questCase.handInNpcId(), handler.rewardNpc(questId),
				"交付 NPC（真端 reward_npc_name）: " + questId);
			assertEquals(questCase.acceptNpcId(), npc(questCase.acceptNpcName()),
				"接取 NPC 名必须唯一解析: " + questCase.acceptNpcName());
			assertEquals(questCase.handInNpcId(), npc(questCase.handInNpcName()),
				"交付 NPC 名必须唯一解析: " + questCase.handInNpcName());
			assertEquals(expectedObjects(questCase), handler.collectObjects(questId),
				"交互物必须逐列来自真端 objectN: " + questId);
			List<Integer> handIns = handler.handInItems(questId);
			assertEquals(expectedItems(questCase), handIns,
				"交付门必须是真端 collect_item 整组（同序同数量）: " + questId);
			List<Integer> requires = new ArrayList<>();
			for (int slot = 1; slot <= handIns.size(); slot++) {
				requires.add(CameraRegistry.instance().require(questId).required(slot));
			}
			assertEquals(List.of(HAND_IN_COUNT, HAND_IN_COUNT).subList(0, handIns.size()), requires,
				"相机槽 required 必须等于真端 collect_item 的计数: " + questId);
		}
	}

	@Test
	void interactionObjectsAreClickableOnlyWhileTheQuestRuns() {
		SimpleCollectItemHandler handler = SimpleCollectItemHandler.instance();
		for (QuestCase questCase : CASES) {
			int questId = questCase.questId();
			Player player = NativeTalkFixture.player();
			List<Integer> objects = handler.collectObjects(questId);
			for (int objectNpcId : objects) {
				assertFalse(handler.allowsItemUse(player, objectNpcId),
					"未接取时交互物不得可交互（真端无路由）: " + questId + " " + objectNpcId);
				assertFalse(handler.onObjectUse(player, questId, objectNpcId),
					"未接取时不得推进相机: " + questId + " " + objectNpcId);
			}
			NativeTalkFixture.start(player, questId);
			for (int objectNpcId : objects) {
				assertTrue(handler.allowsItemUse(player, objectNpcId),
					"进行中的交互物必须可交互: " + questId + " " + objectNpcId);
			}
			// 交付门是整组：单点一次仍然不满（真端 collect_item 计数 ≥1）。
			assertTrue(handler.onObjectUse(player, questId, objects.getFirst()),
				"进行中点击交互物必须推进: " + questId);
		}
	}

	private static List<Integer> expectedObjects(QuestCase questCase) {
		List<Integer> ids = new ArrayList<>();
		for (String name : questCase.objectNames()) {
			ids.addAll(NativeNpcNameResolver.instance().resolveMonsterIds(name));
		}
		return ids;
	}

	private static List<Integer> expectedItems(QuestCase questCase) {
		RetailItemNameIndex index;
		try {
			index = RetailItemNameIndex.loadItemTemplates();
		} catch (IOException e) {
			throw new AssertionError("retail item index unreadable", e);
		}
		List<Integer> ids = new ArrayList<>();
		for (String symbol : questCase.itemSymbols()) {
			Integer id = index.resolve(symbol);
			assertNotNull(id, () -> "真端物品符号必须解析: " + symbol);
			ids.add(id);
		}
		return ids;
	}

	private static int npc(String retailName) {
		List<Integer> ids = NativeNpcNameResolver.instance().resolve(retailName).npcIds();
		assertEquals(1, ids.size(), () -> "真端 NPC 名必须唯一解析: " + retailName);
		return ids.getFirst();
	}

	/** 真端一行：quest id + 接取/交付 NPC + object 列 + collect_item 列。 / One retail row. */
	private record QuestCase(int questId, int acceptNpcId, String acceptNpcName, int handInNpcId,
			String handInNpcName, List<String> objectNames, List<String> itemSymbols) {
	}
}
