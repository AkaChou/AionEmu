package com.aionemu.gameserver.questEngine.tablelane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.RetiredQuestIds;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailClientHandinNpcSets;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.tablelane.NativeItemSymbols.ItemStack;
import com.aionemu.gameserver.services.DialogService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleItemPlay 原生任务处理器（计划 §6.2 / §7 P5 切换批）。
 * <p>
 * 完全由真端表 {@link NativeQuestTableLoader#itemPlayRows()}、真端 {@code quest.xml} 与共用结算口驱动，
 * 绝不生成 IR 节点、绝不回退退役编译器。逐项证据（P5 路由集 6 行：19048/29048/13704/13708/23704/23708，
 * 全部无中继链、无 item_check 门）：
 * <ul>
 *   <li><b>接取</b>：{@code acquired_npc_name} 对话 31 → 接取入口页（客户端任务页声明）→
 *       1002 提交（页 1003）/20000 提交（关窗）→ {@link NativeQuestStartPort} 建档，接取即发
 *       {@code give_item}（演出道具；6 行实测 {@code give_item == use_item_name}）；</li>
 *   <li><b>推进</b>：使用 {@code use_item_name} 道具一步推进 START → REWARD（真端 {@code UseItem} 演出，
 *       不代扣道具——回收在交付动作 1009，与退役编译器同刻）；推进**带步数前置**（真端行主 thunk 即本行相机：
 *       {@code if (status == 3 && step == relayCount) set(questId, relayCount + 1, 0)}）⇒ 步号不等于
 *       {@code relayCount} 时零副作用；推进后步号 = {@code relayCount + 1}；</li>
 *   <li><b>中继链</b>（真端交付节点 {@code slot 3 #K}，与 talk 族 cabb10 同轴）：声明 {@code talk_npcK} 的行
 *       按表序在中继 NPC 上接 {@code 10000 + K - 1} 动作，只有 {@code 步号 == K - 1} 才推进到 K，并发/扣
 *       第 K 步的 {@code give_itemK}/{@code remove_itemK}；步 K 的页 = {@code select(K+1)}(1352/1693/2034)；</li>
 *   <li><b>交付/预览</b>：REWARD 态交付 NPC 的 31/26/USE_OBJECT(-1) 重开奖励窗页 5；1009 回收演出道具
 *       并重开奖励窗；</li>
 *   <li><b>领奖</b>：{@link NativeReportRewardFlow}（真端 reward 列 → 共用结算体），完成页 1008。</li>
 * </ul>
 * 缺行/名字多义/道具未解/未退役的行一律不路由（fail-closed）。中继链与第 K 步发/扣自 P5D 步 2 起已接线
 * （数据面 + 动作面 + 页 + 闸门），声明行仍因 **owner 未退役**（XML 保留裁定）而不上线；声明
 * {@code cutsceneid1} 按原文装载为证据面（本族 2 行：13400=859、23400=860），真端本表无
 * {@code cs1_haction} 列 ⇒ 本车道不合成页动作触发，该面不作为路由闸门。
 * <p>
 * Retail SimpleItemPlay native handler (plan §6.2 / §7 P5). The routed six rows accept at the retail
 * acquire npc (page-4 ask window, 1002/20000 commits granting the play item), advance by using the
 * declared item (START → REWARD), re-open the reward window on the hand-in npc and settle through
 * {@link NativeReportRewardFlow}; the play item is recycled on the retail 1009 action exactly where
 * the retired compiler did it. Unresolved names/items and non-retired rows are never routed; the declared cutsceneid1 rows load that column as an evidence face only (this table has no cs1_haction column, so no page-action trigger is synthesised).
 */
public final class SimpleItemPlayHandler {

	/** 进行中（未满足交付门）页。 / In-progress page. */
	public static final int PAGE_IN_PROGRESS = QuestDialogPage.SELECT_QUEST.id();
	/** 奖励窗页。 / The reward window page. */
	public static final int PAGE_REWARD_WINDOW = QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id();
	/** 接取确认页。 / The accept confirmation page. */
	public static final int PAGE_ACCEPTED = QuestDialogPage.QUEST_ACCEPT_1.id();
	/** 拒绝页。 / The refuse page. */
	public static final int PAGE_REFUSED = QuestDialogPage.QUEST_REFUSE_1.id();

	/** 中继步页（真端 SELECT2..4；客户端契约逐行已声明）。 / Relay step pages (retail SELECT2..4). */
	private static final int[] RELAY_STEP_PAGES = {1352, 1693, 2034};

	private static volatile SimpleItemPlayHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final NativeNpcNameResolver nameResolver;
	private final RetailItemNameIndex itemIndex;
	private final NativeInventoryPort inventory;
	private final NativeReportRewardFlow rewardFlow;
	private final QuestDialogContract dialogContract;

	/** 任务 ID → 接取 NPC。 / Quest id → the acquire npc. */
	/** 接取 NPC 成员集（任一成员可接取）。 / Acquire NPC member set. */
	private final Map<Integer, List<Integer>> acquireNpcIdsByQuestId;
	/** 任务 ID → 交付 NPC 集合。 / Quest id → hand-in npcs. */
	private final Map<Integer, List<Integer>> rewardNpcsByQuestId;
	/** 任务 ID → 接取发放（真端 {@code give_item}；演出道具）。 / Quest id → the accept grant. */
	private final Map<Integer, ItemStack> acceptGiveByQuestId;
	/** 任务 ID → 演出道具（真端 {@code use_item_name}）。 / Quest id → the play item. */
	private final Map<Integer, Integer> playItemByQuestId;
	/** 演出道具 id → 该道具推进的任务。 / Play item id → the quests it advances. */
	private final Map<Integer, List<Integer>> advanceQuestIdsByItemId;
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

	/** 中继引用：任务 ID + 步号 (1..3) + 中继 NPC ID。 / Relay reference: quest id + step (1..3) + relay npc id. */
	public record RelayStep(int questId, int step, int npcId) {
	}

	/** 中继 NPC ID → 该 NPC 上的全部中继步。 / Relay npc id → every relay step bound to it. */
	private final Map<Integer, List<RelayStep>> relaysByNpcId;
	/** 任务 ID → 中继步数（0 = 直交形）。 / Quest id → relay step count (0 = direct hand-in). */
	private final Map<Integer, Integer> relayCountByQuestId;
	/** 任务 ID → 第 K 步的发放（位置保留）。 / Quest id → step grants (positions kept). */
	private final Map<Integer, List<ItemStack>> stepGiveByQuestId;
	/** 任务 ID → 第 K 步的扣除（位置保留）。 / Quest id → step removals (positions kept). */
	private final Map<Integer, List<ItemStack>> stepRemoveByQuestId;

	/** 任务 ID → 过场资源 id（真端交付节点 0x35 槽的 {@code cutsceneid1}，本族 2 行）。 / Quest id → the declared cutscene resource id (retail slot 0x35, two rows). */
	private final Map<Integer, Integer> cutsceneByQuestId;

	/** 任务 ID → 链式接取窗的下一环（真端交付节点 0x1e 槽的 {@code con_quest}）。 / Quest id → the next quest of the chain window. */
	private final Map<Integer, Integer> conQuestByQuestId;
	/** 链式接取窗未闭环的行（本族当前恒空：声明行未路由，闭环由逐行门复算）。 / Rows whose chain window is not realized. */
	private final Set<Integer> unresolvedChainQuestIds;

	/** 生产实例构造（真端表 + 生产背包/结算口）。 / The production wiring. */
	private SimpleItemPlayHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeInventoryPort inventory) {
		this(tableLoader, nameResolver, itemIndex, inventory, NativeReportRewardFlow.instance());
	}

	/**
	 * 门禁用接缝（注入假背包与结算口；数据源与生产一致）。
	 * Gate seam: the data source stays production, only the inventory and settlement sinks are injected.
	 */
	SimpleItemPlayHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeInventoryPort inventory,
			NativeReportRewardFlow rewardFlow) {
		this.tableLoader = tableLoader;
		this.nameResolver = nameResolver;
		this.itemIndex = itemIndex;
		this.inventory = inventory;
		this.rewardFlow = rewardFlow;
		this.dialogContract = QuestDialogContract.loadDefault();

		Set<Integer> xmlOwnedIds = NativeQuestOwnerResolver.instance().xmlOnlyIds();
		Map<Integer, List<Integer>> acquires = new LinkedHashMap<>();
		Map<Integer, List<Integer>> rewards = new LinkedHashMap<>();
		Map<Integer, ItemStack> acceptGives = new LinkedHashMap<>();
		Map<Integer, Integer> playItems = new LinkedHashMap<>();
		Map<Integer, List<Integer>> advanceByItem = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Set<Integer> unroutable = new TreeSet<>();
		Set<String> unresolved = new TreeSet<>();
		Set<String> unresolvedItems = new TreeSet<>();

		Map<Integer, List<RelayStep>> relays = new LinkedHashMap<>();
		Map<Integer, Integer> relayCounts = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepGives = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepRemoves = new LinkedHashMap<>();
		Map<Integer, Integer> conQuests = new LinkedHashMap<>();
		Map<Integer, Integer> cutscenes = new LinkedHashMap<>();
		Set<Integer> unresolvedChain = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleItemPlayRow row : tableLoader.itemPlayRows()) {
			int questId = row.questId();
			owned.add(questId);
			boolean resolvable = true;

			// 接取 NPC（真端必填列）：名字节点语义 = 全部同名成员均可受理；完全无命中才不可路由。
			// The acquire npc (required): the retail name node admits every member; only a total miss blocks.
			List<Integer> acquireIds = nameResolver.resolveMembers(row.acquiredNpcName());
			if (acquireIds.isEmpty()) {
				unresolved.add(row.acquiredNpcName());
				resolvable = false;
			} else {
				acquires.put(questId, acquireIds);
			}

			// 交付 NPC：真端逻辑名唯一解析，或客户端交付集合展开。 / Hand-in npcs: unique name or client set.
			List<Integer> rewardIds = rewardNpcIds(questId, row.rewardNpcName(), unresolved);
			if (rewardIds.isEmpty()) {
				resolvable = false;
			} else {
				rewards.put(questId, rewardIds);
			}

			// 接取发放（真端 give_item；本族 6 行全部 = 演出道具）。 / The accept grant (the play item).
			ItemStack acceptGive = parseSymbol(row.acceptGiveItem(), questId, unresolvedItems);
			if (acceptGive != null) {
				acceptGives.put(questId, acceptGive);
			}

			// 演出道具（真端 use_item_name；本族推进事件）。 / The play item (retail use_item_name).
			ItemStack playItem = parseSymbol(row.useItemName(), questId, unresolvedItems);
			Integer playItemId = null;
			if (row.useItemName() != null && !row.useItemName().isBlank() && playItem == null) {
				resolvable = false;
			}
			if (playItem != null) {
				playItemId = playItem.itemId();
				playItems.put(questId, playItem.itemId());
			}

			// 中继链（真端交付节点 slot 3 #K）：按表序解析中继 NPC；步页/动作/发扣与 look 族同轴。
			// The relay chain (retail slot 3 #K): relay npcs resolve in table order, same axis as the talk lane.
			List<String> talkNpcs = row.talkNpcNames();
			relayCounts.put(questId, talkNpcs.size());
			for (int index = 0; index < talkNpcs.size(); index++) {
				List<Integer> relayMembers = nameResolver.resolveMembers(talkNpcs.get(index));
				if (relayMembers.isEmpty()) {
					unresolved.add(talkNpcs.get(index));
					resolvable = false;
					continue;
				}
				for (int relayNpcId : relayMembers) {
					relays.computeIfAbsent(relayNpcId, key -> new ArrayList<>())
						.add(new RelayStep(questId, index + 1, relayNpcId));
				}
			}

			// 第 K 步发/扣（位置保留）：声明了却解析不出的行不可路由（不半接线）。
			// Step-K give/remove keep positions; a declared symbol that does not resolve keeps the row unroutable.
			List<ItemStack> stepGive = parseStepSymbols(row.stepGiveItems(), questId, unresolvedItems);
			List<ItemStack> stepRemove = parseStepSymbols(row.stepRemoveItems(), questId, unresolvedItems);
			if (stepGive.stream().anyMatch(java.util.Objects::nonNull)) {
				stepGives.put(questId, stepGive);
			}
			if (stepRemove.stream().anyMatch(java.util.Objects::nonNull)) {
				stepRemoves.put(questId, stepRemove);
			}
			boolean stepItemsDeclared = row.stepGiveItems().stream().anyMatch(SimpleItemPlayHandler::declared)
				|| row.stepRemoveItems().stream().anyMatch(SimpleItemPlayHandler::declared);
			boolean stepItemsResolved = (row.stepGiveItems().size() == stepGive.size()
					&& java.util.stream.IntStream.range(0, stepGive.size())
						.allMatch(i -> !declared(row.stepGiveItems().get(i)) || stepGive.get(i) != null))
				&& (row.stepRemoveItems().size() == stepRemove.size()
					&& java.util.stream.IntStream.range(0, stepRemove.size())
						.allMatch(i -> !declared(row.stepRemoveItems().get(i)) || stepRemove.get(i) != null));
			if (stepItemsDeclared && !stepItemsResolved) {
				resolvable = false;
			}

			// 过场（真端 0x35 槽）：本表 2 行声明 cutsceneid1（13400=859、23400=860）。真端本表既无
			// cs1_haction 也无 item_check 列（全表 0 命中，实测）⇒ 不存在页动作触发面；此面按证据装载、
			// 不合成触发、**不作为路由闸门**（旧版「缺触发列即 fail-closed」属本地假设，已拆除）。
			// The cutscene slot 0x35: two rows declare cutsceneid1 (13400=859, 23400=860). This retail
			// table carries neither cs1_haction nor item_check (0 hits measured), so no page-action
			// trigger exists; the face is loaded as evidence, synthesises no trigger and is NOT a
			// routing gate (the former fail-closed rule rested on a local assumption).
			if (row.cutsceneId() != null) {
				cutscenes.put(questId, row.cutsceneId());
			}

			boolean retired = RetiredQuestIds.contains(questId);
			if (!resolvable || !retired || xmlOwnedIds.contains(questId) || !metadataClean(questId)) {
				unroutable.add(questId);
			} else {
				routed.add(questId);
				if (playItemId != null) {
					advanceByItem.computeIfAbsent(playItemId, key -> new ArrayList<>()).add(questId);
				}
			}

			// 链式接取窗（真端 0x1e 槽）按原文装载：本族 9 行声明 con_quest，其中 2 行（13400/23400）
			// 的目标落在本表内（13401/23401），其余 7 行在兄弟族。
			// 过场（真端 0x35 槽）按原文装载为证据面（{@link #cutsceneId(int)}）：真端本表无 cs1_haction 列 ⇒
			// 本车道不存在页动作触发点，不合成触发；播放点绑定（真端 thunk 的节点槽语义）属后续批，见 P9 §8。
			// The cutscene (slot 0x35) loads as an evidence face (see cutsceneId): the table has no cs1_haction
			// column, so this lane has no page action to trigger it and synthesises none; the playback
			// binding is a later batch (P9 §8).
			if (row.conQuest() != null) {
				conQuests.put(questId, row.conQuest());
			}
		}

		// 闭环判据（与 P1B/P4B 同一不变量）：本表内目标逐行验证「下一环的接取 NPC = 本行交付 NPC」，
		// 不闭环即登记 fail-closed 证据；跨族目标（7 行）由逐行对拍门按同一条不变量复算。
		// Closure check (same invariant as P1B/P4B): in-table targets are verified here and non-closing rows
		// are recorded; cross-family targets are recomputed by the per-row gate.
		for (Map.Entry<Integer, Integer> entry : conQuests.entrySet()) {
			int questId = entry.getKey();
			int next = entry.getValue();
			List<Integer> targetAcquires = acquires.get(next);
			if (targetAcquires == null) {
				continue;
			}
			List<Integer> sourceRewards = rewards.get(questId);
			if (routed.contains(next)
					&& (sourceRewards == null || Collections.disjoint(targetAcquires, sourceRewards))) {
				unresolvedChain.add(questId);
			}
		}

		this.relaysByNpcId = Collections.unmodifiableMap(relays);
		this.relayCountByQuestId = Collections.unmodifiableMap(relayCounts);
		this.stepGiveByQuestId = Collections.unmodifiableMap(stepGives);
		this.stepRemoveByQuestId = Collections.unmodifiableMap(stepRemoves);
		this.acquireNpcIdsByQuestId = Collections.unmodifiableMap(acquires);
		this.rewardNpcsByQuestId = Collections.unmodifiableMap(rewards);
		this.acceptGiveByQuestId = Collections.unmodifiableMap(acceptGives);
		this.playItemByQuestId = Collections.unmodifiableMap(playItems);
		this.advanceQuestIdsByItemId = Collections.unmodifiableMap(advanceByItem);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.unroutableQuestIds = Collections.unmodifiableSet(unroutable);
		this.unresolvedNames = Collections.unmodifiableSet(unresolved);
		this.unresolvedItemSymbols = Collections.unmodifiableSet(unresolvedItems);
		this.cutsceneByQuestId = Collections.unmodifiableMap(cutscenes);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
		this.unresolvedChainQuestIds = Collections.unmodifiableSet(unresolvedChain);
	}

	/**
	 * 真端 {@code con_quest}（链式接取窗的下一环，交付节点 0x1e 槽）；未声明返回 null。
	 * The retail {@code con_quest} column (hand-in slot 0x1e); the window is realized by the next quest's
	 * own accept route whenever that row acquires at this row's hand-in NPC.
	 */
	// 表声明的过场资源 id（本族 2 行：13400=859、23400=860；未声明返回 null）。
	// The declared cutscene resource id (13400=859, 23400=860; null when none).
	public Integer cutsceneId(int questId) {
		return cutsceneByQuestId.get(questId);
	}

	public Integer conQuest(int questId) {
		return conQuestByQuestId.get(questId);
	}

	/** 链式接取窗未闭环的行（fail-closed 证据面）。 / Rows whose chain window is not realized. */
	public Set<Integer> unresolvedChainQuestIds() {
		return unresolvedChainQuestIds;
	}

	public static SimpleItemPlayHandler instance() {
		SimpleItemPlayHandler local = instance;
		if (local == null) {
			synchronized (SimpleItemPlayHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleItemPlayHandler(NativeQuestTableLoader.instance(),
						NativeNpcNameResolver.instance(), retailItemIndex(), NativeInventoryPort.live());
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

	/** 接取 NPC（成员集首项；未解析为 null）。 / The acquire npc (first member; null when unresolved). */
	public Integer acquireNpc(int questId) {
		List<Integer> members = acquireNpcIdsByQuestId.get(questId);
		return members == null || members.isEmpty() ? null : members.getFirst();
	}

	/** 接取 NPC 成员集（空表 = 未解析）。 / The acquire NPC member set. */
	public List<Integer> acquireNpcs(int questId) {
		return acquireNpcIdsByQuestId.getOrDefault(questId, List.of());
	}

	/** 交付 NPC 集合。 / The hand-in npc set. */
	public List<Integer> rewardNpcs(int questId) {
		return rewardNpcsByQuestId.getOrDefault(questId, List.of());
	}

	/** 接取发放（真端 {@code give_item}；无则 null）。 / The accept grant, or null. */
	public ItemStack acceptGiveItem(int questId) {
		return acceptGiveByQuestId.get(questId);
	}

	/** 演出道具 id（真端 {@code use_item_name}；无则 null）。 / The play item id, or null. */
	public Integer playItemId(int questId) {
		return playItemByQuestId.get(questId);
	}

	/** 指定演出道具可推进的任务（升序）。 / Quests the play item advances (ascending). */
	public List<Integer> advanceQuestIdsForItem(int itemId) {
		return advanceQuestIdsByItemId.getOrDefault(itemId, List.of());
	}

	/** 任务的中继步数（0 = 直交形）。 / Relay step count (0 = direct hand-in). */
	public int relayCount(int questId) {
		return relayCountByQuestId.getOrDefault(questId, 0);
	}

	/** 指定 NPC 上的中继步（无则空表）。 / Relay steps bound to the npc. */
	public List<RelayStep> relaysForNpc(int npcId) {
		return relaysByNpcId.getOrDefault(npcId, List.of());
	}

	/** 指定任务的按序中继步（证据/门禁用；无则空表）。 / The quest's relay steps in table order. */
	public List<RelayStep> relaysForQuest(int questId) {
		List<RelayStep> steps = new ArrayList<>();
		for (List<RelayStep> relayed : relaysByNpcId.values()) {
			for (RelayStep relay : relayed) {
				if (relay.questId() == questId) {
					steps.add(relay);
				}
			}
		}
		steps.sort(java.util.Comparator.comparingInt(RelayStep::step));
		return Collections.unmodifiableList(steps);
	}

	/** 第 K 中继步（K=1..3）的发放；无则 null。 / The step-K grant, or null. */
	public ItemStack stepGiveItem(int questId, int step) {
		return stepAt(stepGiveByQuestId.get(questId), step);
	}

	/** 第 K 中继步（K=1..3）的扣除；无则 null。 / The step-K removal, or null. */
	public ItemStack stepRemoveItem(int questId, int step) {
		return stepAt(stepRemoveByQuestId.get(questId), step);
	}

	/** 中继步对应的页 id（真端 SELECT2..4）。 / The page id of a relay step (retail SELECT2..4). */
	public static int pageForStep(int step) {
		if (step < 1 || step > RELAY_STEP_PAGES.length) {
			throw new IllegalArgumentException("relay step out of range: " + step);
		}
		return RELAY_STEP_PAGES[step - 1];
	}

	private static ItemStack stepAt(List<ItemStack> stacks, int step) {
		if (stacks == null || step < 1 || step > stacks.size()) {
			return null;
		}
		return stacks.get(step - 1);
	}

	public NativeQuestTableLoader.SimpleItemPlayRow requireRow(int questId) {
		return tableLoader.requireItemPlay(questId);
	}

	/**
	 * 启动期把本族全部 NPC 标记注册进任务引擎（真端 codegen 的静态注册表等价物）。
	 * Registers the family's npc marks at startup (the retail codegen registry equivalent).
	 */
	public void installInterest(com.aionemu.gameserver.questEngine.QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, List<Integer>> entry : acquireNpcIdsByQuestId.entrySet()) {
			if (!routedQuestIds.contains(entry.getKey())) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnQuestStart(entry.getKey());
				engine.registerQuestNpc(npcId).addOnTalkEvent(entry.getKey());
			}
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
	 * 用物演出推进（真端 {@code UseItem}）：START 态且**步号等于 {@code relayCount}** 时使用该行声明的道具，
	 * 一步进 REWARD 并把步号置为 {@code relayCount + 1}。
	 * <p>
	 * 闸门来自真端行主 thunk（本行相机）：{@code if (status == 3 && step == relayCount) set(questId,
	 * relayCount + 1, 0)}——43 行里 41 行满足该式（唯一无相机的 80255/80256 已在计划 §10.3-#16① 冻结）；
	 * 旧编译器（真端+客户端派生契约）同形：{@code var0 == 0 → var0 = 1} 才 started→reward。步号不足
	 * （中继没走完）与步号越界都**零副作用**。
	 * <p>
	 * The retail advance requires {@code step == relayCount} (the row's own camera gate), then flips to
	 * REWARD with {@code step = relayCount + 1}; other states and out-of-step rows are untouched.
	 */
	public boolean onItemUse(Player player, int itemId) {
		if (player == null || player.getQuestStateList() == null || itemId <= 0) {
			return false;
		}
		List<Integer> questIds = advanceQuestIdsForItem(itemId);
		if (questIds.isEmpty()) {
			return false;
		}
		boolean handled = false;
		for (int questId : questIds) {
			QuestState state = player.getQuestStateList().getQuestState(questId);
			if (state == null || state.getStatus() != QuestStatus.START) {
				continue;
			}
			int relayCount = relayCount(questId);
			if (state.getQuestVars().getQuestVars() != relayCount) {
				continue;
			}
			state.getQuestVars().setVar(relayCount + 1);
			state.setStatus(QuestStatus.REWARD);
			state.setPersistentState(PersistentState.UPDATE_REQUIRED);
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_ACTION(questId, state.getStatus(), state.getQuestVars().getQuestVars()));
			handled = true;
		}
		return handled;
	}

	/**
	 * 旧存档自愈（真端相机步号的 native 等价物）：REWARD 态而步号仍为 0 的行补到 {@code relayCount + 1}
	 * ——P5 起的 native 车道推进时未写步号，旧存档会停在 0（老 IR 车道写的是 1）。
	 * <p>
	 * Enter-world heal: a REWARD row whose step is still 0 (saves written by the P5 lane, which did not
	 * record the step) is repaired to {@code relayCount + 1}; other values are left untouched.
	 */
	public boolean onEnterWorld(Player player) {
		if (player == null || player.getQuestStateList() == null) {
			return false;
		}
		boolean healed = false;
		for (int questId : routedQuestIds) {
			QuestState state = player.getQuestStateList().getQuestState(questId);
			if (state == null || state.getStatus() != QuestStatus.REWARD
					|| state.getQuestVars().getQuestVars() != 0) {
				continue;
			}
			int healedVars = relayCount(questId) + 1;
			state.getQuestVars().setVar(healedVars);
			state.setPersistentState(PersistentState.UPDATE_REQUIRED);
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, QuestStatus.REWARD, healedVars));
			healed = true;
		}
		return healed;
	}

	/**
	 * 处理对话与翻页（接取 / 交付预览 / 回收道具 / 领奖）。
	 * Handles dialog and page progression (accept, hand-in preview, item recycle, claim).
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

		boolean fresh = state == null || status == QuestStatus.NONE;
		if (fresh || (status == QuestStatus.COMPLETE && repeatable(questId))) {
			return onAcceptDialog(player, questId, npcId, objectId, dialogId);
		}

		if (status == QuestStatus.REWARD) {
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)) {
				if (dialogId == 31 || dialogId == 26 || dialogId == -1) {
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
					return true;
				}
				if (dialogId == 1009) {
					// 交付预览通道（真端 SELECT_QUEST_REWARD）：回收演出道具后重开奖励窗（回收时机不变）。
					// The preview channel (retail SELECT_QUEST_REWARD): recycle the play item, then
					// re-open the reward window — the removal timing is unchanged.
					ItemStack playItem = playItem(questId);
					if (playItem != null) {
						inventory.remove(player, playItem.itemId(), playItem.count());
					}
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
					return true;
				}
				// 选项段只有 SELECTED_QUEST_REWARD1..15（8..22）；23 = SELECTED_QUEST_NOREWARD 是
				// 无选择确认，不占选项下标（与 Talk/Collect/Hunt/SerialHunt/DataDriven 同口径）。
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

		// START：① 中继步（真端交付节点 slot 3 #K 与 cabb10 同轴）只推进「当前步」，乱序/重复零副作用，
		//        步进即发/扣该步物品，步页 = select(K+1)；② 最终推进事件是用物（真端相机，带步号闸门）；
		//        ③ 交付 NPC 处未满足时给进行中页。
		// START: (1) the relay step (retail slot 3 #K, same axis as cabb10) advances only the current step,
		// issuing/removing that step's items and serving select(K+1); (2) the final advance is the gated
		// item use; (3) the hand-in npc serves the in-progress page until the gate passes.
		if (status == QuestStatus.START) {
			int vars = state.getQuestVars().getQuestVars();
			// 选择对话续页（SELECT⟨n⟩_… 子页动作 = 页 id）：真端原样回发该页（9/28 基线跨任务
			// 实证 1353/1354/1694/1695/2035/2376）；契约未声明该页即 fail-closed 零响应。
			// Selection sub-page actions echo their page back (the 9/28 baseline); a page the client
			// task HTML never declares fails closed.
			if (QuestDialogPage.isSelectionSubPage(dialogId)
					&& dialogContract.hasButtonPage(questId, dialogId)) {
				PacketSendUtility.sendPacket(player,
					new SM_DIALOG_WINDOW(objectId, dialogId, questId));
				return true;
			}
			for (RelayStep relay : relaysForNpc(npcId)) {
				if (relay.questId() != questId) {
					continue;
				}
				int step = relay.step();
				int action = 10000 + step - 1;
				if (dialogId == action) {
					if (vars == step - 1) {
						state.getQuestVars().setVar(step);
						state.setPersistentState(PersistentState.UPDATE_REQUIRED);
						PacketSendUtility.sendPacket(player,
							new SM_QUEST_ACTION(questId, QuestStatus.START, step));
						ItemStack stepGive = stepGiveItem(questId, step);
						if (stepGive != null) {
							inventory.give(player, stepGive.itemId(), stepGive.count());
						}
						ItemStack stepRemove = stepRemoveItem(questId, step);
						if (stepRemove != null) {
							inventory.remove(player, stepRemove.itemId(), stepRemove.count());
						}
						// 推进 after-commit = 关窗（真端 cabb10 同轴：10000/10001/10002 →
						// SetQuestProgress + 0x5d8 + GiveItem + RemoveItem，**零发页**；0x5d8＝关窗，
						// 2026-10-05 实机「仅状态包不关窗、补关窗包一次点击即关」确证）。旧「回页 10」
						// 系翻译夸大（2026-10-05 1131 实机「结束对话后多余弹页」）。
						// The retail after-commit (cabb10 axis) closes the window (0x5d8) and sends no
						// page; the old page-10 tail was a translation artifact (live 1131, 2026-10-05).
						DialogService.closeDialog(player, objectId);
						return true;
					}
					// 未推进（重复/乱序重放）：真端无匹配转换 ⇒ close-dialog 兜底（本服 loop breaker 同语义）。
					// No matching retail transition on a replayed advance: close the dialog.
					DialogService.closeDialog(player, objectId);
					return true;
				}
				if (dialogId == 31 || dialogId == 26 || dialogId == -1) {
					// 任务行打开该中继 NPC 的对话 = 该步页（9/28 基线：vars=0/step=1 点 31 → 1352）；
					// 尚未轮到的步（vars < step-1）零响应不跳步。原实现在 vars<step 时发
					// 「页 10 带 questId」，而任务页契约无页 10 ⇒ 客户端 load fail（1118 同型，
					// 2026-10-04）。
					// Row selection opens the step dialog (= the step page; the 9/28 baseline). A step
					// not reached yet stays silent; the old quest-id-tagged page 10 is never declared
					// by the task HTML and failed the client load.
					if (vars < step - 1) {
						return false;
					}
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, pageForStep(step), questId));
					return true;
				}
			}
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)
					&& (dialogId == 31 || dialogId == 26 || dialogId == -1)) {
				PacketSendUtility.sendPacket(player,
					new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS));
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * 接取对话（真端 NPC 接取形 cab520：31 → 接取入口页；1002 提交 + 页 1003；20000 提交 + 关窗；
	 * 1003 → 拒绝页；1004/20001 → 关窗；1008 → 选择对话页）。
	 * The retail npc accept shape (page-4 ask window, 1002/20000 commits granting the play item).
	 */
	private boolean onAcceptDialog(Player player, int questId, int npcId, int objectId, int dialogId) {
		List<Integer> acquireNpcs = acquireNpcIdsByQuestId.get(questId);
		if (acquireNpcs == null || !acquireNpcs.contains(npcId)) {
			return false;
		}
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
		if (dialogId == QuestDialogPage.SELECT1_1.id() || dialogId == QuestDialogPage.SELECT1_1_1.id()) {
			if (!dialogContract.hasButtonPage(questId, dialogId)) {
				return false;
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId, questId));
			return true;
		}
		if (dialogId == 1002 || dialogId == 20000) {
			if (NativeQuestStartPort.instance().start(player, questId).started()) {
				// 接取即发演出道具（真端 give_item；6 行实测 give_item == use_item_name）。
				// The accept grants the play item (retail give_item).
				ItemStack acceptGive = acceptGiveByQuestId.get(questId);
				if (acceptGive != null) {
					inventory.give(player, acceptGive.itemId(), acceptGive.count());
				}
				if (dialogId == 1002) {
					PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(objectId, PAGE_ACCEPTED, questId));
				} else {
					DialogService.closeDialog(player, objectId);
				}
				return true;
			}
			return false;
		}
		if (dialogId == 1003) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_REFUSED, questId));
			return true;
		}
		if (dialogId == 1004 || dialogId == 20001) {
			DialogService.closeDialog(player, objectId);
			return true;
		}
		if (dialogId == 1008) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS));
			return true;
		}
		return false;
	}

	/** 演出道具（真端 {@code use_item_name}，计数位保留）。 / The play item with its retail count. */
	private ItemStack playItem(int questId) {
		NativeQuestTableLoader.SimpleItemPlayRow row = tableLoader.itemPlayRows().stream()
			.filter(candidate -> candidate.questId() == questId)
			.findFirst()
			.orElse(null);
		if (row == null) {
			return null;
		}
		return parseSymbol(row.useItemName(), questId, new TreeSet<>());
	}

	/** 该位是否真的声明了内容（装载器保留位置，缺位是 null）。 / Whether a positional cell is declared. */
	private static boolean declared(String cell) {
		return cell != null && !cell.isBlank();
	}

	private ItemStack parseSymbol(String symbol, int questId, Set<String> unresolved) {
		return NativeItemSymbols.parse(symbol, questId, itemIndex, unresolved);
	}

	/** 第 K 步发/扣的位置表（缺位保留为 null）。 / The positional step give/remove table (gaps kept as null). */
	private List<ItemStack> parseStepSymbols(List<String> symbols, int questId, Set<String> unresolved) {
		List<ItemStack> parsed = new ArrayList<>(symbols.size());
		for (String symbol : symbols) {
			parsed.add(parseSymbol(symbol, questId, unresolved));
		}
		return Collections.unmodifiableList(parsed);
	}

	/** 真端 {@code max_repeat_count} > 1 ⇒ 可重复（COMPLETE 态可再次开窗）。 / Repeatable per retail max_repeat_count. */
	private boolean repeatable(int questId) {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		Integer maxRepeat = row == null ? null : row.integer("max_repeat_count");
		return maxRepeat != null && maxRepeat > 1;
	}

	/**
	 * 交付 NPC 集合：真端 {@code reward_npc_name} 是逻辑名，静态数据唯一命中即单元素；未命中时按
	 * 客户端交付集合展开，客户端未声明即 fail-closed。
	 * <p>
	 * Hand-in npc set: a unique retail logical name, else the client-declared hand-in set.
	 */
	private List<Integer> rewardNpcIds(int questId, String retailName, Set<String> unresolved) {
		List<Integer> members = nameResolver.resolveMembers(retailName);
		if (!members.isEmpty()) {
			return members;
		}
		unresolved.add(retailName);
		Set<Integer> declared = RetailClientHandinNpcSets.defaultSets().npcIds(questId);
		return declared.isEmpty() ? List.of() : List.copyOf(new TreeSet<>(declared));
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
