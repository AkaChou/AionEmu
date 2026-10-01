package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader.SimpleItemPlayRow;

/**
 * SimpleItemPlay 原生表驱动家族门禁（计划 §6.6 / §7 P5 切换批）。
 * <p>
 * 断言面全部来自真端表行 + 真端 {@code quest.xml} + 生产静态数据 id，不合成语义：
 * <ol>
 *   <li>43 行全量装载与列填充率冻结（接取/交付 NPC 100%、{@code give_item} 34 行、中继链长尾）；</li>
 *   <li>注册/路由分解（owns 43 = routed 6 + 不可路由 37：未退役行与中继/cutscene 长尾行）；</li>
 *   <li>接取（页 4 问询 + 1002 提交发演出道具）→ 用物一步进 REWARD → 交付预览回收道具 → 领奖闭环；</li>
 *   <li>失败面 fail-closed：未接取时用物零副作用、越界按钮不结算。</li>
 * </ol>
 * <p>
 * SimpleItemPlay native family gate: every assertion is sourced from the retail row, the retail
 * {@code quest.xml} or production static-data ids.
 */
class SimpleItemPlayNativeFamilyGateTest {

	/** 真端接取 = 交付 NPC 的演出任务（19048：Andreas 发 doc 道具 → 用后回交）。 / Retail item-play row. */
	private static final int PLAY_QUEST = 19048;
	/** 本批路由集（真端退役 6 行）。 / The six routed rows. */
	private static final Set<Integer> ROUTED_ROWS =
		Set.of(13704, 13708, 19048, 23704, 23708, 29048);

	private static NativeQuestTableLoader loader;
	private static SimpleItemPlayHandler handler;

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		handler = SimpleItemPlayHandler.instance();
	}

	// ---------------------------------------------------------------- 装载面

	@Test
	void loadsAll43RetailRowsWithTheNpcAcceptShape() {
		assertEquals(43, loader.itemPlaySize(), "真端 quest_simpleitemplays 全量行");
		List<SimpleItemPlayRow> rows = List.copyOf(loader.itemPlayRows());
		assertTrue(rows.stream().allMatch(row ->
			!row.acquiredNpcName().isBlank() && !row.rewardNpcName().isBlank()),
			"acquired_npc_name/reward_npc_name 必须 100%（真端 43/43）");
		assertEquals(41, rows.stream().filter(row -> row.useItemName() != null).count(), "use_item_name 覆盖 41 行");
		assertEquals(34, rows.stream().filter(row -> row.acceptGiveItem() != null).count(), "give_item 覆盖 34 行");
		assertEquals(11, rows.stream().filter(row -> row.talkNpcNames().size() >= 1).count(), "talk_npc1 覆盖 11 行");
		assertEquals(6, rows.stream().filter(row -> row.talkNpcNames().size() >= 2).count(), "talk_npc2 覆盖 6 行");
		assertEquals(9, rows.stream().filter(row -> row.conQuest() != null).count(), "con_quest 覆盖 9 行");
		assertEquals(2, rows.stream().filter(row -> row.cutsceneId() != null).count(), "cutsceneid1 覆盖 2 行");
		assertEquals(7, rows.stream().filter(row -> declared(row.stepGiveItems(), 1)).count(), "give_item1 覆盖 7 行");
		assertEquals(5, rows.stream().filter(row -> declared(row.stepGiveItems(), 2)).count(), "give_item2 覆盖 5 行");
		assertEquals(1, rows.stream().filter(row -> declared(row.stepRemoveItems(), 1)).count(), "remove_item1 覆盖 1 行");
		assertEquals(4, rows.stream().filter(row -> declared(row.stepRemoveItems(), 2)).count(), "remove_item2 覆盖 4 行");
	}

	@Test
	void routingSplitIsFrozenToTheRetiredRows() {
		assertEquals(43, handler.ownedQuestIds().size(), "注册集 = 真端表全量行");
		assertEquals(ROUTED_ROWS, handler.routedQuestIds(), "路由集 = 真端退役且接取/交付/道具全可解的 6 行");
		assertEquals(37, handler.unroutableQuestIds().size(), "不可路由行 = 43 − 6");
		assertTrue(new TreeSet<>(handler.routedQuestIds()).containsAll(ROUTED_ROWS));
		for (int questId : ROUTED_ROWS) {
			assertNotNull(handler.acquireNpc(questId), "路由行必须有接取 NPC: " + questId);
			assertFalse(handler.rewardNpcs(questId).isEmpty(), "路由行必须有交付 NPC: " + questId);
			assertNotNull(handler.playItemId(questId), "路由行必须有演出道具: " + questId);
			assertFalse(handler.routedQuestIds().contains(questId) == handler.unroutableQuestIds().contains(questId),
				"路由与不可路由必须互斥: " + questId);
		}
		// 未退役行的接取/交付面不得进路由集（单一 owner 不变量）。
		for (SimpleItemPlayRow row : loader.itemPlayRows()) {
			if (!com.aionemu.gameserver.questEngine.definition.RetiredQuestIds.contains(row.questId())) {
				assertFalse(handler.routes(row.questId()), "未退役行不得由 native 车道路由: " + row.questId());
			}
		}
	}

	@Test
	void routedRowsGrantTheSameItemTheyPlay() {
		for (int questId : ROUTED_ROWS) {
			SimpleItemPlayRow row = loader.requireItemPlay(questId);
			ItemStack acceptGive = handler.acceptGiveItem(questId);
			assertNotNull(acceptGive, "真端 give_item（接取即发演出道具）: " + questId);
			assertEquals(row.useItemName(), row.acceptGiveItem(), "真端两列必须同源: " + questId);
			assertEquals(handler.playItemId(questId), acceptGive.itemId(),
				"接取发放的道具必须是演出道具本体: " + questId);
		}
	}

	// ---------------------------------------------------------------- 行为面

	@Test
	void acceptOpensTheClientEntryPageGrantsThePlayItemAndStartsTheRow() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleItemPlayHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		// 19048 真端 minlevel_permitted = 29（真端 CanAcquireQuest 轴），故用 30 级玩家过接取门。
		// Row 19048 carries the retail minlevel_permitted axis, so the accept test uses a level-30 player.
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 30);
		int acquireNpc = local.acquireNpc(PLAY_QUEST);
		ItemStack playItem = local.acceptGiveItem(PLAY_QUEST);
		assertNotNull(playItem, "19048 接取即发演出道具");

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, PLAY_QUEST, 31)),
			"交付 NPC 的 QUEST_SELECT 必须下发客户端声明的接取入口页");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(PLAY_QUEST));

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, PLAY_QUEST, 1002)),
			"1002 提交必须由 native 接取口服务");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.PAGE_ACCEPTED);
		QuestState state = player.getQuestStateList().getQuestState(PLAY_QUEST);
		assertNotNull(state, "接取后必须建档");
		assertEquals(QuestStatus.START, state.getStatus());
		assertEquals(List.of("give:" + playItem.itemId() + ":" + playItem.count()), inventory.calls(),
			"接取提交即发真端 give_item");
	}

	@Test
	void usingThePlayItemAdvancesToRewardInOneStep() {
		SimpleItemPlayHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, PLAY_QUEST);
		int playItem = local.playItemId(PLAY_QUEST);

		assertTrue(local.advanceQuestIdsForItem(playItem).contains(PLAY_QUEST),
			"演出道具必须指回该行（真端 one-item 推进）");
		assertTrue(local.onItemUse(player, playItem), "用物必须一步推进");
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(PLAY_QUEST).getStatus());
		assertFalse(local.onItemUse(player, playItem), "REWARD 态再次用物零副作用");
	}

	@Test
	void itemUseWithoutTheRowIsANoOp() {
		SimpleItemPlayHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		int playItem = local.playItemId(PLAY_QUEST);
		assertFalse(local.onItemUse(player, playItem), "未接取时用物不得建档/推进");
		assertTrue(player.getQuestStateList().getQuestState(PLAY_QUEST) == null, "未接取不得建档");
	}

	@Test
	void handInPreviewRecyclesThePlayItemAndReopensTheRewardWindow() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleItemPlayHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, PLAY_QUEST, QuestStatus.REWARD, 0);
		int rewardNpc = local.rewardNpcs(PLAY_QUEST).getFirst();
		ItemStack playItem = local.acceptGiveItem(PLAY_QUEST);
		inventory.hold(playItem.itemId(), playItem.count());

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, PLAY_QUEST, 31)),
			"REWARD 态的 QUEST_SELECT 必须重开奖励窗");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.PAGE_REWARD_WINDOW);

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, PLAY_QUEST, 1009)),
			"1009 交付预览必须被服务");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.PAGE_REWARD_WINDOW);
		assertEquals(List.of("remove:" + playItem.itemId() + ":" + playItem.count()), inventory.calls(),
			"1009 必须回收演出道具（回收时机与退役编译器一致）");
	}

	@Test
	void claimCompletesThroughTheRetailDerivedTemplate() {
		List<int[]> claims = new ArrayList<>();
		SimpleItemPlayHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.forTest(SimpleItemPlayNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					claims.add(new int[] {env.getQuestId(), tier});
					return template != null;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, PLAY_QUEST, QuestStatus.REWARD, 0);
		int rewardNpc = local.rewardNpcs(PLAY_QUEST).getFirst();

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, PLAY_QUEST, 8)),
			"奖励窗按钮必须由 native 领奖段服务");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.PAGE_COMPLETE);
		assertEquals(1, claims.size());
		assertEquals(PLAY_QUEST, claims.getFirst()[0]);

		Player other = NativeTalkFixture.player();
		NativeTalkFixture.add(other, PLAY_QUEST, QuestStatus.REWARD, 0);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(other, rewardNpc, PLAY_QUEST, 9999)),
			"非奖励窗动作不得被领奖段消费");
	}

	@Test
	void relayAndCutsceneRowsStayUnrouted() {
		for (SimpleItemPlayRow row : loader.itemPlayRows()) {
			boolean longTail = !row.talkNpcNames().isEmpty()
				|| declared(row.stepGiveItems(), 1) || declared(row.stepGiveItems(), 2)
				|| declared(row.stepRemoveItems(), 1) || declared(row.stepRemoveItems(), 2)
				|| row.cutsceneId() != null || row.itemCheck();
			if (longTail) {
				assertFalse(handler.routes(row.questId()),
					"中继链/换物/过场长尾行本批不路由（fail-closed）: " + row.questId());
			}
		}
	}

	private static boolean declared(List<String> cells, int step) {
		return cells.size() >= step && cells.get(step - 1) != null && !cells.get(step - 1).isBlank();
	}

	private static Optional<RetailQuestMetadataCompiler.Outcome> metadata(int questId) {
		try {
			return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId);
		} catch (java.io.IOException e) {
			return Optional.empty();
		}
	}

	private static SimpleItemPlayHandler handlerWith(NativeInventoryPort inventory,
			NativeReportRewardFlow rewardFlow) {
		try {
			return new SimpleItemPlayHandler(NativeQuestTableLoader.instance(), NativeNpcNameResolver.instance(),
				RetailItemNameIndex.loadItemTemplates(), inventory, rewardFlow);
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
