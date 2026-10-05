package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
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
	/** XML_RETENTION 行数（`retail-xml-retention.xml` 中 SimpleTalk 家族的行数）。 / XML-retained SimpleTalk rows. */
	private static final int XML_RETAINED_ROWS = 18;
	/**
	 * 未解析的 NPC 名数量（P9 语义层收口后冻结）：3 个系统发放哨兵 + 1 个真端缺名
	 * （{@code LDF5A_Munition_Vritra}，2 个中继单元格）。
	 * Frozen unresolved count after the P9 semantic closure: the three system-grant sentinels plus the
	 * single remaining retail data gap ({@code LDF5A_Munition_Vritra}).
	 */
	private static final int FROZEN_UNRESOLVED = 4;
	/** 接取列未解析行数（= 三个哨兵的行数 98+75+8）。 / Rows whose acquire name stays unresolved. */
	private static final int FROZEN_UNRESOLVED_ACQUIRE_ROWS = 181;
	/** 交付列未解析行数（P9 收口后交付面全部可解析）。 / Rows whose reward name stays unresolved. */
	private static final int FROZEN_UNRESOLVED_REWARD_ROWS = 0;
	/** 系统发放哨兵的行数（接取列）。 / Row counts of the system-grant sentinels on the acquire column. */
	private static final Map<String, Integer> FROZEN_SENTINEL_ROWS = Map.of(
			"_faction_", 98, "_challengetask_", 75, "_area_", 8);
	/**
	 * 接取可解析但交付不可解析的行数（真端表交付列落在 DATA_GAP 名上，属静态数据缺口而非分派缺陷）。
	 * Rows with a resolvable acquire NPC but a gapped reward NPC: a static-data gap on the retail reward column.
	 */
	private static final int FROZEN_ACQUIRE_OK_REWARD_GAP_ROWS = 0;
	/** 三段中继 + 三个发物 + item_check 的真端行。 / A three-relay retail row. */
	private static final int CHAINED_QUEST = 41536;
	/** 单中继步 + 接取发放 + 步内发放/扣除的真端行（与 cab520/cabb10 立即数对拍）。 / Retail row 1131. */
	private static final int ITEM_QUEST = 1131;
	/**
	 * 检查型报告行的代表行（routed ∩ 39 用户：select5 按钮 = 39、契约声明失败页 select6=2716）。
	 * A routed check-form report row (the 39 button on select5, the fail page select6 declared).
	 */
	private static final int CHECK_BUTTON_QUEST = 1211;
	/**
	 * 未解析物品面冻结（`p3/simple-talk-unresolved-items.tsv`）：11 个交付门缺口 + 3 个部分缺口符号。
	 * Frozen unresolved item face (evidence snapshot in `p3/simple-talk-unresolved-items.tsv`).
	 */
	/**
	 * item_check 行里「门通道全缺」的行 = 真端不可接取行（quest.xml {@code client_level}/{@code minlevel_permitted}=999）。
	 * 真端 {@code Quest::CanAcquireQuest} 对 {@code level < minlevel} 一律拒绝，故这些行的报告门在真端不可达；
	 * native 侧按 fail-closed 处理（不可观测），并逐行冻结以察觉数据漂移。
	 * The item_check rows with no gate channel at all: unreachable in the true server.
	 */
	private static final Set<Integer> UNREACHABLE_GATE_ROWS =
			Set.of(2732, 30509, 41571, 50011, 50012, 51011, 51012);
	/** item_check 且交付门成立的行数（真端表/quest.xml 全量复算）。 / Rows whose hand-in gate resolves. */
	private static final int GATE_ROWS = 1981;

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
		// 单一 owner：注册集 3152 = 路由集 3134 + XML_RETENTION 18（表行仍带 XML 定义者交给 XML 车道）。
		// Single owner: registration set = routing set + the 18 XML_RETENTION rows.
		assertEquals(EXPECTED_ROWS - XML_RETAINED_ROWS, handler.routedQuestIds().size(),
				() -> "路由集必须 = 注册集 − XML 保留行，实际=" + handler.routedQuestIds().size());
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
		// 入口路由用可接取行 1131（minlevel 10 < 玩家 20 级、天族、无前置）。
		// CHAINED_QUEST 41536 的等级轴 999 = 真端不可达行：31 带 CanAcquireQuest 同源预检
		// （P7-REPORT「清单与接取面同一判定函数」），资格不满足不得进接取面。
		// Route with the eligible row 1131; 41536 (level axis 999) stays out of the acquire face —
		// the 31 entry shares CanAcquireQuest with the nearby list.
		int questId = ITEM_QUEST;
		Npc acquire = createMockNpc(handler.acquireNpc(questId));

		// QUEST_SELECT → 问询页（cab520 的入口动作）。
		assertTrue(handler.onDialog(new QuestEnv(acquire, player, questId, 31)));
		// 非接取 NPC 不响应（真端按节点槽分派，不跨 NPC）。
		assertFalse(handler.onDialog(new QuestEnv(createMockNpc(1), player, questId, 31)));
		// 问询页只开窗、不落库；落库走 native 建档口（见 acceptCommitCreatesTheRetailRow）。
		assertNull(player.getQuestStateList().getQuestState(questId));

		// 不可达行（等级 999 + 前置 Q41535 未完成）：31 不得进接取面（与清单三值同源）。
		Player ineligible = createTestPlayer();
		assertFalse(handler.onDialog(new QuestEnv(createMockNpc(handler.acquireNpc(CHAINED_QUEST)),
				ineligible, CHAINED_QUEST, 31)), "等级轴不可达/前置未完成不得进接取面");
	}

	/** 接取落库：真端条件轴（等级/种族/职业/性别/重复）通过后由 native 状态端口建档到 START。 */
	@Test
	void acceptCommitCreatesTheRetailRow() {
		Player player = createTestPlayer();
		Npc acquire = createMockNpc(handler.acquireNpc(ITEM_QUEST));

		assertTrue(handler.onDialog(new QuestEnv(acquire, player, ITEM_QUEST, 31)), "问询页");
		assertTrue(handler.onDialog(new QuestEnv(acquire, player, ITEM_QUEST, 1002)), "接取必须落库（native 建档口）");
		QuestState state = player.getQuestStateList().getQuestState(ITEM_QUEST);
		assertEquals(QuestStatus.START, state.getStatus());
		assertEquals(0, state.getQuestVars().getQuestVars(), "接取复位 raw vars");

		// 真端 max_repeat_count=1：完成后不得再次接取。
		state.setStatus(QuestStatus.COMPLETE);
		state.setCompleteCount(1);
		assertFalse(handler.onDialog(new QuestEnv(acquire, player, ITEM_QUEST, 1002)),
				"不可重复行完成后必须拒绝接取");
		assertEquals(QuestStatus.COMPLETE, state.getStatus(), "被拒时不得改写状态");
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

	/**
	 * 任务行打开中继对话（2026-10-04，1118 load fail 修复）：31/26 只发**该步页**
	 * （9/28 基线 1118：vars=0/step=1 点 31 → 1352），不再发「页 10 带 questId」（任务页契约
	 * 无页 10 ⇒ 客户端 load fail）；尚未轮到的步零响应不跳步；已推进的步可重看（零副作用）。
	 * Row selection opens the step dialog with the step page (the 1118 load-fail fix); a step not
	 * reached yet stays silent; an advanced step is replayable with no side effect.
	 */
	@Test
	void relayRowSelectionOpensTheStepDialog() {
		Player player = NativeTalkFixture.player();
		int questId = CHAINED_QUEST;
		List<String> relayNames = itemHandler.requireRow(questId).talkNpcNames();
		Npc first = createMockNpc(resolve(relayNames.get(0)));
		Npc second = createMockNpc(resolve(relayNames.get(1)));

		QuestState state = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, state);

		// 当前待推进步：31 → 该步页（契约声明 select2 = 1352），且不触碰物品通道。
		inventory.clear();
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(first, player, questId, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.pageForStep(1));
		assertTrue(inventory.calls.isEmpty(), "打开对话页不得触碰物品通道");

		// 尚未轮到的步：零响应、零下发、零推进。
		NativeTalkFixture.clearPackets(player);
		assertFalse(itemHandler.onDialog(new QuestEnv(second, player, questId, 31)),
				"未轮到的中继步不得服务 / an unreached relay step must not be served");
		assertTrue(NativeTalkFixture.dialogPages(player).isEmpty(), "未轮到的步必须零下发");
		assertEquals(0, state.getQuestVars().getQuestVars());

		// 推进（动作 10000 = select2_1 页的「结束对话」按钮）：after-commit = 真端 cabb10 关窗
		// （SetQuestProgress + 0x5d8、**零发页**；2026-10-05 实机「一次点击即关窗」）——
		// 旧「回选择对话页 10」系翻译夸大，2026-10-05 1131 实机「结束对话后多余弹页」已修正。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(first, player, questId, 10000)));
		assertEquals(1, state.getQuestVars().getQuestVars(), "推进后步号 = 1");
		NativeTalkFixture.assertCloseDialog(player);

		// 重复推进（已推进后重放 SETPRO1）：真端无匹配转换 ⇒ close-dialog 兜底、零推进。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(first, player, questId, 10000)));
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(1, state.getQuestVars().getQuestVars(), "重复推进不得再改步号");

		// 已推进的步可重看（幂等，零状态写）。
		state.getQuestVars().setVar(1);
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(first, player, questId, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.pageForStep(1));
		assertEquals(1, state.getQuestVars().getQuestVars());
	}

	/**
	 * 选择对话续页（2026-10-04，1118 全链修复）：客户端把翻页按钮写作页 id（SELECT2_1=1353），
	 * 真端对该动作**原样回发该页**（9/28 基线跨任务实证 1353/1354/1694/1695/2035/2376）；
	 * 契约未声明该页的行 fail-closed 零响应。
	 * Selection sub-page actions echo their page back (the 9/28 baseline); undeclared pages fail closed.
	 */
	@Test
	void selectSubPageActionsEchoTheClientDeclaredPage() {
		// 1131 契约声明 1353（select2_1）。
		Player player = NativeTalkFixture.player();
		int questId = ITEM_QUEST;
		player.getQuestStateList().addQuest(questId,
				new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null));
		Npc relay = createMockNpc(resolve(itemHandler.requireRow(questId).talkNpcNames().getFirst()));

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(relay, player, questId, QuestDialogPage.SELECT2_1.id())));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT2_1.id());

		// 41536 契约无 1353：fail-closed 零响应。
		Player other = NativeTalkFixture.player();
		int gapped = CHAINED_QUEST;
		other.getQuestStateList().addQuest(gapped,
				new QuestState(gapped, QuestStatus.START, 0, 0, null, 0, null));
		Npc gappedRelay = createMockNpc(resolve(itemHandler.requireRow(gapped).talkNpcNames().get(0)));
		NativeTalkFixture.clearPackets(other);
		assertFalse(itemHandler.onDialog(new QuestEnv(gappedRelay, other, gapped, QuestDialogPage.SELECT2_1.id())),
				"契约未声明的子页动作必须 fail-closed");
		assertTrue(NativeTalkFixture.dialogPages(other).isEmpty());
	}

	/** 物品面：真端 cab520/cabb10 的物品通道与 item_check 交付门（含冻结缺口）。 */
	@Test
	void itemFaceFollowsTheRetailChannels() {
		// 真端符号面 100% 可解（2026-10-01 全量复算：SimpleTalk 表 give/remove 列 663 个符号全为
		// ITEM_X 形；quest.xml collect/work 列 3394 个符号全为原名形，其中含 item_* 真名）→ 白名单归零。
		assertTrue(handler.unresolvedItemSymbols().isEmpty(),
				() -> "物品符号必须 100% 可解，实际未解=" + handler.unresolvedItemSymbols());

		// 1131 ↔ cab520 立即数 182200506（接取发放）与 cabb10 立即数 182200507/182200506（步内发/扣）。
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.acceptGiveItem(ITEM_QUEST));
		assertEquals(new SimpleTalkHandler.ItemStack(182200507, 1), handler.stepGiveItem(ITEM_QUEST, 1));
		assertEquals(new SimpleTalkHandler.ItemStack(182200506, 1), handler.stepRemoveItem(ITEM_QUEST, 1));
		assertTrue(handler.workItems(ITEM_QUEST).isEmpty(), "1131 无 item_check / no hand-in gate");

		// 41536 的交付门 = quest.xml collect_item1..3。
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(182212534, 1),
				new SimpleTalkHandler.ItemStack(182212535, 1), new SimpleTalkHandler.ItemStack(182212536, 1)),
				handler.workItems(CHAINED_QUEST));

		// 1988 个 item_check 行：1981 行交付门成立，7 行门通道全缺（真端不可接取行，见下条门禁）。
		assertEquals(GATE_ROWS,
				handler.ownedQuestIds().stream().filter(id -> !handler.workItems(id).isEmpty()).count());
		// 新解出的两个真端通道样本：16921 = collect_item1（item_idunderrune_quest_01 20）；
		// 80669 = collect_item1..3（第三项 item_exp_extraction_65a 为带前缀的真名）。
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(186000256, 20)),
				itemHandler.workItems(16921));
		// 80669：collect_item1..3 = medal_07 1050 / junk_world_event_s4_quest_01a 1 / item_exp_extraction_65a 1。
		assertEquals(List.of(new SimpleTalkHandler.ItemStack(186000469, 1050),
				new SimpleTalkHandler.ItemStack(182007165, 1), new SimpleTalkHandler.ItemStack(169405376, 1)),
				itemHandler.workItems(80669));
	}

	/** 门通道全缺的行 = 真端不可接取行；fail-closed 处理，且必须逐行冻结。 */
	@Test
	void gateLessRowsAreTheRetailUnreachableSet() {
		Set<Integer> gateLess = new java.util.TreeSet<>();
		for (int questId : handler.ownedQuestIds()) {
			if (handler.workItems(questId).isEmpty() && handler.unresolvedGate(questId)) {
				gateLess.add(questId);
			}
		}
		assertEquals(UNREACHABLE_GATE_ROWS, gateLess, "门缺通道行集漂移 / gate-less row set drifted");
		for (int questId : gateLess) {
			var row = NativeQuestXmlTable.instance().find(questId).orElseThrow();
			assertTrue("999".equals(row.text("client_level")) || "999".equals(row.text("minlevel_permitted")),
					"门缺通道行必须真端不可接取（client_level/minlevel=999）: " + questId);
		}
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

		// 未持门点任务行（2026-10-05 缺陷 T，实机 1126）：报告页照发（41536 契约 select5=2375）——
		// 物品门只在确认动作（39/1009）上分叉，31 零推进零扣物。
		// Row selection with the gate still short (defect T): the confirm page is sent anyway;
		// the item gate forks on the confirm action only.
		Player pagePlayer = NativeTalkFixture.player();
		QuestState pageState = new QuestState(gated, QuestStatus.START, 3, 0, null, 0, null);
		pagePlayer.getQuestStateList().addQuest(gated, pageState);
		inventory.calls.clear();
		NativeTalkFixture.clearPackets(pagePlayer);
		assertTrue(itemHandler.onDialog(new QuestEnv(rewardNpc, pagePlayer, gated, 31)));
		NativeTalkFixture.assertOnlyDialogPage(pagePlayer, QuestDialogPage.SELECT5.id());
		assertEquals(QuestStatus.START, pageState.getStatus(), "未持门 31 零推进");
		assertTrue(inventory.calls.isEmpty(), "未持门 31 零扣物");
	}

	/**
	 * 39 检查按钮（2026-10-04）：检查型报告页的确认动作 = {@code HACTION_CHECK_USER_HAS_QUEST_ITEM}(39)。
	 * 持满交付门 → 与 1009 同义推进 REWARD + 奖励窗 + 扣门物；未持满 → 客户端声明的失败页
	 * （1211 契约 select6=2716），零状态写、零扣物。
	 * The 39 check button: with the gate held it advances like 1009; when the gate is missing it shows
	 * the client-declared fail page (select6).
	 */
	@Test
	void reportCheckButton39AdvancesOrShowsTheDeclaredFailPage() {
		int questId = CHECK_BUTTON_QUEST;
		Player player = NativeTalkFixture.player();
		QuestState state = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		player.getQuestStateList().addQuest(questId, state);
		Npc rewardNpc = createMockNpc(itemHandler.rewardNpc(questId));
		int checkAction = QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id();
		inventory.clear();

		// 未持满：39 → 声明失败页（select6=2716），状态仍 START、零扣物。
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(rewardNpc, player, questId, checkAction)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT6.id());
		assertEquals(QuestStatus.START, state.getStatus(), "失败检查不推进");
		assertTrue(inventory.calls.isEmpty(), "失败检查不扣物品");

		// 持满：39 与 1009 同义——REWARD + 奖励窗 + 按门扣除。
		for (SimpleTalkHandler.ItemStack item : itemHandler.workItems(questId)) {
			inventory.held.put(item.itemId(), (long) item.count());
		}
		List<String> expectedRemovals = itemHandler.workItems(questId).stream()
			.map(item -> "remove:" + item.itemId() + ":" + item.count())
			.toList();
		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(rewardNpc, player, questId, checkAction)));
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleTalkHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD, state.getStatus(), "39 持满即推进");
		assertEquals(expectedRemovals, inventory.calls);
	}

	/**
	 * 报告页分型跳过被中继步占用的 SELECT2（缺陷 S，2026-10-05，1118 实机）：双页任务的中继完成后，
	 * 交付 NPC 的任务行（31）发报告页 select5（2375；按钮「拿出药膏」=SELECT_QUEST_REWARD），
	 * 而不是中继对话树 select2（1352；按钮 SELECT2_1 翻页）。误判让 31 发 1352，报告确认 10000
	 * 落空关窗（1118 实机 2026-10-05 08:53）。1131 双页：select2=Shugo 中继树 / select5=Nadaelo 报告页。
	 * The report-page typing skips the relay-consumed SELECT2 (defect S): with the relay chain done the
	 * delivery npc's row selection shows the select5 report page, not the select2 relay dialog tree.
	 */
	@Test
	void reportPageSkipsTheRelayConsumedSelect2() {
		int questId = ITEM_QUEST; // 1131：单中继步 + 双页（select2 中继 / select5 报告）
		Player player = NativeTalkFixture.player();
		QuestState state = new QuestState(questId, QuestStatus.START, 0, 0, null, 0, null);
		state.getQuestVars().setVar(1); // 中继完成（relayCount=1）
		player.getQuestStateList().addQuest(questId, state);
		Npc rewardNpc = createMockNpc(itemHandler.rewardNpc(questId));

		NativeTalkFixture.clearPackets(player);
		assertTrue(itemHandler.onDialog(new QuestEnv(rewardNpc, player, questId, 31)));
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT5.id());
	}

	/**
	 * Talk 族真端击杀掉落（2026-10-04 修复）：P3 迁移只接手对话面，退役 XML 连同其 {@code <drops>}
	 * 退出 catalog 后本族 1031 行的击杀掉落断供（真机 1105：击杀 210079 无任务道具）。native 必须
	 * 从 quest.xml drop 列接手注册；XML 保留行仍由 XML 车道供源（单一 owner，不得重复注册）。
	 * The Talk family's retail kill drops (fixed 2026-10-04): the kill drops of 1031 rows went dark
	 * with the retired XML; the native lane registers them from the quest.xml columns, and XML-owned
	 * rows stay with the XML lane (single owner).
	 */
	@Test
	void talkFamilyServesTheRetailKillDrops() throws Exception {
		// 1105：击杀 MerdionQ_2_n（210079）掉落 quest_1105a（182200202），退役 XML
		// `drop npc-id=210079 item-id=182200202 chance=100 each-member`。
		Integer itemId = RetailItemNameIndex.loadItemTemplates().resolve("quest_1105a");
		assertNotNull(itemId, "真端物品符号必须解析: quest_1105a");
		List<QuestCatalogDrop> drops = itemHandler.questDropsFor(210079);
		assertTrue(drops.stream().anyMatch(drop -> drop.questId() == 1105 && drop.itemId() == itemId),
			"1105 必须携带 210079 的真端击杀掉落条目");

		// XML 保留行（9548，XmasEvent_Rudolph 99）不得出现在 native 掉落面（单一 owner）。
		List<Integer> rudolphIds = NativeNpcNameResolver.instance().resolveMonsterIds("XmasEvent_Rudolph_99_n");
		assertFalse(rudolphIds.isEmpty(), "真端怪名必须解析: XmasEvent_Rudolph_99_n");
		for (int npcId : rudolphIds) {
			assertTrue(itemHandler.questDropsFor(npcId).stream().noneMatch(drop -> drop.questId() == 9548),
				"XML 保留行的掉落不得由 native 重复供源: 9548 / npc " + npcId);
		}
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
		// setPlayerClass/setLevel 需要经验表就绪（单测无服务栈）⇒ 直接写字段；等级取真端 1131 的 minlevel 之上。
		setField(pcd, PlayerCommonData.class, "playerClass",
				com.aionemu.gameserver.model.PlayerClass.WARRIOR);
		setField(pcd, PlayerCommonData.class, "level", 20);
		setField(player, Player.class, "playerCommonData", pcd);
		player.setQuestStateList(new QuestStateList());
		return player;
	}

	private static void setField(Object target, Class<?> type, String name, Object value) {
		try {
			java.lang.reflect.Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
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
