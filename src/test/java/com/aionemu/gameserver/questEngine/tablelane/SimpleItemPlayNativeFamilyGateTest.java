package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
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
 *   <li>注册/路由分解（owns 43 = routed 8 + 不可路由 35：未退役行与中继/cutscene 长尾行）；</li>
 *   <li>P5D 步 3 激活行的端到端链路（接取 → 中继两步 → 用物 → 领奖）；</li>
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
	/** P5D 步 3 激活的两行（18213 天族 / 28213 魔族，各两步中继）。 / Step-3 activated relay rows. */
	private static final Set<Integer> ACTIVATED_RELAY_ROWS = Set.of(18213, 28213);
	/** 直交形行（真端无 {@code talk_npcK}，接取即发演出道具）。 / The direct hand-in rows. */
	private static final Set<Integer> DIRECT_ROWS = Set.of(13704, 13708, 19048, 23704, 23708, 29048);
	/** 本批路由集（真端退役 8 行）。 / The eight routed rows. */
	private static final Set<Integer> ROUTED_ROWS = Set.of(
		13704, 13708, 19048, 23704, 23708, 29048, 18213, 28213);

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
	void cutsceneColumnIsAnEvidenceFaceOnly() {
		// 真端 Quest_SimpleItemPlay.xml 43 行仅 2 行声明 cutsceneid1（13400=859、23400=860），
		// 且本表无 cs1_haction / item_check 列（0 命中）⇒ 过场面是证据面，不构成路由闸门。
		// Only two of the 43 retail rows declare cutsceneid1 (13400=859, 23400=860), and the table
		// carries neither cs1_haction nor item_check, so the face is evidence-only.
		assertEquals(new java.util.TreeSet<>(java.util.List.of(13400, 23400)),
			loader.itemPlayRows().stream().filter(row -> row.cutsceneId() != null)
				.map(NativeQuestTableLoader.SimpleItemPlayRow::questId)
				.collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new)),
			"真端 cutsceneid1 覆盖行");
		assertEquals(859, handler.cutsceneId(13400), "13400 的过场资源 id（客户端 CutScenes.xml 有 <id>859</id>）");
		assertEquals(860, handler.cutsceneId(23400), "23400 的过场资源 id");
		assertNull(handler.cutsceneId(19048), "未声明行无过场面");
		for (int questId : java.util.List.of(13400, 23400)) {
			assertTrue(handler.owns(questId), "真端表行仍在注册集: " + questId);
			assertTrue(handler.unroutableQuestIds().contains(questId),
				"owner 未退役（不在 retail-xml-retention 清单）⇒ 停在不可路由面: " + questId);
			assertFalse(handler.routes(questId), "未被 owner 裁定的行不上线: " + questId);
		}
	}

	@Test
	void routingSplitIsFrozenToTheRetiredRows() {
		assertEquals(43, handler.ownedQuestIds().size(), "注册集 = 真端表全量行");
		assertEquals(ROUTED_ROWS, handler.routedQuestIds(), "路由集 = 真端退役且接取/交付/道具全可解的 8 行");
		assertEquals(35, handler.unroutableQuestIds().size(), "不可路由行 = 43 − 8");
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
	void directRowsGrantTheSameItemTheyPlay() {
		for (int questId : DIRECT_ROWS) {
			SimpleItemPlayRow row = loader.requireItemPlay(questId);
			ItemStack acceptGive = handler.acceptGiveItem(questId);
			assertNotNull(acceptGive, "真端 give_item（接取即发演出道具）: " + questId);
			assertEquals(row.useItemName(), row.acceptGiveItem(), "真端两列必须同源: " + questId);
			assertEquals(handler.playItemId(questId), acceptGive.itemId(),
				"接取发放的道具必须是演出道具本体: " + questId);
		}
		// 中继形：接取发第 1 步道具（A），演出道具（B）由第 2 步「发 B 扣 A」换得（真端 give_item2/remove_item2）。
		// Relay rows: the accept grants step-1 item A; the play item B arrives with the step-2 give/remove pair.
		for (int questId : ACTIVATED_RELAY_ROWS) {
			ItemStack acceptGive = handler.acceptGiveItem(questId);
			ItemStack stepTwoGive = handler.stepGiveItem(questId, 2);
			ItemStack stepTwoRemove = handler.stepRemoveItem(questId, 2);
			assertNotNull(acceptGive, "中继行接取必须发放第 1 步道具: " + questId);
			assertEquals(handler.playItemId(questId), stepTwoGive.itemId(),
				"演出道具由第 2 步发放: " + questId);
			assertEquals(acceptGive.itemId(), stepTwoRemove.itemId(),
				"第 2 步扣除的是接取发放的道具: " + questId);
			assertTrue(handler.playItemId(questId) != acceptGive.itemId(),
				"中继形两列不同源（A ≠ B）: " + questId);
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
	 * P5D 步 2/3：中继链/步物品/页的接线面按真端证据冻结（交付节点 {@code slot 3 #K} 每 NPC 一节点 +
	 * 行主 thunk 相机）；步 3 已把两行激活（retention 重裁 + 删 XML），其余名字轴无解的行仍 fail-closed。
	 */
	@Test
	void relayFaceIsWiredAndActivatesOnlyTheResolvableRows() {
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

		// 激活（步 3）与仍然 fail-closed 的行：只有名字道具全解且 owner 已退役的行才路由。
		assertTrue(handler.routes(18213), "步 3 激活行必须路由");
		assertTrue(handler.routes(28213), "步 3 激活行必须路由");
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
		// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG：回选择对话页（页 10，questId=0）。
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(1, claims.size());
		assertEquals(PLAY_QUEST, claims.getFirst()[0]);

		// 23 = SELECTED_QUEST_NOREWARD（无选择确认，不占选项下标）：与选项 8 同义结算 + 同收尾页。
		// 1107 同型修复（2026-10-04）：旧区间（8..23）把 23 映射成下标 15 → 按钮面 fail-closed。
		// 23 is the no-selection confirm and maps to index 0 (the 1107 same-shape fix).
		Player confirm = NativeTalkFixture.player();
		NativeTalkFixture.add(confirm, PLAY_QUEST, QuestStatus.REWARD, 0);
		NativeTalkFixture.clearPackets(confirm);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(confirm, rewardNpc, PLAY_QUEST,
			QuestDialogAction.SELECTED_QUEST_NOREWARD.id())), "23 无选择确认必须被领奖段服务");
		NativeTalkFixture.assertOnlyDialogPage(confirm, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(2, claims.size(), "23 必须触发结算");
		assertEquals(0, claims.getLast()[1], "23 的结算档位必须归 0（NOREWARD 不占下标）");

		Player other = NativeTalkFixture.player();
		NativeTalkFixture.add(other, PLAY_QUEST, QuestStatus.REWARD, 0);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(other, rewardNpc, PLAY_QUEST, 9999)),
			"非奖励窗动作不得被领奖段消费");
	}

	/**
	 * 任务行打开中继对话 + 选择对话续页（2026-10-04，1118 同型修复）：31 发该步页、尚未轮到的步
	 * 零响应；子页动作（SELECT2_1=1353）原样回发（9/28 基线），契约未声明的行 fail-closed。
	 * Row selection opens the step page (unreached steps stay silent); selection sub-page actions echo
	 * their page back (the 9/28 baseline) and fail closed when the contract never declares them.
	 */
	@Test
	void relayRowSelectionAndSubPageEchoFollowTheClientContract() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleItemPlayHandler local = handlerWith(inventory, NativeReportRewardFlow.forTest(
			SimpleItemPlayNativeFamilyGateTest::metadata, (env, tier, template) -> true));
		Player player = NativeTalkFixture.player(Race.ELYOS, PlayerClass.WARRIOR, 51);
		NativeTalkFixture.completePrerequisites(player, 18212);
		NativeTalkFixture.add(player, 18213, QuestStatus.START, 0);
		List<SimpleItemPlayHandler.RelayStep> relays = local.relaysForQuest(18213);

		// vars=0/step=1：31 → 该步页（18213 契约声明 select2 = 1352），不触碰物品通道。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(0).npcId(), 18213, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.pageForStep(1));
		assertTrue(inventory.calls().isEmpty(), "打开对话页不得触碰物品通道");

		// vars=0/step=2：未轮到，零响应不跳步。
		NativeTalkFixture.clearPackets(player);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1).npcId(), 18213, 31)));
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "未轮到的步必须零下发");

		// 子页动作 SELECT2_1(1353)（18213 契约声明）：原样回发。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(0).npcId(), 18213,
			QuestDialogPage.SELECT2_1.id())));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT2_1.id());

		// PLAY_QUEST(19048) 契约无子页：fail-closed 零响应。
		Player other = NativeTalkFixture.player();
		NativeTalkFixture.add(other, PLAY_QUEST, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(other);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(other, local.rewardNpcs(PLAY_QUEST).getFirst(),
			PLAY_QUEST, QuestDialogPage.SELECT2_1.id())), "契约未声明的子页动作必须 fail-closed");
		assertTrue(NativeTalkFixture.dialogPages(other).isEmpty());
	}

	/**
	 * P5D 步 3 端到端：激活行（18213 天族 / 28213 魔族）按真端走完整链路——接取入口页 → 页动作 1007
	 * 开问询窗 → 1002 提交（发第 1 步道具 A）→ 中继第 1 步（页 {@code select2}，零发扣）→
	 * 中继第 2 步（页 {@code select3}，发 B 扣 A）→ 用道具 B（相机闸门：步号 == {@code relayCount}）→
	 * 交付 NPC 处领奖（页 5 → 领奖收尾回选择对话页 10，真端 npc-complete finish=SELECTION_DIALOG）。
	 * <p>
	 * Step-3 end-to-end: an activated relay row runs the whole retail chain — accept entry page, the 1007 ask
	 * window, the 1002 commit granting step-1 item A, relay step 1 (select2), relay step 2 (select3 with the
	 * A→B swap), the gated use of B, and the reward claim at the hand-in npc.
	 */
	@Test
	void activatedRelayRowsRunTheWholeChainEndToEnd() {
		for (int questId : ACTIVATED_RELAY_ROWS) {
			NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
			List<Integer> claims = new ArrayList<>();
			SimpleItemPlayHandler local = handlerWith(inventory,
				NativeReportRewardFlow.forTest(SimpleItemPlayNativeFamilyGateTest::metadata,
					(env, tier, template) -> {
						claims.add(env.getQuestId());
						return template != null;
					}));
			// 真端等级门 minlevel_permitted = 51、种族 pc_light/pc_dark、前置 Q18212/Q28212。
			Race race = questId == 18213 ? Race.ELYOS : Race.ASMODIANS;
			Player player = NativeTalkFixture.player(race, PlayerClass.WARRIOR, 51);
			NativeTalkFixture.completePrerequisites(player, questId == 18213 ? 18212 : 28212);

			int acquireNpc = local.acquireNpc(questId);
			List<SimpleItemPlayHandler.RelayStep> relays = local.relaysForQuest(questId);
			assertEquals(2, relays.size(), "激活行两步中继: " + questId);
			assertEquals(local.rewardNpcs(questId).getFirst(), relays.get(1).npcId(),
				"交付节点 slot 4 = 最后一步中继 NPC: " + questId);
			ItemStack stepOneItem = local.acceptGiveItem(questId);
			ItemStack stepTwoItem = local.stepGiveItem(questId, 2);
			ItemStack stepTwoRemove = local.stepRemoveItem(questId, 2);

			// 接取：入口页 → 1007 问询窗 → 1002 提交（发 A）。
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, questId, 31)),
				"接取 NPC 的 QUEST_SELECT 必须下发客户端声明的入口页: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(questId));

			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, questId, 1007)),
				"页动作 1007 必须打开真端问询窗: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.askWindowPage(questId));

			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, questId, 1002)),
				"1002 提交必须由 native 接取口服务: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.PAGE_ACCEPTED);
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus(),
				"接取后必须为 START: " + questId);
			assertEquals(List.of("give:" + stepOneItem.itemId() + ":" + stepOneItem.count()),
				inventory.calls(), "接取提交即发真端 give_item（第 1 步道具）: " + questId);

			// 中继第 1 步（select2_1 页的「结束对话」按钮）：after-commit = 真端 cabb10 关窗
			// （SetQuestProgress + 0x5d8、**零发页**；2026-10-05 实机「一次点击即关窗」），
			// 零发扣，步号 = 1；旧「回选择对话页 10」系翻译夸大。
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(0).npcId(), questId, 10000)),
				"第 1 步动作 10000 必须被中继节点服务: " + questId);
			NativeTalkFixture.assertCloseDialog(player);
			assertEquals(1, player.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars(),
				"第 1 步后步号 = 1: " + questId);
			assertEquals(List.of("give:" + stepOneItem.itemId() + ":" + stepOneItem.count()),
				inventory.calls(), "第 1 步无发扣声明: " + questId);

			// 混沌闸门：中继未走完时用道具 B 零副作用（真端行主 thunk 的 step == relayCount 前置）。
			assertFalse(local.onItemUse(player, stepTwoItem.itemId()),
				"步号 1 < relayCount 2 ⇒ 用物不得推进: " + questId);
			assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(questId).getStatus(),
				"闸门拒绝后状态不变: " + questId);

			// 中继第 2 步（select3_1 页的「结束对话」按钮）：after-commit = 关窗（同第 1 步），
			// 发 B 扣 A，步号 = 2。
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1).npcId(), questId, 10001)),
				"第 2 步动作 10001 必须被中继节点服务: " + questId);
			NativeTalkFixture.assertCloseDialog(player);
			assertEquals(2, player.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars(),
				"第 2 步后步号 = 2: " + questId);
			assertEquals(List.of("give:" + stepOneItem.itemId() + ":" + stepOneItem.count(),
					"give:" + stepTwoItem.itemId() + ":" + stepTwoItem.count(),
					"remove:" + stepTwoRemove.itemId() + ":" + stepTwoRemove.count()),
				inventory.calls(), "第 2 步必须按真端顺序发 B 扣 A: " + questId);

			// 用物推进：步号 == relayCount ⇒ 步号 = relayCount + 1 并转 REWARD。
			assertTrue(local.onItemUse(player, stepTwoItem.itemId()), "步号 == relayCount ⇒ 用物推进: " + questId);
			assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(questId).getStatus(),
				"推进后必须为 REWARD: " + questId);
			assertEquals(3, player.getQuestStateList().getQuestState(questId).getQuestVars().getQuestVars(),
				"推进后步号 = relayCount + 1: " + questId);

			// 领奖：交付 NPC 重开奖励窗 → 按钮结算 → 完成页。
			int rewardNpc = local.rewardNpcs(questId).getFirst();
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, questId, 31)),
				"REWARD 态的 QUEST_SELECT 必须重开奖励窗: " + questId);
			NativeTalkFixture.assertOnlyDialogPage(player, SimpleItemPlayHandler.PAGE_REWARD_WINDOW);
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, questId, 8)),
				"奖励窗按钮必须由 native 领奖段服务: " + questId);
			// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG：回选择对话页（页 10，questId=0）。
			NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT_QUEST.id());
			assertEquals(List.of(questId), claims, "领奖必须经真端派生模板结算: " + questId);
		}
	}

	/**
	 * 长尾行的路由面：P5D 步 3 之后只有 18213/28213（名字与道具全解、owner 已退役）上线，
	 * 其余声明中继/换物/过场/交付门的长尾行必须保持 fail-closed。
	 */
	@Test
	void longTailRowsRouteOnlyWhereEveryFaceResolves() {
		Set<Integer> routedLongTail = new TreeSet<>();
		for (SimpleItemPlayRow row : loader.itemPlayRows()) {
			boolean longTail = !row.talkNpcNames().isEmpty()
				|| declared(row.stepGiveItems(), 1) || declared(row.stepGiveItems(), 2)
				|| declared(row.stepRemoveItems(), 1) || declared(row.stepRemoveItems(), 2)
				|| row.cutsceneId() != null || row.itemCheck();
			if (!longTail) {
				continue;
			}
			if (handler.routes(row.questId())) {
				routedLongTail.add(row.questId());
			}
		}
		assertEquals(new TreeSet<>(ACTIVATED_RELAY_ROWS), routedLongTail,
			"长尾行里只有步 3 激活的两行可路由，其余保持 fail-closed");
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
