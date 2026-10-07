package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader.SimpleUseItemRow;

/**
 * SimpleUseItem 原生表驱动家族门禁（计划 §6.6 / §7 P5 切换批）。
 * <p>
 * 断言面全部来自真端表行 + 真端 {@code quest.xml} + 生产静态数据 id，不合成语义：
 * <ol>
 *   <li>160 行全量装载、{@code use_item_name}/{@code reward_npc_name} 100%、中继链与第 K 步物品列填充率冻结；</li>
 *   <li>注册/路由分解（owns 160 = routed 102 + 不可路由 58）与 fail-closed 残余 {30720, 30723}；</li>
 *   <li>用物开接取窗（页 4）→ 无主 1002 接取 → 中继链（任务行 = 该步页、SETPRO 推进写 {@code var0}、
 *       第 3 步换物）→ 交付门 → 领奖闭环；</li>
 *   <li>失败面 fail-closed：未接取不重复开窗、乱序中继零推进、门未持有不放行、越界按钮不结算。</li>
 * </ol>
 * <p>
 * SimpleUseItem native family gate: every assertion is sourced from the retail row, the retail
 * {@code quest.xml} or production static-data ids. It freezes the 160-row load, the registration /
 * routing split with its fail-closed residue, and the accept-by-item → relay chain → gate → claim
 * loop, including the fail-closed surfaces.
 */
class SimpleUseItemNativeFamilyGateTest {

	/** 真端单步物品接取任务（用物 → 交付）。 / Retail single-step item-accept quest. */
	private static final int ITEM_ACCEPT_QUEST = 1107;
	/** 真端三步中继 + 第 3 步换物任务（1559）。 / Retail three-step relay row with an item swap. */
	private static final int RELAY_QUEST = 1559;
	/** 真端三步中继且交付 NPC 独立于中继链（1718）。 / Retail relay row whose hand-in npc is not a relay npc. */
	private static final int RELAY_HANDIN_QUEST = 1718;
	/** 真端 item_check 门任务（80482，quest.xml {@code check_item1_1}）。 / Retail gate row. */
	private static final int GATE_QUEST = 80482;
	/** item_check 开关行（真端表 5 行）。 / The five switched gate rows. */
	private static final Set<Integer> GATE_ROWS = Set.of(80482, 80486, 80612, 80615, 80616);
	/** fail-closed 残余（复合交付名无客户端登记）。 / The fail-closed residue. */
	private static final Set<Integer> FAIL_CLOSED_ROWS = Set.of(30720, 30723);

	private static NativeQuestTableLoader loader;
	private static SimpleUseItemHandler handler;

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		handler = SimpleUseItemHandler.instance();
	}

	// ---------------------------------------------------------------- 装载面

	@Test
	void loadsAll160RetailRowsWithTheItemAcceptShape() {
		assertEquals(160, loader.useItemSize(), "真端 quest_simpleuseitems 全量行");
		List<SimpleUseItemRow> rows = List.copyOf(loader.useItemRows());
		assertTrue(rows.stream().allMatch(row ->
			!row.useItemName().isBlank() && !row.rewardNpcName().isBlank()),
			"use_item_name/reward_npc_name 必须 100%（真端 160/160）");
		assertEquals(54, rows.stream().filter(row -> row.talkNpcNames().size() >= 1).count(), "talk_npc1 覆盖 54 行");
		assertEquals(26, rows.stream().filter(row -> row.talkNpcNames().size() >= 2).count(), "talk_npc2 覆盖 26 行");
		assertEquals(10, rows.stream().filter(row -> row.talkNpcNames().size() >= 3).count(), "talk_npc3 覆盖 10 行");
		assertEquals(32, rows.stream().filter(row -> row.conQuest() != null).count(), "con_quest 覆盖 32 行");
		assertEquals(GATE_ROWS, rows.stream().filter(SimpleUseItemRow::itemCheck)
			.map(SimpleUseItemRow::questId).collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new)),
			"item_check 开关行必须冻结（真端 5 行）");
		for (int step = 1; step <= 3; step++) {
			int index = step - 1;
			int declaredStep = step;
			assertEquals(new int[] {8, 4, 3}[index], rows.stream()
				.filter(row -> row.stepGiveItems().size() > index && row.stepGiveItems().get(index) != null)
				.count(), "give_item" + declaredStep + " 覆盖行数");
			assertEquals(new int[] {10, 6, 3}[index], rows.stream()
				.filter(row -> row.stepRemoveItems().size() > index && row.stepRemoveItems().get(index) != null)
				.count(), "remove_item" + declaredStep + " 覆盖行数");
		}
		// 第 K 步发/扣与第 K 个中继 NPC 同步声明（真端同源 codegen 的槽语义前提）。
		// Every declared step give/remove column pairs with the same-index relay npc.
		for (SimpleUseItemRow row : rows) {
			for (int step = 1; step <= 3; step++) {
				boolean declaresItem = row.stepGiveItems().size() >= step && row.stepGiveItems().get(step - 1) != null
					|| row.stepRemoveItems().size() >= step && row.stepRemoveItems().get(step - 1) != null;
				if (declaresItem) {
					int declaredStep = step;
					assertTrue(row.talkNpcNames().size() >= step,
						() -> "第 " + declaredStep + " 步发/扣必须有同号中继 NPC: " + row.questId());
				}
			}
		}
	}

	@Test
	void routingSplitAndResidueAreFrozen() {
		assertEquals(160, handler.ownedQuestIds().size(), "注册集 = 真端表全量行");
		assertEquals(102, handler.routedQuestIds().size(), "路由集（退役 ∧ 非 XML-only ∧ 可解）");
		assertEquals(58, handler.unroutableQuestIds().size(),
			"不可路由行 = 未退役行 56（含 Greenhat 两行）+ fail-closed 残余 2（复合交付名；P9 收口后）");
		// 未解名证据面冻结：Greenhat（13060/23060 的交付名）在 P9 组表扩域后已唯一解出，只留在未退役面；
		// 唯一残余 = magician_apprentice（fail-closed 行的复合交付名：真端名册无此名、客户端无登记）。
		// The unresolved-name evidence is frozen: Greenhat now resolves through the P9 group expansion and
		// remains only as an un-retired row; the sole residue is the composite hand-in name of the
		// fail-closed rows, which has neither a retail roster entry nor a client registration.
		assertEquals(Set.of("magician_apprentice"), handler.unresolvedNames(),
			"未解 NPC 名证据面必须冻结（仅 fail-closed 残余）");
		assertTrue(handler.unresolvedItemSymbols().isEmpty(), "未解物品符号证据面必须恒空");
		for (int questId : FAIL_CLOSED_ROWS) {
			assertTrue(handler.owns(questId), "残余行仍在注册集（真端表行）: " + questId);
			assertFalse(handler.routes(questId), "复合交付名无客户端登记的残余行不得路由: " + questId);
			assertTrue(handler.rewardNpcs(questId).isEmpty(), "残余行交付面无登记: " + questId);
		}
		assertEquals(102, handler.routedQuestIds().stream().filter(handler::owns).count(),
			"路由集必须是注册集子集");
	}

	@Test
	void gateRowsCarryTheirRetailCheckItemDeclaration() {
		for (int questId : GATE_ROWS) {
			List<ItemStack> gate = handler.gateItems(questId);
			assertFalse(gate.isEmpty(), "item_check=1 的行必须有真端 check_item 门物品: " + questId);
		}
		// 80482 的门物品 = 真端 quest.xml {@code check_item1_1}（= 用物品本体，read 动作不消耗）。
		// The 80482 gate item is the retail quest.xml check_item1_1 (the read-action use item itself).
		assertEquals(List.of(182215419), handler.gateItems(GATE_QUEST).stream()
			.map(NativeItemSymbols.ItemStack::itemId).toList(), "80482 门物品 = quest_80481a");
		for (SimpleUseItemRow row : loader.useItemRows()) {
			if (!row.itemCheck()) {
				assertTrue(handler.gateItems(row.questId()).isEmpty(),
					"无开关的行不设门（真端 record 开关口径）: " + row.questId());
			}
		}
	}

	// ---------------------------------------------------------------- 行为面

	@Test
	void itemUseOpensTheAskWindowOnlyWhileTheRowIsNotInProgress() {
		Player player = NativeTalkFixture.player();
		Integer itemId = handler.useItemId(ITEM_ACCEPT_QUEST);
		assertNotNull(itemId, "真端 use_item_name 必须解析");
		assertTrue(handler.acceptQuestIdsForItem(itemId).contains(ITEM_ACCEPT_QUEST),
			"接取道具必须指回该行（真端一行一物）");

		NativeTalkFixture.clearPackets(player);
		assertTrue(handler.onItemUse(player, itemId), "用物必须开接取窗");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_ASK_ACCEPT);

		NativeTalkFixture.start(player, ITEM_ACCEPT_QUEST);
		assertFalse(handler.onItemUse(player, itemId), "进行中再次用物不得重复开窗");
	}

	@Test
	void targetlessAcceptStartsTheQuestAndRefuseCloses() {
		Player accept = NativeTalkFixture.player();
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(accept, 0, ITEM_ACCEPT_QUEST, 1002)),
			"无主 1002 必须由 native 接取口服务");
		QuestState state = accept.getQuestStateList().getQuestState(ITEM_ACCEPT_QUEST);
		assertNotNull(state, "接取后必须建档");
		assertEquals(QuestStatus.START, state.getStatus());

		Player refuse = NativeTalkFixture.player();
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(refuse, 0, ITEM_ACCEPT_QUEST, 1003)),
			"无主 1003 拒绝必须被服务（关窗）");
		assertTrue(refuse.getQuestStateList().getQuestState(ITEM_ACCEPT_QUEST) == null, "拒绝不得建档");

		// 带 NPC 的对话不是本族的接取面（本族无 acquired_npc_name 列）。
		// A dialog carrying an npc is not this family's accept face (the family has no acquire npc).
		Player npcDialog = NativeTalkFixture.player();
		assertFalse(handler.onDialog(NativeTalkFixture.dialog(npcDialog, handler.rewardNpcs(ITEM_ACCEPT_QUEST)
			.getFirst(), ITEM_ACCEPT_QUEST, 1002)), "交付 NPC 上的 1002 不得开接取");
	}

	@Test
	void relayChainServesStepPagesAndAdvancesThroughSetproIntoVar0() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleUseItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, RELAY_QUEST);
		QuestState state = player.getQuestStateList().getQuestState(RELAY_QUEST);
		List<Integer> relays = local.relayNpcs(RELAY_QUEST);
		assertEquals(3, relays.size(), "1559 三步中继（真端 talk_npc1..3）");

		// 尚未轮到的第 2 步：任务行打开零响应、乱序推进动作零步进（关窗兜底，不越过步序）。
		NativeTalkFixture.clearPackets(player);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1), RELAY_QUEST, 31)),
			"尚未轮到的步不得打开步页");
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1), RELAY_QUEST, 10001)));
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(0, state.getQuestVars().getQuestVars(), "乱序推进零步进");

		// 任务行打开 = 该步页 SELECT2..4（带 questId）且不推进；子页动作按契约原样回发；
		// SETPRO{K}（10000 + K - 1）推进把步号写 var0 = K（旧 bit16..17 私编的 65536 会让客户端
		// 任务书步骤显示为空）并关窗（真端 0x5d8、零发页）。
		int[] pages = {1352, 1693, 2034};
		int[] subPages = {1353, 1694, 2035};
		for (int step = 1; step <= 3; step++) {
			int relayNpc = relays.get(step - 1);
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relayNpc, RELAY_QUEST, 31)),
				"第 " + step + " 步的任务行 = 该步页");
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, pages[step - 1], RELAY_QUEST);
			assertEquals(step - 1, state.getQuestVars().getQuestVars(), "打开步页不得写步号");
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relayNpc, RELAY_QUEST,
				subPages[step - 1])), "客户端声明的子页动作必须回发");
			NativeTalkFixture.assertOnlyDialogPageWithQuest(player, subPages[step - 1], RELAY_QUEST);
			NativeTalkFixture.clearPackets(player);
			assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relayNpc, RELAY_QUEST,
				10000 + step - 1)), "SETPRO" + step + " 必须推进");
			NativeTalkFixture.assertCloseDialog(player);
			assertEquals(step, state.getQuestVars().getQuestVars(), "步号 = var0 = " + step);
		}
		// 第 3 步换物：真端 give_item3/remove_item3（1559 = 换出 1559A、换入 1559B）。
		// The retail step-3 swap: give_item3/remove_item3 of row 1559.
		NativeItemSymbols.ItemStack give = local.stepGiveItem(RELAY_QUEST, 3);
		NativeItemSymbols.ItemStack remove = local.stepRemoveItem(RELAY_QUEST, 3);
		assertNotNull(give);
		assertNotNull(remove);
		assertEquals(List.of("give:" + give.itemId() + ":" + give.count(),
			"remove:" + remove.itemId() + ":" + remove.count()), inventory.calls(),
			"第 3 步必须按真端列执行发放与扣除");

		// 链满后任务行让位给交付面：1559 的 talk_npc1 = reward_npc，点任务行（31）必须发报告确认页
		// select5=2375（中继步页 1352 不得再出现）。
		// Once the chain is complete the hand-in face owns the row selection.
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), RELAY_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
	}

	/**
	 * 用户实机回归（2026-10-08，3058 / NPC 798189 Oileus）：与 talk_npc1 对话后任务书步骤整块空白——
	 * 旧私编把步号写 bit16..17（65536）让客户端按打包整数匹配任务 steps 行落空。
	 * <p>
	 * Live regression (2026-10-08): the relay advance of 3058 blanked the journal steps.
	 */
	@Test
	void quest3058RelayWritesVar0AndRemovesItsItemAtStepTwo() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleUseItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, 3058);
		QuestState state = player.getQuestStateList().getQuestState(3058);
		assertTrue(local.routes(3058), "3058 必须在 native 路由集内（实机可玩面）");
		List<Integer> relays = local.relayNpcs(3058);
		assertEquals(2, relays.size(), "3058 两步中继（真端 Oileus/Lavirintos）");
		assertEquals(798189, relays.getFirst(), "talk_npc1 = Oileus（实机 NPC 798189）");

		// 与 Oileus：任务行 → 步 1 页（1352）→ 子页 1353 → SETPRO1 推 step=1。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), 3058, 31)),
			"与 Oileus 的任务行必须开启步 1 页");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1352, 3058);
		assertEquals(0, state.getQuestVars().getQuestVars(), "打开步页不得写步号");
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), 3058, 1353)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1353, 3058);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.getFirst(), 3058, 10000)));
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(1, state.getQuestVars().getQuestVars(),
			"步号 = var0 = 1（bit16 私编的 65536 会让客户端任务书步骤空白——实机 2026-10-08）");

		// 与 Lavirintos：步 2 页（1693）→ 1694 → SETPRO2 推 step=2 并移除 ITEM_QUEST_3058A。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1), 3058, 31)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1693, 3058);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1), 3058, 1694)));
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1694, 3058);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, relays.get(1), 3058, 10001)));
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(2, state.getQuestVars().getQuestVars(), "步号 = var0 = 2");
		NativeItemSymbols.ItemStack removed = local.stepRemoveItem(3058, 2);
		assertNotNull(removed, "3058 remove_item2 = ITEM_QUEST_3058A");
		assertEquals(List.of("remove:" + removed.itemId() + ":" + removed.count()), inventory.calls(),
			"第 2 步必须按真端列移除接取道具");
	}

	@Test
	void enterWorldNormalizesTheLegacyStepEncoding() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, 3058, QuestStatus.START, 1 << 16);
		QuestState state = player.getQuestStateList().getQuestState(3058);

		assertTrue(handler.onEnterWorld(player),
			"旧 bit16..17 编码必须在进世界时归一（否则任务书步骤显示为空）");
		assertEquals(1, state.getQuestVars().getQuestVars(), "步号归一为 var0 = 1");

		// 干净行（vars=0）不受影响。
		Player clean = NativeTalkFixture.player();
		NativeTalkFixture.start(clean, 3058);
		assertFalse(handler.onEnterWorld(clean), "干净行不得被改写");
		assertEquals(0, clean.getQuestStateList().getQuestState(3058).getQuestVars().getQuestVars());
	}

	@Test
	void relayStepsKeepItemPositionsAcrossTheWholeTable() {
		for (SimpleUseItemRow row : loader.useItemRows()) {
			List<String> give = row.stepGiveItems();
			List<String> remove = row.stepRemoveItems();
			assertEquals(3, give.size(), "give_item1..3 必须位置保留: " + row.questId());
			assertEquals(3, remove.size(), "remove_item1..3 必须位置保留: " + row.questId());
			for (int index = 0; index < 3; index++) {
				if (give.get(index) == null && remove.get(index) == null) {
					continue;
				}
				int stepNumber = index + 1;
				assertTrue(row.talkNpcNames().size() > index,
					() -> "第 " + stepNumber + " 步物品必须落在同号中继步上: " + row.questId());
			}
		}
	}

	@Test
	void handInRequiresTheRetailCheckItemBeforeFlippingReward() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleUseItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, GATE_QUEST);
		int rewardNpc = local.rewardNpcs(GATE_QUEST).getFirst();
		NativeItemSymbols.ItemStack gateItem = local.gateItems(GATE_QUEST).getFirst();

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, GATE_QUEST, 26)),
			"门未持有时必须由进行中页服务");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_IN_PROGRESS);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(GATE_QUEST).getStatus(),
			"门未过不得翻 REWARD");
		assertTrue(inventory.calls().isEmpty(), "门未过不得扣除门物品");

		// 未持门点任务行（2026-10-05 缺陷 T，实机 1126）：报告页照发（80482 契约 select5=2375）——
		// 物品门只在确认动作（39/1009）上分叉，31 零推进零扣物。
		// Row selection with the gate still short (defect T): the confirm page is sent anyway.
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, GATE_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(GATE_QUEST).getStatus(),
			"未持门 31 零推进");
		assertTrue(inventory.calls().isEmpty(), "未持门 31 零扣物");

		inventory.hold(gateItem.itemId(), gateItem.count());
		// 两步报告（裁定 a）：31 只发客户端声明的报告确认页（80482 契约声明 2375）不扣物品；
		// 1009 报告确认才扣门物品 + 翻 REWARD + 奖励窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, GATE_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
		assertEquals(QuestStatus.START, player.getQuestStateList().getQuestState(GATE_QUEST).getStatus(),
			"31 只发确认页不推进");
		assertTrue(inventory.calls().isEmpty(), "第一步不得扣门物品");
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, GATE_QUEST, 1009)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD, player.getQuestStateList().getQuestState(GATE_QUEST).getStatus());
		assertEquals(List.of("remove:" + gateItem.itemId() + ":" + gateItem.count()), inventory.calls(),
			"交付门通过即扣除真端 check_item");
	}

	@Test
	void ungatedRowsHandInWithoutAnyItemGate() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleUseItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, ITEM_ACCEPT_QUEST);
		int rewardNpc = local.rewardNpcs(ITEM_ACCEPT_QUEST).getFirst();

		// 两步报告（裁定 a）：31 发 1107 契约声明的确认页（2375），1009 推进 REWARD。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, ITEM_ACCEPT_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, ITEM_ACCEPT_QUEST, 1009)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(ITEM_ACCEPT_QUEST).getStatus());
		assertTrue(inventory.calls().isEmpty(), "无门行不得凭空扣除物品");
	}

	@Test
	void relayChainGateBlocksTheHandInUntilTheChainIsComplete() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleUseItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, RELAY_HANDIN_QUEST);
		int rewardNpc = local.rewardNpcs(RELAY_HANDIN_QUEST).getFirst();
		assertFalse(local.relayNpcs(RELAY_HANDIN_QUEST).contains(rewardNpc),
			"该行的交付 NPC 独立于中继链（真端列事实）");

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, RELAY_HANDIN_QUEST, 26)),
			"中继链未走完时交付 NPC 给进行中页");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleUseItemHandler.PAGE_IN_PROGRESS);
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(RELAY_HANDIN_QUEST).getStatus());
	}

	/** 交付后领取奖励：走 native 完成口（真端 reward 列 → 共用结算体）。 / Claim through the native settlement port. */
	@Test
	void claimCompletesThroughTheRetailDerivedTemplate() {
		List<ClaimCall> calls = new ArrayList<>();
		SimpleUseItemHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.forTest(SimpleUseItemNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					calls.add(new ClaimCall(env.getQuestId(), tier, template));
					return true;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, ITEM_ACCEPT_QUEST, QuestStatus.REWARD, 0);
		int rewardNpc = local.rewardNpcs(ITEM_ACCEPT_QUEST).getFirst();

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, ITEM_ACCEPT_QUEST, 8)),
			"奖励窗按钮必须由 native 领奖段服务");
		// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG：回选择对话页（页 10，questId=0）。
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(1, calls.size());
		assertEquals(ITEM_ACCEPT_QUEST, calls.getFirst().questId());
		assertNotNull(calls.getFirst().template(), "结算体必须拿到真端奖励列重建的 typed 模板");

		// 23 = SELECTED_QUEST_NOREWARD（无选择确认，不占选项下标）：与选项 8 同义结算 + 同收尾页。
		// 1107 实机 2026-10-04：旧区间（8..23）把 23 映射成下标 15 → 按钮面 fail-closed（无声明
		// 选项）→ 发奖中止、奖励窗反复重开。
		// 23 is the no-selection confirm and maps to index 0 (the 1107 live fix).
		Player confirm = NativeTalkFixture.player();
		NativeTalkFixture.add(confirm, ITEM_ACCEPT_QUEST, QuestStatus.REWARD, 0);
		int confirmNpc = local.rewardNpcs(ITEM_ACCEPT_QUEST).getFirst();
		NativeTalkFixture.clearPackets(confirm);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(confirm, confirmNpc, ITEM_ACCEPT_QUEST,
			QuestDialogAction.SELECTED_QUEST_NOREWARD.id())), "23 无选择确认必须被领奖段服务");
		NativeTalkFixture.assertOnlyDialogPage(confirm, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(2, calls.size(), "23 必须触发结算");
		assertEquals(0, calls.getLast().tier(), "23 的结算档位必须归 0（NOREWARD 不占下标）");

		// 越界按钮（非奖励窗动作）不得结算。
		Player other = NativeTalkFixture.player();
		NativeTalkFixture.add(other, ITEM_ACCEPT_QUEST, QuestStatus.REWARD, 0);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(other,
			local.rewardNpcs(ITEM_ACCEPT_QUEST).getFirst(), ITEM_ACCEPT_QUEST, 9999)),
			"非奖励窗动作不得被领奖段消费");
	}

	/**
	 * 无目标领奖（真端 {@code QuestDialog} 无主键协议；任务窗/实时奖励槽确认包不带 NPC 上下文，
	 * 引擎以 npcId=0 进入）：按 questId 结算 + 关窗（真端 0x5d8；Playbook 案例 8.3 合同）。
	 * 退役迁移曾丢失该面（13830 实机 2026-10-07）。
	 * <p>
	 * The targetless claim: no NPC context, settled by quest id with the close-dialog tail.
	 */
	@Test
	void targetlessClaimSettlesByQuestIdAndClosesTheWindow() {
		List<ClaimCall> calls = new ArrayList<>();
		SimpleUseItemHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.forTest(SimpleUseItemNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					calls.add(new ClaimCall(env.getQuestId(), tier, template));
					return true;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, ITEM_ACCEPT_QUEST, QuestStatus.REWARD, 0);

		// 实时奖励槽 110（任务窗「实时奖励」按钮的原始动作）。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(new QuestEnv(null, player, ITEM_ACCEPT_QUEST, 110)),
			"无目标实时奖励确认必须被领奖段服务");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(1, calls.size());
		assertEquals(ITEM_ACCEPT_QUEST, calls.getFirst().questId(), "结算体必须拿到 questId");

		// 状态门：START 态零认领、零发页。
		Player started = NativeTalkFixture.player();
		NativeTalkFixture.add(started, ITEM_ACCEPT_QUEST, QuestStatus.START, 0);
		NativeTalkFixture.clearPackets(started);
		assertFalse(local.onDialog(new QuestEnv(null, started, ITEM_ACCEPT_QUEST, 110)),
			"START 态不得认领无目标领奖");
		assertTrue(NativeTalkFixture.dialogPages(started).isEmpty(), "START 态零发页");
	}

	private record ClaimCall(int questId, int tier, QuestTemplate template) {
	}

	private static Optional<RetailQuestMetadataCompiler.Outcome> metadata(int questId) {
		try {
			return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId);
		} catch (java.io.IOException e) {
			return Optional.empty();
		}
	}

	private static SimpleUseItemHandler handlerWith(NativeInventoryPort inventory,
			NativeReportRewardFlow rewardFlow) {
		try {
			return new SimpleUseItemHandler(NativeQuestTableLoader.instance(), NativeNpcNameResolver.instance(),
				RetailItemNameIndex.loadItemTemplates(), NativeQuestXmlTable.instance(), inventory, rewardFlow);
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
