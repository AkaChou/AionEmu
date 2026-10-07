package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.retail.RetailRecipeIndex;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 CombineTask 原生任务处理器（计划 §7 P6 切换批）。
 * <p>
 * 完全由真端表 {@link NativeQuestTableLoader#combineRows()}、真端 {@code quest.xml} 与共用结算口驱动，
 * 绝不生成 IR 节点、绝不回退退役编译器。真端 helper（{@code f731:1355 FUN_180caac10} 一系）的参数序
 * 逐段落在这里：
 * <ol>
 *   <li><b>接取</b>：接取 NPC 的 31/26 问询 → 客户端任务页声明的接取入口页（本族 574 行未登记页 ⇒
 *       契约回落页 4）；1002/20000 建档 START + 按表序发 {@code give_component1..8} + 学配方
 *       （真端表只有配方符号，配方 id 由 {@code (combineskill, product)} 在生产配方表唯一反查）；
 *       1003 → 页 1004、1004/20001 → 收窗；</li>
 *   <li><b>交付</b>：交付 NPC 的 {@code SELECT_QUEST_REWARD}(1009)：持有产物（表列 {@code product} 的
 *       符号与数量）→ 回收剩余分量（{@code RemoveItem(component, ALL)}）+ 翻 REWARD + 开奖励窗页 5；
 *       缺产物 → 真端回退页 {@code SELECT3_2}(1779)，状态与背包零变更；</li>
 *   <li><b>领奖</b>：{@link NativeReportRewardFlow}（真端 reward 列 → 共用结算体）→ 完成后再走真端
 *       条件回收段（扣产物 + 忘配方）→ 完成页 1008；</li>
 *   <li><b>放弃</b>：{@link #onAbandon(Player, int)} 只做族级动作（忘配方）；共用清理段（状态复位 +
 *       真端 {@code quest_work_item*} 工作物品回收）由 {@code QuestService} 的原生放弃路径承担。</li>
 * </ol>
 * 缺行/名字多义/符号未解/配方非唯一/元数据不干净/未退役的行一律不路由（fail-closed）。
 * <p>
 * Retail CombineTask native handler (plan §7 P6). Accept grants the table's components and learns the
 * recipe resolved from {@code (combineskill, product)}; hand-in requires the product, recycles the
 * leftover components and flips to REWARD; the reward window settles through
 * {@link NativeReportRewardFlow} and then removes the product and the recipe (the retail completion
 * helper sets the quest state first and runs its removal loop afterwards); abandoning only forgets the
 * recipe, with the shared cleanup owned by {@code QuestService}. Unresolved names/symbols, ambiguous
 * recipes, unclean metadata and non-retired rows are never routed.
 */
public final class SimpleCombineTaskHandler {

	/** 进行中页（未满足交付门）。 / In-progress page. */
	public static final int PAGE_IN_PROGRESS = QuestDialogPage.SELECT_QUEST.id();
	/** 奖励窗页。 / The reward window page. */
	public static final int PAGE_REWARD_WINDOW = QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id();
	/** 接取确认页。 / The accept confirmation page. */
	public static final int PAGE_ACCEPTED = QuestDialogPage.QUEST_ACCEPT_1.id();
	/** 拒绝页。 / The refuse page. */
	public static final int PAGE_REFUSED = QuestDialogPage.QUEST_REFUSE_1.id();
	/** 交付门未过时的真端回退页（{@code SELECT3_2}）。 / The retail fallback page when the product is missing. */
	public static final int PAGE_HANDIN_BLOCKED = QuestDialogPage.SELECT3_2.id();

	private static volatile SimpleCombineTaskHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final NativeNpcNameResolver nameResolver;
	private final RetailItemNameIndex itemIndex;
	private final RetailRecipeIndex recipeIndex;
	private final NativeInventoryPort inventory;
	private final NativeRecipePort recipes;
	private final NativeReportRewardFlow rewardFlow;
	private final QuestDialogContract dialogContract;

	/** 任务 ID → 接取/交付 NPC 集合（真端 {@code task_npc}，天/魔各一）。 / Quest id → accept/hand-in npcs. */
	private final Map<Integer, List<Integer>> taskNpcsByQuestId;
	/** 任务 ID → 产物（真端 {@code product} 单元）。 / Quest id → the product stack. */
	private final Map<Integer, ItemStack> productByQuestId;
	/** 任务 ID → 接取发放的分量（真端 {@code give_component1..8}，按表序）。 / Quest id → the component grants. */
	private final Map<Integer, List<ItemStack>> componentsByQuestId;
	/** 任务 ID → 配方 id（{@code (combineskill, product)} 唯一反查）。 / Quest id → the resolved recipe id. */
	private final Map<Integer, Integer> recipeIdByQuestId;
	/** 表行全量（注册集）。 / Every table row (the registration set). */
	private final Set<Integer> ownedQuestIds;
	/** 路由集 = 退役 ∧ 非 XML-only ∧ 全部声明面可解。 / The routing set. */
	private final Set<Integer> routedQuestIds;
	/** 装载但不可路由的行（fail-closed 证据面）。 / Loaded but unroutable rows. */
	private final Set<Integer> unroutableQuestIds;
	/** 唯一解析失败的 NPC 名（证据面）。 / NPC names that did not resolve uniquely. */
	private final Set<String> unresolvedNames;
	/** 未解析的物品符号（证据面）。 / Unresolved item symbols. */
	private final Set<String> unresolvedItemSymbols;
	/** 未解析的合成技能 / 配方符号（证据面）。 / Unresolved combine-skill or recipe symbols. */
	private final Set<String> unresolvedRecipeSymbols;

	/** 生产实例构造（真端表 + 生产背包/配方/结算口）。 / The production wiring. */
	private SimpleCombineTaskHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, RetailRecipeIndex recipeIndex, NativeInventoryPort inventory) {
		this(tableLoader, nameResolver, itemIndex, recipeIndex, inventory, NativeRecipePort.live(),
			NativeReportRewardFlow.instance());
	}

	/**
	 * 门禁用接缝（注入假背包/配方/结算口；数据源与生产一致）。
	 * Gate seam: the data source stays production, only the inventory, recipe and settlement sinks are injected.
	 */
	SimpleCombineTaskHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, RetailRecipeIndex recipeIndex, NativeInventoryPort inventory,
			NativeRecipePort recipes, NativeReportRewardFlow rewardFlow) {
		this.tableLoader = tableLoader;
		this.nameResolver = nameResolver;
		this.itemIndex = itemIndex;
		this.recipeIndex = recipeIndex;
		this.inventory = inventory;
		this.recipes = recipes;
		this.rewardFlow = rewardFlow;
		this.dialogContract = QuestDialogContract.loadDefault();

		Set<Integer> xmlOwnedIds = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		Map<Integer, List<Integer>> taskNpcs = new LinkedHashMap<>();
		Map<Integer, ItemStack> products = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> components = new LinkedHashMap<>();
		Map<Integer, Integer> recipeIds = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Set<Integer> unroutable = new TreeSet<>();
		Set<String> unresolved = new TreeSet<>();
		Set<String> unresolvedItems = new TreeSet<>();
		Set<String> unresolvedRecipes = new TreeSet<>();

		for (NativeQuestTableLoader.CombineTaskRow row : tableLoader.combineRows()) {
			int questId = row.questId();
			owned.add(questId);
			boolean resolvable = true;

			// 接取/交付 NPC（真端必填列，两名）：非唯一解析即不可路由。 / Required accept npcs.
			Set<Integer> npcIds = new LinkedHashSet<>();
			for (String name : row.taskNpcNames()) {
				NativeNpcNameResolver.Match match = nameResolver.resolve(name);
				if (match.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
					unresolved.add(name);
					resolvable = false;
					continue;
				}
				npcIds.add(match.npcIds().getFirst());
			}
			if (npcIds.isEmpty()) {
				resolvable = false;
			} else {
				taskNpcs.put(questId, List.copyOf(npcIds));
			}

			// 产物（真端 product 单元，全族单槽）。 / The single product slot.
			ItemStack product = parseSymbol(row.product(), questId, unresolvedItems);
			if (product == null) {
				resolvable = false;
			} else {
				products.put(questId, product);
			}

			// 分量（真端 give_component1..8，按位置保留；全缺即不可路由）。 / The declared components.
			List<ItemStack> componentStacks = new ArrayList<>();
			for (String cell : row.components()) {
				if (cell == null || cell.isBlank()) {
					continue;
				}
				ItemStack component = parseSymbol(cell, questId, unresolvedItems);
				if (component == null) {
					resolvable = false;
					continue;
				}
				componentStacks.add(component);
			}
			if (componentStacks.isEmpty()) {
				resolvable = false;
			} else {
				components.put(questId, List.copyOf(componentStacks));
			}

			// 配方：技能符号 → id，再按 (skill, product) 在生产配方表唯一反查。
			// Recipe: the skill symbol resolves to an id, then (skill, product) must hit exactly one template.
			Integer skillId = RetailQuestMetadataCompiler.combineSkillId(row.combineSkill());
			if (skillId == null) {
				unresolvedRecipes.add(String.valueOf(row.combineSkill()));
				resolvable = false;
			} else if (product != null) {
				Integer recipeId = recipeIndex.resolveUnique(skillId, product.itemId()).orElse(null);
				if (recipeId == null) {
					unresolvedRecipes.add(String.valueOf(row.recipeName()));
					resolvable = false;
				} else {
					recipeIds.put(questId, recipeId);
				}
			}

			boolean retired = RetiredQuestIds.contains(questId);
			if (!resolvable || !retired || xmlOwnedIds.contains(questId) || !metadataClean(questId)) {
				unroutable.add(questId);
			} else {
				routed.add(questId);
			}
		}

		this.taskNpcsByQuestId = Collections.unmodifiableMap(taskNpcs);
		this.productByQuestId = Collections.unmodifiableMap(products);
		this.componentsByQuestId = Collections.unmodifiableMap(components);
		this.recipeIdByQuestId = Collections.unmodifiableMap(recipeIds);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.unroutableQuestIds = Collections.unmodifiableSet(unroutable);
		this.unresolvedNames = Collections.unmodifiableSet(unresolved);
		this.unresolvedItemSymbols = Collections.unmodifiableSet(unresolvedItems);
		this.unresolvedRecipeSymbols = Collections.unmodifiableSet(unresolvedRecipes);
	}

	/** 生产单例（真端表 + 生产端口）。 / The production singleton. */
	public static SimpleCombineTaskHandler instance() {
		SimpleCombineTaskHandler local = instance;
		if (local == null) {
			synchronized (SimpleCombineTaskHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleCombineTaskHandler(NativeQuestTableLoader.instance(),
						NativeNpcNameResolver.instance(), retailItemIndex(), retailRecipeIndex(),
						NativeInventoryPort.live());
					instance = local;
				}
			}
		}
		return local;
	}

	private static RetailItemNameIndex retailItemIndex() {
		try {
			return RetailItemNameIndex.loadItemTemplates();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("NATIVE_ITEM_INDEX_FAILED: " + e.getMessage(), e);
		}
	}

	private static RetailRecipeIndex retailRecipeIndex() {
		try (java.io.InputStream input = SimpleCombineTaskHandler.class.getClassLoader()
				.getResourceAsStream("aion/data/static_data/recipe/recipe_templates.xml")) {
			if (input == null) {
				throw new IllegalStateException("NATIVE_RECIPE_INDEX_FAILED: missing recipe_templates.xml");
			}
			return RetailRecipeIndex.build(List.of(input));
		} catch (java.io.IOException e) {
			throw new IllegalStateException("NATIVE_RECIPE_INDEX_FAILED: " + e.getMessage(), e);
		}
	}

	/** 是否拥有该任务（注册集 = 真端表全量行）。 / Checks whether the row is in the registration set. */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/** 是否由本车道路由（退役 ∧ 非 XML-only ∧ 全部声明面可解）。 / Whether this lane routes the row. */
	public boolean routes(int questId) {
		return routedQuestIds.contains(questId);
	}

	public Set<Integer> ownedQuestIds() {
		return ownedQuestIds;
	}

	public Set<Integer> routedQuestIds() {
		return routedQuestIds;
	}

	/** 已装载但当前不可路由的行。 / Loaded but unroutable rows. */
	public Set<Integer> unroutableQuestIds() {
		return unroutableQuestIds;
	}

	/** 唯一解析失败的 NPC 名（证据面）。 / NPC names that did not resolve uniquely. */
	public Set<String> unresolvedNames() {
		return unresolvedNames;
	}

	/** 未解析的物品符号（证据面）。 / Unresolved item symbols. */
	public Set<String> unresolvedItemSymbols() {
		return unresolvedItemSymbols;
	}

	/** 未解析的合成技能 / 配方符号（证据面）。 / Unresolved combine-skill or recipe symbols. */
	public Set<String> unresolvedRecipeSymbols() {
		return unresolvedRecipeSymbols;
	}

	/** 接取/交付 NPC 集合（真端 {@code task_npc}）。 / The accept/hand-in npcs. */
	public List<Integer> taskNpcs(int questId) {
		return taskNpcsByQuestId.getOrDefault(questId, List.of());
	}

	/** 产物（真端 {@code product}；无则 null）。 / The product, or null. */
	public ItemStack product(int questId) {
		return productByQuestId.get(questId);
	}

	/** 接取发放的分量（真端 {@code give_component1..8}，按表序）。 / The accept component grants. */
	public List<ItemStack> components(int questId) {
		return componentsByQuestId.getOrDefault(questId, List.of());
	}

	/** 配方 id（{@code (combineskill, product)} 反查；无则 null）。 / The resolved recipe id, or null. */
	public Integer recipeId(int questId) {
		return recipeIdByQuestId.get(questId);
	}

	public NativeQuestTableLoader.CombineTaskRow requireRow(int questId) {
		return tableLoader.requireCombine(questId);
	}

	/** 配方端口（门禁用：确认生产接线走 live 端口）。 / The recipe port (the wiring gate reads it). */
	NativeRecipePort recipePort() {
		return recipes;
	}

	/**
	 * 启动期把本族全部 NPC 标记注册进任务引擎（真端 codegen 的静态注册表等价物）。
	 * Registers the family's npc marks at startup (the retail codegen registry equivalent).
	 */
	public void installInterest(QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, List<Integer>> entry : taskNpcsByQuestId.entrySet()) {
			if (!routedQuestIds.contains(entry.getKey())) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnQuestStart(entry.getKey());
				engine.registerQuestNpc(npcId).addOnTalkEvent(entry.getKey());
			}
		}
	}

	/**
	 * 处理接取 / 交付 / 领奖对话。
	 * Handles accept, hand-in and reward-window dialogs.
	 */
	public boolean onDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		if (!routes(questId)) {
			return false;
		}
		Npc npc = env.getVisibleObject() instanceof Npc target ? target : null;
		int npcId = npc != null ? npc.getNpcId() : 0;
		int objectId = npc != null ? npc.getObjectId() : 0;
		int dialogId = env.getDialogId();

		// 无目标领奖（真端 QuestDialog 无主键协议；任务窗/实时奖励槽的确认包不带 NPC 上下文）：
		// 按 questId 结算 + 关窗收尾，并走本族的完成流条件回收段（扣产物 + 忘配方，与 1009 交付同序）。
		// owner 门由上面的 routes(questId) 保证。
		// Targetless reward claim (the ownerless retail QuestDialog protocol), including this family's
		// completion-flow recycling (product removal and recipe forgetting), in the hand-in order.
		if (npcId == 0 && NativeTargetlessReward.claim(player, questId, dialogId, rewardFlow, () -> {
			recycleProduct(player, questId);
			forgetRecipe(player, questId);
		})) {
			return true;
		}

		if (!taskNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)) {
			return false;
		}
		QuestState state = player.getQuestStateList() == null ? null
			: player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state != null ? state.getStatus() : QuestStatus.NONE;

		if (state == null || status == QuestStatus.NONE
				|| (status == QuestStatus.COMPLETE && repeatable(questId))) {
			return onAcceptDialog(player, questId, objectId, dialogId);
		}
		if (status == QuestStatus.START) {
			return onHandInDialog(player, state, questId, objectId, dialogId);
		}
		if (status == QuestStatus.REWARD) {
			return onClaimDialog(env, player, questId, objectId, dialogId);
		}
		return false;
	}

	/**
	 * 接取对话（真端 NPC 接取形：31/26 → 接取入口页；1002 提交 + 页 1003；20000 提交 + 收窗；
	 * 1003 → 页 1004；1004/20001 → 收窗）。
	 * The retail npc accept shape (page-4 ask window with the 1002/20000 commits).
	 */
	private boolean onAcceptDialog(Player player, int questId, int objectId, int dialogId) {
		if (dialogId == 31 || dialogId == 26) {
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(objectId, dialogContract.retailEntryPage(questId), questId));
			return true;
		}
		if (dialogId == QuestDialogAction.ASK_QUEST_ACCEPT.id()) {
			// 真端页动作 1007（ASK_QUEST_ACCEPT → mgr+0x1a0）：打开接取窗页 4；客户端未声明即 fail-closed。
			// Retail page action 1007 (mgr+0x1a0) opens ask window page 4; undeclared pages fail closed.
			int askWindow = dialogContract.askWindowPage(questId);
			if (askWindow < 0) {
				return false;
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, askWindow, questId));
			return true;
		}
		if (dialogId == 1002 || dialogId == 20000) {
			if (!NativeQuestStartPort.instance().start(player, questId).started()) {
				return false;
			}
			grantComponents(player, questId);
			learnRecipe(player, questId);
			if (dialogId == 1002) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_ACCEPTED, questId));
			} else {
				DialogService.closeDialog(player, objectId);
			}
			return true;
		}
		if (dialogId == 1003) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_REFUSED, questId));
			return true;
		}
		if (dialogId == 1004 || dialogId == 20001) {
			DialogService.closeDialog(player, objectId);
			return true;
		}
		return false;
	}

	/**
	 * 交付对话：{@code SELECT_QUEST_REWARD}(1009) 持有产物 → 回收剩余分量 + 翻 REWARD + 奖励窗；
	 * 缺产物 → 真端回退页（状态与背包零变更）。其余对话动作给进行中页。
	 * Hand-in: holding the product recycles the leftover components and flips to REWARD; a missing
	 * product answers the retail fallback page with no state or inventory mutation.
	 */
	private boolean onHandInDialog(Player player, QuestState state, int questId, int objectId, int dialogId) {
		if (dialogId == QuestDialogAction.SELECT_QUEST_REWARD.id()) {
			ItemStack product = productByQuestId.get(questId);
			if (product != null && inventory.count(player, product.itemId()) >= product.count()) {
				recycleComponents(player, questId);
				state.setStatus(QuestStatus.REWARD);
				state.setPersistentState(PersistentState.UPDATE_REQUIRED);
				PacketSendUtility.sendPacket(player,
					new SM_QUEST_ACTION(questId, state.getStatus(), state.getQuestVars().getQuestVars()));
				PacketSendUtility.sendPacket(player,
					new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
				return true;
			}
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(objectId, PAGE_HANDIN_BLOCKED, questId));
			return true;
		}
		if (dialogId == 31 || dialogId == 26 || dialogId == -1) {
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS));
			return true;
		}
		return false;
	}

	/**
	 * 领奖段：奖励窗重开面 + 确认动作区间（8..23 / 108 / 110..124）。真端完成流先写任务成功、
	 * 再走条件回收（扣产物 + 忘配方），故本段按该序执行。
	 * Claim segment: window re-open actions plus the confirmation range. The retail completion helper
	 * sets the quest state first and runs its removal loop afterwards, so the order here is fixed.
	 */
	private boolean onClaimDialog(QuestEnv env, Player player, int questId, int objectId, int dialogId) {
		if (dialogId == 31 || dialogId == 26 || dialogId == QuestDialogAction.SELECT_QUEST_REWARD.id()
				|| dialogId == -1) {
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
			return true;
		}
		int first = QuestDialogAction.SELECTED_QUEST_REWARD1.id();
		int last = QuestDialogAction.SELECTED_QUEST_REWARD15.id();
		// 选项段只有 SELECTED_QUEST_REWARD1..15（8..22）；23 = SELECTED_QUEST_NOREWARD 是无选择确认，
		// 不占选项下标——发放由结算体按 dialogId==23 + extendedRewardIndex 决定。旧区间（8..23）会把
		// 23 映射成下标 15 ⇒ 按钮面 fail-closed、奖励窗无法完成；与其余六族同口径修正（同型修复先例：
		// 1107 领奖循环，99684c70d 覆盖两族；本族为当时的漏网族）。
		// Only SELECTED_QUEST_REWARD1..15 (8..22) index options; 23 is the no-selection confirm, whose
		// grant the settlement resolves via dialogId==23 + extendedRewardIndex. The old 8..23 band
		// mapped 23 to option index 15 and failed the reward window closed.
		if ((dialogId >= first && dialogId <= last)
				|| dialogId == QuestDialogAction.SELECTED_QUEST_NOREWARD.id()
				|| dialogId == 108 || (dialogId >= 110 && dialogId <= 124)) {
			int rewardIndex = dialogId >= first && dialogId <= last ? dialogId - first : 0;
			if (!rewardFlow.claim(env, rewardIndex).completed()) {
				return false;
			}
			recycleProduct(player, questId);
			forgetRecipe(player, questId);
			// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG（4801/4805）：回选择对话页
			// （页 10，questId=0；9/28 旧引擎基线「状态=5 → 页=10」）。
			// The claim tail follows the retail npc-complete finish=SELECTION_DIALOG: back to the
			// selection dialog (page 10, questId=0; the legacy 9/28 log baseline).
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(objectId, QuestDialogPage.SELECT_QUEST.id()));
			return true;
		}
		return false;
	}

	/**
	 * 放弃的族级动作：忘配方（真端放弃段清配方）。共用清理段（状态复位 + 工作物品回收）由
	 * {@code QuestService} 的原生放弃路径执行，本方法只做本族声明面。
	 * The family-level abandon action: forget the recipe. The shared cleanup (state reset plus work-item
	 * recycling) belongs to {@code QuestService}'s native abandon path.
	 */
	public boolean onAbandon(Player player, int questId) {
		if (player == null || !routedQuestIds.contains(questId)) {
			return false;
		}
		Integer recipeId = recipeIdByQuestId.get(questId);
		return recipeId != null && recipes.forget(player, recipeId);
	}

	/** 接取发放真端 {@code give_component1..8}（按表序，一条一个端口调用）。 / Grants the retail components in table order. */
	private void grantComponents(Player player, int questId) {
		for (ItemStack component : componentsByQuestId.getOrDefault(questId, List.of())) {
			inventory.give(player, component.itemId(), component.count());
		}
	}

	/** 交付段回收剩余分量（真端 {@code RemoveItem(component, ALL)}）：按当前持有量清空。 /
	 * Recycles the leftover components on hand-in exactly as the retail @{code RemoveItem(x, ALL)} does. */
	private void recycleComponents(Player player, int questId) {
		for (ItemStack component : componentsByQuestId.getOrDefault(questId, List.of())) {
			long held = inventory.count(player, component.itemId());
			if (held > 0) {
				inventory.remove(player, component.itemId(), (int) Math.min(held, Integer.MAX_VALUE));
			}
		}
	}

	/** 完成段扣产物（真端完成流的条件回收段）。 / Removes the product on completion (the retail removal loop). */
	private void recycleProduct(Player player, int questId) {
		ItemStack product = productByQuestId.get(questId);
		if (product == null) {
			return;
		}
		long held = inventory.count(player, product.itemId());
		if (held > 0) {
			inventory.remove(player, product.itemId(), (int) Math.min(held, Integer.MAX_VALUE));
		}
	}

	private void learnRecipe(Player player, int questId) {
		Integer recipeId = recipeIdByQuestId.get(questId);
		if (recipeId != null) {
			recipes.learn(player, recipeId);
		}
	}

	private void forgetRecipe(Player player, int questId) {
		Integer recipeId = recipeIdByQuestId.get(questId);
		if (recipeId != null) {
			recipes.forget(player, recipeId);
		}
	}

	/** 真端 {@code max_repeat_count} > 1 ⇒ 可重复（COMPLETE 态可再次开窗）。 / Repeatable per retail max_repeat_count. */
	private boolean repeatable(int questId) {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		Integer maxRepeat = row == null ? null : row.integer("max_repeat_count");
		return maxRepeat != null && maxRepeat > 1;
	}

	private ItemStack parseSymbol(String symbol, int questId, Set<String> unresolved) {
		return NativeItemSymbols.parse(symbol, questId, itemIndex, unresolved);
	}

	/** 真端 {@code quest.xml} 元数据可编译（缺行/未解即不可路由）。 / Retail metadata must compile cleanly. */
	private static boolean metadataClean(int questId) {
		try {
			return com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.ensureLoaded()
				.retailMetadataOf(questId)
				.map(RetailQuestMetadataCompiler.Outcome::clean)
				.orElse(false);
		} catch (java.io.IOException | RuntimeException e) {
			return false;
		}
	}
}
