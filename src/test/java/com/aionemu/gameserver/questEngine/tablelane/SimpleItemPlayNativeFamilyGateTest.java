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

	/**
	 * P5D 步 2：中继链/步物品/页 **已接线**，但 owner 仍为 XML 保留的行**不上线**（接线 ≠ 激活，
	 * 激活随 retention 重裁）。真端证据：交付节点 {@code slot 3 #K}（每 NPC 一节点）+ 行主 thunk 相机。
	 */
	@Test
	void relayFaceIsWiredWhileXmlRetainedRowsStayUnrouted() {
		assertEquals(0, handler.relayCount(PLAY_QUEST), "直交形行无中继（6 路由行全为直交形）");
		assertEquals(2, handler.relayCount(18213), "18213 真端 slot 3 #0/#1 ⇒ 两步中继");
		assertEquals(2, handler.relayCount(9623), "9623 两步中继");
		assertEquals(1, handler.relayCount(50048), "50048 一步中继");
		assertEquals(1352, SimpleItemPlayHandler.pageForStep(1), "真端步页 select2");
		assertEquals(1693, SimpleItemPlayHandler.pageForStep(2), "真端步页 select3");
		assertEquals(2034, SimpleItemPlayHandler.pageForStep(3), "真端步页 select4");

		// 中继步序 = 表序（talk_npc1/2），绑定到各自 NPC 节点。
		List<SimpleItemPlayHandler.RelayStep> relays = handler.relaysForQuest(18213);
		assertEquals(2, relays.size(), "18213 两个中继步");
		assertEquals(1, relays.get(0).step(), "第 1 步");
		assertEquals(2, relays.get(1).step(), "第 2 步");
		assertTrue(handler.relaysForNpc(relays.get(0).npcId()).stream()
				.anyMatch(relay -> relay.questId() == 18213 && relay.step() == 1), "第 1 步绑定该 NPC");
		assertTrue(handler.relaysForNpc(relays.get(1).npcId()).stream()
				.anyMatch(relay -> relay.questId() == 18213 && relay.step() == 2), "第 2 步绑定该 NPC");

		// 第 K 步发/扣（18213 真端 give_item2/remove_item2；第 1 步无声明）。
		ItemStack stepTwoGive = handler.stepGiveItem(18213, 2);
		ItemStack stepTwoRemove = handler.stepRemoveItem(18213, 2);
		assertNotNull(stepTwoGive, "18213 第 2 步发放已解");
		assertNotNull(stepTwoRemove, "18213 第 2 步扣除已解");
		assertTrue(handler.stepGiveItem(18213, 1) == null, "18213 第 1 步无发放声明");

		// 接线 ≠ 激活：XML 保留行（含中继名无解的 50048）不得路由。
		assertFalse(handler.routes(18213), "XML 保留行不得路由（owner 未退役）");
		assertFalse(handler.routes(50048), "中继名无解 + XML 保留 ⇒ 双保险 fail-closed");
		assertFalse(handler.routes(9623), "不在生产的行不得路由");
	}

	/**
	 * P5D 步 2：用道具推进必须过真端相机闸门（{@code step == relayCount}），并写回 {@code relayCount + 1}
	 * （与旧编译器 {@code var0 == 0 → var0 = 1} 同形）。
	 */
	@Test
	void itemUseIsGatedOnTheRetailCameraStep() {
		SimpleItemPlayHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.instance());
		int playItem = local.playItemId(PLAY_QUEST);

		// 步号 != relayCount(0)（异构/旧存档形态）⇒ 闸门拒绝，状态与步号零变更。
		Player gatedPlayer = NativeTalkFixture.player();
		NativeTalkFixture.add(gatedPlayer, PLAY_QUEST, QuestStatus.START, 1);
		assertFalse(local.onItemUse(gatedPlayer, playItem), "步号不等于 relayCount ⇒ 用物不得推进");
		QuestState gated = gatedPlayer.getQuestStateList().getQuestState(PLAY_QUEST);
		assertEquals(QuestStatus.START, gated.getStatus(), "被闸门拒绝的任务状态不得变化");
		assertEquals(1, gated.getQuestVars().getQuestVars(), "被闸门拒绝的任务步号不得变化");

		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, PLAY_QUEST, QuestStatus.START, 0);
		assertTrue(local.onItemUse(player, playItem), "步号等于 relayCount ⇒ 用物推进");
		QuestState advanced = player.getQuestStateList().getQuestState(PLAY_QUEST);
		assertEquals(QuestStatus.REWARD, advanced.getStatus());
		assertEquals(1, advanced.getQuestVars().getQuestVars(), "推进后步号 = relayCount + 1");
	}

	/** P5D 步 2：REWARD 态而步号仍为 0 的旧存档（P5 车道未写步号）在进入世界时自愈到 relayCount + 1。 */
	@Test
	void rewardSavesWithStepZeroAreHealedOnEnterWorld() {
		SimpleItemPlayHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, PLAY_QUEST, QuestStatus.REWARD, 0);

		assertTrue(local.onEnterWorld(player), "REWARD + 步号 0 的旧存档必须自愈");
		assertEquals(1, player.getQuestStateList().getQuestState(PLAY_QUEST).getQuestVars().getQuestVars(),
			"自愈步号 = relayCount + 1");
		assertFalse(local.onEnterWorld(player), "已自愈后再次进入世界零副作用");
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
