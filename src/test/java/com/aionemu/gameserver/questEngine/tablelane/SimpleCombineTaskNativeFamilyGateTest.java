package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.retail.RetailRecipeIndex;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;
import com.aionemu.gameserver.questEngine.tablelane.NativeQuestTableLoader.CombineTaskRow;

/**
 * CombineTask 原生表驱动家族门禁（计划 §7 P6 切换批）。
 * <p>
 * 断言面全部来自真端表行 + 真端 {@code quest.xml} + 生产静态数据（物品名索引 / 配方表），不合成语义：
 * <ol>
 *   <li>574 行全量装载与列填充率冻结（双接取/交付 NPC、技能/技能点、配方符号、产物、分量 1..2 槽）；</li>
 *   <li>注册/路由分解（owns 574 = routes 574，零 fail-closed；未退役行不得进路由集）；</li>
 *   <li>接取（31 问询 → 客户端入口页；1002/20000 建档 + 发分量 + 学配方）→ 交付（产物门 → 回收剩余分量
 *       → REWARD 页 5；缺产物 → 真端回退页 1779）→ 领奖（8..23/108/110..124 → 真端奖励面结算 → 扣产物、
 *       忘配方 → 领奖收尾回选择对话页 10，真端 npc-complete finish=SELECTION_DIALOG）；</li>
 *   <li>放弃（族级动作 = 忘配方）与原生放弃面接线（owner 判定 + 真端元数据轴）；</li>
 *   <li>失败面 fail-closed：非本族 NPC / 未路由行 / 越界按钮一律不接管，交付门未过时状态与背包零变更。</li>
 * </ol>
 * <p>
 * CombineTask native family gate: every assertion is sourced from the retail row, the retail
 * {@code quest.xml} or production static data.
 */
class SimpleCombineTaskNativeFamilyGateTest {

	/** 代表行（真端 5000 = weaponsmith / r_ws_q5000；天族接取 NPC Anteros）。 / The representative row. */
	private static final int COMBINE_QUEST = 5000;
	/** 真端 Quest_CombineTask.xml 行数。 / The frozen retail row count. */
	private static final int FROZEN_ROWS = 574;

	private static NativeQuestTableLoader loader;
	private static SimpleCombineTaskHandler handler;

	@BeforeAll
	static void setUp() {
		loader = NativeQuestTableLoader.instance();
		handler = SimpleCombineTaskHandler.instance();
	}

	// ---------------------------------------------------------------- 装载面

	@Test
	void loadsAll574RetailRowsWithTheCombineShape() {
		assertEquals(FROZEN_ROWS, loader.combineSize(), "真端 quest_combinetasks 全量行");
		List<CombineTaskRow> rows = List.copyOf(loader.combineRows());
		assertTrue(rows.stream().allMatch(row -> row.taskNpcNames().size() == 2),
			"task_npc 必须 100% 两名（天/魔各一）");
		assertTrue(rows.stream().allMatch(row -> !row.combineSkill().isBlank() && !row.recipeName().isBlank()
				&& !row.product().isBlank()), "combineskill / recipe_name / product 必须 100%");
		assertEquals(FROZEN_ROWS, rows.stream().filter(row -> declared(row.components(), 1)).count(),
			"give_component1 覆盖 574 行");
		assertEquals(152, rows.stream().filter(row -> declared(row.components(), 2)).count(),
			"give_component2 覆盖 152 行（真端第 3 槽起恒空）");
	}

	@Test
	void routingSplitIsFrozenToTheRetiredRows() {
		assertEquals(FROZEN_ROWS, handler.ownedQuestIds().size(), "注册集 = 真端表全量行");
		assertEquals(FROZEN_ROWS, handler.routedQuestIds().size(), "路由集 = 全部退役且可解的行（P6 步骤 1 已冻结）");
		assertTrue(handler.unroutableQuestIds().isEmpty(), "本族不得有 fail-closed 行");
		assertTrue(handler.unresolvedNames().isEmpty(), "不得有未解析 NPC 名");
		assertTrue(handler.unresolvedItemSymbols().isEmpty(), "不得有未解析物品符号");
		assertTrue(handler.unresolvedRecipeSymbols().isEmpty(), "不得有未解析配方符号");
		for (CombineTaskRow row : loader.combineRows()) {
			if (!RetiredQuestIds.contains(row.questId())) {
				assertFalse(handler.routes(row.questId()), "未退役行不得由 native 车道路由: " + row.questId());
			}
			assertEquals(handler.owns(row.questId()), handler.routes(row.questId()),
				"本族 owns ≡ routes（零 fail-closed 残余）: " + row.questId());
		}
	}

	/** 逐行对拍：handler 的每张面都必须逐元素等于真端表行。 / Per-row element-wise alignment. */
	@Test
	void handlerSurfacesFollowTheRetailRow() throws IOException {
		RetailItemNameIndex items = RetailItemNameIndex.loadItemTemplates();
		RetailRecipeIndex recipes;
		try (java.io.InputStream input = getClass().getResourceAsStream(
				"/aion/data/static_data/recipe/recipe_templates.xml")) {
			assertNotNull(input, "missing recipe_templates.xml");
			recipes = RetailRecipeIndex.build(List.of(input));
		}
		List<String> problems = new ArrayList<>();
		for (CombineTaskRow row : loader.combineRows()) {
			int questId = row.questId();
			List<Integer> expectedNpcs = new ArrayList<>();
			for (String name : row.taskNpcNames()) {
				var match = NativeNpcNameResolver.instance().resolve(name);
				if (match.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
					problems.add(questId + " NPC 名非唯一：" + name);
					continue;
				}
				if (!expectedNpcs.contains(match.npcIds().getFirst())) {
					expectedNpcs.add(match.npcIds().getFirst());
				}
			}
			if (!expectedNpcs.equals(handler.taskNpcs(questId))) {
				problems.add(questId + " NPC 集漂移：" + expectedNpcs + " -> " + handler.taskNpcs(questId));
			}
			ItemStack product = handler.product(questId);
			if (product == null || product.itemId() != symbol(row.product(), items)
					|| product.count() != cellCount(row.product())) {
				problems.add(questId + " 产物漂移：" + row.product() + " -> " + product);
			}
			List<ItemStack> expectedComponents = new ArrayList<>();
			for (String cell : row.components()) {
				if (cell == null || cell.isBlank()) {
					continue;
				}
				expectedComponents.add(new ItemStack(symbol(cell, items), cellCount(cell)));
			}
			if (!expectedComponents.equals(handler.components(questId))) {
				problems.add(questId + " 分量漂移：" + expectedComponents + " -> " + handler.components(questId));
			}
			Integer expectedRecipe = product == null ? null
				: recipes.resolveUnique(
					RetailQuestMetadataCompiler.combineSkillId(row.combineSkill()), product.itemId()).orElse(null);
			if (expectedRecipe == null || !expectedRecipe.equals(handler.recipeId(questId))) {
				problems.add(questId + " 配方漂移：" + expectedRecipe + " -> " + handler.recipeId(questId));
			}
		}
		assertTrue(problems.isEmpty(), () -> "逐行对拍失败 " + problems.size() + " 行：\n"
			+ String.join("\n", problems.subList(0, Math.min(20, problems.size()))));
	}

	// ---------------------------------------------------------------- 行为面

	@Test
	void acceptOpensTheClientEntryPageGrantsComponentsAndLearnsTheRecipe() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		RecordingRecipes recipes = new RecordingRecipes();
		SimpleCombineTaskHandler local = handler(inventory, recipes, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 31)),
			"接取 NPC 的问询必须下发客户端声明的接取入口页");
		NativeTalkFixture.assertOnlyDialogPage(player, NativeTalkFixture.clientEntryPage(COMBINE_QUEST));
		assertEquals(QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(),
			NativeTalkFixture.clientEntryPage(COMBINE_QUEST),
			"本族 574 行未登记任务页 ⇒ 契约回落真端接取窗页 4");

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 1002)),
			"1002 提交必须由 native 接取口服务");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCombineTaskHandler.PAGE_ACCEPTED);
		QuestState state = player.getQuestStateList().getQuestState(COMBINE_QUEST);
		assertNotNull(state, "接取后必须建档");
		assertEquals(QuestStatus.START, state.getStatus());
		List<String> expectedGives = new ArrayList<>();
		for (ItemStack component : local.components(COMBINE_QUEST)) {
			expectedGives.add("give:" + component.itemId() + ":" + component.count());
		}
		assertEquals(expectedGives, inventory.calls(), "接取提交即按表序发真端 give_component1..8");
		assertEquals(List.of("learn:" + local.recipeId(COMBINE_QUEST)), recipes.calls(),
			"接取提交即学 (combineskill, product) 反查出的配方");
		assertTrue(recipes.holds(player, local.recipeId(COMBINE_QUEST)), "接取后配方必须已掌握");
	}

	@Test
	void acceptViaTheWindowCommitAlsoGrantsAndClosesTheWindow() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		RecordingRecipes recipes = new RecordingRecipes();
		SimpleCombineTaskHandler local = handler(inventory, recipes, NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 20000)),
			"20000 提交必须由 native 接取口服务");
		NativeTalkFixture.assertOnlyDialogPage(player, 0);
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(COMBINE_QUEST).getStatus());
		assertFalse(inventory.calls().isEmpty(), "20000 与 1002 同形：同样发放分量");
		assertEquals(List.of("learn:" + local.recipeId(COMBINE_QUEST)), recipes.calls());
	}

	@Test
	void handInRecyclesTheLeftoverComponentsAndFlipsToReward() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCombineTaskHandler local = handler(inventory, new RecordingRecipes(),
			NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, COMBINE_QUEST);
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();
		ItemStack product = local.product(COMBINE_QUEST);
		inventory.hold(product.itemId(), product.count());
		List<String> expectedRemovals = new ArrayList<>();
		List<String> expectedHold = new ArrayList<>();
		for (ItemStack component : local.components(COMBINE_QUEST)) {
			// 剩余分量 = 真端 RemoveItem(component, ALL)：按当前持有量清空（刻意多持 3 件）。
			// Leftovers follow the retail RemoveItem(x, ALL) semantics, so the fixture over-holds by three.
			inventory.hold(component.itemId(), component.count() + 3L);
			expectedHold.add(component.itemId() + "=" + (component.count() + 3));
			expectedRemovals.add("remove:" + component.itemId() + ":" + (component.count() + 3));
		}

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 1009)),
			"交付动作 1009 必须由 native 交付口服务（持有产物）");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCombineTaskHandler.PAGE_REWARD_WINDOW);
		assertEquals(QuestStatus.REWARD,
			player.getQuestStateList().getQuestState(COMBINE_QUEST).getStatus(), "交付成功即翻 REWARD");
		assertEquals(expectedRemovals, inventory.calls(), "交付成功即回收剩余分量（真端 RemoveItem ALL）: "
			+ expectedHold);
	}

	@Test
	void handInWithoutTheProductAnswersTheRetailFallbackPage() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		SimpleCombineTaskHandler local = handler(inventory, new RecordingRecipes(),
			NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, COMBINE_QUEST);
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 1009)),
			"缺产物时交付动作仍须被服务（回退页而不是静默）");
		NativeTalkFixture.assertOnlyDialogPage(player, SimpleCombineTaskHandler.PAGE_HANDIN_BLOCKED);
		assertEquals(SimpleCombineTaskHandler.PAGE_HANDIN_BLOCKED, QuestDialogPage.SELECT3_2.id(),
			"回退页 = 真端 SELECT3_2(1779)");
		assertEquals(QuestStatus.START,
			player.getQuestStateList().getQuestState(COMBINE_QUEST).getStatus(), "缺产物不得翻态");
		assertTrue(inventory.calls().isEmpty(), "缺产物不得扣任何分量");
	}

	@Test
	void claimSettlesThroughTheRetailDerivedTemplateThenRemovesProductAndRecipe() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		RecordingRecipes recipes = new RecordingRecipes();
		List<int[]> claims = new ArrayList<>();
		SimpleCombineTaskHandler local = handler(inventory, recipes,
			NativeReportRewardFlow.forTest(SimpleCombineTaskNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					claims.add(new int[] {env.getQuestId(), tier});
					return template != null;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, COMBINE_QUEST, QuestStatus.REWARD, 0);
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();
		ItemStack product = local.product(COMBINE_QUEST);
		int recipeId = local.recipeId(COMBINE_QUEST);
		inventory.hold(product.itemId(), product.count() + 1L);
		recipes.known.add(recipeId);

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 8)),
			"奖励窗按钮 8 必须由 native 领奖段服务");
		// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG：回选择对话页（页 10，questId=0）。
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(1, claims.size(), "结算体必须被调用一次");
		assertEquals(COMBINE_QUEST, claims.getFirst()[0]);
		assertEquals(0, claims.getFirst()[1], "单槽行固定首档");
		assertEquals(List.of("remove:" + product.itemId() + ":" + (product.count() + 1)), inventory.calls(),
			"真端完成流的条件回收段：扣产物（ALL）");
		assertEquals(List.of("forget:" + recipeId), recipes.calls(), "完成即忘配方");
		assertFalse(recipes.holds(player, recipeId), "完成后配方必须已忘");

		Player other = NativeTalkFixture.player();
		NativeTalkFixture.add(other, COMBINE_QUEST, QuestStatus.REWARD, 0);
		assertFalse(local.onDialog(NativeTalkFixture.dialog(other, npcId, COMBINE_QUEST, 9999)),
			"非奖励窗动作不得被领奖段消费");
	}

	/**
	 * 无目标领奖（真端 {@code QuestDialog} 无主键协议；任务窗/实时奖励槽确认包不带 NPC 上下文，
	 * 引擎以 npcId=0 进入）：按 questId 结算 + 关窗，并同样走本族完成流的条件回收段（扣产物 + 忘配方）。
	 * <p>
	 * The targetless claim including this family's completion-flow recycling (product removal and
	 * recipe forgetting), with the close-dialog tail.
	 */
	@Test
	void targetlessClaimSettlesAndRunsTheCompletionRecycling() {
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		RecordingRecipes recipes = new RecordingRecipes();
		List<int[]> claims = new ArrayList<>();
		SimpleCombineTaskHandler local = handler(inventory, recipes,
			NativeReportRewardFlow.forTest(SimpleCombineTaskNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					claims.add(new int[] {env.getQuestId(), tier});
					return template != null;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, COMBINE_QUEST, QuestStatus.REWARD, 0);
		ItemStack product = local.product(COMBINE_QUEST);
		int recipeId = local.recipeId(COMBINE_QUEST);
		inventory.hold(product.itemId(), product.count() + 1L);
		recipes.known.add(recipeId);

		// 实时奖励槽 110：结算 + 条件回收（扣产物 + 忘配方）+ 关窗。
		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(new QuestEnv(null, player, COMBINE_QUEST, 110)),
			"无目标实时奖励确认必须被领奖段服务");
		NativeTalkFixture.assertCloseDialog(player);
		assertEquals(1, claims.size());
		assertEquals(List.of("remove:" + product.itemId() + ":" + (product.count() + 1)), inventory.calls(),
			"无目标领奖同样走完成流条件回收段：扣产物（ALL）");
		assertEquals(List.of("forget:" + recipeId), recipes.calls(), "无目标领奖同样忘配方");
	}

	/**
	 * 23 = SELECTED_QUEST_NOREWARD 是无选择确认，不占选项下标（与其余六族同口径；同型修复先例：
	 * 1107 领奖循环，99684c70d 覆盖两族，本族为当时的漏网族）：旧区间（8..23）曾把 23 映射成
	 * 下标 15 ⇒ 按钮面 fail-closed、奖励窗无法完成。
	 * <p>
	 * The no-selection confirm indexes no option on the CombineTask lane either.
	 */
	@Test
	void noRewardConfirmAction23MapsToIndexZeroNotFifteen() {
		List<int[]> claims = new ArrayList<>();
		NativeTalkFixture.RecordingInventory inventory = new NativeTalkFixture.RecordingInventory();
		RecordingRecipes recipes = new RecordingRecipes();
		SimpleCombineTaskHandler local = handler(inventory, recipes,
			NativeReportRewardFlow.forTest(SimpleCombineTaskNativeFamilyGateTest::metadata,
				(env, tier, template) -> {
					claims.add(new int[] {env.getQuestId(), tier});
					return template != null;
				}));
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.add(player, COMBINE_QUEST, QuestStatus.REWARD, 0);
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();
		ItemStack product = local.product(COMBINE_QUEST);
		inventory.hold(product.itemId(), product.count() + 1L);
		recipes.known.add(local.recipeId(COMBINE_QUEST));

		NativeTalkFixture.clearPackets(player);
		assertTrue(local.onDialog(NativeTalkFixture.dialog(player, npcId, COMBINE_QUEST, 23)),
			"23 无选择确认必须被领奖段服务");
		NativeTalkFixture.assertOnlyDialogPage(player, QuestDialogPage.SELECT_QUEST.id());
		assertEquals(1, claims.size(), "23 必须触发结算");
		assertEquals(0, claims.getFirst()[1], "23 的结算档位必须归 0（NOREWARD 不占下标）");
	}

	@Test
	void abandonForgetsTheRecipeThroughTheFamilyAction() {
		RecordingRecipes recipes = new RecordingRecipes();
		SimpleCombineTaskHandler local = handler(NativeTalkFixture.RecordingInventory.EMPTY, recipes,
			NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		NativeTalkFixture.start(player, COMBINE_QUEST);
		int recipeId = local.recipeId(COMBINE_QUEST);
		recipes.known.add(recipeId);

		assertTrue(local.onAbandon(player, COMBINE_QUEST), "已路由行的放弃族级动作必须被服务");
		assertEquals(List.of("forget:" + recipeId), recipes.calls(), "放弃族级动作 = 忘配方");
		assertFalse(local.onAbandon(player, 999999), "非本族行不得被本族放弃口接管");
	}

	/**
	 * 原生放弃面接线：owner 判定 ∪ 真端 {@code quest.xml} 元数据轴（{@code cannot_giveup}）∪
	 * {@code QuestService} 的原生分支。前两面在引擎上实测，第三面锁生产源（放弃需要在线玩家控制器，
	 * 单测栈无该面——由族门锁源码而不是伪造）。
	 * The native abandon wiring: owner detection plus the retail metadata axis are exercised on the
	 * engine; the {@code QuestService} branch is locked at the source level (the live path needs a
	 * player controller the unit stack does not have).
	 */
	@Test
	void nativeAbandonFaceIsWiredToTheRetailMetadataAxis() throws IOException {
		QuestEngine engine = new QuestEngine();
		assertTrue(engine.isNativeOwner(COMBINE_QUEST), "CombineTask 行必须被识别为原生 owner");
		assertTrue(engine.hasNativeAbandonRoute(COMBINE_QUEST), "原生行必须有放弃面");
		QuestMetadata metadata = engine.nativeMetadata(COMBINE_QUEST).orElse(null);
		assertNotNull(metadata, "原生放弃的元数据轴必须取到真端 quest.xml 行（不干净即 empty ⇒ fail-closed）");
		assertFalse(metadata.cannotGiveup(), "真端 cannot_giveup=0 ⇒ 可放弃");
		assertEquals(localComponentCount(), metadata.questWorkItems().size(),
			"共用清理段的工作物品面 = 真端 quest_work_item*（与表列 give_component* 同形）");

		String service = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/services/QuestService.java"));
		assertTrue(service.contains("questEngine.isNativeOwner(questId)"), "QuestService 必须按 owner 分流");
		assertTrue(service.contains("questEngine.nativeMetadata(questId)"), "QuestService 必须取真端元数据轴");
		assertTrue(service.contains("questEngine.onNativeAbandon(player, questId)"), "QuestService 必须派发族级放弃动作");
	}

	@Test
	void foreignNpcsAndUnroutedRowsAreNeverServed() {
		SimpleCombineTaskHandler local = handler(NativeTalkFixture.RecordingInventory.EMPTY,
			new RecordingRecipes(), NativeReportRewardFlow.instance());
		Player player = NativeTalkFixture.player();
		int npcId = local.taskNpcs(COMBINE_QUEST).getFirst();

		assertFalse(local.onDialog(NativeTalkFixture.dialog(player, npcId + 1, COMBINE_QUEST, 31)),
			"非本族声明的 NPC 不得接管");
		assertFalse(local.onDialog(NativeTalkFixture.dialog(player, npcId, 999999, 31)),
			"未路由的行不得接管");
		assertFalse(local.owns(999999), "非本族行不在注册集");
	}

	// ---------------------------------------------------------------- 夹具

	private int localComponentCount() {
		return handler.components(COMBINE_QUEST).size();
	}

	private static Optional<RetailQuestMetadataCompiler.Outcome> metadata(int questId) {
		try {
			return RetailQuestDriver.ensureLoaded().retailMetadataOf(questId);
		} catch (java.io.IOException e) {
			return Optional.empty();
		}
	}

	private static SimpleCombineTaskHandler handler(NativeInventoryPort inventory, NativeRecipePort recipes,
			NativeReportRewardFlow rewardFlow) {
		try {
			return new SimpleCombineTaskHandler(NativeQuestTableLoader.instance(),
				NativeNpcNameResolver.instance(), RetailItemNameIndex.loadItemTemplates(), recipeIndex(),
				inventory, recipes, rewardFlow);
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static RetailRecipeIndex recipeIndex() throws IOException {
		try (java.io.InputStream input = SimpleCombineTaskNativeFamilyGateTest.class.getClassLoader()
				.getResourceAsStream("aion/data/static_data/recipe/recipe_templates.xml")) {
			if (input == null) {
				throw new IllegalStateException("missing recipe_templates.xml");
			}
			return RetailRecipeIndex.build(List.of(input));
		}
	}

	/** 记录式假配方端口。 / A recording fake recipe port. */
	private static final class RecordingRecipes implements NativeRecipePort {
		private final Set<Integer> known = new LinkedHashSet<>();
		private final List<String> calls = new ArrayList<>();

		@Override
		public boolean holds(Player player, int recipeId) {
			return known.contains(recipeId);
		}

		@Override
		public boolean learn(Player player, int recipeId) {
			calls.add("learn:" + recipeId);
			known.add(recipeId);
			return true;
		}

		@Override
		public boolean forget(Player player, int recipeId) {
			calls.add("forget:" + recipeId);
			known.remove(recipeId);
			return true;
		}

		private List<String> calls() {
			return List.copyOf(calls);
		}
	}

	private static boolean declared(List<String> cells, int step) {
		return cells.size() >= step && cells.get(step - 1) != null && !cells.get(step - 1).isBlank();
	}

	/** 单元 = 「符号 [数量]」；符号走两通道（原名 → {@code ITEM_} 前缀）。 / The retail two-channel symbol rule. */
	private static Integer symbol(String cell, RetailItemNameIndex items) {
		String name = cell.trim().split("\\s+")[0].toLowerCase(java.util.Locale.ROOT);
		Integer direct = items.resolve(name);
		if (direct != null) {
			return direct;
		}
		return name.startsWith("item_") ? items.resolve(name.substring("item_".length())) : null;
	}

	private static int cellCount(String cell) {
		String[] parts = cell.trim().split("\\s+");
		return parts.length < 2 ? 1 : Integer.parseInt(parts[1]);
	}
}
