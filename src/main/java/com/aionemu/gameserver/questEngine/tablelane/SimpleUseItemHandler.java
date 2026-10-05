package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailClientHandinNpcSets;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleUseItem 原生任务处理器（计划 §6.2 / §7 P5 切换批）。
 * <p>
 * 完全由真端表 {@link NativeQuestTableLoader#useItemRows()}、真端 {@code quest.xml} 与共用结算口驱动，
 * 绝不生成 IR 节点、绝不回退退役编译器。逐项证据：
 * <ul>
 *   <li><b>接取</b>：本族**没有** {@code acquired_npc_name} 列 ⇒ 接取 = 使用 {@code use_item_name}
 *       声明的道具（真端 codegen 的无主物品接取形 {@code canonicalItemAcceptFlow}：用物下发接取窗页 4，
 *       接受/拒绝/关窗为无主对话 1002/1003/1008）；</li>
 *   <li><b>中继链</b>：{@code talk_npc1..3} 严格表序推进；第 K 步按位置执行 {@code give_itemK}/
 *       {@code remove_itemK}（真端同源 codegen 的 cabb10 槽语义，实测声明这些列的行**都**声明第 K 个中继 NPC）；</li>
 *   <li><b>交付</b>：交付 NPC 处在中继链走完 + {@code item_check} 门通过时翻 REWARD 并下发奖励窗页 5；
 *       否则进行中页 10。{@code item_check} 是族表列（真端 record 的引擎开关）+ 门物品取真端
 *       {@code quest.xml} 的 {@code check_itemK_L} 列（同一行两条真端声明，缺一即 fail-closed）；</li>
 *   <li><b>领奖</b>：{@link NativeReportRewardFlow}（真端 reward 列 → 共用结算体），完成页 1008。</li>
 * </ul>
 * 缺行/名字多义/物品未解/门声明不可解/未退役的行一律不路由（fail-closed）。
 * <p>
 * Retail SimpleUseItem native handler (plan §6.2 / §7 P5). The family has no accept NPC column, so the
 * quest is accepted by using the declared item (the retail codegen's ownerless item-accept shape:
 * page 4 ask window plus targetless 1002/1003/1008 dialogs); the {@code talk_npc1..3} relay chain
 * advances in table order and executes the position-aligned step give/remove columns; the hand-in at
 * the reward npc flips REWARD once the relay chain is complete and the {@code item_check} gate (the
 * family-table engine flag plus its {@code quest.xml check_itemK_L} declaration) is satisfied, and
 * settlement goes through {@link NativeReportRewardFlow}. Unresolved names/items/gates and
 * non-retired rows are never routed — fail closed.
 */
public final class SimpleUseItemHandler {

	/** 接取问询窗页（真端物品接取形）。 / The item-accept ask window page. */
	public static final int PAGE_ASK_ACCEPT = QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id();
	/** 进行中（未满足交付门）页。 / In-progress page. */
	public static final int PAGE_IN_PROGRESS = QuestDialogPage.SELECT_QUEST.id();
	/** 奖励窗页。 / The reward window page. */
	public static final int PAGE_REWARD_WINDOW = QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id();
	/** 中继步页（真端 SELECT2/SELECT3/SELECT4；客户端未声明即不发页）。 / Relay step pages. */
	private static final int[] RELAY_STEP_PAGES = {1352, 1693, 2034};

	/** 中继步位段（真端守卫位 0x40000000 之下，避开该族未使用的低位）。 / The relay-step field. */
	private static final int RELAY_STEP_SHIFT = 16;
	private static final int RELAY_STEP_MASK = 0x3 << RELAY_STEP_SHIFT;

	private static final Pattern CHECK_ITEM = Pattern.compile("check_item(\\d+)_(\\d+)");

	private static volatile SimpleUseItemHandler instance;

	/** 交付 NPC 上的一条中继步。 / One relay step bound to a hand-in-adjacent npc. */
	private record RelayStep(int questId, int step, int npcId) {
	}

	private final NativeQuestTableLoader tableLoader;
	private final NativeNpcNameResolver nameResolver;
	private final RetailItemNameIndex itemIndex;
	private final NativeQuestXmlTable questXml;
	private final NativeInventoryPort inventory;
	private final NativeReportRewardFlow rewardFlow;
	/** 客户端任务页契约（中继步页的渲染面）。 / The client page contract (relay-step pages). */
	private final QuestDialogContract dialogContract;

	/** 任务 ID → 接取道具 id。 / Quest id → accept item id. */
	private final Map<Integer, Integer> useItemByQuestId;
	/** 接取道具 id → 该道具可开的任务（真端一行一物，同物多行时按 id 序稳定）。 / Item id → quests. */
	private final Map<Integer, List<Integer>> acceptQuestIdsByItemId;
	/** 任务 ID → 交付 NPC 集合（真端逻辑名 + 客户端交付集合展开）。 / Quest id → hand-in npcs. */
	private final Map<Integer, List<Integer>> rewardNpcsByQuestId;
	/** 任务 ID → 中继 NPC id（表序）。 / Quest id → relay npc ids in table order. */
	private final Map<Integer, List<Integer>> relayNpcsByQuestId;
	/** 中继 NPC id → 该 NPC 上的中继步。 / Relay npc id → its relay steps. */
	private final Map<Integer, List<RelayStep>> relaysByNpcId;
	/** 任务 ID → 第 K 步发放（下标 0..2，null = 无）。 / Quest id → step grants (index 0..2). */
	private final Map<Integer, List<ItemStack>> stepGiveByQuestId;
	/** 任务 ID → 第 K 步扣除（下标 0..2，null = 无）。 / Quest id → step removals (index 0..2). */
	private final Map<Integer, List<ItemStack>> stepRemoveByQuestId;
	/** 任务 ID → item_check 门的工作物品（真端 quest.xml {@code check_itemK_L}）。 / The gate items. */
	private final Map<Integer, List<ItemStack>> gateItemsByQuestId;
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

	/** 任务 ID → 链式接取窗的下一环（真端交付节点 0x1e 槽的 {@code con_quest}）。 / Quest id → the next quest of the chain window. */
	private final Map<Integer, Integer> conQuestByQuestId;

	/** 生产实例构造（真端表 + 生产背包/结算口）。 / The production wiring. */
	private SimpleUseItemHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeQuestXmlTable questXml, NativeInventoryPort inventory) {
		this(tableLoader, nameResolver, itemIndex, questXml, inventory, NativeReportRewardFlow.instance());
	}

	/**
	 * 门禁用接缝（注入假背包与结算口；数据源与生产一致）。
	 * Gate seam: the data source stays production, only the inventory and settlement sinks are injected.
	 */
	SimpleUseItemHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeQuestXmlTable questXml, NativeInventoryPort inventory,
			NativeReportRewardFlow rewardFlow) {
		this.tableLoader = tableLoader;
		this.nameResolver = nameResolver;
		this.itemIndex = itemIndex;
		this.questXml = questXml;
		this.inventory = inventory;
		this.rewardFlow = rewardFlow;
		this.dialogContract = QuestDialogContract.loadDefault();

		Set<Integer> xmlOwnedIds = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		Map<Integer, Integer> useItems = new LinkedHashMap<>();
		Map<Integer, List<Integer>> acceptByItem = new LinkedHashMap<>();
		Map<Integer, List<Integer>> rewards = new LinkedHashMap<>();
		Map<Integer, List<Integer>> relayNpcs = new LinkedHashMap<>();
		Map<Integer, List<RelayStep>> relays = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepGives = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepRemoves = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> gates = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Set<Integer> unroutable = new TreeSet<>();
		Set<String> unresolved = new TreeSet<>();
		Set<String> unresolvedItems = new TreeSet<>();

		Map<Integer, Integer> conQuests = new LinkedHashMap<>();

		for (NativeQuestTableLoader.SimpleUseItemRow row : tableLoader.useItemRows()) {
			int questId = row.questId();
			owned.add(questId);
			boolean resolvable = true;

			// 接取道具（真端必填列）：解析失败即不可路由。 / The accept item (required): unresolved ⇒ unroutable.
			ItemStack useItem = parseSymbol(row.useItemName(), questId, unresolvedItems);
			if (useItem == null) {
				resolvable = false;
			} else {
				useItems.put(questId, useItem.itemId());
			}

			// 交付 NPC：真端逻辑名唯一解析，或客户端交付集合展开；两者皆无 ⇒ fail-closed。
			// Hand-in npcs: a unique retail logical name, else the client-declared hand-in set.
			List<Integer> rewardIds = rewardNpcIds(tableLoader, nameResolver, questId, row.rewardNpcName(),
					unresolved);
			if (rewardIds.isEmpty()) {
				resolvable = false;
			} else {
				rewards.put(questId, rewardIds);
			}

			// 中继链：逐位解析（表序）；真端名字节点的全部成员都可受理，完全无命中才不可路由。
			// Relay chain: position by position; every member of the retail name node is admitted.
			List<Integer> relayIds = new ArrayList<>(row.talkNpcNames().size());
			for (int index = 0; index < row.talkNpcNames().size(); index++) {
				List<Integer> relayMembers = nameResolver.resolveMembers(row.talkNpcNames().get(index));
				if (relayMembers.isEmpty()) {
					unresolved.add(row.talkNpcNames().get(index));
					resolvable = false;
					continue;
				}
				for (int npcId : relayMembers) {
					relays.computeIfAbsent(npcId, key -> new ArrayList<>())
						.add(new RelayStep(questId, index + 1, npcId));
				}
				relayIds.add(relayMembers.getFirst());
			}
			if (!relayIds.isEmpty()) {
				relayNpcs.put(questId, List.copyOf(relayIds));
			}

			// 第 K 步发/扣：位置对齐（缺位 = null），任一非空符号未解即该行不可路由。
			List<ItemStack> stepGive = parseStepSymbols(row.stepGiveItems(), questId, unresolvedItems);
			List<ItemStack> stepRemove = parseStepSymbols(row.stepRemoveItems(), questId, unresolvedItems);
			// 「声明了但解析不出」才不可路由：装载器始终保留 3 个位置，未声明的位是 null（不是未解）。
			// Only a declared-but-unparsed step item makes the row unroutable: the loader always keeps
			// three positions and an undeclared slot is null rather than unresolved.
			if (hasUnparsed(row.stepGiveItems(), stepGive) || hasUnparsed(row.stepRemoveItems(), stepRemove)) {
				resolvable = false;
			}
			if (stepGive.stream().anyMatch(java.util.Objects::nonNull)) {
				stepGives.put(questId, stepGive);
			}
			if (stepRemove.stream().anyMatch(java.util.Objects::nonNull)) {
				stepRemoves.put(questId, stepRemove);
			}

			// item_check 门：真端族表开关 + 真端 quest.xml {@code check_itemK_L} 门物品，缺一 fail-closed。
			// 实测（2026-10-01 全量复算）：5 个开关行全部声明门物品；另有 4 行仅声明门物品而无开关，
			// 本车道按引擎开关（record 字段）取数，保持与 SimpleTalk 车道同一闸门口径。
			// The item_check gate: the family-table switch plus its quest.xml check_item declaration.
			// All five switched rows declare gate items (verified over the whole table); four further
			// rows declare gate items without the engine switch and stay ungated here, matching the
			// SimpleTalk lane's switch-keyed convention.
			if (row.itemCheck()) {
				List<ItemStack> gate = parseSymbols(checkItemSymbols(questId), questId, unresolvedItems);
				if (gate.isEmpty() || gate.stream().anyMatch(java.util.Objects::isNull)) {
					resolvable = false;
				} else {
					gates.put(questId, List.copyOf(gate));
				}
			}

			// 路由判据（与 P5 步骤 1 冻结门同口径）：退役 ∧ 非 XML-only ∧ 全部声明面可解 ∧ 元数据可编译。
			// Routing verdict, the same rule the P5 step-1 inventory gate froze.
			boolean retired = RetiredQuestIds.contains(questId);
			if (!resolvable || !retired || xmlOwnedIds.contains(questId) || !metadataClean(questId)) {
				unroutable.add(questId);
			} else {
				routed.add(questId);
				if (useItem != null) {
					acceptByItem.computeIfAbsent(useItem.itemId(), key -> new ArrayList<>()).add(questId);
				}
			}

			// 链式接取窗（真端 0x1e 槽）按原文装载：本族的接取面是「用物品」，没有自己的接取 NPC，
			// 故闭环判据（下一环的接取 NPC = 本行的交付 NPC）由逐行对拍门跨族复算，handler 只暴露证据面。
			// The chain window (retail slot 0x1e) loads verbatim. This family accepts by using an item and
			// has no acquire NPC of its own, so the closure invariant (the next quest acquires at this row's
			// hand-in NPC) is recomputed cross-family by the per-row gate; the handler only exposes the face.
			if (row.conQuest() != null) {
				conQuests.put(questId, row.conQuest());
			}
		}

		// 闭环判据的归属：本族没有接取 NPC 面（接取 = 使用道具），因此 handler 内**无法**判定
		// 「下一环的接取 NPC = 本行交付 NPC」；该不变量由逐行对拍门跨族独立复算（当前 32 行目标
		// 30 兄弟族 + 2 无行，0 例外），本处只留证据面，不静默放行也不新增路由。
		// Closure ownership: this family has no acquire-NPC face (accept = using an item), so the handler
		// cannot decide the invariant itself; the per-row gate recomputes it cross-family (32 targets =
		// 30 sibling + 2 no-row, 0 exceptions). No routing is added here.

		this.useItemByQuestId = Collections.unmodifiableMap(useItems);
		this.acceptQuestIdsByItemId = Collections.unmodifiableMap(acceptByItem);
		this.rewardNpcsByQuestId = Collections.unmodifiableMap(rewards);
		this.relayNpcsByQuestId = Collections.unmodifiableMap(relayNpcs);
		this.relaysByNpcId = Collections.unmodifiableMap(relays);
		this.stepGiveByQuestId = Collections.unmodifiableMap(stepGives);
		this.stepRemoveByQuestId = Collections.unmodifiableMap(stepRemoves);
		this.gateItemsByQuestId = Collections.unmodifiableMap(gates);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.unroutableQuestIds = Collections.unmodifiableSet(unroutable);
		this.unresolvedNames = Collections.unmodifiableSet(unresolved);
		this.unresolvedItemSymbols = Collections.unmodifiableSet(unresolvedItems);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
	}

	/**
	 * 真端 {@code con_quest}（链式接取窗的下一环，交付节点 0x1e 槽）；未声明返回 null。
	 * <p>
	 * 真端该列由交付节点 0x1e 槽消费（{@code mgr+0x1a8(player, con_quest)} = 下一环接取窗）。本族接取 =
	 * 使用道具、没有接取 NPC 面，故「下一环在本行交付 NPC 上可接取」这条等价不变量由逐行对拍门跨族复算。
	 * The retail {@code con_quest} column (hand-in slot 0x1e). This family accepts by using an item and has
	 * no acquire NPC, so the equivalence is recomputed cross-family by the per-row alignment gate.
	 */
	public Integer conQuest(int questId) {
		return conQuestByQuestId.get(questId);
	}

	public static SimpleUseItemHandler instance() {
		SimpleUseItemHandler local = instance;
		if (local == null) {
			synchronized (SimpleUseItemHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleUseItemHandler(NativeQuestTableLoader.instance(),
						NativeNpcNameResolver.instance(), retailItemIndex(),
						NativeQuestXmlTable.instance(), NativeInventoryPort.live());
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

	/** 判断是否拥有该任务（注册集 = 真端表全量行）。 / Checks whether the row is in the registration set. */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/** 判断是否由本车道路由（退役 ∧ 非 XML-only ∧ 可解）。 / Whether this lane routes the row. */
	public boolean routes(int questId) {
		return routedQuestIds.contains(questId);
	}

	public Set<Integer> ownedQuestIds() {
		return ownedQuestIds;
	}

	public Set<Integer> routedQuestIds() {
		return routedQuestIds;
	}

	/** 已装载但当前不可路由的行（未退役/缺面未解）。 / Loaded but unroutable rows. */
	public Set<Integer> unroutableQuestIds() {
		return unroutableQuestIds;
	}

	/** 唯一解析失败的 NPC 名（证据面，恒空为门禁）。 / NPC names that did not resolve uniquely. */
	public Set<String> unresolvedNames() {
		return unresolvedNames;
	}

	/** 未解析的物品符号（证据面，恒空为门禁）。 / Unresolved item symbols. */
	public Set<String> unresolvedItemSymbols() {
		return unresolvedItemSymbols;
	}

	/** 接取道具 id（表 {@code use_item_name}；无行返回 null）。 / The accept item id, or null. */
	public Integer useItemId(int questId) {
		return useItemByQuestId.get(questId);
	}

	/** 指定道具可开的任务（按 id 升序，稳定）。 / Quests the item opens (ascending). */
	public List<Integer> acceptQuestIdsForItem(int itemId) {
		return acceptQuestIdsByItemId.getOrDefault(itemId, List.of());
	}

	/** 交付 NPC 集合（真端逻辑名 + 客户端集合展开）。 / The hand-in npc set. */
	public List<Integer> rewardNpcs(int questId) {
		return rewardNpcsByQuestId.getOrDefault(questId, List.of());
	}

	/** 中继 NPC 序列（表序）。 / The relay npc sequence in table order. */
	public List<Integer> relayNpcs(int questId) {
		return relayNpcsByQuestId.getOrDefault(questId, List.of());
	}

	/** 第 K 中继步的发放；无则 null。 / The step-K grant, or null. */
	public ItemStack stepGiveItem(int questId, int step) {
		return stepAt(stepGiveByQuestId.get(questId), step);
	}

	/** 第 K 中继步的扣除；无则 null。 / The step-K removal, or null. */
	public ItemStack stepRemoveItem(int questId, int step) {
		return stepAt(stepRemoveByQuestId.get(questId), step);
	}

	/** item_check 门的工作物品（空 = 无门）。 / The item_check gate items (empty = no gate). */
	public List<ItemStack> gateItems(int questId) {
		return gateItemsByQuestId.getOrDefault(questId, List.of());
	}

	public NativeQuestTableLoader.SimpleUseItemRow requireRow(int questId) {
		return tableLoader.requireUseItem(questId);
	}

	/**
	 * 启动期把本族全部 NPC 标记注册进任务引擎（真端 codegen 的静态注册表等价物）。
	 * 物品接取无 NPC 边，故只注册中继与交付 NPC。
	 * <p>
	 * Registers the family's npc marks at startup; the item-accept face has no npc edge, so only the
	 * relay and hand-in npcs are registered.
	 */
	public void installInterest(com.aionemu.gameserver.questEngine.QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, List<Integer>> entry : rewardNpcsByQuestId.entrySet()) {
			if (!routedQuestIds.contains(entry.getKey())) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnTalkEvent(entry.getKey());
			}
		}
		for (Map.Entry<Integer, List<RelayStep>> entry : relaysByNpcId.entrySet()) {
			for (RelayStep relay : entry.getValue()) {
				if (routedQuestIds.contains(relay.questId())) {
					engine.registerQuestNpc(entry.getKey()).addOnTalkEvent(relay.questId());
				}
			}
		}
	}

	/**
	 * 用物接取口（真端 {@code UseItem} 无主事件）：道具命中且该行未接取（可重行的 COMPLETE 态亦开窗）
	 * 时下发接取窗页 4。进行中/待领奖的行不改状态（消费由道具自身动作决定，引擎不代扣）。
	 * <p>
	 * The retail ownerless item-use accept entry: when the item matches a routed row that is not yet in
	 * progress (repeatable rows re-open at COMPLETE), the page-4 ask window is sent. In-progress rows
	 * are left untouched — the engine never consumes the item itself.
	 */
	public boolean onItemUse(Player player, int itemId) {
		if (player == null || player.getQuestStateList() == null || itemId <= 0) {
			return false;
		}
		List<Integer> questIds = acceptQuestIdsForItem(itemId);
		if (questIds.isEmpty()) {
			return false;
		}
		boolean handled = false;
		for (int questId : questIds) {
			QuestState state = player.getQuestStateList().getQuestState(questId);
			QuestStatus status = state == null ? QuestStatus.NONE : state.getStatus();
			boolean fresh = state == null || status == QuestStatus.NONE;
			boolean repeatableComplete = status == QuestStatus.COMPLETE && repeatable(questId);
			if (!fresh && !repeatableComplete) {
				continue;
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, PAGE_ASK_ACCEPT, questId));
			handled = true;
		}
		return handled;
	}

	/**
	 * 处理对话与翻页（无主接取确认 / 中继链 / 交付 / 领奖）。
	 * Handles dialog and page progression (ownerless accept, relay chain, hand-in, claim).
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
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state != null ? state.getStatus() : QuestStatus.NONE;

		if (state == null || status == QuestStatus.NONE
				|| (status == QuestStatus.COMPLETE && repeatable(questId))) {
			return onAcceptDialog(player, questId, npcId, dialogId);
		}

		if (status == QuestStatus.START) {
			if (talkChainStep(player, questId, npcId, objectId)) {
				return true;
			}
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)) {
				// 两步报告（裁定 a，2026-10-03）：任务行（31）只发客户端声明的报告确认页不推进；
				// 报告确认（1009）才扣门物品 + 翻 REWARD + 奖励窗；开门（-1/26）不推进、不跳步。
				// Two-step report (adjudication a): 31 shows the declared confirm page, 1009 advances.
				// 报告页只随「中继走完」下发——物品门不计入页条件（缺陷 T；退役 XML 80482：started
				// 态 31→SELECT5 无 conditions，就绪分叉在 39/1009 确认动作上）。
				// The confirm page rides the completed relay alone (defect T); the item gate forks on
				// the confirm action.
				boolean relayDone = relayComplete(player, questId);
				boolean reportReady = handInReady(player, questId);
				if (dialogId == 31 && relayDone) {
					// 报告页分型跳过被中继步占用的 SELECT2（缺陷 S，2026-10-05）：双页任务的 select2
					// 是中继步页（按钮 SETPRO1/翻页），报告页是 select5（按钮 SELECT_QUEST_REWARD/39）。
					// The report-page typing skips SELECT2 when relay steps consume it (defect S).
					int reportPage = QuestDialogContract.loadDefault().reportConfirmPage(questId,
						relayNpcs(questId).size());
					if (reportPage > 0) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, reportPage, questId));
						return true;
					}
				}
				if (reportReady && (dialogId == 31 || dialogId == 1009)) {
					if (handIn(player, questId, state)) {
						PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
						return true;
					}
					return false;
				}
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS));
					return true;
				}
			}
			return false;
		}

		if (status == QuestStatus.REWARD) {
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)) {
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
					return true;
				}
				// 选项段只有 SELECTED_QUEST_REWARD1..15（8..22）；23 = SELECTED_QUEST_NOREWARD 是
				// 无选择确认，不占选项下标（与 Talk/Collect/Hunt/SerialHunt/DataDriven 同口径）。
				// 1107 实机 2026-10-04：23 被旧区间（8..23）映射成下标 15 → 按钮面 fail-closed
				// （无声明选项）→ 发奖中止，奖励窗反复重开。
				// Only SELECTED_QUEST_REWARD1..15 (8..22) index options; 23 is the no-selection
				// confirm and maps to index 0 (same shape as the other families).
				if ((dialogId >= 8 && dialogId <= 22)
						|| dialogId == QuestDialogAction.SELECTED_QUEST_NOREWARD.id()
						|| dialogId == 108 || (dialogId >= 110 && dialogId <= 124)) {
					int rewardIndex = dialogId >= 8 && dialogId <= 22 ? dialogId - 8 : 0;
					if (rewardFlow.claim(env, rewardIndex).completed()) {
						// 领奖收尾 = 真端 npc-complete finish=SELECTION_DIALOG（4801/4805）：回选择对话页
						// （页 10，questId=0；9/28 旧引擎基线「状态=5 → 页=10」）。
						// The claim tail follows the retail npc-complete finish=SELECTION_DIALOG: back to
						// the selection dialog (page 10, questId=0; the legacy 9/28 log baseline).
						PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(objectId, QuestDialogPage.SELECT_QUEST.id()));
						return true;
					}
				}
			}
			return false;
		}
		return false;
	}

	/**
	 * 无主接取对话（真端物品接取形的 1002/1003/1008；本族无接取 NPC，带 NPC 的对话不属于本面）。
	 * The ownerless accept dialogs of the retail item-accept shape.
	 */
	private boolean onAcceptDialog(Player player, int questId, int npcId, int dialogId) {
		if (npcId != 0) {
			return false;
		}
		if (dialogId == 1002 || dialogId == 20000) {
			// 拒绝走 startTraced 打 QUEST-TRACE，不再静默。 / Refusals are traced instead of silent.
			if (NativeQuestStartPort.instance().startTraced(player, questId, dialogId).started()) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
				return true;
			}
			return false;
		}
		if (dialogId == 1003 || dialogId == 1004 || dialogId == 20001 || dialogId == 1008) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
			return true;
		}
		return false;
	}

	/**
	 * 中继链步进：命中表序中的下一个 NPC 才推进（乱序/重复零推进），步进即执行该步发/扣。
	 * Relay-chain step: only the next npc in table order advances (out-of-order repeats are no-ops);
	 * the step's give/remove columns execute on the step.
	 */
	private boolean talkChainStep(Player player, int questId, int npcId, int objectId) {
		List<Integer> npcs = relayNpcsByQuestId.get(questId);
		if (npcs == null || npcs.isEmpty() || npcId <= 0) {
			return false;
		}
		int index = npcs.indexOf(npcId);
		if (index < 0) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state == null || index != relayStep(state)) {
			return false;
		}
		int step = index + 1;
		give(player, stepGiveItem(questId, step));
		remove(player, stepRemoveItem(questId, step));
		int vars = (state.getQuestVars().getQuestVars() & ~RELAY_STEP_MASK) | (step << RELAY_STEP_SHIFT);
		state.getQuestVars().setVar(vars);
		state.setPersistentState(PersistentState.UPDATE_REQUIRED);
		PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, state.getStatus(), vars));
		// 步页（真端 SELECT2..4 协议常量）：客户端未声明该页即只推进状态、不发页（fail-closed）。
		// The step page (retail SELECT2..4 protocol constants): undeclared pages advance the state
		// without a page turn instead of guessing a renderable page.
		int page = pageForStep(step);
		if (dialogContract.hasButtonPage(questId, page)) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, page, questId));
		}
		return true;
	}

	/** 当前中继步（= 已完成的 talk 数）。 / Current relay step (the number of finished talks). */
	private static int relayStep(QuestState state) {
		return (state.getQuestVars().getQuestVars() & RELAY_STEP_MASK) >>> RELAY_STEP_SHIFT;
	}

	/** 中继链是否走完。 / Whether the relay chain is complete. */
	private boolean relayComplete(Player player, int questId) {
		List<Integer> npcs = relayNpcsByQuestId.get(questId);
		if (npcs == null || npcs.isEmpty()) {
			return true;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		return state != null && relayStep(state) >= npcs.size();
	}

	/**
	 * 交付门：中继链走完 + item_check 门物品持有（无门列的行不设门）→ 扣除门物品并翻 REWARD。
	 * 门物品缺一即不放行（不部分扣除）。
	 * <p>
	 * Hand-in: the relay chain must be complete and every declared check item must be held; the gate
	 * items are then consumed and the row flips to REWARD. A single shortfall holds it without any
	 * partial removal.
	 */
	private boolean handInReady(Player player, int questId) {
		if (!relayComplete(player, questId)) {
			return false;
		}
		List<ItemStack> gate = gateItemsByQuestId.get(questId);
		if (gate != null) {
			for (ItemStack item : gate) {
				if (inventory.count(player, item.itemId()) < item.count()) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean handIn(Player player, int questId, QuestState state) {
		if (!handInReady(player, questId)) {
			return false;
		}
		List<ItemStack> gate = gateItemsByQuestId.get(questId);
		if (gate != null) {
			for (ItemStack item : gate) {
				if (!inventory.remove(player, item.itemId(), item.count())) {
					return false;
				}
			}
		}
		state.setStatus(QuestStatus.REWARD);
		state.setPersistentState(PersistentState.UPDATE_REQUIRED);
		PacketSendUtility.sendPacket(player,
			new SM_QUEST_ACTION(questId, state.getStatus(), state.getQuestVars().getQuestVars()));
		return true;
	}

	/** 中继步物品页（客户端声明时才下发；真端 SELECT2..4 未声明即不发页）。 / The relay step page when declared. */
	private static int pageForStep(int step) {
		if (step < 1 || step > RELAY_STEP_PAGES.length) {
			throw new IllegalArgumentException("relay step out of range: " + step);
		}
		return RELAY_STEP_PAGES[step - 1];
	}

	/**
	 * 中继步页 id（真端 SELECT2..4 常量；本族用到的行客户端均已声明，见族门）。
	 * The relay step page ids (retail SELECT2..4 protocol constants).
	 */
	public static int relayStepPage(int step) {
		return pageForStep(step);
	}

	private void give(Player player, ItemStack item) {
		if (item != null) {
			inventory.give(player, item.itemId(), item.count());
		}
	}

	private void remove(Player player, ItemStack item) {
		if (item != null) {
			inventory.remove(player, item.itemId(), item.count());
		}
	}

	private static ItemStack stepAt(List<ItemStack> stacks, int step) {
		if (stacks == null || step < 1 || step > stacks.size()) {
			return null;
		}
		return stacks.get(step - 1);
	}

	/** 真端 {@code max_repeat_count} > 1 ⇒ 可重复（COMPLETE 态可再次开窗）。 / Repeatable per retail max_repeat_count. */
	private boolean repeatable(int questId) {
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		Integer maxRepeat = row == null ? null : row.integer("max_repeat_count");
		return maxRepeat != null && maxRepeat > 1;
	}

	/**
	 * 交付 NPC 集合：真端 {@code reward_npc_name} 是逻辑名，静态数据唯一命中即单元素；未命中
	 * （复合名如 {@code <地图>_<势力名>}）时按客户端交付集合展开，客户端未声明即 fail-closed。
	 * <p>
	 * Hand-in npc set: a unique retail logical name, else the client-declared hand-in set; an
	 * undeclared composite name fails closed (the same arbitration face the retired compiler used).
	 */
	private static List<Integer> rewardNpcIds(NativeQuestTableLoader tableLoader,
			NativeNpcNameResolver nameResolver, int questId, String retailName, Set<String> unresolved) {
		List<Integer> members = nameResolver.resolveMembers(retailName);
		if (!members.isEmpty()) {
			return members;
		}
		unresolved.add(retailName);
		Set<Integer> declared = RetailClientHandinNpcSets.defaultSets().npcIds(questId);
		return declared.isEmpty() ? List.of() : List.copyOf(new TreeSet<>(declared));
	}

	/**
	 * item_check 门物品：真端 {@code quest.xml} 的 {@code check_itemK_L} 列（同一真端声明面，
	 * 与族表 {@code item_check} 开关配套）。
	 * <p>
	 * The gate items: the retail {@code quest.xml check_itemK_L} columns that pair with the family
	 * table's {@code item_check} switch.
	 */
	private static List<String> checkItemSymbols(int questId) {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		if (row == null) {
			return List.of();
		}
		java.util.TreeMap<Integer, String> bySlot = new java.util.TreeMap<>();
		for (Map.Entry<String, List<String>> field : row.fields().entrySet()) {
			Matcher matcher = CHECK_ITEM.matcher(field.getKey());
			if (!matcher.matches() || field.getValue().isEmpty()) {
				continue;
			}
			bySlot.putIfAbsent(Integer.parseInt(matcher.group(1)), field.getValue().getFirst());
		}
		return List.copyOf(bySlot.values());
	}

	/** 逐下标解析中继步物品列（保持位置，null 表示该步无此操作/未解）。 / Resolves a step-indexed column, keeping positions. */
	/** 原始列里有声明但解析结果为空 ⇒ 该步未解。 / A declared cell without a parsed stack is unresolved. */
	private static boolean hasUnparsed(List<String> symbols, List<ItemStack> parsed) {
		for (int index = 0; index < symbols.size(); index++) {
			String symbol = symbols.get(index);
			if (symbol != null && !symbol.isBlank() && parsed.get(index) == null) {
				return true;
			}
		}
		return false;
	}

	private List<ItemStack> parseStepSymbols(List<String> symbols, int questId,
			Set<String> unresolved) {
		List<ItemStack> parsed = new ArrayList<>(symbols.size());
		for (String symbol : symbols) {
			parsed.add(symbol == null || symbol.isBlank() ? null : parseSymbol(symbol, questId, unresolved));
		}
		return parsed;
	}

	private List<ItemStack> parseSymbols(List<String> symbols, int questId, Set<String> unresolved) {
		List<ItemStack> parsed = new ArrayList<>(symbols.size());
		for (String symbol : symbols) {
			parsed.add(parseSymbol(symbol, questId, unresolved));
		}
		return parsed;
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
