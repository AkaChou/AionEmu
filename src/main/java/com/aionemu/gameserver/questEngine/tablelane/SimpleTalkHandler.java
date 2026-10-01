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
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleTalk 原生任务处理器（计划 §7 P3 步骤 2；**本步未接线**，族门未开）。
 * <p>
 * 完全由真端表 {@code Quest_SimpleTalk.xml}（{@link NativeQuestTableLoader.SimpleTalkRow}）驱动，
 * 不生成 IR 节点图、不经旧编译器。对话状态机按真端 DLL 的两段分派器还原：
 * <ul>
 *   <li>接取侧（真端 {@code cab520} 语义）：接取 NPC 的 QUEST_SELECT → 问询页 4；
 *       1002/20000 → {@code SetQuestAcquired} + 页 1003（20000 同时发放 {@code give_item}）；
 *       1003/1004/20001 → 页 1004；</li>
 *   <li>对话侧（真端 {@code cabb10} 语义）：中继 NPC 按 {@code talk_npc1..3} 步进，
 *       {@code 10000/10001/10002} → {@code SetQuestProgress(+0xf0)}(quest, 1/2/3)
 *       + {@code GiveItem}(give_itemK) + {@code RemoveItem}(remove_itemK)，
 *       步页 = SELECT2/SELECT3/SELECT4（1352/1693/2034）；乱序或重复的动作零推进；</li>
 *   <li>报告（真端 {@code cabb10} finalStep + {@code caad20} 完成门）：中继全满且交付门通过时，
 *       交付 NPC 的 1009 → 扣除工作物品 + REWARD + 奖励窗（页 5）；未满/未持有 → 进行中页 10；</li>
 *   <li>领奖：8..23 / 108 / 110+k → 结算并完成（页 1008）。</li>
 * </ul>
 * <p>
 * 交付门（{@code item_check=1}，1988 行）与工作物品按真端通道解析：{@code quest.xml} 的
 * {@code collect_item1..N} → {@code quest_work_item1..N} → 表内发放符号，解析失败即 fail-closed
 * （记入 {@link #unresolvedItemSymbols()}，报告门不放行）。物品id←符号名的解析复用
 * {@link RetailItemNameIndex}（两侧车道同一份物品名事实来源）。
 * <p>
 * Retail SimpleTalk native handler (plan §7 P3 step 2). Driven purely by the retail table; the
 * dialog state machine mirrors the DLL's two dispatchers (cab520 accept side / cabb10 dialog side),
 * and the inventory face of both dispatchers funnels through {@link NativeInventoryPort}. The
 * item_check hand-in gate resolves its work items through the retail channels (quest.xml
 * collect_item → quest_work_item → the row's own grant symbol) and fails closed when unresolved.
 */
public final class SimpleTalkHandler {

	/** 中继引用：任务 ID + 步号 (1..3) + 中继 NPC ID。 / Relay reference: quest id + step (1..3) + relay NPC id. */
	public record RelayStep(int questId, int step, int npcId) {
	}

	/** 物品栈：item_id + 数量。 / One item stack: id + count. */
	public record ItemStack(int itemId, int count) {
	}

	/** 接取问询页（真端 select1 之前的一步）。 / The accept ask page. */
	public static final int PAGE_ASK_ACCEPT = 4;
	/** 进行中（未满足报告门）页。 / In-progress page. */
	public static final int PAGE_IN_PROGRESS = 10;
	/** 奖励选择窗页。 / Reward window page. */
	public static final int PAGE_REWARD_WINDOW = 5;
	/** 接取确认页。 / Accept confirmation page. */
	public static final int PAGE_ACCEPTED = 1003;
	/** 拒绝页。 / Refuse page. */
	public static final int PAGE_REFUSED = 1004;
	/** 完成页。 / Completion page. */
	public static final int PAGE_COMPLETE = 1008;
	/** 中继步页（真端 SELECT2/SELECT3/SELECT4）。 / Relay step pages (retail SELECT2..4). */
	private static final int[] RELAY_STEP_PAGES = {1352, 1693, 2034};

	private static volatile SimpleTalkHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final NativeNpcNameResolver nameResolver;
	private final NativeInventoryPort inventory;

	private final Map<Integer, Integer> acquireNpcByQuestId;
	private final Map<Integer, Integer> rewardNpcByQuestId;
	/** 中继 NPC ID → 该 NPC 上的全部中继步。 / Relay NPC id → every relay step bound to it. */
	private final Map<Integer, List<RelayStep>> relaysByNpcId;
	/** 任务 ID → 中继步数（0 = 直交形）。 / Quest id → relay step count (0 = direct hand-in). */
	private final Map<Integer, Integer> relayCountByQuestId;
	/** 任务 ID → 接取侧发放（真端 give_item）。 / Quest id → accept-side grant (retail give_item). */
	private final Map<Integer, ItemStack> acceptGiveByQuestId;
	/** 任务 ID → 第 K 中继步的发放（下标 0..2，null = 无）。 / Quest id → step grants (index 0..2, null = none). */
	private final Map<Integer, List<ItemStack>> stepGiveByQuestId;
	/** 任务 ID → 第 K 中继步的扣除（下标 0..2，null = 无）。 / Quest id → step removals (index 0..2, null = none). */
	private final Map<Integer, List<ItemStack>> stepRemoveByQuestId;
	/** 任务 ID → item_check 交付门的工作物品（缺失 = 该行交付门未解析）。 / Quest id → work items of the item_check gate. */
	private final Map<Integer, List<ItemStack>> workItemsByQuestId;
	private final Set<Integer> ownedQuestIds;
	/** 唯一解析失败的 NPC 名（证据面）。 / NPC names that did not resolve uniquely (evidence surface). */
	private final Set<String> unresolvedNames;
	/** 未解析的物品符号（证据面；非空即报告门 fail-closed）。 / Unresolved item symbols (evidence surface). */
	private final Set<String> unresolvedItemSymbols;

	private SimpleTalkHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver) {
		this(tableLoader, nameResolver, retailItemIndex(), NativeQuestXmlTable.instance(), NativeInventoryPort.live());
	}

	SimpleTalkHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeQuestXmlTable questXml, NativeInventoryPort inventory) {
		this.tableLoader = tableLoader;
		this.nameResolver = nameResolver;
		this.inventory = inventory;

		Map<Integer, Integer> acquires = new LinkedHashMap<>();
		Map<Integer, Integer> rewards = new LinkedHashMap<>();
		Map<Integer, List<RelayStep>> relays = new LinkedHashMap<>();
		Map<Integer, Integer> relayCounts = new LinkedHashMap<>();
		Map<Integer, ItemStack> acceptGives = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepGives = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepRemoves = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> workItems = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<String> unresolved = new TreeSet<>();
		Set<String> unresolvedItems = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleTalkRow row : tableLoader.talkRows()) {
			int qid = row.questId();
			owned.add(qid);
			resolveInto(acquires, qid, row.acquiredNpcName(), unresolved);
			resolveInto(rewards, qid, row.rewardNpcName(), unresolved);
			List<String> talkNpcs = row.talkNpcNames();
			relayCounts.put(qid, talkNpcs.size());
			for (int index = 0; index < talkNpcs.size(); index++) {
				NativeNpcNameResolver.Match match = nameResolver.resolve(talkNpcs.get(index));
				if (match.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
					unresolved.add(talkNpcs.get(index));
					continue;
				}
				int npcId = match.npcIds().get(0);
				relays.computeIfAbsent(npcId, key -> new ArrayList<>())
						.add(new RelayStep(qid, index + 1, npcId));
			}

			ItemStack acceptGive = parseSymbol(row.acceptGiveItem(), qid, itemIndex, unresolvedItems);
			if (acceptGive != null) {
				acceptGives.put(qid, acceptGive);
			}
			List<ItemStack> stepGive = parseStepSymbols(row.stepGiveItems(), qid, itemIndex, unresolvedItems);
			List<ItemStack> stepRemove = parseStepSymbols(row.stepRemoveItems(), qid, itemIndex, unresolvedItems);
			if (stepGive.stream().anyMatch(java.util.Objects::nonNull)) {
				stepGives.put(qid, stepGive);
			}
			if (stepRemove.stream().anyMatch(java.util.Objects::nonNull)) {
				stepRemoves.put(qid, stepRemove);
			}

			if (row.itemCheck()) {
				List<ItemStack> gate = gateItems(qid, questXml, itemIndex, unresolvedItems);
				if (gate.isEmpty()) {
					// 回退：真端该行的发放符号（老链路的 workItemRequirement 同法）。
					ItemStack fallback = acceptGive != null ? acceptGive : lastNonNull(stepGive);
					if (fallback != null) {
						gate = List.of(fallback);
					}
				}
				if (gate.isEmpty()) {
					unresolvedItems.add("item_check:" + qid);
				} else {
					workItems.put(qid, gate);
				}
			}
		}

		this.acquireNpcByQuestId = Collections.unmodifiableMap(acquires);
		this.rewardNpcByQuestId = Collections.unmodifiableMap(rewards);
		this.relaysByNpcId = Collections.unmodifiableMap(relays);
		this.relayCountByQuestId = Collections.unmodifiableMap(relayCounts);
		this.acceptGiveByQuestId = Collections.unmodifiableMap(acceptGives);
		this.stepGiveByQuestId = Collections.unmodifiableMap(stepGives);
		this.stepRemoveByQuestId = Collections.unmodifiableMap(stepRemoves);
		this.workItemsByQuestId = Collections.unmodifiableMap(workItems);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.unresolvedNames = Collections.unmodifiableSet(unresolved);
		this.unresolvedItemSymbols = Collections.unmodifiableSet(unresolvedItems);
	}

	private static RetailItemNameIndex retailItemIndex() {
		try {
			return RetailItemNameIndex.loadItemTemplates();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("NATIVE_ITEM_INDEX_FAILED: " + e.getMessage(), e);
		}
	}

	/**
	 * item_check 交付门的工作物品：真端 quest.xml {@code collect_item1..N}（收集交付物）
	 * → {@code quest_work_item1..N}（工作物品通道，取首项，与老链路同法）。
	 * <p>
	 * The work items of the item_check hand-in gate: retail quest.xml collect_item1..N first,
	 * then the quest_work_item1..N channel (first entry, same rule as the retired lane).
	 */
	private static List<ItemStack> gateItems(int questId, NativeQuestXmlTable questXml,
			RetailItemNameIndex itemIndex, Set<String> unresolved) {
		NativeQuestXmlTable.QuestRow row = questXml.find(questId).orElse(null);
		if (row == null) {
			return List.of();
		}
		List<ItemStack> collected = parseSymbols(row.numbered("collect_item"), questId, itemIndex, unresolved);
		if (!collected.isEmpty()) {
			return collected;
		}
		List<ItemStack> work = parseSymbols(row.numbered("quest_work_item"), questId, itemIndex, unresolved);
		return work.isEmpty() ? List.of() : List.of(work.getFirst());
	}

	private static List<ItemStack> parseSymbols(List<String> symbols, int questId,
			RetailItemNameIndex itemIndex, Set<String> unresolved) {
		List<ItemStack> parsed = new ArrayList<>(symbols.size());
		for (String symbol : symbols) {
			ItemStack stack = parseSymbol(symbol, questId, itemIndex, unresolved);
			if (stack != null) {
				parsed.add(stack);
			}
		}
		return List.copyOf(parsed);
	}

	/** 逐下标解析中继步物品列（保持位置，null 表示该步无此操作）。 / Resolves a step-indexed column, keeping positions. */
	private static List<ItemStack> parseStepSymbols(List<String> symbols, int questId,
			RetailItemNameIndex itemIndex, Set<String> unresolved) {
		List<ItemStack> parsed = new ArrayList<>(symbols.size());
		for (String symbol : symbols) {
			parsed.add(parseSymbol(symbol, questId, itemIndex, unresolved));
		}
		return Collections.unmodifiableList(parsed);
	}

	/**
	 * 解析真端物品符号（形如 {@code ITEM_QUEST_1131A 1}）。
	 * Parses a retail item symbol ({@code NAME COUNT}); count defaults to 1.
	 */
	private static ItemStack parseSymbol(String symbol, int questId,
			RetailItemNameIndex itemIndex, Set<String> unresolved) {
		if (symbol == null || symbol.isBlank()) {
			return null;
		}
		String[] parts = symbol.trim().split("\\s+");
		int count;
		try {
			count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
		} catch (NumberFormatException e) {
			throw new IllegalStateException(
					"NATIVE_TABLE_PARSE_FAILED: quest " + questId + " has a non-numeric item count in " + symbol);
		}
		// 真端 quest.xml/表用 {@code ITEM_} 前缀的符号名，物品模板的 name_desc 去掉该前缀
		// （如 ITEM_QUEST_1131A ↔ quest_1131a = 182200506）；与老链路 resolveItemId 同法。
		String stem = parts[0].toLowerCase(java.util.Locale.ROOT);
		if (stem.startsWith("item_")) {
			stem = stem.substring("item_".length());
		}
		Integer itemId = itemIndex.resolve(stem);
		if (itemId == null) {
			unresolved.add(symbol);
			return null;
		}
		return new ItemStack(itemId, count);
	}

	private static ItemStack lastNonNull(List<ItemStack> stacks) {
		for (int index = stacks.size() - 1; index >= 0; index--) {
			if (stacks.get(index) != null) {
				return stacks.get(index);
			}
		}
		return null;
	}

	private void resolveInto(Map<Integer, Integer> target, int questId, String npcName, Set<String> unresolved) {
		NativeNpcNameResolver.Match match = nameResolver.resolve(npcName);
		if (match.resolution() != NativeNpcNameResolver.Resolution.UNIQUE) {
			unresolved.add(npcName);
			return;
		}
		target.put(questId, match.npcIds().get(0));
	}

	public static SimpleTalkHandler instance() {
		SimpleTalkHandler local = instance;
		if (local == null) {
			synchronized (SimpleTalkHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleTalkHandler(NativeQuestTableLoader.instance(),
							NativeNpcNameResolver.instance());
					instance = local;
				}
			}
		}
		return local;
	}

	/**
	 * 启动期把本族全部 NPC 标记注册进任务引擎（真端 codegen 的静态注册表等价物）。
	 * Registers the family's NPC marks into the engine at startup (the retail codegen registry equivalent).
	 */
	public void installInterest(QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, Integer> entry : acquireNpcByQuestId.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnQuestStart(questId);
			engine.registerQuestNpc(npcId).addOnTalkEvent(questId);
		}
		for (Map.Entry<Integer, Integer> entry : rewardNpcByQuestId.entrySet()) {
			int questId = entry.getKey();
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnTalkEvent(questId);
		}
		for (Map.Entry<Integer, List<RelayStep>> entry : relaysByNpcId.entrySet()) {
			int npcId = entry.getKey();
			for (RelayStep relay : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnTalkEvent(relay.questId());
			}
		}
	}

	/** 判断是否拥有该任务。 / Checks whether this handler owns the quest. */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/** 拥有的任务 ID 集合。 / Managed quest ids. */
	public Set<Integer> ownedQuestIds() {
		return ownedQuestIds;
	}

	/** 拥有任务数。 / Managed quest count. */
	public int ownedQuestCount() {
		return ownedQuestIds.size();
	}

	/** 未唯一解析的 NPC 名（冻结证据面）。 / NPC names without a unique resolution (frozen evidence). */
	public Set<String> unresolvedNames() {
		return unresolvedNames;
	}

	/** 未解析的物品符号或交付门（冻结证据面）。 / Unresolved item symbols or gates (frozen evidence). */
	public Set<String> unresolvedItemSymbols() {
		return unresolvedItemSymbols;
	}

	/** 接取 NPC。 / The acquire NPC. */
	public Integer acquireNpc(int questId) {
		return acquireNpcByQuestId.get(questId);
	}

	/** 交付 NPC。 / The reward NPC. */
	public Integer rewardNpc(int questId) {
		return rewardNpcByQuestId.get(questId);
	}

	/** 任务的中继步数（0 = 直交形）。 / Relay step count (0 = direct hand-in). */
	public int relayCount(int questId) {
		return relayCountByQuestId.getOrDefault(questId, 0);
	}

	/** 指定 NPC 上的中继步（无则空表）。 / Relay steps bound to the NPC. */
	public List<RelayStep> relaysForNpc(int npcId) {
		return relaysByNpcId.getOrDefault(npcId, List.of());
	}

	/** 接取侧发放（真端 give_item）。 / Accept-side grant (retail give_item). */
	public ItemStack acceptGiveItem(int questId) {
		return acceptGiveByQuestId.get(questId);
	}

	/** 第 K 中继步（K=1..3）的发放；无则 null。 / The step-K grant, or null. */
	public ItemStack stepGiveItem(int questId, int step) {
		return stepAt(stepGiveByQuestId.get(questId), step);
	}

	/** 第 K 中继步（K=1..3）的扣除；无则 null。 / The step-K removal, or null. */
	public ItemStack stepRemoveItem(int questId, int step) {
		return stepAt(stepRemoveByQuestId.get(questId), step);
	}

	private static ItemStack stepAt(List<ItemStack> stacks, int step) {
		if (stacks == null || step < 1 || step > stacks.size()) {
			return null;
		}
		return stacks.get(step - 1);
	}

	/** item_check 交付门的工作物品（空 = 无门）。 / The item_check gate items (empty = no gate). */
	public List<ItemStack> workItems(int questId) {
		return workItemsByQuestId.getOrDefault(questId, List.of());
	}

	/** 真端行（缺行 fail-closed）。 / The retail row (missing rows fail closed). */
	public NativeQuestTableLoader.SimpleTalkRow requireRow(int questId) {
		return tableLoader.requireTalk(questId);
	}

	/** 中继步对应的页 id（真端 SELECT2..4）。 / The page id of a relay step (retail SELECT2..4). */
	public static int pageForStep(int step) {
		if (step < 1 || step > RELAY_STEP_PAGES.length) {
			throw new IllegalArgumentException("relay step out of range: " + step);
		}
		return RELAY_STEP_PAGES[step - 1];
	}

	/**
	 * 处理 NPC 对话（接取 / 中继步进 / 报告 / 领奖）。
	 * Handles a dialog event (accept, relay step, report, reward).
	 *
	 * @param env 任务环境 / Quest environment
	 * @return 是否有处理器接管 / Whether a handler took over
	 */
	public boolean onDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		if (!owns(questId)) {
			return false;
		}
		Npc npc = env.getVisibleObject() instanceof Npc n ? n : null;
		int npcId = npc != null ? npc.getNpcId() : 0;
		int targetObjectId = npc != null ? npc.getObjectId() : 0;
		int dialogId = env.getDialogId();

		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = qs != null ? qs.getStatus() : QuestStatus.NONE;

		// 1. 接取（真端 cab520）：接取 NPC 的问询 → 确认（20000 同时发放 give_item）/ 拒绝。
		if (qs == null || status == QuestStatus.NONE) {
			Integer acquireNpc = acquireNpcByQuestId.get(questId);
			if (acquireNpc == null || acquireNpc != npcId) {
				return false;
			}
			if (dialogId == 31 || dialogId == 26) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, PAGE_ASK_ACCEPT, questId));
				return true;
			}
			if (dialogId == 1002 || dialogId == 20000) {
				if (QuestService.startQuest(env)) {
					if (dialogId == 20000) {
						give(player, acceptGiveByQuestId.get(questId));
					}
					PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(targetObjectId, PAGE_ACCEPTED, questId));
					return true;
				}
				return false;
			}
			if (dialogId == 1003 || dialogId == 1004 || dialogId == 20001) {
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, PAGE_REFUSED, questId));
				return true;
			}
			return false;
		}

		if (status == QuestStatus.START) {
			int vars = qs.getQuestVars().getQuestVars();
			// 2. 中继步（真端 cabb10）：只推进「当前步」，乱序/重复零副作用；步进即发放/扣除该步物品。
			for (RelayStep relay : relaysForNpc(npcId)) {
				if (relay.questId() != questId) {
					continue;
				}
				int step = relay.step();
				int action = 10000 + step - 1;
				if (dialogId == action) {
					if (vars == step - 1) {
						qs.getQuestVars().setVar(step);
						qs.setPersistentState(PersistentState.UPDATE_REQUIRED);
						PacketSendUtility.sendPacket(player,
								new SM_QUEST_ACTION(questId, QuestStatus.START, step));
						give(player, stepGiveItem(questId, step));
						remove(player, stepRemoveItem(questId, step));
					}
					int page = vars == step - 1 ? pageForStep(step) : pageForStep(Math.max(1, vars));
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, page, questId));
					return true;
				}
				if (dialogId == 31 || dialogId == 26) {
					int page = vars >= step ? pageForStep(step) : PAGE_IN_PROGRESS;
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, page, questId));
					return true;
				}
			}
			// 3. 报告（真端 cabb10 finalStep + caad20 完成门）：中继全满且交付门通过才开奖励窗。
			Integer rewardNpc = rewardNpcByQuestId.get(questId);
			if (rewardNpc != null && rewardNpc == npcId) {
				if (dialogId == 1009 || dialogId == 31 || dialogId == 26 || dialogId == -1) {
					if (vars >= relayCount(questId) && holdsGateItems(questId, player)) {
						removeGateItems(questId, player);
						qs.setStatus(QuestStatus.REWARD);
						qs.setPersistentState(PersistentState.UPDATE_REQUIRED);
						PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, QuestStatus.REWARD, vars));
						PacketSendUtility.sendPacket(player,
								new SM_DIALOG_WINDOW(targetObjectId, PAGE_REWARD_WINDOW, questId));
						return true;
					}
					PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(targetObjectId, PAGE_IN_PROGRESS, questId));
					return true;
				}
			}
			return false;
		}

		// 4. 领奖：交付 NPC 的奖励窗与结算档位。
		if (status == QuestStatus.REWARD) {
			Integer rewardNpc = rewardNpcByQuestId.get(questId);
			if (rewardNpc != null && rewardNpc == npcId) {
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					PacketSendUtility.sendPacket(player,
							new SM_DIALOG_WINDOW(targetObjectId, PAGE_REWARD_WINDOW, questId));
					return true;
				}
				if ((dialogId >= 8 && dialogId <= 23) || dialogId == 108
						|| (dialogId >= 110 && dialogId <= 124)) {
					int rewardIndex = (dialogId >= 8 && dialogId <= 23) ? (dialogId - 8) : 0;
					if (QuestService.finishQuest(env, rewardIndex)) {
						PacketSendUtility.sendPacket(player,
								new SM_DIALOG_WINDOW(targetObjectId, PAGE_COMPLETE, questId));
						return true;
					}
				}
			}
			return false;
		}

		return false;
	}

	/** 交付门（item_check）持有量检查：缺一即不放行。 / The item_check hold gate; a single shortfall holds it. */
	private boolean holdsGateItems(int questId, Player player) {
		for (ItemStack item : workItems(questId)) {
			if (inventory.count(player, item.itemId()) < item.count()) {
				return false;
			}
		}
		return true;
	}

	private void removeGateItems(int questId, Player player) {
		for (ItemStack item : workItems(questId)) {
			inventory.remove(player, item.itemId(), item.count());
		}
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
}
