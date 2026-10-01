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
 *       不代扣道具——回收在交付动作 1009，与退役编译器同刻）；</li>
 *   <li><b>交付/预览</b>：REWARD 态交付 NPC 的 31/26/USE_OBJECT(-1) 重开奖励窗页 5；1009 回收演出道具
 *       并重开奖励窗；</li>
 *   <li><b>领奖</b>：{@link NativeReportRewardFlow}（真端 reward 列 → 共用结算体），完成页 1008。</li>
 * </ul>
 * 缺行/名字多义/道具未解/未退役的行一律不路由（fail-closed）；中继链（{@code talk_npc1/2}）、
 * 第 K 步发/扣与 {@code cutsceneid1} 只在未退役行上出现，本批不路由。
 * <p>
 * Retail SimpleItemPlay native handler (plan §6.2 / §7 P5). The routed six rows accept at the retail
 * acquire npc (page-4 ask window, 1002/20000 commits granting the play item), advance by using the
 * declared item (START → REWARD), re-open the reward window on the hand-in npc and settle through
 * {@link NativeReportRewardFlow}; the play item is recycled on the retail 1009 action exactly where
 * the retired compiler did it. Unresolved names/items and non-retired rows are never routed.
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
	/** 完成页。 / The completion page. */
	public static final int PAGE_COMPLETE = QuestDialogPage.QUEST_COMPLETE.id();

	private static volatile SimpleItemPlayHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final NativeNpcNameResolver nameResolver;
	private final RetailItemNameIndex itemIndex;
	private final NativeInventoryPort inventory;
	private final NativeReportRewardFlow rewardFlow;
	private final QuestDialogContract dialogContract;

	/** 任务 ID → 接取 NPC。 / Quest id → the acquire npc. */
	private final Map<Integer, Integer> acquireNpcByQuestId;
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
		Map<Integer, Integer> acquires = new LinkedHashMap<>();
		Map<Integer, List<Integer>> rewards = new LinkedHashMap<>();
		Map<Integer, ItemStack> acceptGives = new LinkedHashMap<>();
		Map<Integer, Integer> playItems = new LinkedHashMap<>();
		Map<Integer, List<Integer>> advanceByItem = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Set<Integer> unroutable = new TreeSet<>();
		Set<String> unresolved = new TreeSet<>();
		Set<String> unresolvedItems = new TreeSet<>();

		Map<Integer, Integer> conQuests = new LinkedHashMap<>();
		Set<Integer> unresolvedChain = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleItemPlayRow row : tableLoader.itemPlayRows()) {
			int questId = row.questId();
			owned.add(questId);
			boolean resolvable = true;

			// 接取 NPC（真端必填列）：非唯一解析即不可路由。 / The acquire npc (required): must resolve uniquely.
			NativeNpcNameResolver.Match acquire = nameResolver.resolve(row.acquiredNpcName());
			if (acquire.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
				unresolved.add(row.acquiredNpcName());
				resolvable = false;
			} else {
				acquires.put(questId, acquire.npcIds().getFirst());
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

			// 中继链与第 K 步发/扣：本批 6 行均未声明；声明了却不能全解的行不可路由（不半接线）。
			// The relay chain and step give/remove columns: none of the routed rows declares them; a row
			// that declares them without a full resolution stays unroutable instead of half-wired.
			boolean declaresStepItems = row.stepGiveItems().stream().anyMatch(SimpleItemPlayHandler::declared)
				|| row.stepRemoveItems().stream().anyMatch(SimpleItemPlayHandler::declared);
			if (!row.talkNpcNames().isEmpty() || declaresStepItems
					|| row.cutsceneId() != null || row.itemCheck()) {
				resolvable = false;
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
			// 过场（真端 0x35 槽）本族 2 行声明 cutsceneid1（859/860），但真端表**没有 cs1_haction 列**
			// （全表 0 命中；对比 CollectItem 2 行带动作列）⇒ 0x35 槽的触发动作在真端数据里不存在，
			// 本车道按「未被服务的动作不播」冻结为休眠面，只留证据、不合成页动作；声明行本身也因
			// 中继/步物品/过场/交付门落在不路由长尾上（见上 resolvable 判定），运行期不上线。
			// The chain window (slot 0x1e) loads verbatim: nine rows declare con_quest and two of them
			// (13400/23400) target in-table quests (13401/23401). Two rows declare cutsceneid1 (859/860) for
			// slot 0x35, yet this retail table carries no cs1_haction column at all (0 hits vs. 2 in
			// CollectItem), so the slot's trigger action does not exist in the retail data: the face stays
			// dormant with evidence recorded and no synthesised page. Declaring rows also sit in the
			// unrouted long tail (see the resolvable check above).
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
			Integer targetAcquire = acquires.get(next);
			if (targetAcquire == null) {
				continue;
			}
			List<Integer> sourceRewards = rewards.get(questId);
			if (routed.contains(next)
					&& (sourceRewards == null || !sourceRewards.contains(targetAcquire))) {
				unresolvedChain.add(questId);
			}
		}

		this.acquireNpcByQuestId = Collections.unmodifiableMap(acquires);
		this.rewardNpcsByQuestId = Collections.unmodifiableMap(rewards);
		this.acceptGiveByQuestId = Collections.unmodifiableMap(acceptGives);
		this.playItemByQuestId = Collections.unmodifiableMap(playItems);
		this.advanceQuestIdsByItemId = Collections.unmodifiableMap(advanceByItem);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.unroutableQuestIds = Collections.unmodifiableSet(unroutable);
		this.unresolvedNames = Collections.unmodifiableSet(unresolved);
		this.unresolvedItemSymbols = Collections.unmodifiableSet(unresolvedItems);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
		this.unresolvedChainQuestIds = Collections.unmodifiableSet(unresolvedChain);
	}

	/**
	 * 真端 {@code con_quest}（链式接取窗的下一环，交付节点 0x1e 槽）；未声明返回 null。
	 * The retail {@code con_quest} column (hand-in slot 0x1e); the window is realized by the next quest's
	 * own accept route whenever that row acquires at this row's hand-in NPC.
	 */
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

	/** 接取 NPC。 / The acquire npc. */
	public Integer acquireNpc(int questId) {
		return acquireNpcByQuestId.get(questId);
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
		for (Map.Entry<Integer, Integer> entry : acquireNpcByQuestId.entrySet()) {
			if (!routedQuestIds.contains(entry.getKey())) {
				continue;
			}
			engine.registerQuestNpc(entry.getValue()).addOnQuestStart(entry.getKey());
			engine.registerQuestNpc(entry.getValue()).addOnTalkEvent(entry.getKey());
		}
		for (Map.Entry<Integer, List<Integer>> entry : rewardNpcsByQuestId.entrySet()) {
			if (!routedQuestIds.contains(entry.getKey())) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnTalkEvent(entry.getKey());
			}
		}
	}

	/**
	 * 用物演出推进（真端 {@code UseItem}）：START 态使用该行声明的道具一步进 REWARD。
	 * 未接取/待领奖时零副作用（本族接取走 NPC 对话，不由用物开窗）。
	 * <p>
	 * The retail item-play advance: using the declared item in START flips the row to REWARD in one
	 * step; other states are untouched (this family accepts at an npc, never through the item).
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
			state.setStatus(QuestStatus.REWARD);
			state.setPersistentState(PersistentState.UPDATE_REQUIRED);
			PacketSendUtility.sendPacket(player,
				new SM_QUEST_ACTION(questId, state.getStatus(), state.getQuestVars().getQuestVars()));
			handled = true;
		}
		return handled;
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
				if ((dialogId >= 8 && dialogId <= 23) || dialogId == 108
						|| (dialogId >= 110 && dialogId <= 124)) {
					int rewardIndex = dialogId >= 8 && dialogId <= 23 ? dialogId - 8 : 0;
					if (rewardFlow.claim(env, rewardIndex).completed()) {
						PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(objectId, PAGE_COMPLETE, questId));
						return true;
					}
				}
			}
			return false;
		}

		// START：推进事件是用物（真端演出），对话不推进；未满条件时给进行中页。
		// START: the advance event is the item use; a dialog never advances the row.
		if (status == QuestStatus.START) {
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)
					&& (dialogId == 31 || dialogId == 26 || dialogId == -1)) {
				PacketSendUtility.sendPacket(player,
					new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS, questId));
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
		Integer acquireNpc = acquireNpcByQuestId.get(questId);
		if (acquireNpc == null || acquireNpc != npcId) {
			return false;
		}
		if (dialogId == 31 || dialogId == 26) {
			PacketSendUtility.sendPacket(player,
				new SM_DIALOG_WINDOW(objectId, dialogContract.acceptEntryPage(questId), questId));
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
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
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
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(0, 0));
			return true;
		}
		if (dialogId == 1008) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS, questId));
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
		NativeNpcNameResolver.Match match = nameResolver.resolve(retailName);
		if (match.resolution() == NativeNpcNameResolver.Resolution.UNIQUE) {
			return List.of(match.npcIds().getFirst());
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
