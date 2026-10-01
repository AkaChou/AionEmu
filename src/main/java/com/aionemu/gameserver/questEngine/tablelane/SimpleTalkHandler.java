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
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.services.QuestService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleTalk 原生任务处理器（计划 §7 P3 步骤 2；**本步未接线**，族门未开）。
 * <p>
 * 完全由真端表 {@code Quest_SimpleTalk.xml}（{@link NativeQuestTableLoader.SimpleTalkRow}）驱动，
 * 不生成 IR 节点图、不经旧编译器。对话状态机按真端 DLL 的两段分派器还原：
 * <ul>
 *   <li>接取侧（真端 {@code cab520} 语义）：接取 NPC 的 QUEST_SELECT → 接取入口页
 *       （真端表只有 NPC/物品列、没有页列，故取客户端任务页声明的可渲染页：
 *       {@code ask_quest_accept}(4) → {@code select_none}(4762) → {@code select1}(1011)，见
 *       {@link QuestDialogContract#acceptEntryPage(int)}；{@code select1} 首屏的 1012/1013
 *       翻页动作按真端 cab520「原样回发」）；
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
 * 另外三条表声明面同样按真端还原：
 * <ul>
 *   <li>{@code _faction_} 等接取哨兵（真端系统发放）：本类只提供发放判定与发放入口，
 *       由发放子系统（NPC 阵营日常轮换）调用，见 {@link #isSystemGranted(int)} /
 *       {@link #factionRotationCandidates(int)} / {@link #grantSystemStart(Player, int)}；</li>
 *   <li>{@code cutsceneid1}/{@code cs1_haction} 过场（真端槽 0x35 PlayMovie）：动作命中触发行时
 *       经 {@link NativeMoviePort} 播放，见 {@link #cutscene(int)}；</li>
 *   <li>旧存档任务书行自愈（真端编译边登记 P0c-28）：{@code REWARD} 态进入世界时把异常行值修回
 *       真端投影行，见 {@link #onEnterWorld(Player)}。</li>
 * </ul>
 * <p>
 * 路由集 = 真端表行 **减去** XML-only 行（XML 定义仍在 = XML 车道 owns，native 不路由，
 * 见 {@link #routes(int)}）：表行与 XML 定义的交集不再是双主，而是「XML 保留」的显式结论。
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

	/** 过场引用：movie id + 触发动作 id（{@code cs1_haction}；-1 = 表未声明触发）。 */
	public record Cutscene(int movieId, int triggerAction) {
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

	/** 任务 ID → 真端 {@code con_quest}（链式接取窗的下一环；无声明则缺席）。 / Quest id → retail {@code con_quest}. */
	private final Map<Integer, Integer> conQuestByQuestId;
	/**
	 * 链式接取窗未在本行交付 NPC 上闭环的行（fail-closed 证据面）。
	 * Rows whose chain accept window is not realized at this row's reward NPC (fail-closed surface).
	 */
	private final Set<Integer> unresolvedChainQuestIds;

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
	/** 交付门声明了物品但无法全部解析的行（fail-closed：报告门永不放行）。 / Rows whose declared gate cannot be resolved. */
	private final Set<Integer> unresolvedGateQuestIds;
	/** 任务 ID → 接取名类别（{@code _faction_} 等哨兵 = 系统发放）。 / Quest id → acquire-name category. */
	private final Map<Integer, RetailGrantKind> grantKindByQuestId;
	/** 任务 ID → 真端势力 id（{@code quest.xml npcfaction_name}；无则 0）。 / Quest id → retail faction id. */
	private final Map<Integer, Integer> factionByQuestId;
	/** 任务 ID → 过场引用（表 {@code cutsceneid1}/{@code cs1_haction}）。 / Quest id → cutscene reference. */
	private final Map<Integer, Cutscene> cutsceneByQuestId;
	/** 表行全量（注册集）。 / Every table row (the registration set). */
	private final Set<Integer> ownedQuestIds;
	/** 路由集 = 注册集 − XML-only 行。 / The routing set: registration set minus XML-owned rows. */
	private final Set<Integer> routedQuestIds;
	private final NativeMoviePort moviePort;
	/** 客户端任务页契约：接取入口页与 select1 续页的唯一取数面。 / Client task-page contract for entry pages. */
	private final QuestDialogContract dialogContract;
	/** 唯一解析失败的 NPC 名（证据面）。 / NPC names that did not resolve uniquely (evidence surface). */
	private final Set<String> unresolvedNames;
	/** 未解析的物品符号（证据面；非空即报告门 fail-closed）。 / Unresolved item symbols (evidence surface). */
	private final Set<String> unresolvedItemSymbols;

	private SimpleTalkHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver) {
		this(tableLoader, nameResolver, retailItemIndex(), NativeQuestXmlTable.instance(), NativeInventoryPort.live(),
				NativeQuestOwnerResolver.instance().xmlOnlyIds(), NativeMoviePort.live());
	}

	SimpleTalkHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeQuestXmlTable questXml, NativeInventoryPort inventory) {
		this(tableLoader, nameResolver, itemIndex, questXml, inventory,
				NativeQuestOwnerResolver.instance().xmlOnlyIds(), NativeMoviePort.live());
	}

	SimpleTalkHandler(NativeQuestTableLoader tableLoader, NativeNpcNameResolver nameResolver,
			RetailItemNameIndex itemIndex, NativeQuestXmlTable questXml, NativeInventoryPort inventory,
			Set<Integer> xmlOwnedIds, NativeMoviePort moviePort) {
		this.tableLoader = tableLoader;
		this.nameResolver = nameResolver;
		this.inventory = inventory;
		this.moviePort = moviePort;
		this.dialogContract = QuestDialogContract.loadDefault();

		Map<Integer, Integer> acquires = new LinkedHashMap<>();
		Map<Integer, Integer> rewards = new LinkedHashMap<>();
		Map<Integer, List<RelayStep>> relays = new LinkedHashMap<>();
		Map<Integer, Integer> relayCounts = new LinkedHashMap<>();
		Map<Integer, ItemStack> acceptGives = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepGives = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> stepRemoves = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> workItems = new LinkedHashMap<>();
		Map<Integer, RetailGrantKind> grantKinds = new LinkedHashMap<>();
		Map<Integer, Integer> factions = new LinkedHashMap<>();
		Map<Integer, Cutscene> cutscenes = new LinkedHashMap<>();
		Set<Integer> unresolvedGates = new TreeSet<>();
		Map<Integer, Integer> conQuests = new LinkedHashMap<>();
		Set<Integer> unresolvedChain = new TreeSet<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Set<String> unresolved = new TreeSet<>();
		Set<String> unresolvedItems = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleTalkRow row : tableLoader.talkRows()) {
			int qid = row.questId();
			owned.add(qid);
			if (!xmlOwnedIds.contains(qid)) {
				routed.add(qid);
			}
			grantKinds.put(qid, RetailGrantKind.of(row.acquiredNpcName()));
			int factionId = NativeNpcFactionNames.idOf(
					questXml.find(qid).map(meta -> meta.text("npcfaction_name")).orElse(""));
			if (factionId != 0) {
				factions.put(qid, factionId);
			}
			if (row.cutsceneId() != null) {
				cutscenes.put(qid, new Cutscene(row.cutsceneId(),
						row.cutsceneAction() == null ? -1 : row.cutsceneAction()));
			}
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
				// 逐行判定（不得用全局集合增量：同一未解符号在首行登记后，后续同形行会漏判为可解）。
				Set<String> gateUnresolved = new TreeSet<>();
				List<ItemStack> gate = gateItems(qid, questXml, itemIndex, gateUnresolved);
				unresolvedItems.addAll(gateUnresolved);
				boolean gateSymbolsFailed = !gateUnresolved.isEmpty();
				if (gate.isEmpty()) {
					// 回退：真端该行的发放符号（老链路的 workItemRequirement 同法）。
					ItemStack fallback = acceptGive != null ? acceptGive : lastNonNull(stepGive);
					if (fallback != null) {
						gate = List.of(fallback);
					}
				}
				if (gate.isEmpty() || gateSymbolsFailed) {
					// 门声明了物品但无法全部解析 ⇒ 报告门永不放行（fail-closed），不按可解子集放行；
					// 门通道全缺⇒同样 fail-closed（行级事实由 unresolvedGate 承载，不混入符号证据面）。
					unresolvedGates.add(qid);
				} else {
					workItems.put(qid, gate);
				}
			}
		}

		// 真端 0x1e 槽（交付 NPC 节点）：接续下一任务 {@code con_quest} 的接取窗。本车道的接取路由按
		// NPC 建表，故该窗的等价物 = 「下一环的接取 NPC 恰是本行的交付 NPC」。逐行验证并把不闭环的
		// 行登记为 fail-closed 证据（不新增第二套路由：下一环的接取路由永远由它自己那一行提供）。
		// Retail slot 0x1e (on the reward-NPC node) opens the next quest's accept window. This lane keys
		// accept routes by NPC, so the equivalent is "the next quest acquires at this row's reward NPC";
		// every row is verified and non-closing rows are recorded as fail-closed evidence.
		for (NativeQuestTableLoader.SimpleTalkRow row : tableLoader.talkRows()) {
			Integer next = row.conQuest();
			if (next == null) {
				continue;
			}
			int questId = row.questId();
			conQuests.put(questId, next);
			Integer targetAcquire = acquires.get(next);
			if (routed.contains(next) && targetAcquire != null
					&& !targetAcquire.equals(rewards.get(questId))) {
				unresolvedChain.add(questId);
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
		this.unresolvedGateQuestIds = Collections.unmodifiableSet(unresolvedGates);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
		this.unresolvedChainQuestIds = Collections.unmodifiableSet(unresolvedChain);
		this.grantKindByQuestId = Collections.unmodifiableMap(grantKinds);
		this.factionByQuestId = Collections.unmodifiableMap(factions);
		this.cutsceneByQuestId = Collections.unmodifiableMap(cutscenes);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
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
		// 两个通道各有一套统一约定（真端表事实，2026-10-01 全量复算）：SimpleTalk 表 give/remove 列
		// 663 个符号全为 {@code ITEM_X} 形式；quest.xml collect_item_/quest_work_item 列 3394 个符号
		// 全为原名形式，其中含 {@code item_*} 真名（如 item_idunderrune_quest_01）。故先按原名查，
		// 未命中再按 {@code ITEM_} 前缀别名重查；3145 个去重符号两步规则 0 冲突 0 未解。
		// （真端运行期不做名字解析：thunk 内是离线 codegen 解析好的数值 id。）
		String stem = parts[0].toLowerCase(java.util.Locale.ROOT);
		Integer itemId = itemIndex.resolve(stem);
		if (itemId == null && stem.startsWith("item_")) {
			itemId = itemIndex.resolve(stem.substring("item_".length()));
		}
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
			if (!routedQuestIds.contains(questId)) {
				continue;
			}
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnQuestStart(questId);
			engine.registerQuestNpc(npcId).addOnTalkEvent(questId);
		}
		for (Map.Entry<Integer, Integer> entry : rewardNpcByQuestId.entrySet()) {
			int questId = entry.getKey();
			if (!routedQuestIds.contains(questId)) {
				continue;
			}
			int npcId = entry.getValue();
			engine.registerQuestNpc(npcId).addOnTalkEvent(questId);
		}
		for (Map.Entry<Integer, List<RelayStep>> entry : relaysByNpcId.entrySet()) {
			int npcId = entry.getKey();
			for (RelayStep relay : entry.getValue()) {
				if (routedQuestIds.contains(relay.questId())) {
					engine.registerQuestNpc(npcId).addOnTalkEvent(relay.questId());
				}
			}
		}
	}

	/** 判断是否拥有该任务（注册集）。 / Checks whether this handler owns the quest (registration set). */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/**
	 * 判断该任务是否由 native 车道**路由**（注册集 − XML-only 行）。
	 * 与 {@link #owns(int)} 的区别：XML 定义仍在的任务由 XML 车道 owns，native 只注册不路由（单 owner）。
	 * Whether the native lane routes this quest (registration set minus XML-owned rows).
	 */
	public boolean routes(int questId) {
		return routedQuestIds.contains(questId);
	}

	/** 路由集（不变量：与 XML-only 集交集为空）。 / The routing set (disjoint from the XML-owned set). */
	public Set<Integer> routedQuestIds() {
		return routedQuestIds;
	}

	/** 接取名类别（真端哨兵 = 系统发放）。 / Acquire-name category (a retail sentinel means system-granted). */
	public RetailGrantKind grantKind(int questId) {
		return grantKindByQuestId.getOrDefault(questId, RetailGrantKind.NPC);
	}

	/**
	 * 是否系统发放（无 NPC 接取路由，且该类别在本服确有发放入口）。
	 * <p>
	 * 判据用 {@link RetailGrantKind#grantable()} 而非 {@code knownGrant()}：{@code _challengetask_}
	 * 在本服只有完成回调、没有受理入口，若按「已知哨兵」放行，{@code grantSystemStart} 会替它建档，
	 * 与「挑战任务行不得被系统发放」的既定合同相反（P3 步骤 4 由 native 侧契约门抓出）。
	 * System-grant verdict, keyed on {@link RetailGrantKind#grantable()} so that {@code _challengetask_}
	 * rows (no intake in this server) can never be admitted by {@code grantSystemStart}.
	 */
	public boolean isSystemGranted(int questId) {
		RetailGrantKind kind = grantKind(questId);
		return routes(questId) && kind != RetailGrantKind.NPC && kind.grantable();
	}

	/** 真端势力 id（quest.xml {@code npcfaction_name}；无则 0）。 / The retail faction id, or 0. */
	public int factionId(int questId) {
		return factionByQuestId.getOrDefault(questId, 0);
	}

	/**
	 * 指定势力的当前可轮换任务 id（真端 {@code _faction_} 行 ∩ 路由集）。
	 * Faction-rotation candidates of one faction: routed rows whose acquire name is {@code _faction_}.
	 */
	public Set<Integer> factionRotationCandidates(int factionId) {
		Set<Integer> candidates = new TreeSet<>();
		for (Map.Entry<Integer, Integer> entry : factionByQuestId.entrySet()) {
			int questId = entry.getKey();
			if (entry.getValue() == factionId && routes(questId)
					&& grantKind(questId) == RetailGrantKind.FACTION) {
				candidates.add(questId);
			}
		}
		return Collections.unmodifiableSet(candidates);
	}

	/** 过场引用（表未声明返回 null）。 / The cutscene reference (null when the row declares none). */
	public Cutscene cutscene(int questId) {
		return cutsceneByQuestId.get(questId);
	}

	/** 交付门是否因物品数据缺口而 fail-closed。 / Whether the hand-in gate fails closed on an item data gap. */
	public boolean unresolvedGate(int questId) {
		return unresolvedGateQuestIds.contains(questId);
	}

	/**
	 * 真端 {@code con_quest}（链式接取窗的下一环）；未声明返回 null。
	 * <p>
	 * 真端该列由交付 NPC 节点上的 0x1e 槽消费（{@code mgr+0x1a8(player, con_quest)} = 下一环的接取窗）。
	 * 本车道按 NPC 建接取路由，故只要下一环的接取 NPC 等于本行的交付 NPC，该窗即已由下一环自身那一行
	 * 实现；{@link #unresolvedChainQuestIds()} 为空即全表闭环。
	 * <p>
	 * The retail {@code con_quest} column, consumed by slot 0x1e on the reward-NPC node ({@code
	 * mgr+0x1a8(player, con_quest)} opens the next quest's accept window). This lane registers accept
	 * routes per NPC, so the window is already realized by the next quest's own row whenever that row
	 * acquires at this row's reward NPC; an empty {@link #unresolvedChainQuestIds()} means the whole
	 * table closes.
	 */
	public Integer conQuest(int questId) {
		return conQuestByQuestId.get(questId);
	}

	/** 链式接取窗未闭环的行（fail-closed 证据面）。 / Rows whose chain window is not realized. */
	public Set<Integer> unresolvedChainQuestIds() {
		return unresolvedChainQuestIds;
	}

	/**
	 * 系统发放入口（{@code _faction_} 等哨兵行）：无进度时直接进 START，等价于旧
	 * {@code RetailSystemGrantDispatcher} 对 SystemGrant 边的处理。
	 * System-grant entry: starts the quest directly when it has no progress.
	 *
	 * @return 实际发放成功时为 true / true when the quest was granted
	 */
	public boolean grantSystemStart(Player player, int questId) {
		if (player == null || !isSystemGranted(questId)) {
			return false;
		}
		QuestState existing = player.getQuestStateList().getQuestState(questId);
		if (existing != null && existing.getStatus() != QuestStatus.NONE) {
			return false;
		}
		// 系统发放：资格已由 factionRotationEligible 按同一 quest.xml 轴判定，此处只做状态面建档。
		return NativeQuestStartPort.instance().grant(player, questId).started();
	}

	/**
	 * 阵营日常轮换的 native 资格判定（真端 {@code quest.xml} 轴：势力/等级/种族/职业/性别/可重复）。
	 * <p>
	 * typed 元数据在本族切走后不复存在，因此上层的 {@code PlayerQuestStartEligibilityPort} 会以
	 * {@code QUEST_METADATA_MISSING} 拒绝；本方法是同一判据在 native 车道的直读实现（不构造 IR 元数据）。
	 * Native eligibility for the faction daily rotation, read straight from the retail quest.xml axes.
	 * The typed eligibility port cannot serve switched families (no metadata), so the same predicates are
	 * evaluated here without synthesizing IR metadata.
	 */
	public boolean factionRotationEligible(Player player, int questId, int factionId) {
		if (player == null || !isSystemGranted(questId) || grantKind(questId) != RetailGrantKind.FACTION
				|| factionId <= 0 || factionId(questId) != factionId) {
			return false;
		}
		var factions = player.getNpcFactions();
		var faction = factions == null ? null : factions.getNpcFactionById(factionId);
		if (faction == null || !faction.isActive()) {
			return false;
		}
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		if (row == null || player.getQuestStateList() == null) {
			return false;
		}
		QuestState existing = player.getQuestStateList().getQuestState(questId);
		boolean repeatable = intOrZero(row, "max_repeat_count") > 0;
		if (existing != null && existing.getStatus() != QuestStatus.NONE
				&& existing.getStatus() != QuestStatus.LOCKED && !repeatable) {
			return false;
		}
		// 轴判定与 NPC 接取共用同一实现（NativeQuestStartPort），避免第二套事实来源。
		int minLevel = intOrZero(row, "minlevel_permitted");
		int maxLevel = intOrDefault(row, "maxlevel_permitted", Integer.MAX_VALUE);
		int level = player.getLevel();
		if (minLevel != 999 && level < minLevel) {
			// 系统发放的阵营日常不走 NPC 接取（真端 CanAcquireQuest 不参与），故 999 在此不阻断。
			return false;
		}
		if (level > maxLevel) {
			return false;
		}
		if (!NativeQuestStartPort.racePermitted(row.text("race_permitted"),
				player.getRace() == null ? null : player.getRace().name())) {
			return false;
		}
		String classToken = player.getCommonData() == null || player.getCommonData().getPlayerClass() == null
				? null
				: player.getCommonData().getPlayerClass().name().toUpperCase(java.util.Locale.ROOT);
		// 职业轴与 NPC 接取/生产元数据同源（真端 token → PlayerClass）。
		// The class axis shares the retail token mapping used by NPC acquisition and quest metadata.
		java.util.Set<String> permittedClasses = RetailQuestMetadataCompiler.permittedClassNames(
				row.text("class_permitted"), minLevel);
		if (!permittedClasses.isEmpty() && (classToken == null || !permittedClasses.contains(classToken))) {
			return false;
		}
		String genderToken = player.getGender() == null ? null
				: player.getGender().name().toLowerCase(java.util.Locale.ROOT);
		return NativeQuestStartPort.tokenPermitted(row.text("gender_permitted"), genderToken);
	}

	private static int intOrZero(NativeQuestXmlTable.QuestRow row, String tag) {
		Integer value = row.integer(tag);
		return value == null ? 0 : value;
	}

	private static int intOrDefault(NativeQuestXmlTable.QuestRow row, String tag, int fallback) {
		Integer value = row.integer(tag);
		return value == null ? fallback : value;
	}

	/**
	 * 进世界自愈（真端 P0c-28 旧存档修复边的 native 等价物）：
	 * <ul>
	 *   <li>链形行（中继 ≥1）在 {@code REWARD} 态且 vars=0（XML 时代存档）→ vars = 中继步数（真端投影行）；</li>
	 *   <li>单步行在 {@code REWARD} 态且 vars=1（1 基行号残留）→ vars = 0（真端投影在行 0）。</li>
	 * </ul>
	 * Enter-world save heal for the native lane (the retail-table equivalent of the P0c-28 heal edges).
	 *
	 * @return 是否有行被修复 / whether any row was healed
	 */
	public boolean onEnterWorld(Player player) {
		if (player == null || player.getQuestStateList() == null) {
			return false;
		}
		boolean healed = false;
		for (int questId : routedQuestIds) {
			QuestState state = player.getQuestStateList().getQuestState(questId);
			if (state == null || state.getStatus() != QuestStatus.REWARD) {
				continue;
			}
			int vars = state.getQuestVars().getQuestVars();
			int relayCount = relayCount(questId);
			int healedVars = relayCount > 0 ? (vars == 0 ? relayCount : -1) : (vars == 1 ? 0 : -1);
			if (healedVars < 0) {
				continue;
			}
			state.getQuestVars().setVar(healedVars);
			state.setPersistentState(PersistentState.UPDATE_REQUIRED);
			PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(questId, QuestStatus.REWARD, healedVars));
			healed = true;
		}
		return healed;
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
		boolean handled = handleDialog(env);
		if (handled) {
			playCutsceneIfTriggered(env.getPlayer(), env.getQuestId(), env.getDialogId());
		}
		return handled;
	}

	/**
	 * 真端过场（槽 0x35 PlayMovie）：动作命中 {@code cs1_haction} 时按 CUTSCENE 类型下发，
	 * 是状态机之外的副作用（不推进节点）。 / Retail cutscene: sent as a side effect when the
	 * client action matches cs1_haction; it never advances the node.
	 */
	private void playCutsceneIfTriggered(Player player, int questId, int dialogId) {
		Cutscene cutscene = cutsceneByQuestId.get(questId);
		if (cutscene != null && cutscene.triggerAction() == dialogId) {
			moviePort.play(player, cutscene.movieId());
		}
	}

	private boolean handleDialog(QuestEnv env) {
		if (env == null || env.getPlayer() == null) {
			return false;
		}
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		if (!routes(questId)) {
			return false;
		}
		Npc npc = env.getVisibleObject() instanceof Npc n ? n : null;
		int npcId = npc != null ? npc.getNpcId() : 0;
		int targetObjectId = npc != null ? npc.getObjectId() : 0;
		int dialogId = env.getDialogId();

		QuestState qs = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = qs != null ? qs.getStatus() : QuestStatus.NONE;

		// 1. 接取（真端 cab520）：接取 NPC 的问询 → 确认（20000 同时发放 give_item）/ 拒绝。
		// 可重复行在 COMPLETE 态同样开放接取窗（真端 finishedcount < max_repeat_count 时再次可接）。
		boolean fresh = qs == null || status == QuestStatus.NONE;
		if (fresh || (status == QuestStatus.COMPLETE && repeatable(questId))) {
			Integer acquireNpc = acquireNpcByQuestId.get(questId);
			if (acquireNpc == null || acquireNpc != npcId) {
				return false;
			}
			if (dialogId == 31 || dialogId == 26) {
				// 接取入口页 = 客户端任务页声明的可渲染页（真端表无页列）。
				// The accept entry page is the page the client task HTML declares.
				PacketSendUtility.sendPacket(player,
						new SM_DIALOG_WINDOW(targetObjectId, dialogContract.acceptEntryPage(questId), questId));
				return true;
			}
			// select1 续页翻页（真端 cab520 对 1012/1013 原样回发）；客户端未声明该页即 fail-closed。
			// select1 page turns (cab520 echoes 1012/1013); undeclared pages fail closed.
			if (dialogId == QuestDialogPage.SELECT1_1.id() || dialogId == QuestDialogPage.SELECT1_1_1.id()) {
				if (!dialogContract.hasButtonPage(questId, dialogId)) {
					return false;
				}
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(targetObjectId, dialogId, questId));
				return true;
			}
			if (dialogId == 1002 || dialogId == 20000) {
				// 真端接取：条件判定 + 建档/复位走 native 状态端口（不依赖 typed QuestTemplate）。
				if (NativeQuestStartPort.instance().start(player, questId).started()) {
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

		// 表声明的过场触发动作（真端把 movie 挂在页动作上，如 SELECT2_1/SELECT3_1/QUEST_REFUSE_4）：
		// 不推进状态，但必须被服务（否则客户端停在页上），movie 由 onDialog 包装层下发。
		Cutscene cutscene = cutsceneByQuestId.get(questId);
		if (cutscene != null && cutscene.triggerAction() == dialogId) {
			return true;
		}
		return false;
	}

	/** 交付门（item_check）持有量检查：缺一即不放行。 / The item_check hold gate; a single shortfall holds it. */
	private boolean holdsGateItems(int questId, Player player) {
		if (unresolvedGateQuestIds.contains(questId)) {
			// 门声明了无法解析的物品 ⇒ 永不放行（与旧 IR 的 HasItem(全部声明物) 等价）。
			return false;
		}
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

	/** 真端 {@code max_repeat_count} > 1 ⇒ 可重复（COMPLETE 态可再次开窗）。 / Repeatable per retail max_repeat_count. */
	private boolean repeatable(int questId) {
		NativeQuestXmlTable.QuestRow row = NativeQuestXmlTable.instance().find(questId).orElse(null);
		Integer maxRepeat = row == null ? null : row.integer("max_repeat_count");
		return maxRepeat != null && maxRepeat > 1;
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
