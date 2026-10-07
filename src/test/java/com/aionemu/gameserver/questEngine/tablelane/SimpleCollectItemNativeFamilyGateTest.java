package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader.SimpleCollectItemRow;

/**
 * SimpleCollectItem 原生表驱动家族门禁（计划 §6.6 / §7 P4 切换批）。
 * <p>
 * 断言面全部来自真端表行 + {@code quest.xml} 元数据 + 生产静态数据 id，不合成语义：
 * <ol>
 *   <li>262 行全量装载、双 NPC 结构 100%、9 行不可路由（5 TEST 无采集物 + 4 事件行无采集计数）；</li>
 *   <li>相机行与 {@code collect_item} 计数逐行一致（253 行有计数 − 3 行真端休眠 = 250 行，槽 = 交付列序）；</li>
 *   <li>接取 → 采集对象认领（物品驱动，真端无相机）→ 交付 → 领奖闭环；</li>
 *   <li>中继链（{@code talk_npc1..3}）门控与乱序零推进；</li>
 *   <li>失败面 fail-closed：未接取不推进、缺物品不放行、越界按钮不结算。</li>
 * </ol>
 * <p>
 * SimpleCollectItem native family gate: every assertion is sourced from the retail row, the retail
 * {@code quest.xml} metadata or production static-data ids. It covers the full 262-row load, the
 * per-row camera reconciliation, the accept → collect → hand-in → claim loop, the relay-chain gate
 * and the fail-closed surfaces.
 */
class SimpleCollectItemNativeFamilyGateTest {

	/** 真端单对象采集任务（1 个化石 / required 1）。 / Retail single-object collect quest. */
	private static final int SINGLE_OBJECT_QUEST = 1137;
	/** 真端多计数采集任务（3 个 / Cherubim pouch）。 / Retail multi-count collect quest. */
	private static final int MULTI_COUNT_QUEST = 1103;
	/** 带中继 NPC 的采集任务（talk_npc1）。 / Retail collect quest with a relay npc. */
	private static final int RELAY_QUEST = 14120;
	/** 真端多列采集行（object1/object2 两列 × collect_item1/2 各 5 件）。 / Retail multi-column row. */
	private static final int MULTI_COLUMN_QUEST = 18501;
	private static final int MULTI_COLUMN_COUNT = 5;

	/** 无采集物的 TEST 行（只声明 reward_check）。 / The TEST rows without collect objects. */
	private static final Set<Integer> TEST_ROWS = Set.of(9649, 9650, 9654, 9655, 9656);
	/** 无采集计数的 9 行（TEST 5 + 事件行 4）。 / The nine rows without a collect count. */
	private static final Set<Integer> NO_COLLECT_COUNT_ROWS =
		Set.of(9649, 9650, 9654, 9655, 9656, 50017, 50018, 51017, 51018);

	private static NativeQuestTableLoader loader;
	private static CameraRegistry cameraRegistry;
	private static SimpleCollectItemHandler handler;

	/** 真端 minlevel=999 的休眠采集行（元数据 min>max，不派生相机行）。 / Retail dormant rows. */
	private static final Set<Integer> DORMANT_LEVEL_ROWS = Set.of(36017, 46017, 47112);

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		cameraRegistry = CameraRegistry.instance();
		handler = SimpleCollectItemHandler.instance();
	}

	// ---------------------------------------------------------------- 装载面

	@Test
	void loadsAll262RetailRowsWithTheTwoNpcShape() {
		assertEquals(262, loader.collectSize(), "真端 quest_simplecollectitems 全量行");
		List<SimpleCollectItemRow> rows = List.copyOf(loader.collectRows());
		assertTrue(rows.stream().allMatch(row ->
			!row.acquiredNpcName().isBlank() && !row.rewardNpcName().isBlank()),
			"双 NPC 结构必须 100%（真端 262/262）");
		assertEquals(257, rows.stream().filter(row -> !row.objects().isEmpty()).count(),
			"object1..4 覆盖 257 行");
		assertEquals(5, rows.stream().filter(row -> !row.talkNpcNames().isEmpty()).count(),
			"talk_npc1 覆盖 5 行（9620/14150/14120/9655/9656）");
		assertEquals(80, rows.stream().filter(SimpleCollectItemRow::partyDrop).count(),
			"party_drop 覆盖 80 行");
		assertEquals(37, rows.stream().filter(row -> row.conQuest() != null).count(),
			"con_quest 覆盖 37 行");
	}

	@Test
	void fiveTestRowsCarryNoObjectsAndTheNineEventRowsStayUnrouted() {
		for (int questId : TEST_ROWS) {
			assertTrue(loader.requireCollect(questId).objects().isEmpty(), "TEST 行无采集物: " + questId);
		}
		for (int questId : NO_COLLECT_COUNT_ROWS) {
			assertFalse(handler.routes(questId), "无采集物/无计数的行不可路由: " + questId);
		}
		assertTrue(handler.owns(1137), "有采集物的行必须在注册集内");
		for (int questId : DORMANT_LEVEL_ROWS) {
			assertFalse(handler.routes(questId), "休眠行不可路由: " + questId);
		}
	}

	/**
	 * 采集族真端无相机（camera-params.tsv 262/262 无调用；2026-10-04 修正），且 native 侧必须
	 * 从真端 drop 列接手任务掉落（退役 XML 的 {@code <drops>} 已随 catalog 退场）。
	 * The collect family has no retail camera, and the native lane must serve the retail drop column.
	 */
	@Test
	void collectFamilyDerivesNoCameraRowsAndServesRetailDrops() {
		for (SimpleCollectItemRow row : loader.collectRows()) {
			assertTrue(cameraRegistry.find(row.questId()).isEmpty(),
				"采集族不得派生相机行（真端 262/262 无相机调用）: " + row.questId());
		}
		// 1103：谷物袋子（700105）掉落 quest_1103a（182200201），chance=100（真端 drop_prob_1）。
		var drops = handler.questDropsFor(700105);
		assertTrue(drops.stream().anyMatch(drop -> drop.questId() == 1103
			&& drop.itemId() == 182200201 && drop.chance() == 100),
			"native 必须接手真端掉落列（真机 1103 谷物袋子）");
		assertTrue(loader.collectRows().stream().anyMatch(row -> row.questId() == 1137),
			"1137 仍在采集族装载面");
	}

	// ---------------------------------------------------------------- 行为面

	@Test
	void acceptGatesOnTheRetailQuestXmlAxesAndStartsTheQuest() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.completePrerequisites(player, 1102);
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCollectItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		int acquireNpc = local.acquireNpc(SINGLE_OBJECT_QUEST);
		assertNotNull(acquireNpc, "真端行必须有可解析的接取 NPC");

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, SINGLE_OBJECT_QUEST, 26)),
			"点接取 NPC 必须下发客户端声明的接取入口页");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(SINGLE_OBJECT_QUEST));
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, SINGLE_OBJECT_QUEST, 1007)),
			"页动作 1007（ASK_QUEST_ACCEPT）必须打开接取窗页 4");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.askWindowPage(SINGLE_OBJECT_QUEST));
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, SINGLE_OBJECT_QUEST, 1002)),
			"确认接取必须由 native 接取口服务");
		assertEquals(local.acceptGiveItems(SINGLE_OBJECT_QUEST).size(), inventory.calls().size(),
			"接取侧发放面 = quest_work_item 列（1137 = 1 件）");
		assertEquals(1011, NativeTalkFixture.clientEntryPage(SINGLE_OBJECT_QUEST),
			"该行的客户端入口页 = select1(1011) 信页（真端入口页表；页 4 只由 1007 打开）");
		assertEquals(4, NativeTalkFixture.askWindowPage(SINGLE_OBJECT_QUEST),
			"该行客户端声明 ask_quest_accept(4) ⇒ 1007 可打开接取窗");
		QuestState state = player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST);
		assertNotNull(state, "接取后必须建档");
		assertEquals(QuestStatus.START, state.getStatus());
	}

	/**
	 * 可重复行 COMPLETE 重开局（真端 {@code finishedcount < max_repeat_count}）：9620
	 * （max_repeat_count=255）完成后点任务行必须重新开放接取面并复位档案；1137（max=1）保持关闭。
	 * <p>
	 * Repeatable COMPLETE re-open: 9620 (max_repeat_count=255) must reopen its accept face after
	 * completion and reset on re-accept; the single-shot 1137 stays closed.
	 */
	@Test
	void repeatableCompletedRowReopensTheAcceptFace() {
		Player player = NativeTalkFixture.player();
		SimpleCollectItemHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.instance());
		QuestState state = NativeTalkFixture.add(player, 9620, QuestStatus.COMPLETE, 0);
		state.setCompleteCount(1);
		Integer acquireNpc = local.acquireNpc(9620);
		assertNotNull(acquireNpc, "真端行必须有可解析的接取 NPC");

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, 9620, 31)),
			"可重复行 COMPLETE 态点任务行必须开放接取面");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, NativeTalkFixture.clientEntryPage(9620), 9620);

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, acquireNpc, 9620, 1002)),
			"重复接取收尾（0x3ea）必须由 native 接取口服务");
		assertEquals(QuestStatus.START, state.getStatus(), "重复接取必须复位为 START");
		NativeTalkFixture.assertOnlyDialogPageWithQuest(player, 1003, 9620);

		// 非可重复行（1137：max_repeat_count=1）不受影响：COMPLETE 态仍不进接取面。
		QuestState single = NativeTalkFixture.add(player, SINGLE_OBJECT_QUEST, QuestStatus.COMPLETE, 0);
		single.setCompleteCount(1);
		NativeTalkFixture.clearPackets(player);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(player, local.acquireNpc(SINGLE_OBJECT_QUEST),
			SINGLE_OBJECT_QUEST, 31)), "max_repeat_count=1 的行 COMPLETE 态不得开放接取面");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "非可重复行不得下发接取页");
	}

	@Test
	void collectingTheObjectClaimsTheInteractionWithoutWritingState() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.completePrerequisites(player, 1102);
		NativeTalkFixture.start(player, SINGLE_OBJECT_QUEST);
		int objectNpc = handler.collectObjects(SINGLE_OBJECT_QUEST).getFirst();
		QuestState state = player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST);
		int before = state.getQuestVars().getQuestVars();

		assertTrue(handler.onObjectUse(player, SINGLE_OBJECT_QUEST, objectNpc),
			"点击采集对象必须被认领（掉落链由 AI 侧接手）");
		assertEquals(QuestStatus.START, state.getStatus(), "采集不改变状态");
		assertEquals(before, state.getQuestVars().getQuestVars(),
			"交互零状态写（物品驱动：var0 保持，客户端按 collect_progress=0 维持采集步）");
	}

	@Test
	void repeatedObjectClaimsStayZeroWrite() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, SINGLE_OBJECT_QUEST);
		int objectNpc = handler.collectObjects(SINGLE_OBJECT_QUEST).getFirst();
		QuestState state = player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST);
		assertTrue(handler.onObjectUse(player, SINGLE_OBJECT_QUEST, objectNpc));
		assertTrue(handler.onObjectUse(player, SINGLE_OBJECT_QUEST, objectNpc),
			"重复交互同样认领（数量由掉落上限控制，不在交互层）");
		assertEquals(0, state.getQuestVars().getQuestVars(), "重复交互始终零写");
	}

	@Test
	void killOfTheCollectMonsterWritesNoTaskState() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.completePrerequisites(player, 1102);
		NativeTalkFixture.start(player, MULTI_COUNT_QUEST);
		int monster = handler.collectMonsterTargets(MULTI_COUNT_QUEST).getFirst();
		QuestState state = player.getQuestStateList().getQuestState(MULTI_COUNT_QUEST);

		assertFalse(handler.onKill(player, monster), "击杀不做任务侧写入（掉落由通用击杀装配）");
		assertEquals(0, state.getQuestVars().getQuestVars(), "击杀零状态写");
	}

	@Test
	void relayChainGatesCollectionInTableOrder() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, RELAY_QUEST);
		int objectNpc = handler.collectObjects(RELAY_QUEST).getFirst();
		assertFalse(handler.onObjectUse(player, RELAY_QUEST, objectNpc),
			"中继链未走完时不得开始采集");
		int relayNpc = handler.relayNpcs(RELAY_QUEST).getFirst();
		assertTrue(handler.onDialog(NativeTalkFixture.dialog(player, relayNpc, RELAY_QUEST, 26)),
			"中继 NPC 对话必须推进链条");
		assertTrue(handler.onObjectUse(player, RELAY_QUEST, objectNpc),
			"中继链走完后必须可采集");
	}

	@Test
	void handInRequiresTheCollectItems() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCollectItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		NativeTalkFixture.start(player, SINGLE_OBJECT_QUEST);
		int rewardNpc = local.rewardNpc(SINGLE_OBJECT_QUEST);
		int handInItem = local.handInItems(SINGLE_OBJECT_QUEST).getFirst();

		// 未持有：交付 NPC 处仍是进行中页。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, 26)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCollectItemHandler.PAGE_IN_PROGRESS);

		// 未持有交付物点任务行（2026-10-05 缺陷 T，实机 1126）：报告页照发（1137 契约 select5=2375）——
		// 物品门只在确认动作（39/1009）上分叉，31 零推进零扣物。
		// Row selection while the hand-in items are missing (defect T): the confirm page is sent anyway.
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(), "未持有 31 零推进");
		assertTrue(inventory.calls().isEmpty(), "未持有 31 零扣物");

		// 持有交付物即满足真端 check_item 门（无相机门——2026-10-04 起采集为物品驱动）。
		inventory.hold(handInItem, 1);
		NativeTalkFixture.clearPackets(player);
		// 两步报告（裁定 a）：31 只发客户端声明的报告确认页（1137 契约声明 2375=select5）不推进；
		// 1009 报告确认才翻 REWARD + 开奖励窗。开门动作（26/-1）不推进。
		// Two-step report: 31 shows the declared confirm page; 1009 advances to REWARD.
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(), "31 不推进状态");
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, 1009)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCollectItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(),
			"交付门通过后翻 REWARD");
		assertEquals(List.of("remove:" + handInItem + ":1"), inventory.calls());
	}

	/**
	 * 39 检查按钮（2026-10-04 真机 1103 报告页按钮）：报告确认动作随任务页而分——检查型的
	 * select5 按钮 = {@code HACTION_CHECK_USER_HAS_QUEST_ITEM}(39)。持满时 39 与 1009 同义
	 * （推进 REWARD + 奖励窗 + 扣物）；未持满时下发客户端声明的失败页（1137 契约 select6=2716）。
	 * The 39 check button (the live 1103 report page): the confirm action varies per task page — the
	 * check form puts 39 on its select5 page. With the whole group held, 39 advances like 1009; when
	 * the group is missing it shows the declared fail page (select6).
	 */
	@Test
	void reportPageCheckButton39AdvancesOrShowsTheDeclaredFailPage() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCollectItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		NativeTalkFixture.start(player, SINGLE_OBJECT_QUEST);
		int rewardNpc = local.rewardNpc(SINGLE_OBJECT_QUEST);
		int handInItem = local.handInItems(SINGLE_OBJECT_QUEST).getFirst();
		int checkAction = QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id();

		// 未持满：39 → 声明失败页（select6=2716），零状态写、零扣物。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, checkAction)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT6.id());
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(), "失败检查不推进");
		assertTrue(inventory.calls().isEmpty(), "失败检查不扣物品");

		// 持满：39 与 1009 同义——REWARD + 奖励窗 + 按真端计数扣物。
		inventory.hold(handInItem, 1);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, checkAction)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCollectItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(), "39 持满即推进");
		assertEquals(List.of("remove:" + handInItem + ":1"), inventory.calls());
	}

	/**
	 * 20002 检查按钮（{@code HACTION_CHECK_USER_HAS_QUEST_ITEM_SIMPLE}，2026-10-06 实机 14110 同类）：
	 * 本族 93 行的报告页按钮用 20002 编码（如 1144「拿出南瓜」），只匹配 39 的判定会让这些行的
	 * 确认动作整体落空。20002 必须与 39 同族同义：持满 → REWARD + 奖励窗 + 扣物；未持满 → 声明失败页。
	 * The 20002 check button must behave exactly like 39 for this family's 93 rows.
	 */
	@Test
	void reportPageCheckButtonSimpleEncodingBehavesLikeThePlainForm() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCollectItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		NativeTalkFixture.start(player, SINGLE_OBJECT_QUEST);
		int rewardNpc = local.rewardNpc(SINGLE_OBJECT_QUEST);
		int handInItem = local.handInItems(SINGLE_OBJECT_QUEST).getFirst();
		int checkAction = QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM_SIMPLE.id();

		// 未持满：20002 → 同 39，声明失败页（select6=2716），零状态写、零扣物。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, checkAction)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT6.id());
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(), "失败检查不推进");
		assertTrue(inventory.calls().isEmpty(), "失败检查不扣物品");

		// 持满：20002 与 1009 同义——REWARD + 奖励窗 + 按真端计数扣物。
		inventory.hold(handInItem, 1);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, checkAction)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCollectItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(SINGLE_OBJECT_QUEST).getStatus(), "20002 持满即推进");
		assertEquals(List.of("remove:" + handInItem + ":1"), inventory.calls());
	}

	/**
	 * 多列行（18501 两列 ×5）必须逐列持有：只持一列不得放行交付，两列交付物齐（真端 check_item）才翻 REWARD。
	 * Multi-column rows need every column's items held: one column never opens the hand-in.
	 */
	@Test
	void multiColumnRowsRequireEveryColumnsItemsBeforeHandInOpens() {
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCollectItemHandler local = handlerWith(inventory, NativeReportRewardFlow.instance());
		NativeTalkFixture.start(player, MULTI_COLUMN_QUEST);
		List<Integer> objects = local.collectObjects(MULTI_COLUMN_QUEST);
		assertEquals(2, objects.size(), "18501 真端两列对象");
		List<Integer> handIns = local.handInItems(MULTI_COLUMN_QUEST);
		assertEquals(2, handIns.size(), "18501 真端两列交付物");
		int rewardNpc = local.rewardNpc(MULTI_COLUMN_QUEST);

		// 交互认领（零写）；只持第一列 ⇒ 第二列缺 ⇒ 交付 NPC 处仍是进行中页。
		assertTrue(local.onObjectUse(player, MULTI_COLUMN_QUEST, objects.get(0)));
		inventory.hold(handIns.get(0), MULTI_COLUMN_COUNT);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, MULTI_COLUMN_QUEST, 26)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCollectItemHandler.PAGE_IN_PROGRESS);
		assertTrue(inventory.calls().isEmpty(), "缺列不得扣物品");

		// 两列交付物齐 ⇒ 翻 REWARD、开奖励窗、两份都扣。
		inventory.hold(handIns.get(1), MULTI_COLUMN_COUNT);
		NativeTalkFixture.clearPackets(player);
		// 两步报告（裁定 a）：31 发 18501 契约声明的报告确认页（2375），1009 推进 REWARD。
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, MULTI_COLUMN_QUEST, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, 2375);
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, MULTI_COLUMN_QUEST, 1009)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCollectItemHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(MULTI_COLUMN_QUEST).getStatus());
		assertEquals(List.of("remove:" + handIns.get(0) + ":" + MULTI_COLUMN_COUNT,
			"remove:" + handIns.get(1) + ":" + MULTI_COLUMN_COUNT), inventory.calls(),
			"两列交付物必须各按真端计数扣一份");
	}

	/** 交付后领取奖励：走 native 完成口（真端 reward 列 → 共用结算体）。 / Claim through the native settlement port. */
	@Test
	void claimCompletesThroughTheRetailDerivedTemplate() {
		List<ClaimCall> calls = new ArrayList<>();
		SimpleCollectItemHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY,
			NativeReportRewardFlow.forTest(SimpleCollectItemNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					calls.add(new ClaimCall(env.getQuestId(), tier, template));
					return true;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, SINGLE_OBJECT_QUEST, QuestStatus.REWARD, 1);
		int rewardNpc = local.rewardNpc(SINGLE_OBJECT_QUEST);

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, SINGLE_OBJECT_QUEST, 8)),
			"奖励窗按钮必须由 native 领奖段服务");
		// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG：回选择对话页（页 10，questId=0）。
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(1, calls.size());
		assertEquals(SINGLE_OBJECT_QUEST, calls.getFirst().questId());
		assertEquals(0, calls.getFirst().tier(), "单奖励槽行固定首档（真端 reward_*1）");
		assertNotNull(calls.getFirst().template(), "结算体必须拿到真端奖励列重建的 typed 模板");
	}

	@Test
	void collectRowsAreOwnedByTheNativeLaneAndAbsentFromTheRetailCompiler() {
		assertTrue(handler.ownedQuestIds().containsAll(List.of(SINGLE_OBJECT_QUEST, MULTI_COUNT_QUEST,
			RELAY_QUEST)), "三个代表行必须在 native 注册集内");
		assertTrue(Set.of(SINGLE_OBJECT_QUEST, MULTI_COUNT_QUEST, RELAY_QUEST)
			.stream().allMatch(handler::routes), "P4 切换后代表行必须由 native 路由");
		// 同批删旧（P7 步 f 闭环）：旧编译 owner 面已随 RetailQuestDriver 编译车道整体退场（结构性）。
	}

	/**
	 * 链式接取窗（真端交付节点 0x1e 槽 = {@code mgr+0x1a8(player, con_quest)}）：37 行全部装载，且
	 * 本表内的下一环必须在本行的交付 NPC 上可接取（本车道按 NPC 建接取路由 ⇒ 该窗已由下一环自身实现）。
	 * <p>
	 * The chain window (retail hand-in slot 0x1e): all 37 rows load and every in-table next quest acquires
	 * at this row's hand-in NPC, so the window is already realized by the next quest's own accept route.
	 */
	@Test
	void chainWindowsAreLoadedAndCloseAtTheHandInNpc() {
		int declared = 0;
		int inTable = 0;
		for (SimpleCollectItemRow row : loader.collectRows()) {
			Integer next = row.conQuest();
			assertEquals(next, handler.conQuest(row.questId()), "con_quest 装载漂移: " + row.questId());
			if (next == null) {
				continue;
			}
			declared++;
			if (loader.collectRows().stream().noneMatch(candidate -> candidate.questId() == next)) {
				continue;
			}
			inTable++;
			Integer targetAcquire = handler.acquireNpc(next);
			assertNotNull(targetAcquire, "本表内下一环必须有接取 NPC: " + row.questId() + "->" + next);
			assertTrue(handler.rewardNpcs(row.questId()).contains(targetAcquire),
				"链式接取窗未在本行交付 NPC 上闭环: " + row.questId() + "->" + next);
		}
		assertEquals(37, declared, "真端 con_quest 覆盖 37 行");
		assertEquals(6, inTable, "本表内链式目标 6 行（其余为跨族/无行，本族对拍门复算）");
		assertTrue(handler.unresolvedChainQuestIds().isEmpty(),
			() -> "本族链式接取窗未闭环: " + handler.unresolvedChainQuestIds());
	}

	/**
	 * 过场（真端交付节点 0x35 槽 PlayMovie）：动作命中表声明的 {@code cs1_haction} 时下发
	 * {@code cutsceneid1}，是状态机之外的副作用；未声明的行与触发动作之外的动作都不下发。
	 * <p>
	 * The cutscene (retail hand-in slot 0x35 PlayMovie): the declared action triggers the declared movie
	 * as a side effect outside the state machine, and nothing else does.
	 */
	@Test
	void cutscenePlaysOnTheDeclaredActionWithoutAdvancingTheNode() {
		RecordingMovies movies = new RecordingMovies();
		SimpleCollectItemHandler local = handlerWith(NativeTalkFixture.RecordingInventory.EMPTY, movies,
			NativeReportRewardFlow.instance());
		assertEquals(456, local.cutscene(MULTI_COLUMN_QUEST).movieId(), "18501 真端 cutsceneid1");
		assertEquals(QuestDialogPage.SELECT1_1.id(), local.cutscene(MULTI_COLUMN_QUEST).triggerAction(),
			"18501 真端 cs1_haction = select1_1(1012)");
		assertNull(local.cutscene(SINGLE_OBJECT_QUEST), "未声明过场的行不得有过场面");

		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, MULTI_COLUMN_QUEST, QuestStatus.START, 0);
		int rewardNpc = local.rewardNpc(MULTI_COLUMN_QUEST);
		int varsBefore = player.getQuestStateList().getQuestState(MULTI_COLUMN_QUEST)
			.getQuestVars().getQuestVars();

		// 非触发动作（26 = 交付问询）：被服务但不播过场。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, MULTI_COLUMN_QUEST, 26)));
		assertTrue(movies.played().isEmpty(), "非触发动作不得下发过场");

		// 交付面的动作集（31/26/1009）不含该页动作 ⇒ 该状态不服务、不下发（真端该动作挂在接取页链上）。
		NativeTalkFixture.clearPackets(player);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(player, rewardNpc, MULTI_COLUMN_QUEST,
			QuestDialogPage.SELECT1_1.id())), "交付面不服务接取侧页动作（真端该 action 属 select1 页链）");
		assertTrue(movies.played().isEmpty(), "未服务的动作不得下发过场");
		assertEquals(varsBefore, player.getQuestStateList().getQuestState(MULTI_COLUMN_QUEST)
			.getQuestVars().getQuestVars(), "任何对话都不得改写任务 vars");

		// 接取页链（1012 = select1_1，客户端契约已声明该页）：动作被服务 ⇒ 下发真端 movie，
		// 且只是状态机之外的副作用（不建任务档、不推进节点）。
		Player accepting = NativeTalkFixture.player();
		movies.clear();
		NativeTalkFixture.clearPackets(accepting);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(accepting, local.acquireNpc(MULTI_COLUMN_QUEST),
			MULTI_COLUMN_QUEST, QuestDialogPage.SELECT1_1.id())));
		assertEquals(List.of(456), movies.played(), "命中 cs1_haction 必须下发 cutsceneid1");
		NativeTalkFixture.assertOnlyDialogPage(accepting, QuestDialogPage.SELECT1_1.id());
		assertNull(accepting.getQuestStateList().getQuestState(MULTI_COLUMN_QUEST),
			"过场不建任务档（仍须走 20000/1002 接取）");
	}

	// ---------------------------------------------------------------- 夹具

	/** 记录式假过场端口（真端 0x35 槽）。 / A recording fake cutscene port (retail slot 0x35). */
	private static final class RecordingMovies implements NativeMoviePort {
		private final List<Integer> played = new ArrayList<>();

		@Override
		public void play(Player player, int movieId) {
			played.add(movieId);
		}

		private List<Integer> played() {
			return List.copyOf(played);
		}

		private void clear() {
			played.clear();
		}
	}

	private record ClaimCall(int questId, int tier, com.aionemu.gameserver.model.templates.QuestTemplate template) {
	}

	private static java.util.Optional<RetailQuestMetadataCompiler.Outcome> metadata(int questId) {
		try {
			return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId);
		} catch (java.io.IOException | RuntimeException e) {
			return java.util.Optional.empty();
		}
	}

	private static SimpleCollectItemHandler handlerWith(NativeInventoryPort inventory,
			NativeReportRewardFlow rewardFlow) {
		return handlerWith(inventory, NativeMoviePort.live(), rewardFlow);
	}

	private static SimpleCollectItemHandler handlerWith(NativeInventoryPort inventory, NativeMoviePort moviePort,
			NativeReportRewardFlow rewardFlow) {
		return new SimpleCollectItemHandler(NativeQuestTableLoader.instance(),
			NativeNpcNameResolver.instance(), HtmlPagesRegistry.instance(), inventory, moviePort, rewardFlow,
			NativeQuestOwnerResolver.instance().xmlOnlyIds(), new java.util.TreeSet<>());
	}
}
