package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;

/**
 * SimpleTalk 原生族门禁（计划 §7 P3 步骤 2；**本步未接线**，族门未开）。
 * <p>
 * 断言：① 真端 3152 行 100% 纳入处理器；② 未解析 NPC 名冻结为证据快照
 * （`p3/simple-talk-unresolved-npcs.tsv`：系统发放哨兵 / 测试行 / 复合单元格 / 数据缺口）；
 * ③ 表中继步索引与行集一致；④ cab520 接取入口；⑤ cabb10 中继只推当前步（乱序/重复零副作用）；
 * ⑥ 中继全满才开奖励窗；⑦ 缺行 fail-closed。
 * <p>
 * Gate for the SimpleTalk native family (plan §7 P3 step 2; not wired yet, family gate shut).
 */
class SimpleTalkNativeFamilyGateTest {

	private static final int EXPECTED_ROWS = 3152;
	/** 未唯一解析的 NPC 名数量（冻结证据：`p3/simple-talk-unresolved-npcs.tsv`）。 / Frozen unresolved count. */
	private static final int FROZEN_UNRESOLVED = 39;
	/** 接取列未解析行数（哨兵 181 + 数据缺口 26 行的接取面）。 / Rows whose acquire name stays unresolved. */
	private static final int FROZEN_UNRESOLVED_ACQUIRE_ROWS = 207;
	/** 交付列未解析行数。 / Rows whose reward name stays unresolved. */
	private static final int FROZEN_UNRESOLVED_REWARD_ROWS = 86;
	/** 系统发放哨兵的行数（接取列）。 / Row counts of the system-grant sentinels on the acquire column. */
	private static final Map<String, Integer> FROZEN_SENTINEL_ROWS = Map.of(
			"_faction_", 98, "_challengetask_", 75, "_area_", 8);
	/**
	 * 接取可解析但交付不可解析的行数（真端表交付列落在 DATA_GAP 名上，属静态数据缺口而非分派缺陷）。
	 * Rows with a resolvable acquire NPC but a gapped reward NPC: a static-data gap on the retail reward column.
	 */
	private static final int FROZEN_ACQUIRE_OK_REWARD_GAP_ROWS = 9;
	/** 三段中继 + 三个发物 + item_check 的真端行。 / A three-relay retail row. */
	private static final int CHAINED_QUEST = 41536;
	/** 单中继步 + 接取发放 + 步内发放/扣除的真端行（与 cab520/cabb10 立即数对拍）。 / Retail row 1131. */
	private static final int ITEM_QUEST = 1131;
	/**
	 * 未解析物品面冻结（`p3/simple-talk-unresolved-items.tsv`）：11 个交付门缺口 + 3 个部分缺口符号。
	 * Frozen unresolved item face (evidence snapshot in `p3/simple-talk-unresolved-items.tsv`).
	 */
	private static final Set<String> FROZEN_UNRESOLVED_ITEMS = Set.of(
			"item_check:2732", "item_check:30509", "item_check:41571",
			"item_check:50011", "item_check:50012", "item_check:51011", "item_check:51012",
			"item_check:16921", "item_check:26921", "item_check:16930", "item_check:26930",
			"item_exp_extraction_65a 1", "item_idunderrune_quest_01 20", "item_idruneweapon_quest_01 10");

	private static SimpleTalkHandler handler;
	private static SimpleTalkHandler itemHandler;
	private static FakeInventoryPort inventory;

	@BeforeAll
	static void setUp() throws Exception {
		handler = SimpleTalkHandler.instance();
		inventory = new FakeInventoryPort();
		itemHandler = new SimpleTalkHandler(NativeQuestTableLoader.instance(), NativeNpcNameResolver.instance(),
				RetailItemNameIndex.loadItemTemplates(), NativeQuestXmlTable.instance(), inventory);
	}

	@Test
	void everyRetailTalkRowIsOwned() {
		assertEquals(EXPECTED_ROWS, handler.ownedQuestCount());
		assertEquals(FROZEN_UNRESOLVED, handler.unresolvedNames().size(),
				() -> "未解析名集合漂移，实际=" + handler.unresolvedNames());
		assertTrue(handler.unresolvedNames().containsAll(List.of("_area_", "_faction_", "_challengetask_")),
				"系统发放哨兵必须在未解析面内 / system-grant sentinels must be present");

		// 未解析面按行冻结：接取 207 行 / 交付 86 行，且哨兵行数逐名对拍（表侧实测）。
		Map<String, Integer> acquireRows = new TreeMap<>();
		Map<String, Integer> rewardRows = new TreeMap<>();
		long acquireResolved = 0;
		long rewardResolved = 0;
		for (int questId : handler.ownedQuestIds()) {
			NativeQuestTableLoader.SimpleTalkRow row = handler.requireRow(questId);
			if (handler.acquireNpc(questId) == null) {
				acquireRows.merge(row.acquiredNpcName(), 1, Integer::sum);
			} else {
				acquireResolved++;
			}
			if (handler.rewardNpc(questId) == null) {
				rewardRows.merge(row.rewardNpcName(), 1, Integer::sum);
			} else {
				rewardResolved++;
			}
		}
		assertEquals(FROZEN_UNRESOLVED_ACQUIRE_ROWS, EXPECTED_ROWS - acquireResolved,
				"接取列未解析行数漂移 / unresolved acquire rows drifted");
		assertEquals(FROZEN_UNRESOLVED_REWARD_ROWS, EXPECTED_ROWS - rewardResolved,
				"交付列未解析行数漂移 / unresolved reward rows drifted");
		for (Map.Entry<String, Integer> sentinel : FROZEN_SENTINEL_ROWS.entrySet()) {
			assertEquals(sentinel.getValue(), acquireRows.get(sentinel.getKey()),
					"哨兵行数漂移 / sentinel row count drifted: " + sentinel.getKey());
		}
		long gappedReward = handler.ownedQuestIds().stream()
				.filter(qid -> handler.acquireNpc(qid) != null && handler.rewardNpc(qid) == null)
				.count();
		assertEquals(FROZEN_ACQUIRE_OK_REWARD_GAP_ROWS, gappedReward,
				"接取可解析但交付缺口的行数漂移 / rows with acquire ok but reward gapped drifted, actual=" + gappedReward);
	}

	@Test
	void relayIndexMatchesTheTableChains() {
		// 表侧实测：468 行带中继链（talk_npc1），talk_npc2/3 分别为 193/68。
		// Measured: 468 rows carry a relay chain.
		assertEquals(468, handler.ownedQuestIds().stream().filter(qid -> handler.relayCount(qid) > 0).count());
		assertEquals(0, handler.relayCount(85), "85 为直交形 / quest 85 is a direct hand-in");
		assertEquals(3, handler.relayCount(CHAINED_QUEST));
		for (String relayName : handler.requireRow(CHAINED_QUEST).talkNpcNames()) {
			assertTrue(handler.relaysForNpc(resolve(relayName)).stream()
					.anyMatch(relay -> relay.questId() == CHAINED_QUEST),
				"中继 NPC 必须挂回任务 / relay npc must index back to the quest: " + relayName);
		}
		// 真端页阶梯：SELECT2/SELECT3/SELECT4。
		assertEquals(1352, SimpleTalkHandler.pageForStep(1));
		assertEquals(1693, SimpleTalkHandler.pageForStep(2));
		assertEquals(2034, SimpleTalkHandler.pageForStep(3));
		assertThrows(IllegalArgumentException.class, () -> SimpleTalkHandler.pageForStep(4));
	}

	@Test
	void missingRowsFailClosed() {
		assertFalse(handler.owns(999999));
		assertThrows(IllegalStateException.class, () -> handler.requireRow(999999));
	}

	@Test
	void acceptEntryFollowsTheRetailCab520Routing() {
		Player player = createTestPlayer();
		int questId = CHAINED_QUEST;
		Npc acquire = createMockNpc(handler.acquireNpc(questId));

		// QUEST_SELECT → 问询页（cab520 的入口动作）。
		assertTrue(handler.onDialog(new QuestEnv(acquire, player, questId, 31)));
		// 非接取 NPC 不响应（真端按节点槽分派，不跨 NPC）。
		assertFalse(handler.onDialog(new QuestEnv(createMockNpc(1), player, questId, 31)));
		// 1002/20000 的接取落库依赖生产数据持有者（QuestService.questsData），由启动门覆盖。
		assertNull(player.getQuestStateList().getQuestState(questId));
	}

	@Test
	void relayStepsAdvanceOnlyTheCurrentStep() {
		Player player = createTestPlayer();
		int questId = CHAINED_QUEST;
		List<String> relayNames = itemHandler.requireRow(questId).talkNpcNames();
		Npc first = createMockNpc(resolve(relayNames.get(0)));
		Npc second = createMockNpc(resolve(relayNames.get(1)));
		Npc third = createMockNpc(resolve(relayNames.get(2)));
		Npc reward = createMockNpc(itemHandler.rewardNpc(questId));

		QuestState state = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, state);

		// 乱序：第 2 步的动作在第 1 步之前零推进。
		assertTrue(itemHandler.onDialog(new QuestEnv(second, player, questId, 10001)));
		assertEquals(0, state.getQuestVars().getQuestVars(), "乱序动作不得推进 / out-of-order must not advance");

		// 顺序推进 + 重复幂等。
		assertTrue(itemHandler.onDialog(new QuestEnv(first, player, questId, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars());
		assertTrue(itemHandler.onDialog(new QuestEnv(first, player, questId, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars(), "重复动作幂等 / repeated action is idempotent");

		// 中继未满：报告门不放行。
		assertTrue(itemHandler.onDialog(new QuestEnv(reward, player, questId, 1009)));
		assertEquals(QuestStatus.START, state.getStatus(), "中继未满不得进入 REWARD / report gate holds");

		// 走完全部中继 → 持有交付门物品 → 报告 → REWARD。
		assertTrue(itemHandler.onDialog(new QuestEnv(second, player, questId, 10001)));
		assertTrue(itemHandler.onDialog(new QuestEnv(third, player, questId, 10002)));
		assertEquals(3, state.getQuestVars().getQuestVars());
		for (SimpleTalkHandler.ItemStack item : itemHandler.workItems(questId)) {
			inventory.held.put(item.itemId(), (long) item.count());
		}
		assertTrue(itemHandler.onDialog(new QuestEnv(reward, player, questId, 1009)));
		assertEquals(QuestStatus.REWARD, state.getStatus());
	}

	/** 物品面：真端 cab520/cabb10 的物品通道与 item_check 交付门（含冻结缺口）。 */
	@Test
	void itemFaceFollowsTheRetailChannels() {
		assertEquals(FROZEN_UNRESOLVED_ITEMS, handler.unresolvedItemSymbols(),
				() -> "未解析物品面漂移，实际=" + handler.unresolvedItemSymbols());

		// 1131 ↔ cab520 立即数 182200506（接取发放）与 cabb10 立即数 182200507/182200506（步内发/扣）。
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.acceptGiveItem(ITEM_QUEST));
		assertEquals(new SimpleTalkHandler.ItemStack(182200507, 1), handler.stepGiveItem(ITEM_QUEST, 1));
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.stepRemoveItem(ITEM_QUEST, 1));
		assertTrue(handler.workItems(ITEM_QUEST).isEmpty(), "1131 无 item_check / no hand-in gate");

		// 41536 的交付门 = quest.xml collect_item1..3。
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(182212534, 1),
				new SimpleTalkHandler.ItemStack(182212535, 1), new SimpleTalkHandler.ItemStack(182212536, 1)),
				handler.workItems(CHAINED_QUEST));

		// 1988 个 item_check 行中 11 行门不可解（缺口冻结），其余 1977 行门成立。
		assertEquals(1977, handler.ownedQuestIds().stream().filter(id -> !handler.workItems(id).isEmpty()).count());
	}

	/** 中继步按真端顺序发放/扣除物品；交付门未持有则不放行。 */
	@Test
	void stepItemsAndHandInGateFollowTheRetailOrder() {
		int questId = ITEM_QUEST;
		Player player = createTestPlayer();
		QuestState state = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, state);
		Npc relay = createMockNpc(resolve(itemHandler.requireRow(questId).talkNpcNames().getFirst()));

		inventory.calls.clear();
		assertTrue(itemHandler.onDialog(new QuestEnv(relay, player, questId, 10000)));
		assertEquals(List.of("give:182200507:1", "remove:182200506:1"), inventory.calls,
				"步进发放/扣除非真端顺序 / step item order deviates from retail");

		// 41536：中继全满但未持有交付门物品 → 不放行（页 10，状态仍 START）。
		int gated = CHAINED_QUEST;
		Player gatedPlayer = createTestPlayer();
		QuestState gatedState = new QuestState(gated, QuestStatus.START, 3, 0, null, 0, null);
		gatedPlayer.getQuestStateList().addQuest(gated, gatedState);
		Npc rewardNpc = createMockNpc(itemHandler.rewardNpc(gated));
		inventory.clear();
		assertTrue(itemHandler.onDialog(new QuestEnv(rewardNpc, gatedPlayer, gated, 1009)));
		assertEquals(QuestStatus.START, gatedState.getStatus(), "交付门未持有不得进入 REWARD / gate holds");

		// 持有全部交付门物品 → REWARD + 按门扣除。
		for (SimpleTalkHandler.ItemStack item : itemHandler.workItems(gated)) {
			inventory.held.put(item.itemId(), (long) item.count());
		}
		assertTrue(itemHandler.onDialog(new QuestEnv(rewardNpc, gatedPlayer, gated, 1009)));
		assertEquals(QuestStatus.REWARD, gatedState.getStatus());
		assertEquals(List.of("remove:182212534:1", "remove:182212535:1", "remove:182212536:1"), inventory.calls);
	}

	/** 记录式假背包端口（族门用）。 / Recording fake inventory port for the family gate. */
	private static final class FakeInventoryPort implements NativeInventoryPort {
		private final Map<Integer, Long> held = new LinkedHashMap<>();
		private final List<String> calls = new ArrayList<>();

		void clear() {
			held.clear();
			calls.clear();
		}

		@Override
		public long count(Player player, int itemId) {
			return held.getOrDefault(itemId, 0L);
		}

		@Override
		public void give(Player player, int itemId, int count) {
			calls.add("give:" + itemId + ":" + count);
			held.merge(itemId, (long) count, Long::sum);
		}

		@Override
		public boolean remove(Player player, int itemId, int count) {
			calls.add("remove:" + itemId + ":" + count);
			long current = held.getOrDefault(itemId, 0L);
			if (current < count) {
				return false;
			}
			held.put(itemId, current - count);
			return true;
		}
	}

	private static int resolve(String npcName) {
		NativeNpcNameResolver.Match match = NativeNpcNameResolver.instance().resolve(npcName);
		assertEquals(NativeNpcNameResolver.Resolution.UNIQUE, match.resolution(), "NPC 名必须唯一解析: " + npcName);
		return match.npcIds().get(0);
	}

	private static Player createTestPlayer() {
		Player player = new ObjenesisStd().newInstance(Player.class);
		PlayerCommonData pcd = new PlayerCommonData(10001);
		pcd.setRace(Race.ELYOS);
		pcd.setGender(Gender.MALE);
		try {
			java.lang.reflect.Field field = Player.class.getDeclaredField("playerCommonData");
			field.setAccessible(true);
			field.set(player, pcd);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		player.setQuestStateList(new QuestStateList());
		return player;
	}

	private static Npc createMockNpc(int npcId) {
		Npc npc = new ObjenesisStd().newInstance(Npc.class);
		try {
			var idField = com.aionemu.gameserver.model.gameobjects.AionObject.class.getDeclaredField("objectId");
			idField.setAccessible(true);
			idField.set(npc, 9999000 + npcId);

			NpcTemplate template = new NpcTemplate();
			var templateId = NpcTemplate.class.getDeclaredField("npcId");
			templateId.setAccessible(true);
			templateId.set(template, npcId);

			var objectTemplate = com.aionemu.gameserver.model.gameobjects.VisibleObject.class
					.getDeclaredField("objectTemplate");
			objectTemplate.setAccessible(true);
			objectTemplate.set(npc, template);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return npc;
	}
}
