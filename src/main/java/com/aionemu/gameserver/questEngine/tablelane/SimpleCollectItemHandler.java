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
import com.aionemu.gameserver.questEngine.definition.QuestCatalogDrop;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;
import com.aionemu.gameserver.questEngine.definition.QuestDialogPage;
import com.aionemu.gameserver.questEngine.definition.QuestItemRequirement;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailClientHandinNpcSets;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;
import com.aionemu.gameserver.questEngine.retail.RetailItemNameIndex;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 真端 SimpleCollectItem 原生任务处理器（计划 §6.2 / §7 P4 切换批）。
 * <p>
 * 完全由真端表 {@link NativeQuestTableLoader#collectRows()} 与 {@code quest.xml} 元数据驱动，
 * 绝不生成 IR 节点、绝不回退旧编译器。逐项证据：
 * <ul>
 *   <li><b>接取</b>：{@code acquired_npc_name} → 问询页 4 / 确认 1002/20000 → {@link NativeQuestStartPort}
 *       （真端 Quest::CanAcquireQuest 轴），随后按 {@code give_item} 原样发放；</li>
 *   <li><b>中继链</b>：{@code talk_npc1..3} 严格按表序推进（真端 cabb10 语义，乱序零推进）；
 *       {@code collect_progress} 与 talk 链长一致（实测 9620=3、14150/14120=1、其余 0），
 *       因此"链未走完不得开始采集"就是真端该列的直接含义；</li>
 *   <li><b>采集（物品驱动，2026-10-04 修正）</b>：本族真端无相机——camera-params.tsv 的 2463 个
 *       相机调用点中本族 262 行 0 命中（对照 SimpleHunt 1812/1863），旧 XML 的交互/击杀转换亦零
 *       var 写。点击采集对象或击杀 {@code drop_monster_N} 只认领交互；任务物品由掉落列
 *       （{@code drop_monster_K → drop_item_K}，经 {@code QuestEngine.questDrops} 聚合本类的
 *       {@link #questDropsFor(int)}）发放，{@code isQuestDrop} 按 {@code collect_item} 上限判定
 *       "未持满才掉"。var0 保持 0（250/262 行 {@code collect_progress=0}）；</li>
 *   <li><b>交付</b>：交付 NPC 处按 {@code check_item} 门（{@code NativeInventoryPort} 持有量）
 *       扣工作物品 → REWARD + 奖励窗页 5；未持有 → 进行中页 10；</li>
 *   <li><b>领奖</b>：{@link NativeReportRewardFlow}（真端 reward 列 → 共用结算体），完成页 1008；</li>
 *   <li><b>链式接取窗</b>：{@code con_quest}（真端交付节点 0x1e 槽 {@code mgr+0x1a8(player, con_quest)}）
 *       —— 本车道按 NPC 建接取路由，该窗的等价物 = 「下一环的接取 NPC 就是本行的交付 NPC」，
 *       逐行验证、不闭环的行登记 fail-closed（{@link #unresolvedChainQuestIds()}）；</li>
 *   <li><b>过场</b>：{@code cutsceneid1}/{@code cs1_haction}（真端交付节点 0x35 槽 PlayMovie）经
 *       {@link NativeMoviePort} 下发，是页动作上的副作用、不推进节点。</li>
 * </ul>
 * 缺行/名字多义/物品未解的行走 fail-closed（不路由、不发放）。
 * <p>
 * Retail SimpleCollectItem native handler (plan §6.2 / §7 P4): driven purely by the family table
 * and retail quest.xml metadata; it generates no IR nodes and never falls back to the retired
 * compiler. Accept flows through {@link NativeQuestStartPort}; the {@code talk_npc1..3} relay chain
 * gates collection exactly as the retail {@code collect_progress} column states. Collection is
 * ITEM-DRIVEN (fixed 2026-10-04): the family has no retail camera (0 of 262 rows among the 2463
 * camera call sites), interact/kill only claims, and the drops column (aggregated by
 * {@code QuestEngine.questDrops} through {@link #questDropsFor(int)}) grants the items, capped by
 * the {@code collect_item} requirement; hand-in consumes the {@code check_item} work items and
 * settles through {@link NativeReportRewardFlow}; the retail chain window ({@code con_quest} on
 * slot 0x1e) is realized by the next quest's own accept route and the cutscene slot (0x35) is
 * served through {@link NativeMoviePort}. Missing rows, ambiguous names and unresolved items fail
 * closed.
 */
public final class SimpleCollectItemHandler implements NativeSystemGrantLane {

	/** 接取问询页。 / The accept ask page. */
	public static final int PAGE_ASK_ACCEPT = 4;
	/** 确认接取页。 / The accept confirmation page. */
	public static final int PAGE_ACCEPTED = 1003;
	/** 进行中页（未满足交付门）。 / In-progress page. */
	public static final int PAGE_IN_PROGRESS = 10;
	/** 奖励窗页。 / The reward window page. */
	public static final int PAGE_REWARD_WINDOW = 5;

	/**
	 * 采集对象引用：任务 id + 目标 NPC 模板 id + 真端列序槽位。
	 * <p>
	 * 槽位由真端掉落列决定（{@code drop_monster_K} → {@code drop_item_K} → 该物品在 {@code collect_itemN}
	 * 交付列里的位置），对象点击只能推进它自己那一槽；对象列序**不是**槽序（真端 4046/2487/2346/1154/41510）。
	 * Collect-object reference: quest id + object npc template id + the retail camera slot. The slot comes
	 * from the retail drop column (drop_monster_K → drop_item_K → its position among collect_itemN), not
	 * from the object column order (retail rows 4046/2487/2346/1154/41510 show the difference).
	 */
	public record CollectTargetRef(int questId, int objectNpcId, int slot) {
	}

	/** 采集怪引用：任务 id + 相机槽。 / Collect-monster reference: quest id + camera slot. */
	public record CollectMonsterRef(int questId, int slot) {
	}

	/** 过场引用：movie id + 触发动作 id（{@code cs1_haction}；-1 = 表未声明触发）。 */
	public record Cutscene(int movieId, int triggerAction) {
	}

	/** 工作物品（已解析成 id + 数量）。 / A resolved work item (id + count). */
	private record ItemStack(int itemId, int count) {
	}

	private static volatile SimpleCollectItemHandler instance;

	private final NativeQuestTableLoader tableLoader;
	private final NativeNpcNameResolver nameResolver;
	private final HtmlPagesRegistry pagesRegistry;
	private final NativeInventoryPort inventory;
	private final NativeMoviePort moviePort;
	private final NativeReportRewardFlow rewardFlow;

	/** 接取 NPC 成员集（任一成员可接取）。 / Acquire NPC member set. */
	private final Map<Integer, List<Integer>> acquireNpcIdsByQuestId;
	/** 任务 ID → 全部交付 NPC（真端逻辑名 + 客户端交付集合展开；首项为兼容易）。
	 * Quest id → every hand-in NPC (retail logical name expanded through the client set). */
	private final Map<Integer, List<Integer>> rewardNpcsByQuestId;
	private final Map<Integer, List<Integer>> talkNpcsByQuestId;
	private final Map<Integer, List<Integer>> objectsByQuestId;
	/** 掉落列来源（对象/怪，模板 id）→ 任务掉落条目（真端 quest.xml drop 列编译产物）。
	 * Drop-column source (object/monster template id) → quest drop entries. */
	private final Map<Integer, List<QuestCatalogDrop>> dropsByNpcId;
	private final Map<Integer, List<ItemStack>> handInByQuestId;
	private final Map<Integer, List<ItemStack>> acceptGiveByQuestId;
	private final Map<Integer, List<CollectMonsterRef>> collectTargetsByNpcId;
	private final Set<Integer> ownedQuestIds;
	private final Set<Integer> routedQuestIds;
	private final Set<Integer> unroutableQuestIds;

	/** 任务 ID → 接取名类别（{@code _faction_} 等哨兵 = 系统发放）。 / Quest id → acquire-name category. */
	private final Map<Integer, RetailGrantKind> grantKindByQuestId;
	/** 任务 ID → 真端势力 id（{@code quest.xml npcfaction_name}；无则 0）。 / Quest id → retail faction id. */
	private final Map<Integer, Integer> factionByQuestId;

	private final Map<Integer, List<CollectTargetRef>> objectsByNpcId;

	/** 任务 ID → 链式接取窗的下一环（真端交付节点 0x1e 槽的 {@code con_quest}）。 / Quest id → the next quest of the chain window. */
	private final Map<Integer, Integer> conQuestByQuestId;

	/** 链式接取窗未闭环的行（fail-closed 证据面）。 / Rows whose chain window is not realized. */
	private final Set<Integer> unresolvedChainQuestIds;

	/** 任务 ID → 过场引用（表 {@code cutsceneid1}/{@code cs1_haction}）。 / Quest id → cutscene reference. */
	private final Map<Integer, Cutscene> cutsceneByQuestId;

	/** 包内可见：家族门禁用注入的背包/完成端口构造。 / Package-visible: family gates inject inventory and settlement ports. */
	SimpleCollectItemHandler(NativeQuestTableLoader tableLoader,
			NativeNpcNameResolver nameResolver, HtmlPagesRegistry pagesRegistry, NativeInventoryPort inventory,
			NativeMoviePort moviePort, NativeReportRewardFlow rewardFlow, Set<Integer> xmlOnlyIds,
			Set<Integer> unresolvedMetadata) {
		this.tableLoader = tableLoader;
		this.nameResolver = nameResolver;
		this.pagesRegistry = pagesRegistry;
		this.inventory = inventory;
		this.moviePort = moviePort;
		this.rewardFlow = rewardFlow;

		Map<Integer, List<Integer>> acquires = new LinkedHashMap<>();
		Map<Integer, List<Integer>> rewards = new LinkedHashMap<>();
		Map<Integer, List<Integer>> talks = new LinkedHashMap<>();
		Map<Integer, List<Integer>> objects = new LinkedHashMap<>();
		Map<Integer, List<QuestCatalogDrop>> dropsByNpc = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> handIns = new LinkedHashMap<>();
		Map<Integer, List<ItemStack>> acceptGives = new LinkedHashMap<>();
		Map<Integer, List<CollectMonsterRef>> monsters = new LinkedHashMap<>();
		Map<Integer, List<CollectTargetRef>> objectRefs = new LinkedHashMap<>();
		Set<Integer> owned = new TreeSet<>();
		Set<Integer> routed = new TreeSet<>();
		Set<Integer> unroutable = new TreeSet<>();
		Map<Integer, RetailGrantKind> grantKinds = new LinkedHashMap<>();
		Map<Integer, Integer> factions = new LinkedHashMap<>();
		Map<Integer, Integer> conQuests = new LinkedHashMap<>();
		Map<Integer, Cutscene> cutscenes = new LinkedHashMap<>();
		Set<Integer> unresolvedChain = new TreeSet<>();

		for (NativeQuestTableLoader.SimpleCollectItemRow row : tableLoader.collectRows()) {
			int questId = row.questId();
			owned.add(questId);
			// 接取哨兵（`_faction_` 等系统发放）与势力轴：与 SimpleTalk 同源，供阵营日常轮换共用。
			// Acquire sentinels (system grants) and the faction axis, shared with the SimpleTalk lane.
			grantKinds.put(questId, RetailGrantKind.of(row.acquiredNpcName()));
			int rowFactionId = NativeFactionRotation.factionIdOf(questId);
			if (rowFactionId != 0) {
				factions.put(questId, rowFactionId);
			}

			List<Integer> acquireIds = nameResolver.resolveMembers(row.acquiredNpcName());
			if (!acquireIds.isEmpty()) {
				acquires.put(questId, acquireIds);
			}
			List<Integer> rewardIds = rewardNpcIds(questId, row.rewardNpcName());
			if (!rewardIds.isEmpty()) {
				rewards.put(questId, rewardIds);
			}
			List<Integer> talkIds = new ArrayList<>(row.talkNpcNames().size());
			for (String talkName : row.talkNpcNames()) {
				NativeNpcNameResolver.Match talk = nameResolver.resolve(talkName);
				if (talk.resolution() == NativeNpcNameResolver.Resolution.UNIQUE) {
					talkIds.add(talk.npcIds().get(0));
				}
			}
			if (!talkIds.isEmpty()) {
				talks.put(questId, List.copyOf(talkIds));
			}
			RetailQuestMetadataCompiler.Outcome metadata = metadataOf(questId);
			if (metadata == null) {
				unresolvedMetadata.add(questId);
			}
			// 采集槽由真端掉落列决定：{@code drop_monster_K} 列出的来源（对象或怪）产出
			// {@code drop_item_K}，该物品在交付列里的位置就是相机槽（真端数据里一行的对象列序**不等于**槽序，
			// 见 4046/2487/2346/1154/41510；同一来源也不会同时落在两个槽上）。
			// The camera slot comes from the retail drop column: drop_monster_K yields drop_item_K and
			// the position of that item among the hand-in columns is the slot. The retail data shows the
			// object column order is not the slot order (4046/2487/2346/1154/41510).
			Map<Integer, Integer> slotByDropSource = dropSourceSlots(metadata);

			List<Integer> objectIds = new ArrayList<>(row.objects().size());
			for (String object : row.objects()) {
				List<Integer> candidates = nameResolver.resolveMonsterIds(object);
				if (candidates.size() != 1) {
					if (candidates.size() > 1) {
						unroutable.add(questId);
					}
					continue;
				}
				int objectId = candidates.get(0);
				Integer slot = slotByDropSource.get(objectId);
				if (slot == null) {
					// 对象不在任何掉落列里 ⇒ 真端没有给它槽位（如 41216 的来源是另一个 FOBJ）；
					// fail-closed，不按列序猜槽。
					// The object appears in no drop column, so the retail data assigns it no slot: fail
					// closed instead of guessing from the column order.
					unroutable.add(questId);
					continue;
				}
				objectIds.add(objectId);
				objectRefs.computeIfAbsent(objectId, key -> new ArrayList<>())
					.add(new CollectTargetRef(questId, objectId, slot));
			}
			if (!objectIds.isEmpty()) {
				objects.put(questId, List.copyOf(objectIds));
			}
			List<ItemStack> handIn = resolveItems(metadata, true);
			if (!handIn.isEmpty()) {
				handIns.put(questId, handIn);
			}
			List<ItemStack> acceptGive = new ArrayList<>(resolveItems(metadata, false));
			ItemStack rowGive = parseSymbol(row.acceptGiveItem());
			if (rowGive != null && acceptGive.stream().noneMatch(item -> item.itemId() == rowGive.itemId())) {
				acceptGive.add(rowGive);
			}
			if (!acceptGive.isEmpty()) {
				acceptGives.put(questId, List.copyOf(acceptGive));
			}
			if (metadata != null && metadata.clean()) {
				for (var drop : metadata.metadata().drops()) {
					// 掉落注册无条件（真端 drop 列就是发放面：对象交互走 QuestItemNpcAI2 掉落列表、
					// 击杀走通用击杀掉落装配，两侧都经 QuestService.getQuestDrop → isQuestDrop 的
					// collect_item 上限判定）。2026-10-04 修复：退役 XML 删除后 catalog 掉落缺席，
					// native 侧必须接手（原实现漏注册，真机 1103 采集拿不到 quest_1103a）。
					// Drops register unconditionally: the retail drop column is the grant face, and both
					// consumers (object interaction, kill assembly) resolve through QuestService.getQuestDrop.
					dropsByNpc.computeIfAbsent(drop.npcId(), key -> new ArrayList<>())
						.add(QuestCatalogDrop.catalog(questId, metadata.metadata(), drop));
					Integer slot = slotByDropSource.get(drop.npcId());
					if (slot == null) {
						// 掉落物不在交付列：无法定槽 ⇒ fail-closed（不猜槽位）。
						// A drop without a matching hand-in column cannot be slotted: fail closed.
						unroutable.add(questId);
						continue;
					}
					monsters.computeIfAbsent(drop.npcId(), key -> new ArrayList<>())
						.add(new CollectMonsterRef(questId, slot));
				}
			}

			// 可路由 = 本行有采集对象（objectIds）+ 元数据可解 + 名字唯一 + 非 XML-only。
			// 采集族真端无相机（camera-params.tsv 262/262 无调用；旧 XML 交互亦无 var 写），
			// 路由不再要求相机参数（原条件随 P4 相机设计撤销；原"无计数即无相机即不路由"的
			// 拦截由本行无对象承接）。交付门在该行 itemRequirements 非空但解析失败时同样
			// fail-closed（handIns 缺项即门永不放行）。
			// Routable = this row's collect objects (objectIds) + resolvable metadata + unique names +
			// not XML-only. The collect family carries no retail camera (0 of 262 rows among the camera
			// call sites), so the camera condition is gone with the P4 camera design; the rows it used to
			// gate (no collect count ⇒ no camera) carry no objects either.
			boolean gateResolved = metadata != null
				&& (metadata.metadata().itemRequirements().isEmpty() || handIns.containsKey(questId));
			boolean routable = !unroutable.contains(questId)
				&& !objectIds.isEmpty()
				&& gateResolved
				&& rewards.containsKey(questId)
				&& (xmlOnlyIds == null || !xmlOnlyIds.contains(questId));
			if (routable) {
				routed.add(questId);
			}

			// 链式接取窗（真端 0x1e 槽）与过场（真端 0x35 槽）按原文装载：两侧语义分别在构造函数尾部
			// 与 onDialog 上消费，装载面不做任何推断。
			// The chain window (retail slot 0x1e) and the cutscene (retail slot 0x35) load verbatim; their
			// semantics are consumed in the constructor tail and in onDialog.
			Integer next = row.conQuest();
			if (next != null) {
				conQuests.put(questId, next);
			}
			if (row.cutsceneId() != null) {
				cutscenes.put(questId, new Cutscene(row.cutsceneId(),
						row.cutsceneAction() == null ? -1 : row.cutsceneAction()));
			}
		}

		// 真端 0x1e 槽（交付节点）：接续下一任务 {@code con_quest} 的接取窗。本车道的接取路由按 NPC 建表，
		// 故该窗的等价物 = 「下一环的接取 NPC 恰是本行的交付 NPC」。本表内目标逐行验证，不闭环的行登记为
		// fail-closed 证据（跨族目标由逐行对拍门按同一条不变量复算）；不新增第二套路由：下一环的接取路由
		// 永远由它自己那一行提供。
		// Retail slot 0x1e (on the hand-in node) opens the next quest's accept window. This lane keys
		// accept routes by NPC, so the equivalent is "the next quest acquires at this row's hand-in NPC".
		// In-table targets are verified here and non-closing rows are recorded as fail-closed evidence;
		// cross-family targets are recomputed by the per-row alignment gate under the same invariant.
		for (Map.Entry<Integer, Integer> entry : conQuests.entrySet()) {
			int questId = entry.getKey();
			int next = entry.getValue();
			List<Integer> targetAcquires = acquires.get(next);
			if (routed.contains(next) && targetAcquires != null
					&& Collections.disjoint(targetAcquires, rewards.getOrDefault(questId, List.of()))) {
				unresolvedChain.add(questId);
			}
		}

		this.acquireNpcIdsByQuestId = Collections.unmodifiableMap(acquires);
		this.rewardNpcsByQuestId = Collections.unmodifiableMap(rewards);
		this.talkNpcsByQuestId = Collections.unmodifiableMap(talks);
		this.objectsByQuestId = Collections.unmodifiableMap(objects);
		this.dropsByNpcId = Collections.unmodifiableMap(dropsByNpc);
		this.handInByQuestId = Collections.unmodifiableMap(handIns);
		this.acceptGiveByQuestId = Collections.unmodifiableMap(acceptGives);
		this.collectTargetsByNpcId = Collections.unmodifiableMap(monsters);
		this.objectsByNpcId = Collections.unmodifiableMap(objectRefs);
		this.ownedQuestIds = Collections.unmodifiableSet(owned);
		this.routedQuestIds = Collections.unmodifiableSet(routed);
		this.unroutableQuestIds = Collections.unmodifiableSet(unroutable);
		this.grantKindByQuestId = Collections.unmodifiableMap(grantKinds);
		this.factionByQuestId = Collections.unmodifiableMap(factions);
		this.conQuestByQuestId = Collections.unmodifiableMap(conQuests);
		this.unresolvedChainQuestIds = Collections.unmodifiableSet(unresolvedChain);
		this.cutsceneByQuestId = Collections.unmodifiableMap(cutscenes);
	}

	private static RetailQuestMetadataCompiler.Outcome metadataOf(int questId) {
		try {
			return com.aionemu.gameserver.questEngine.retail.RetailQuestDriver.ensureLoaded()
				.retailMetadataOf(questId).orElse(null);
		} catch (java.io.IOException | RuntimeException e) {
			// 元数据不可编译（真端休眠行 36017/46017/47112 的 minlevel=999）：按未解处理，fail-closed。
			// Uncompilable metadata (the retail dormant rows 36017/46017/47112 with minlevel=999)
			// counts as unresolved and fails closed.
			return null;
		}
	}

	/**
	 * 采集族的交付/发放物品：交付面 = {@code itemRequirements}（真端 {@code collect_item}），
	 * 接取发放面 = {@code questWorkItems}（真端 {@code quest_work_item}）。
	 * <p>
	 * Retail hand-in items ({@code collect_item}) and accept-time grants ({@code quest_work_item}).
	 */
	private static List<ItemStack> resolveItems(RetailQuestMetadataCompiler.Outcome metadata, boolean handIn) {
		if (metadata == null) {
			return List.of();
		}
		if (metadata.unresolved().stream().anyMatch(entry -> entry.startsWith("collect_item")
				|| entry.startsWith("quest_work_item"))) {
			return List.of();
		}
		List<QuestItemRequirement> source = handIn
			? metadata.metadata().itemRequirements()
			: metadata.metadata().questWorkItems();
		List<ItemStack> items = new ArrayList<>(source.size());
		for (QuestItemRequirement requirement : source) {
			items.add(new ItemStack(requirement.itemId(), requirement.count()));
		}
		return List.copyOf(items);
	}

	private static ItemStack parseSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			return null;
		}
		String[] parts = symbol.trim().split("\\s+");
		String stem = parts[0].toLowerCase(java.util.Locale.ROOT);
		RetailItemNameIndex index;
		try {
			index = RetailItemNameIndex.loadItemTemplates();
		} catch (java.io.IOException e) {
			return null;
		}
		Integer itemId = index.resolve(stem);
		if (itemId == null && stem.startsWith("item_")) {
			itemId = index.resolve(stem.substring("item_".length()));
		}
		if (itemId == null) {
			return null;
		}
		int count = 1;
		if (parts.length > 1) {
			try {
				count = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return new ItemStack(itemId, count);
	}

	public static SimpleCollectItemHandler instance() {
		SimpleCollectItemHandler local = instance;
		if (local == null) {
			synchronized (SimpleCollectItemHandler.class) {
				local = instance;
				if (local == null) {
					local = new SimpleCollectItemHandler(NativeQuestTableLoader.instance(),
						NativeNpcNameResolver.instance(),
						HtmlPagesRegistry.instance(), NativeInventoryPort.live(),
						NativeMoviePort.live(), NativeReportRewardFlow.instance(),
						NativeQuestOwnerResolver.instance().xmlOnlyIds(), new TreeSet<>());
					instance = local;
				}
			}
		}
		return local;
	}

	/** 判断是否拥有该任务（注册集）。 / Checks whether the handler manages the quest (registration set). */
	public boolean owns(int questId) {
		return ownedQuestIds.contains(questId);
	}

	/** 路由集 = 注册集 − XML-only 行 − 不可路由行。 / Routing set = registration set minus XML-only and unroutable rows. */
	public boolean routes(int questId) {
		return routedQuestIds.contains(questId);
	}

	public Set<Integer> ownedQuestIds() {
		return ownedQuestIds;
	}

	public Set<Integer> routedQuestIds() {
		return routedQuestIds;
	}

	/** 已装载但当前不可路由的采集行（缺对象/缺相机/名字多义/物品未解）。 / Loaded but currently unroutable collect rows. */
	public Set<Integer> unroutableQuestIds() {
		return unroutableQuestIds;
	}

	/** 接取名类别（真端哨兵 = 系统发放）。 / The acquire-name category (a sentinel means system-granted). */
	@Override
	public RetailGrantKind grantKind(int questId) {
		return grantKindByQuestId.getOrDefault(questId, RetailGrantKind.NPC);
	}

	/** 真端 {@code quest.xml} 的势力 id（{@code npcfaction_name}；无则 0）。 / The retail faction id, or 0. */
	@Override
	public int factionId(int questId) {
		return factionByQuestId.getOrDefault(questId, 0);
	}

	/**
	 * 指定势力的当前可轮换任务 id（真端 {@code _faction_} 行 ∩ 本族路由集）。
	 * Faction-rotation candidates of one faction: routed rows whose acquire name is {@code _faction_}.
	 */
	@Override
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

	/**
	 * 是否系统发放（哨兵行且本服确有该发放入口）；判据与 SimpleTalk 车道同形：{@link RetailGrantKind#grantable()}
	 * 让 {@code _challengetask_} 这类没有受理入口的哨兵保持拒绝。
	 * System-grant verdict, keyed on {@link RetailGrantKind#grantable()} so sentinels without an intake
	 * in this server can never be granted.
	 */
	@Override
	public boolean isSystemGranted(int questId) {
		RetailGrantKind kind = grantKind(questId);
		return routes(questId) && kind != RetailGrantKind.NPC && kind.grantable();
	}

	/** 阵营日常轮换资格（与 SimpleTalk 共用 {@link NativeFactionRotation} 的真端轴判定）。 /
	 * Rotation eligibility, sharing the retail axis adjudication with the SimpleTalk lane. */
	@Override
	public boolean factionRotationEligible(Player player, int questId, int factionId) {
		return NativeFactionRotation.eligible(player, questId, factionId, isSystemGranted(questId),
			NativeFactionRotation.factionKind(grantKind(questId)), factionId(questId));
	}

	/**
	 * 系统发放入口（{@code _faction_} 等哨兵行）：无进度时直接进 START，随后按本族采集流程推进。
	 * System-grant entry: starts the quest directly when it has no progress; the collect flow drives it.
	 */
	@Override
	public boolean grantSystemStart(Player player, int questId) {
		if (player == null || !isSystemGranted(questId)) {
			return false;
		}
		QuestState existing = player.getQuestStateList().getQuestState(questId);
		if (existing != null && existing.getStatus() != QuestStatus.NONE) {
			return false;
		}
		return NativeQuestStartPort.instance().grant(player, questId).started();
	}

	/** 任务起始 NPC（成员集首项；未解析为 null）。 / The acquire NPC for the quest. */
	public Integer acquireNpc(int questId) {
		List<Integer> members = acquireNpcIdsByQuestId.get(questId);
		return members == null || members.isEmpty() ? null : members.getFirst();
	}

	/** 接取 NPC 成员集（空表 = 未解析）。 / The acquire NPC member set. */
	public List<Integer> acquireNpcs(int questId) {
		return acquireNpcIdsByQuestId.getOrDefault(questId, List.of());
	}

	/**
	 * 任务交付 NPC（首项；多交付 NPC 行见 {@link #rewardNpcs(int)}）。
	 * The quest's hand-in NPC (the first one; see {@link #rewardNpcs(int)} for multi-NPC rows).
	 */
	public Integer rewardNpc(int questId) {
		List<Integer> npcs = rewardNpcsByQuestId.get(questId);
		return npcs == null || npcs.isEmpty() ? null : npcs.getFirst();
	}

	/** 任务的全部交付 NPC（真端逻辑名 + 客户端交付集合展开）。 / Every hand-in NPC of the quest. */
	public List<Integer> rewardNpcs(int questId) {
		return rewardNpcsByQuestId.getOrDefault(questId, List.of());
	}

	/** 任务的采集对象 NPC 模板 id。 / The collect-object npc template ids of the quest. */
	public List<Integer> collectObjects(int questId) {
		return objectsByQuestId.getOrDefault(questId, List.of());
	}

	/** 任务的中继 NPC id（表序 1..3）。 / The relay npc ids of the quest (table order 1..3). */
	public List<Integer> relayNpcs(int questId) {
		return talkNpcsByQuestId.getOrDefault(questId, List.of());
	}

	/** 任务的采集怪 NPC 模板 id（去重、表序）。 / The collect-monster npc template ids of the quest (deduped, table order). */
	public List<Integer> collectMonsterTargets(int questId) {
		Set<Integer> monsters = new TreeSet<>();
		for (Map.Entry<Integer, List<CollectMonsterRef>> entry : collectTargetsByNpcId.entrySet()) {
			for (CollectMonsterRef ref : entry.getValue()) {
				if (ref.questId() == questId) {
					monsters.add(entry.getKey());
				}
			}
		}
		return List.copyOf(monsters);
	}

	/** 任务的接取发放物品 id（真端 {@code quest_work_item}）。 / The accept-time grant item ids of the quest (retail quest_work_item). */
	public List<Integer> acceptGiveItems(int questId) {
		List<ItemStack> items = acceptGiveByQuestId.get(questId);
		if (items == null) {
			return List.of();
		}
		List<Integer> ids = new ArrayList<>(items.size());
		for (ItemStack item : items) {
			ids.add(item.itemId());
		}
		return List.copyOf(ids);
	}

	/** 任务的交付物品 id（真端 {@code collect_item}，与相机计数同源）。 / The hand-in item ids of the quest (retail collect_item). */
	public List<Integer> handInItems(int questId) {
		List<ItemStack> items = handInByQuestId.get(questId);
		if (items == null) {
			return List.of();
		}
		List<Integer> ids = new ArrayList<>(items.size());
		for (ItemStack item : items) {
			ids.add(item.itemId());
		}
		return List.copyOf(ids);
	}

	/** 任务在指定 NPC 上的采集目标（注册索引对拍用）。 / Collect targets of the quest on the given npc. */
	public List<CollectTargetRef> targetsForNpc(int npcId) {
		return objectsByNpcId.getOrDefault(npcId, List.of());
	}

	/**
	 * 任务的全部采集来源 → 真端交付列槽（对象与怪同源；证据/对拍面）。
	 * <p>
	 * 槽 = 该来源产出的物品在交付列（{@code collect_itemN}）里的位置（真端数据契约面，供对拍；
	 * 采集推进本身是物品驱动，不消费该槽）；不在交付列的来源不出现（该行走 fail-closed）。
	 * Every collect source (object or monster) of the quest mapped to its retail hand-in slot; the slot is
	 * the position of the produced item among the hand-in columns. Sources without a hand-in column are
	 * absent (their row fails closed).
	 */
	public Map<Integer, Integer> collectSources(int questId) {
		Map<Integer, Integer> slots = new LinkedHashMap<>();
		for (Map.Entry<Integer, List<CollectTargetRef>> entry : objectsByNpcId.entrySet()) {
			for (CollectTargetRef ref : entry.getValue()) {
				if (ref.questId() == questId) {
					slots.putIfAbsent(entry.getKey(), ref.slot());
				}
			}
		}
		for (Map.Entry<Integer, List<CollectMonsterRef>> entry : collectTargetsByNpcId.entrySet()) {
			for (CollectMonsterRef ref : entry.getValue()) {
				if (ref.questId() == questId) {
					slots.putIfAbsent(entry.getKey(), ref.slot());
				}
			}
		}
		return Collections.unmodifiableMap(slots);
	}

	/**
	 * 真端 {@code con_quest}（链式接取窗的下一环）；未声明返回 null。
	 * <p>
	 * 真端该列由交付节点上的 0x1e 槽消费（{@code mgr+0x1a8(player, con_quest)} = 下一环的接取窗：
	 * ScriptDLL64 注册 {@code FUN_180cb2ac0(..., 0x1e, FUN_180d39370, 0)}，quest 1103 的 thunk 内是
	 * {@code mgr+0x1a8(player, 1104)}）。本车道按 NPC 建接取路由，故只要下一环的接取 NPC 等于本行的
	 * 交付 NPC，该窗即已由下一环自身那一行实现；{@link #unresolvedChainQuestIds()} 为空即全表闭环。
	 * <p>
	 * The retail {@code con_quest} column, consumed by slot 0x1e on the hand-in node (the
	 * {@code mgr+0x1a8(player, con_quest)} next-quest accept window). This lane registers accept routes
	 * per NPC, so the window is already realized by the next quest's own row whenever that row acquires
	 * at this row's hand-in NPC; an empty {@link #unresolvedChainQuestIds()} means the whole table closes.
	 */
	public Integer conQuest(int questId) {
		return conQuestByQuestId.get(questId);
	}

	/** 链式接取窗未闭环的行（fail-closed 证据面）。 / Rows whose chain window is not realized. */
	public Set<Integer> unresolvedChainQuestIds() {
		return unresolvedChainQuestIds;
	}

	/** 过场引用（表未声明返回 null）。 / The cutscene reference (null when the row declares none). */
	public Cutscene cutscene(int questId) {
		return cutsceneByQuestId.get(questId);
	}

	/** 启动期注册 native 兴趣到 {@link QuestEngine}。 / Registers native interests into the engine at startup. */
	public void installInterest(QuestEngine engine) {
		if (engine == null) {
			return;
		}
		for (Map.Entry<Integer, List<Integer>> entry : acquireNpcIdsByQuestId.entrySet()) {
			int questId = entry.getKey();
			if (!routedQuestIds.contains(questId)) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnQuestStart(questId);
				engine.registerQuestNpc(npcId).addOnTalkEvent(questId);
			}
		}
		for (Map.Entry<Integer, List<Integer>> entry : rewardNpcsByQuestId.entrySet()) {
			int questId = entry.getKey();
			if (!routedQuestIds.contains(questId)) {
				continue;
			}
			for (int rewardNpc : entry.getValue()) {
				engine.registerQuestNpc(rewardNpc).addOnTalkEvent(questId);
			}
		}
		for (Map.Entry<Integer, List<Integer>> entry : talkNpcsByQuestId.entrySet()) {
			int questId = entry.getKey();
			if (!routedQuestIds.contains(questId)) {
				continue;
			}
			for (int npcId : entry.getValue()) {
				engine.registerQuestNpc(npcId).addOnTalkEvent(questId);
			}
		}
		for (Map.Entry<Integer, List<CollectTargetRef>> entry : objectsByNpcId.entrySet()) {
			for (CollectTargetRef ref : entry.getValue()) {
				if (!routedQuestIds.contains(ref.questId())) {
					continue;
				}
				engine.registerQuestNpc(entry.getKey()).addOnTalkEvent(ref.questId());
			}
		}
		for (Map.Entry<Integer, List<CollectMonsterRef>> entry : collectTargetsByNpcId.entrySet()) {
			for (CollectMonsterRef ref : entry.getValue()) {
				if (!routedQuestIds.contains(ref.questId())) {
					continue;
				}
				engine.registerQuestNpc(entry.getKey()).addOnKillEvent(ref.questId());
			}
		}
	}

	/**
	 * 击杀采集怪（真端 {@code drop_monster_N} 的怪来源）：**零任务侧写入**。
	 * <p>
	 * 采集族真端无相机（camera-params.tsv 的 2463 个调用点中本族 262 行 0 命中，对照
	 * SimpleHunt 1812/1863；旧 XML 的交互/击杀转换同样零 var 写）——击杀的任务物品由通用
	 * 击杀掉落装配（{@code QuestService.getQuestDrop} → {@code isQuestDrop} 的
	 * {@code collect_item} 上限判定）发放，本方法不做任何动作。
	 * <p>
	 * Collect-monster kills carry no task-side write: the retail collect family has no camera
	 * (0 of 262 rows among the 2463 camera call sites) and the drop assembly grants the items,
	 * capped by the {@code collect_item} requirement.
	 */
	public boolean onKill(Player player, int npcId) {
		return false;
	}

	/**
	 * 采集对象（{@code objectN}）被点击时的认领口（**零状态写入**）。
	 * <p>
	 * 真端采集族是物品驱动：交互由掉落链（对象 AI 的掉落列表，经 {@code QuestService.getQuestDrop}
	 * → {@code isQuestDrop} 的 {@code collect_item} 上限判定）发放任务物品，var0 保持 0
	 * （250/262 行 {@code collect_progress=0}——客户端据此维持采集步与报告对白条件）。
	 * 本方法只认领交互（返回 true 抑制通用页回落，配合 {@code QuestEngine} 的采集物先手），
	 * 不写任何 quest 状态。2026-10-04 修复：原实现按 P4 误设计的"采集相机"推进 var0，
	 * 真机 1103 采 1 个即写 var0=1 → 客户端按 var0 显示进入下一步（应采 3 个）。
	 * <p>
	 * The retail collect family is item-driven: the drop chain grants the quest items and var0
	 * stays 0 (250 of 262 rows declare {@code collect_progress=0}). This method only claims the
	 * interaction and writes no quest state. Fixed 2026-10-04: the previous form advanced var0
	 * through the mis-designed P4 collect camera (live 1103 wrote var0=1 on the first grab and the
	 * client advanced its step display; the quest requires three).
	 */
	public boolean onObjectUse(Player player, int questId, int objectNpcId) {
		if (player == null || !routes(questId)) {
			return false;
		}
		List<Integer> objects = objectsByQuestId.get(questId);
		if (objects == null || !objects.contains(objectNpcId)) {
			return false;
		}
		if (!talkChainComplete(player, questId)) {
			return false;
		}
		QuestState state = player.getQuestStateList() == null ? null
			: player.getQuestStateList().getQuestState(questId);
		return state != null && state.getStatus() == QuestStatus.START;
	}

	/**
	 * 该 NPC（掉落列来源的模板 id）的全部任务掉落条目（真端 quest.xml drop 列编译产物）。
	 * 由 {@code QuestEngine.questDrops} 聚合进掉落查询（对象交互与击杀装配共用同一条查询）。
	 * <p>
	 * Every quest drop entry keyed by the drop-column source's template id; aggregated by
	 * {@code QuestEngine.questDrops} for both the object-interaction and kill-assembly consumers.
	 */
	public List<QuestCatalogDrop> questDropsFor(int npcId) {
		return dropsByNpcId.getOrDefault(npcId, List.of());
	}

	/**
	 * 真端掉落来源（对象或怪）→ 相机槽。槽 = {@code drop_item_K} 在交付列（{@code collect_itemN}）里的位置；
	 * 掉落物不在交付列时该来源不定槽（调用方 fail-closed）。
	 * <p>
	 * Retail drop source (object or monster) to camera slot: the slot is the position of the drop's item
	 * among the hand-in columns. A source whose item is not a hand-in item gets no slot.
	 */
	private static Map<Integer, Integer> dropSourceSlots(RetailQuestMetadataCompiler.Outcome metadata) {
		if (metadata == null || !metadata.clean()) {
			return Map.of();
		}
		List<QuestItemRequirement> collected = metadata.metadata().itemRequirements();
		Map<Integer, Integer> slots = new LinkedHashMap<>();
		for (var drop : metadata.metadata().drops()) {
			int slot = slotOfItem(collected, drop.itemId());
			if (slot != 0) {
				slots.putIfAbsent(drop.npcId(), slot);
			}
		}
		return slots;
	}

	/**
	 * 交付 NPC 集合：真端 {@code reward_npc_name} 是**逻辑名**，静态数据唯一命中即单元素；未命中
	 * （如 {@code <地图>_<势力名>} 复合名 LF4_GuardianOfDivine）时按客户端交付集合
	 * （{@code quest_client_handin_npc_sets.xml}）逐元素展开——与退役前的
	 * {@code RetailSimpleCollectItemDefinitionCompiler} 同一仲裁面；客户端未声明即 fail-closed。
	 * <p>
	 * Hand-in NPC set: a retail logical name resolves to one id when unique; names missing from static
	 * data (the {@code <map>_<faction>} composites) expand through the client-declared hand-in set, the
	 * same arbitration face the retired compiler used; an undeclared row stays fail-closed.
	 */
	private static List<Integer> rewardNpcIds(int questId, String retailName) {
		List<Integer> members = NativeNpcNameResolver.instance().resolveMembers(retailName);
		if (!members.isEmpty()) {
			return members;
		}
		Set<Integer> declared = RetailClientHandinNpcSets.defaultSets().npcIds(questId);
		return declared.isEmpty() ? List.of() : List.copyOf(new TreeSet<>(declared));
	}

	/** 交付列里该物品的槽位（1..N）；不在交付列返回 0。 / The slot of an item in the hand-in columns, or 0. */
	private static int slotOfItem(List<QuestItemRequirement> collected, int itemId) {
		for (int index = 0; index < collected.size(); index++) {
			if (collected.get(index).itemId() == itemId) {
				return index + 1;
			}
		}
		return 0;
	}

	/**
	 * 中继链是否走完：talk_npc1..3 按表序推进（真端 {@code collect_progress} 的直接语义）。
	 * Whether the relay chain is done: talk_npc1..3 are visited in table order, which is exactly what
	 * the retail {@code collect_progress} column states for this family.
	 */
	private boolean talkChainComplete(Player player, int questId) {
		List<Integer> talkNpcs = talkNpcsByQuestId.get(questId);
		if (talkNpcs == null || talkNpcs.isEmpty()) {
			return true;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		return state != null && talkStep(state) >= talkNpcs.size();
	}

	/**
	 * 处理对话与翻页（接取 / 中继链 / 采集对象 / 交付 / 领奖）。
	 * Handles dialog and page progression (accept / relay chain / collect objects / hand-in / reward).
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
		Npc npc = env.getVisibleObject() instanceof Npc target ? target : null;
		int npcId = npc != null ? npc.getNpcId() : 0;
		int objectId = npc != null ? npc.getObjectId() : 0;
		int dialogId = env.getDialogId();
		QuestState state = player.getQuestStateList().getQuestState(questId);
		QuestStatus status = state != null ? state.getStatus() : QuestStatus.NONE;

		if (state == null || status == QuestStatus.NONE) {
			return onAcceptDialog(player, questId, npcId, objectId, dialogId);
		}

		if (status == QuestStatus.START) {
			if (objectsByQuestId.containsKey(questId) && objectsByQuestId.get(questId).contains(npcId)) {
				// 真端采集物交互（1103 旧 XML：TALK_TO_NPC npc-id=700105 started→started）无
				// after-commit 页；QE-044 口径=只发 PACKET_ONLY 状态同步（advance 内已发
				// SM_QUEST_ACTION）。2026-10-04 修复：原实现对采集物件开对话窗（页10）导致
				// 客户端 load fail（真机 1103 谷物袋子）。
				// The retail collect-object interaction carries no after-commit page; a dialog
				// window aimed at a collect object makes the client fail to load it (live 1103).
				return onObjectUse(player, questId, npcId);
			}
			if (talkChainStep(player, questId, npcId)) {
				return true;
			}
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)) {
				// handInReady 无副作用（不扣物品）：第一步发确认页后物品仍在，第二步才扣除推进。
				// handInReady is side-effect-free: the first step must not consume the items.
				boolean reportReady = handInReady(player, questId);
				// 报告页只随「中继走完」下发——物品门不计入页条件（缺陷 T，2026-10-05；退役 XML 1137：
				// started 态 31→SELECT5 无 conditions，就绪分叉在 39/1009 确认动作上）。
				// The confirm page rides the completed relay alone (defect T); the item gate forks on
				// the confirm action.
				boolean relayDone = talkChainComplete(player, questId);
				// 两步报告（裁定 a，2026-10-03；39 检查按钮 2026-10-04）：任务行（31）只发客户端声明的
				// 报告确认页（NPC_REPORT 分型 1352/2375/10002）；报告确认才推进 REWARD + 奖励窗——确认
				// 动作随任务页而分：直翻型 = 1009；检查型 = 报告页的 39（HACTION_CHECK_USER_HAS_QUEST_ITEM，
				// 真机 1103「拿出找到的谷物袋子」；两族 39 用户全量普查 Talk 675 + CollectItem 85）。
				// 39 未持满时下发客户端声明的失败页（select6=2716，真端失败应答文案）。
				// 开门动作（-1/26）不推进、不跳步。
				// Two-step report (adjudication a, 2026-10-03; the 39 check button, 2026-10-04): 31 shows the
				// declared confirm page; the confirm action advances — 1009 for the direct form, 39 (the report
				// page's item-check button) for the check form; a failed 39 check shows the declared fail page.
				if (dialogId == 31 && relayDone) {
					// 报告页分型跳过被中继步占用的 SELECT2（缺陷 S，2026-10-05）：9620/9655/9656 等
					// 双页任务的 select2/3/4 是中继步页（SETPRO1/2/3），报告页是 select5。
					// The report-page typing skips SELECT2 when relay steps consume it (defect S).
					int reportPage = QuestDialogContract.loadDefault().reportConfirmPage(questId,
						relayNpcs(questId).size());
					if (reportPage > 0) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, reportPage, questId));
						return true;
					}
				}
				if (dialogId == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id() && !reportReady) {
					int failPage = QuestDialogContract.loadDefault().checkFailPage(questId);
					if (failPage > 0) {
						PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, failPage, questId));
						return true;
					}
				}
				if (reportReady && (dialogId == 31 || dialogId == 1009
						|| dialogId == QuestDialogAction.CHECK_USER_HAS_QUEST_ITEM.id())) {
					if (!handInComplete(player, questId)) {
						return false;
					}
					state.setStatus(QuestStatus.REWARD);
					state.setPersistentState(PersistentState.UPDATE_REQUIRED);
					PacketSendUtility.sendPacket(player,
						new SM_QUEST_ACTION(questId, state.getStatus(), state.getQuestVars().getQuestVars()));
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
					return true;
				}
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_IN_PROGRESS));
					return true;
				}
			}
			return false;
		}

		if (status == QuestStatus.REWARD) {
			if (rewardNpcsByQuestId.getOrDefault(questId, List.of()).contains(npcId)) {
				if (dialogId == 31 || dialogId == 26 || dialogId == 1009 || dialogId == -1) {
					PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_REWARD_WINDOW, questId));
					return true;
				}
				// 选项段只有 SELECTED_QUEST_REWARD1..15（8..22）；23 = NOREWARD 无选择确认，
				// 不占选项下标——发放由结算体按 dialogId==23 + extendedRewardIndex 决定。
				// Only SELECTED_QUEST_REWARD1..15 (8..22) index options; 23 is the no-selection
				// confirm whose grant the settlement resolves via dialogId==23 + extendedRewardIndex.
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

		// 表声明的过场触发动作（真端把 movie 挂在页动作上；本族两行 = SELECT1_1(1012)）：
		// 不推进状态，但必须被服务（否则客户端停在页上），movie 由 onDialog 包装层下发。
		// The declared cutscene trigger action (the retail page action; SELECT1_1(1012) for this family's
		// two rows) advances no state but must be served, otherwise the client stalls on the page; the
		// movie itself is sent by the onDialog wrapper.
		Cutscene cutscene = cutsceneByQuestId.get(questId);
		if (cutscene != null && cutscene.triggerAction() == dialogId) {
			return true;
		}
		return false;
	}

	private boolean onAcceptDialog(Player player, int questId, int npcId, int objectId, int dialogId) {
		List<Integer> acquireNpcs = acquireNpcIdsByQuestId.get(questId);
		if (acquireNpcs == null || !acquireNpcs.contains(npcId)) {
			return false;
		}
		if (dialogId == 31 || dialogId == 26) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId,
				QuestDialogContract.loadDefault().retailEntryPage(questId), questId));
			return true;
		}
		if (dialogId == QuestDialogAction.ASK_QUEST_ACCEPT.id()) {
			// 真端页动作 1007（ASK_QUEST_ACCEPT → mgr+0x1a0）：打开接取窗页 4；客户端未声明即 fail-closed。
			// Retail page action 1007 (mgr+0x1a0) opens ask window page 4; undeclared pages fail closed.
			int askWindow = QuestDialogContract.loadDefault().askWindowPage(questId);
			if (askWindow < 0) {
				return false;
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, askWindow, questId));
			return true;
		}
		if (dialogId == QuestDialogPage.SELECT1_1.id() || dialogId == QuestDialogPage.SELECT1_1_1.id()) {
			if (!QuestDialogContract.loadDefault().hasButtonPage(questId, dialogId)) {
				return false;
			}
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, dialogId, questId));
			return true;
		}
		if (dialogId == 1002 || dialogId == 20000) {
			// 拒绝走 startTraced 打 QUEST-TRACE，不再静默。 / Refusals are traced instead of silent.
			if (NativeQuestStartPort.instance().startTraced(player, questId, dialogId).started()) {
				grantAcceptItems(player, questId);
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, PAGE_ACCEPTED, questId));
				return true;
			}
			return false;
		}
		if (dialogId == 1003 || dialogId == 1004 || dialogId == 20001) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(objectId, 1004, questId));
			return true;
		}
		return false;
	}

	private void grantAcceptItems(Player player, int questId) {
		List<ItemStack> items = acceptGiveByQuestId.get(questId);
		if (items == null) {
			return;
		}
		for (ItemStack item : items) {
			inventory.give(player, item.itemId(), item.count());
		}
	}

	/**
	 * 中继链步进：命中表序中的下一个 NPC 才推进（乱序/重复零推进）。
	 * Relay-chain step: only the next NPC in table order advances (out-of-order repeats are no-ops).
	 */
	private boolean talkChainStep(Player player, int questId, int npcId) {
		List<Integer> talkNpcs = talkNpcsByQuestId.get(questId);
		if (talkNpcs == null || talkNpcs.isEmpty()) {
			return false;
		}
		int index = talkNpcs.indexOf(npcId);
		if (index < 0) {
			return false;
		}
		QuestState state = player.getQuestStateList().getQuestState(questId);
		if (state == null) {
			return false;
		}
		if (index != talkStep(state)) {
			return false;
		}
		int next = index + 1;
		int vars = (state.getQuestVars().getQuestVars() & ~RELAY_STEP_MASK) | (next << RELAY_STEP_SHIFT);
		state.getQuestVars().setVar(vars);
		state.setPersistentState(PersistentState.UPDATE_REQUIRED);
		PacketSendUtility.sendPacket(player,
			new SM_QUEST_ACTION(questId, state.getStatus(), state.getQuestVars().getQuestVars()));
		return true;
	}

	/** 中继步位段（相机槽 0..5，中继步放 16..17，恒低于真端守卫位 0x40000000）。 /
	 * Relay-step field: the camera uses bits 0..5, the relay step lives at bits 16..17, always
	 * below the retail guard bit 0x40000000. */
	private static final int RELAY_STEP_SHIFT = 16;
	private static final int RELAY_STEP_MASK = 0x3 << RELAY_STEP_SHIFT;

	/** 当前中继步（= 已完成的 talk 数）。 / Current relay step (the number of finished talks). */
	private static int talkStep(QuestState state) {
		return (state.getQuestVars().getQuestVars() & RELAY_STEP_MASK) >>> RELAY_STEP_SHIFT;
	}

	/**
	 * 交付就绪检查（**无副作用**，两步报告的第一、二步共用）：中继链走完 + 持有交付物（真端 check_item）。
	 * 采集族无相机（2026-10-04 修复：原实现要求相机满值，物品驱动下该条件永真不成立即把门堵死）。
	 * Side-effect-free hand-in readiness check shared by both report steps: the relay chain is done and
	 * the hand-in items are held (the retail check_item gate). The collect family has no camera.
	 */
	private boolean handInReady(Player player, int questId) {
		if (!talkChainComplete(player, questId)) {
			return false;
		}
		List<ItemStack> items = handInByQuestId.get(questId);
		if (items == null || items.isEmpty()) {
			return false;
		}
		for (ItemStack item : items) {
			if (inventory.count(player, item.itemId()) < item.count()) {
				return false;
			}
		}
		return true;
	}

	private boolean handInComplete(Player player, int questId) {
		if (!handInReady(player, questId)) {
			return false;
		}
		for (ItemStack item : handInByQuestId.get(questId)) {
			if (!inventory.remove(player, item.itemId(), item.count())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 采集对象的可交互性：玩家在该对象上有一条已接取（START）的采集任务，且中继链已走完。
	 * 供 {@code QuestEngine.onCanAct(ACTION_ITEM_USE)} 判定（交互物 AI 的对话入口）。
	 * <p>
	 * Usability of a collect object: the player has a STARTED collect quest on that npc and the relay
	 * chain is complete. Consulted by {@code QuestEngine.onCanAct(ACTION_ITEM_USE)}.
	 */
	public boolean allowsItemUse(Player player, int objectNpcId) {
		if (player == null || player.getQuestStateList() == null) {
			return false;
		}
		List<CollectTargetRef> refs = objectsByNpcId.get(objectNpcId);
		if (refs == null || refs.isEmpty()) {
			return false;
		}
		for (CollectTargetRef ref : refs) {
			if (!routes(ref.questId())) {
				continue;
			}
			QuestState state = player.getQuestStateList().getQuestState(ref.questId());
			if (state != null && state.getStatus() == QuestStatus.START
					&& talkChainComplete(player, ref.questId())) {
				return true;
			}
		}
		return false;
	}

	/** 采集对象命中判定（供引擎的 USE_OBJECT 兜底路径使用）。 / Whether the npc is a collect object of the quest. */
	public boolean isCollectObject(int questId, int npcId) {
		List<Integer> objects = objectsByQuestId.get(questId);
		return objects != null && objects.contains(npcId);
	}

}
