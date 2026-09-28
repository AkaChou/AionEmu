package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.ImmutableQuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;
import com.aionemu.gameserver.questEngine.definition.QuestDialogContract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生产装载链的"真端优先"驱动（提示词 §4.C 的 overlay 落点）。
 * <p>
 * 职责单一：对保留清单（{@code quest_retail/retail-xml-retention.tsv}）判定为
 * {@code RETAIL_TABLE} 的任务，用真端模板表 + 真端 quest.xml 元数据 + DLL 语义
 * 合成完整定义，替换/补入 XML 目录；保留清单内的任务一律维持 XML。
 * {@link #overlay} 为局部目录测试保留宽松入口；生产必须调用
 * {@link #overlayProduction}，逐任务核对完整归属。已删除的 XML 无法由开关恢复。
 * <p>
 * The retail-first overlay; production verifies every owner rather than silently
 * dropping retired quests when the driver is disabled or unavailable.
 */
public final class RetailQuestDriver {

	/** 保留清单资源（quest_id, owner, family, reason, evidence）。 / The retention manifest resource. */
	private static final String RETENTION_RESOURCE =
		"/aion/data/static_data/quest_retail/retail-xml-retention.tsv";
	private static final String SIMPLE_HUNT_TABLE = "/aion/data/static_data/quest_retail/Quest_SimpleHunt.xml";
	/** 真端 SimpleSerialHunt 模板表（行直接转换为 hunt 形 Entry，复用计划与合成器）。 /
	 * The retail SimpleSerialHunt template table (rows convert to hunt-shaped entries). */
	private static final String SIMPLE_SERIAL_HUNT_TABLE =
		"/aion/data/static_data/quest_retail/Quest_SimpleSerialHunt.xml";
	/** 真端 SimpleUseItem 模板表（用物品接取 + 报告 NPC）。 / The retail SimpleUseItem template table. */
	private static final String SIMPLE_USE_ITEM_TABLE =
		"/aion/data/static_data/quest_retail/Quest_SimpleUseItem.xml";
	/** 真端 SimpleItemPlay 模板表（接取发物 → 用物品演出 → 交付）。 /
	 * The retail SimpleItemPlay template table (accept grants the item, using it advances, hand-in). */
	private static final String SIMPLE_ITEM_PLAY_TABLE =
		"/aion/data/static_data/quest_retail/Quest_SimpleItemPlay.xml";
	/** 真端 DataDriven 模板表（接取方式 × 进度步骤链）。 / The retail DataDriven template table. */
	private static final String DATA_DRIVEN_TABLE =
		"/aion/data/static_data/quest_retail/data_driven_quest.xml";
	private static final String SIMPLE_TALK_TABLE = "/aion/data/static_data/quest_retail/Quest_SimpleTalk.xml";
	/** 真端 CombineTask 模板表。 / The retail CombineTask template table. */
	private static final String COMBINE_TASK_TABLE = "/aion/data/static_data/quest_retail/Quest_CombineTask.xml";
	/** 真端 SimpleCollectItem 模板表。 / The retail SimpleCollectItem template table. */
	private static final String SIMPLE_COLLECT_ITEM_TABLE =
		"/aion/data/static_data/quest_retail/Quest_SimpleCollectItem.xml";
	/** 本服配方模板（{@code (skillid, productid)} → recipe id）。 / Local recipe templates. */
	private static final String RECIPE_TEMPLATES = "/aion/data/static_data/recipe/recipe_templates.xml";
	/** 客户端任务书行数登记表（REWARD 投影 = 末行行号）。 / Client journal row counts for the reward projection. */
	private static final String CLIENT_SUMMARY_ROWS =
		"/aion/data/static_data/quest_retail/quest_client_summary_rows.tsv";
	/** 客户端报告页登记（承载 HACTION_SELECT_QUEST_REWARD 的页）。 / Client report pages hosting the reward button. */
	private static final String CLIENT_KILL_TARGETS =
		"/aion/data/static_data/quest_retail/quest_client_kill_targets.tsv";
	/** enterarea 别名 → 登记区名解析表（EA 步死边防线）。 / Enterarea alias to registered zone name (EA dead-edge guard). */
	private static final String ENTERAREA_ZONE_RESOLUTION =
		"/aion/data/static_data/quest_retail/quest_enterarea_zone_resolution.tsv";
	private static final String RETAIL_QUEST_XML = "/aion/data/static_data/quest_retail/quest.xml";
	private static final String NAME_IDS_TSV = "/aion/data/static_data/quest_retail/quest_name_string_ids.tsv";
	/** 真端对话名组表（守备队同组共用 ScriptDLL 对话名）。 / Retail dialog-name group table. */
	private static final String QUEST_AI_NAME_GROUPS =
		"/aion/data/static_data/quest_retail/retail-quest-ai-name-groups.tsv";
	private static final String RANDOM_REWARDS = "/aion/data/static_data/quest_random_rewards.xml";
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String ITEM_DIR = "/aion/data/static_data/items/item/";
	/** 生产区域发放表（{@code <quest_area>} 绑定；真端世界文件 questscript_area 的落点）。 /
	 * Production quest-area grant table ({@code <quest_area>} bindings from the retail world files). */
	private static final String AI_AREAS = "/aion/definitions/compact/ai/ai-areas.xml";
	private static final String SWITCH_PROPERTY = "aion.quest.retailDriver";
	private static final int PRODUCTION_QUEST_COUNT = 6224;

	/** 与生产资源一致的 npc 模板文件清单。 / NPC template files, matching production resources. */
	private static final List<String> NPC_FILES = List.of("npc_template_200000_216188.xml",
		"npc_template_216189_235748.xml", "npc_template_235749_247606.xml", "npc_template_247607_270057.xml",
		"npc_template_270058_286320.xml", "npc_template_286321_800030.xml", "npc_template_800031_834289.xml",
		"npc_template_834290_885645.xml");

	private static volatile RetailQuestDriver instance;
	private static volatile boolean loadFailed;

	/** 保留清单里标记为 RETAIL_TABLE 的全部任务（按家族分派到各自编译器）。 / All retail-owned quests. */
	private final Set<Integer> retailOwned;
	private final Set<Integer> retailOwnedSimpleHunt;
	private final Set<Integer> retailOwnedSimpleSerialHunt;
	private final Set<Integer> retailOwnedSimpleUseItem;
	private final Set<Integer> retailOwnedSimpleItemPlay;
	private final Set<Integer> retailOwnedDataDriven;
	private final Set<Integer> retailOwnedSimpleTalk;
	private final Set<Integer> retailOwnedCombineTask;
	private final Set<Integer> retailOwnedSimpleCollectItem;
	private final RetailQuestCatalog catalog;
	private final RetailSimpleTalkTable simpleTalkTable;
	private final RetailCombineTaskTable combineTaskTable;
	private final RetailSimpleCollectItemTable simpleCollectItemTable;
	private final RetailRecipeIndex recipeIndex;
	private final RetailClientDialogExits clientDialogExits;
	private final RetailClientSummaryRows clientSummaryRows;
	private final RetailClientRewardNpcs clientRewardNpcs;
	private final RetailQuestAreaIndex questAreas;
	private final RetailQuestXmlTable retailTable;
	private final RetailNpcNameIndex npcIndex;
	private final RetailItemNameIndex itemIndex;
	private final Map<String, Integer> randomRewards;
	private final Map<Integer, Integer> nameIds;
	private final RetailSimpleSerialHuntTable serialHuntTable;
	private final RetailClientHuntStages clientHuntStages;
	private final RetailSimpleUseItemTable useItemTable;
	private final RetailClientUseItemReport useItemReport;
	private final RetailSimpleItemPlayTable itemPlayTable;
	private final RetailDataDrivenTable dataDrivenTable;
	private final RetailClientHandinPages clientHandinPages;
	/** 交互物 NPC 集（掉落箱的 ACTION_ITEM_USE 路由判据）。 / Interaction-object npc set. */
	private final RetailQuestUseItemNpcs interactionObjects;
	private final RetailClientTalkChainPages clientTalkChainPages;
	private final RetailClientTalkCollectChainPages clientTalkCollectChainPages;
	private final RetailClientTalkChainSteps chainSteps;
	private final RetailClientKillTargets clientKillTargets;
	private final RetailClientHuntProgressRows clientHuntProgressRows;
	private final RetailEnterAreaZoneResolution enterAreaZoneResolution;
	private final Map<Integer, Optional<CompiledQuestDefinition>> cache = new ConcurrentHashMap<>();
	/** 入口页契约快照（见 {@link #refreshAcceptEntryContract()}）。 / Accept entry page contract snapshot. */
	private volatile QuestDialogContract acceptEntryContract;
	private final Map<Integer, String> rejections = new ConcurrentHashMap<>();

	private RetailQuestDriver(Set<Integer> retailOwnedSimpleHunt, Set<Integer> retailOwnedSimpleSerialHunt,
			Set<Integer> retailOwnedSimpleUseItem, Set<Integer> retailOwnedSimpleItemPlay,
			Set<Integer> retailOwnedDataDriven,
			Set<Integer> retailOwnedSimpleTalk,
			Set<Integer> retailOwnedCombineTask, Set<Integer> retailOwnedSimpleCollectItem,
			RetailQuestCatalog catalog, RetailSimpleTalkTable simpleTalkTable,
			RetailCombineTaskTable combineTaskTable, RetailSimpleCollectItemTable simpleCollectItemTable,
			RetailRecipeIndex recipeIndex,
			RetailClientDialogExits clientDialogExits, RetailClientSummaryRows clientSummaryRows,
			RetailClientRewardNpcs clientRewardNpcs,
			RetailQuestXmlTable retailTable, RetailNpcNameIndex npcIndex, RetailItemNameIndex itemIndex,
			Map<String, Integer> randomRewards, Map<Integer, Integer> nameIds,
			RetailSimpleSerialHuntTable serialHuntTable, RetailClientHuntStages clientHuntStages,
			RetailSimpleUseItemTable useItemTable, RetailClientUseItemReport useItemReport,
			RetailSimpleItemPlayTable itemPlayTable,
			RetailQuestAreaIndex questAreas,
			RetailDataDrivenTable dataDrivenTable,
			RetailClientHandinPages clientHandinPages,
			RetailQuestUseItemNpcs interactionObjects,
			RetailClientTalkChainPages clientTalkChainPages,
			RetailClientTalkCollectChainPages clientTalkCollectChainPages,
			RetailClientTalkChainSteps chainSteps,
			RetailClientKillTargets clientKillTargets, RetailClientHuntProgressRows clientHuntProgressRows,
			RetailEnterAreaZoneResolution enterAreaZoneResolution) {
		this.retailOwnedSimpleHunt = retailOwnedSimpleHunt;
		this.retailOwnedSimpleSerialHunt = retailOwnedSimpleSerialHunt;
		this.retailOwnedSimpleUseItem = retailOwnedSimpleUseItem;
		this.retailOwnedSimpleItemPlay = retailOwnedSimpleItemPlay;
		this.retailOwnedDataDriven = retailOwnedDataDriven;
		this.retailOwnedSimpleTalk = retailOwnedSimpleTalk;
		this.retailOwnedCombineTask = retailOwnedCombineTask;
		this.retailOwnedSimpleCollectItem = retailOwnedSimpleCollectItem;
		this.retailOwned = new TreeSet<>();
		this.retailOwned.addAll(retailOwnedSimpleHunt);
		this.retailOwned.addAll(retailOwnedSimpleSerialHunt);
		this.retailOwned.addAll(retailOwnedSimpleUseItem);
		this.retailOwned.addAll(retailOwnedSimpleItemPlay);
		this.retailOwned.addAll(retailOwnedDataDriven);
		this.retailOwned.addAll(retailOwnedSimpleTalk);
		this.retailOwned.addAll(retailOwnedCombineTask);
		this.retailOwned.addAll(retailOwnedSimpleCollectItem);
		this.catalog = catalog;
		this.simpleTalkTable = simpleTalkTable;
		this.combineTaskTable = combineTaskTable;
		this.simpleCollectItemTable = simpleCollectItemTable;
		this.recipeIndex = recipeIndex;
		this.clientDialogExits = clientDialogExits;
		this.clientSummaryRows = clientSummaryRows;
		this.clientRewardNpcs = clientRewardNpcs;
		this.questAreas = questAreas;
		this.retailTable = retailTable;
		this.npcIndex = npcIndex;
		this.itemIndex = itemIndex;
		this.randomRewards = randomRewards;
		this.nameIds = nameIds;
		this.serialHuntTable = serialHuntTable;
		this.clientHuntStages = clientHuntStages;
		this.useItemTable = useItemTable;
		this.useItemReport = useItemReport;
		this.itemPlayTable = itemPlayTable;
		this.dataDrivenTable = dataDrivenTable;
		this.clientHandinPages = clientHandinPages;
		this.interactionObjects = interactionObjects;
		this.clientTalkChainPages = clientTalkChainPages;
		this.clientTalkCollectChainPages = clientTalkCollectChainPages;
		this.chainSteps = chainSteps;
		this.clientKillTargets = clientKillTargets;
		this.clientHuntProgressRows = clientHuntProgressRows;
		this.enterAreaZoneResolution = enterAreaZoneResolution;
	}

	/** 开关是否开启（默认 true；{@code -Daion.quest.retailDriver=false} 回退改造前）。 */
	public static boolean isEnabled() {
		return Boolean.parseBoolean(System.getProperty(SWITCH_PROPERTY, "true"));
	}

	/**
	 * 局部目录兼容入口：retail-owned 任务用真端定义替换/补入，装载失败返回原 XML 目录。
	 * 不能用于生产启动：退役 XML 已删除，失败回退将丢失任务；生产必须调用 {@link #overlayProduction}。
	 * Compatibility overlay for partial catalogs; production must use the fail-closed entry point.
	 */
	public static QuestCatalog overlay(QuestCatalog xmlCatalog) {
		Objects.requireNonNull(xmlCatalog, "xmlCatalog");
		if (!isEnabled()) {
			return xmlCatalog;
		}
		RetailQuestDriver driver;
		try {
			driver = currentOrLoad();
		} catch (RuntimeException | IOException e) {
			loadFailed = true;
			return xmlCatalog;
		}
		return driver.apply(xmlCatalog);
	}

	/**
	 * 生产装载入口：驱动不可用、保留清单缺行或任何真端 owner 未落到最终目录时拒绝启动。
	 * 关闭开关只在 XML 目录仍包含全部任务时才可回退，不能把已退役任务静默丢弃。
	 * Production entry point: fails closed unless every manifest owner is present in the catalog.
	 */
	public static QuestCatalog overlayProduction(QuestCatalog xmlCatalog) {
		Objects.requireNonNull(xmlCatalog, "xmlCatalog");
		boolean enabled = isEnabled();
		if (!enabled) {
			verifyProductionCoverage(xmlCatalog, xmlCatalog, false);
			return xmlCatalog;
		}
		RetailQuestDriver driver;
		try {
			driver = currentOrLoad();
		} catch (RuntimeException | IOException e) {
			loadFailed = true;
			throw new IllegalStateException("retail quest driver unavailable; retired XML cannot be restored", e);
		}
		QuestCatalog result = driver.apply(xmlCatalog);
		verifyProductionCoverage(xmlCatalog, result, true);
		return result;
	}

	/**
	 * 对完整生产目录逐 ID 核对来源与执行定义；局部测试桩不走此入口。
	 * Verifies exact production ownership; partial test catalogs use the lenient overlay instead.
	 */
	static void verifyProductionCoverage(QuestCatalog xmlCatalog, QuestCatalog actual, boolean enabled) {
		Map<Integer, String> owners = new HashMap<>();
		try {
			for (String line : lines(open(RETENTION_RESOURCE))) {
				if (line.startsWith("#") || line.isBlank()) {
					continue;
				}
				String[] parts = line.split("\t", -1);
				if (parts.length != 5 || (!"RETAIL_TABLE".equals(parts[1])
						&& !"XML_RETENTION".equals(parts[1]))) {
					throw new IllegalStateException("invalid retail quest owner row: " + line);
				}
				int questId;
				try {
					questId = Integer.parseInt(parts[0]);
				} catch (NumberFormatException e) {
					throw new IllegalStateException("invalid retail quest id in owner row: " + line, e);
				}
				if (questId <= 0) {
					throw new IllegalStateException("nonpositive retail quest id in owner row: " + line);
				}
				if (owners.putIfAbsent(questId, parts[1]) != null) {
					throw new IllegalStateException("duplicate retail quest owner: " + questId);
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("retail quest owner manifest unavailable", e);
		}
		if (owners.size() != PRODUCTION_QUEST_COUNT) {
			throw new IllegalStateException("retail quest owner manifest size " + owners.size()
				+ " != " + PRODUCTION_QUEST_COUNT);
		}
		List<Integer> missing = new java.util.ArrayList<>();
		List<Integer> wrongOwner = new java.util.ArrayList<>();
		for (Map.Entry<Integer, String> row : owners.entrySet()) {
			int questId = row.getKey();
			var xml = xmlCatalog.findEntry(questId);
			var entry = actual.findEntry(questId);
			if (entry.isEmpty()) {
				missing.add(questId);
			} else if (enabled && "RETAIL_TABLE".equals(row.getValue())) {
				if (xml.isPresent() || entry.orElseThrow().executable().isEmpty()) {
					wrongOwner.add(questId);
				}
			} else if (xml.isEmpty() || xml.orElseThrow() != entry.orElseThrow()) {
				wrongOwner.add(questId);
			}
		}
		for (QuestCatalogEntry entry : actual.entries()) {
			if (!owners.containsKey(entry.id())) {
				wrongOwner.add(entry.id());
			}
		}
		if (!missing.isEmpty() || !wrongOwner.isEmpty() || actual.entries().size() != owners.size()) {
			missing.sort(Integer::compareTo);
			wrongOwner.sort(Integer::compareTo);
			throw new IllegalStateException("retail production catalog incomplete: entries="
				+ actual.entries().size() + "/" + owners.size() + ", missing="
				+ missing.stream().limit(10).toList() + " (" + missing.size() + "), wrongOwner="
				+ wrongOwner.stream().limit(10).toList() + " (" + wrongOwner.size() + ")");
		}
	}

	/** 测试与诊断用：当前驱动实例。 / The loaded driver for tests and diagnostics. */
	public static Optional<RetailQuestDriver> current() {
		return Optional.ofNullable(instance);
	}

	/** 装载是否降级（真端资源不可用）。 / Whether the driver degraded on load. */
	public static boolean isDegraded() {
		return loadFailed;
	}

	/**
	 * 同一进程只装载一次真端表与索引，避免预加载线程和测试入口重复构建整个目录。
	 * Loads the retail tables and indexes once across concurrent production/preload callers.
	 */
	private static RetailQuestDriver currentOrLoad() throws IOException {
		RetailQuestDriver driver = instance;
		if (driver == null) {
			synchronized (RetailQuestDriver.class) {
				driver = instance;
				if (driver == null) {
					driver = load();
					instance = driver;
					loadFailed = false;
				}
			}
		}
		return driver;
	}

	private static RetailQuestDriver load() throws IOException {
		Set<Integer> retailOwnedHunt = new TreeSet<>();
		Set<Integer> retailOwnedSerialHunt = new TreeSet<>();
		Set<Integer> retailOwnedUseItem = new TreeSet<>();
		Set<Integer> retailOwnedItemPlay = new TreeSet<>();
		Set<Integer> retailOwnedDataDriven = new TreeSet<>();
		Set<Integer> retailOwnedTalk = new TreeSet<>();
		Set<Integer> retailOwnedCombine = new TreeSet<>();
		Set<Integer> retailOwnedCollectItem = new TreeSet<>();
		Map<Integer, String> reasons = new HashMap<>();
		for (String line : lines(open(RETENTION_RESOURCE))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t", -1);
			if ("RETAIL_TABLE".equals(parts[1])) {
				int questId = Integer.parseInt(parts[0]);
				if ("SimpleHunt".equals(parts[2])) {
					retailOwnedHunt.add(questId);
				} else if ("SimpleSerialHunt".equals(parts[2])) {
					retailOwnedSerialHunt.add(questId);
				} else if ("SimpleUseItem".equals(parts[2])) {
					retailOwnedUseItem.add(questId);
				} else if ("SimpleItemPlay".equals(parts[2])) {
					retailOwnedItemPlay.add(questId);
				} else if ("DataDriven".equals(parts[2])) {
					retailOwnedDataDriven.add(questId);
				} else if ("SimpleTalk".equals(parts[2])) {
					retailOwnedTalk.add(questId);
				} else if ("CombineTask".equals(parts[2])) {
					retailOwnedCombine.add(questId);
				} else if ("SimpleCollectItem".equals(parts[2])) {
					retailOwnedCollectItem.add(questId);
				}
			} else {
				reasons.put(Integer.parseInt(parts[0]), parts[1] + ":" + parts[3]);
			}
		}
		RetailSimpleHuntTable table;
		try (InputStream input = open(SIMPLE_HUNT_TABLE)) {
			table = RetailSimpleHuntTable.load(input);
		}
		RetailSimpleSerialHuntTable serialHuntTable;
		try (InputStream input = open(SIMPLE_SERIAL_HUNT_TABLE)) {
			serialHuntTable = RetailSimpleSerialHuntTable.load(input);
		}
		RetailQuestAreaIndex questAreas;
		try (InputStream input = open(AI_AREAS)) {
			questAreas = RetailQuestAreaIndex.load(input);
		}
		RetailSimpleUseItemTable useItemTable;
		try (InputStream input = open(SIMPLE_USE_ITEM_TABLE)) {
			useItemTable = RetailSimpleUseItemTable.load(input);
		}
		RetailClientUseItemReport useItemReport = RetailClientUseItemReport.defaultReport();
		RetailSimpleItemPlayTable itemPlayTable;
		try (InputStream input = open(SIMPLE_ITEM_PLAY_TABLE)) {
			itemPlayTable = RetailSimpleItemPlayTable.load(input);
		}
		RetailClientTalkChainSteps chainSteps = RetailClientTalkChainSteps.load();
		RetailDataDrivenTable dataDrivenTable;
		try (InputStream input = open(DATA_DRIVEN_TABLE)) {
			dataDrivenTable = RetailDataDrivenTable.load(input);
		}
		RetailSimpleTalkTable simpleTalkTable;
		try (InputStream input = open(SIMPLE_TALK_TABLE)) {
			simpleTalkTable = RetailSimpleTalkTable.load(input);
		}
		RetailCombineTaskTable combineTaskTable;
		try (InputStream input = open(COMBINE_TASK_TABLE)) {
			combineTaskTable = RetailCombineTaskTable.load(input);
		}
		RetailSimpleCollectItemTable simpleCollectItemTable;
		try (InputStream input = open(SIMPLE_COLLECT_ITEM_TABLE)) {
			simpleCollectItemTable = RetailSimpleCollectItemTable.load(input);
		}
		RetailRecipeIndex recipeIndex = RetailRecipeIndex.build(List.of(open(RECIPE_TEMPLATES)));
		RetailClientDialogExits clientDialogExits = RetailClientDialogExits.defaultExits();
		RetailClientSummaryRows clientSummaryRows;
		try (InputStream input = open(CLIENT_SUMMARY_ROWS)) {
			clientSummaryRows = RetailClientSummaryRows.load(input);
		}
		RetailClientRewardNpcs clientRewardNpcs = RetailClientRewardNpcs.defaultRewardNpcs();
		RetailClientHuntStages clientHuntStages = RetailClientHuntStages.defaultHuntStages();
		RetailClientHandinPages clientHandinPages = RetailClientHandinPages.defaultHandinPages();
		RetailClientTalkChainPages clientTalkChainPages = RetailClientTalkChainPages.defaultTalkChainPages();
		RetailClientTalkCollectChainPages clientTalkCollectChainPages =
			RetailClientTalkCollectChainPages.defaultTalkCollectChainPages();
		RetailClientKillTargets clientKillTargets;
		try (InputStream input = open(CLIENT_KILL_TARGETS)) {
			clientKillTargets = RetailClientKillTargets.load(input);
		}
		clientKillTargets = clientKillTargets.withDefaultStages();
		RetailClientHuntProgressRows clientHuntProgressRows = RetailClientHuntProgressRows.defaultHuntProgressRows();
		RetailEnterAreaZoneResolution enterAreaZoneResolution;
		try (InputStream input = open(ENTERAREA_ZONE_RESOLUTION)) {
			enterAreaZoneResolution = RetailEnterAreaZoneResolution.load(input);
		}
		RetailQuestXmlTable retailTable;
		try (InputStream input = open(RETAIL_QUEST_XML)) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		// 真端对话名组表（守备队同组共用 ScriptDLL 对话名）：接取/交付字段可写组名的唯一展开通道。
		// The retail dialog-name group table: the only expansion channel for group names written in
		// the acquire/hand-in fields (one guard squad shares a ScriptDLL dialog name).
		RetailNpcNameIndex npcIndex;
		try (InputStream groups = open(QUEST_AI_NAME_GROUPS)) {
			npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_FILES), List.of(groups));
		}
		RetailQuestUseItemNpcs interactionObjects =
			RetailQuestUseItemNpcs.fromIds(npcIndex.questUseItemNpcIds());
		RetailItemNameIndex itemIndex = RetailItemNameIndex.build(
			openAll(ITEM_DIR, listXmlNames(ITEM_DIR)));
		return new RetailQuestDriver(retailOwnedHunt, retailOwnedSerialHunt, retailOwnedUseItem,
			retailOwnedItemPlay, retailOwnedDataDriven, retailOwnedTalk, retailOwnedCombine, retailOwnedCollectItem,
			new RetailQuestCatalog(table, combineTaskTable, npcIndex), simpleTalkTable, combineTaskTable,
			simpleCollectItemTable, recipeIndex, clientDialogExits, clientSummaryRows, clientRewardNpcs,
			retailTable, npcIndex, itemIndex, randomRewardIds(), nameIds(), serialHuntTable, clientHuntStages,
			useItemTable, useItemReport, itemPlayTable, questAreas,
			dataDrivenTable, clientHandinPages, interactionObjects,
			clientTalkChainPages, clientTalkCollectChainPages, chainSteps, clientKillTargets,
			clientHuntProgressRows, enterAreaZoneResolution);
	}

	private QuestCatalog apply(QuestCatalog xmlCatalog) {
		List<QuestCatalogEntry> entries = new java.util.ArrayList<>();
		int replaced = 0;
		int added = 0;
		for (QuestCatalogEntry entry : xmlCatalog.entries()) {
			Optional<CompiledQuestDefinition> retail = definition(entry.id());
			if (retail.isPresent()) {
				entries.add(QuestCatalogEntry.executable(retail.orElseThrow()));
				replaced++;
			} else {
				entries.add(entry);
			}
		}
		for (int questId : retailOwned) {
			if (xmlCatalog.findEntry(questId).isEmpty()) {
				Optional<CompiledQuestDefinition> retail = definition(questId);
				retail.ifPresent(definition -> {
					entries.add(QuestCatalogEntry.executable(definition));
				});
				if (retail.isPresent()) {
					added++;
				}
			}
		}
		appliedReplaced = replaced;
		appliedAdded = added;
		return ImmutableQuestCatalog.fromEntries(entries);
	}

	private volatile int appliedReplaced;
	private volatile int appliedAdded;

	/** 最近一次 overlay 的替换/补入统计（诊断用）。 / Overlay stats for diagnostics. */
	public String overlayStats() {
		return "replaced=" + appliedReplaced + " added=" + appliedAdded;
	}

	/** 真端定义缓存（拒绝结果同样缓存，含拒绝码）。 / Cached outcome per quest, rejections included. */
	public Optional<CompiledQuestDefinition> definition(int questId) {
		if (!retailOwned.contains(questId)) {
			return Optional.empty();
		}
		refreshAcceptEntryContract();
		return cache.computeIfAbsent(questId, this::compile);
	}

	/**
	 * 接取入口页的客户端契约快照。任务热重载会 {@code QuestDialogContract.invalidateDefault()} 换掉契约
	 * 实例，此时丢弃逐任务定义缓存，让入口页按新契约重算；契约未变时只是一次 volatile 比较。
	 * Contract snapshot for the accept entry page. A quest hot reload swaps the contract instance
	 * ({@code QuestDialogContract.invalidateDefault()}), so the per-quest cache is dropped and the entry
	 * pages follow the new contract; an unchanged contract costs one volatile comparison.
	 */
	private void refreshAcceptEntryContract() {
		QuestDialogContract contract = QuestDialogContract.loadDefault();
		if (acceptEntryContract == contract) {
			return;
		}
		synchronized (this) {
			if (acceptEntryContract != contract) {
				cache.clear();
				acceptEntryContract = contract;
			}
		}
	}

	/** 拒绝码（未拒绝任务无映射）。 / The rejection code for a retail-owned quest, if any. */
	public Optional<String> rejectionCode(int questId) {
		return Optional.ofNullable(rejections.get(questId));
	}

	public int retailOwnedCount() {
		return retailOwned.size();
	}

	/**
	 * 合成一行并做接取入口页的客户端契约修复（真端表无对话页列，见
	 * {@link RetailClientAcceptEntryPage}）。
	 * Compiles one row and applies the client accept-entry-page contract repair (the retail tables
	 * carry no dialog-page column; see {@link RetailClientAcceptEntryPage}).
	 */
	private Optional<CompiledQuestDefinition> compile(int questId) {
		return compileByFamily(questId).map(definition ->
			RetailClientAcceptEntryPage.repair(definition, QuestDialogContract.loadDefault()));
	}

	private Optional<CompiledQuestDefinition> compileByFamily(int questId) {
		if (retailOwnedSimpleTalk.contains(questId)) {
			return compileSimpleTalk(questId);
		}
		if (retailOwnedCombineTask.contains(questId)) {
			return compileCombineTask(questId);
		}
		if (retailOwnedSimpleCollectItem.contains(questId)) {
			return compileSimpleCollectItem(questId);
		}
		if (retailOwnedSimpleSerialHunt.contains(questId)) {
			return compileSimpleSerialHunt(questId);
		}
		if (retailOwnedSimpleUseItem.contains(questId)) {
			return compileSimpleUseItem(questId);
		}
		if (retailOwnedSimpleItemPlay.contains(questId)) {
			return compileSimpleItemPlay(questId);
		}
		if (retailOwnedDataDriven.contains(questId)) {
			return compileDataDriven(questId);
		}
		return compileSimpleHunt(questId);
	}

	/** CombineTask 行 → 定义（真端表 + 配方索引 + 元数据交叉校验；其余形态按稳定码拒绝）。 */
	private Optional<CompiledQuestDefinition> compileCombineTask(int questId) {
		try {
			var row = combineTaskTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var outcome = RetailCombineTaskDefinitionCompiler.compile(row.orElseThrow(), npcIndex, itemIndex,
				recipeIndex, metadata);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** SimpleCollectItem 行 → 定义（交付检查 + 完成流；异形按稳定码拒绝留 XML）。 */
	private Optional<CompiledQuestDefinition> compileSimpleCollectItem(int questId) {
		try {
			var row = simpleCollectItemTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var outcome = RetailSimpleCollectItemDefinitionCompiler.compile(row.orElseThrow(), npcIndex, metadata,
				clientDialogExits, clientSummaryRows, clientRewardNpcs);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** SimpleSerialHunt 行 → 定义（客户端链式阶段契约的串行阶梯；乱序击杀不计数）。 */
	private Optional<CompiledQuestDefinition> compileSimpleSerialHunt(int questId) {
		try {
			var row = serialHuntTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var entry = row.orElseThrow();
			Set<Integer> acquired = npcIndex.resolveAll(
				List.of(entry.acquiredNpc() == null ? "" : entry.acquiredNpc())).npcIds();
			Set<Integer> reward = npcIndex.resolveAll(
				List.of(entry.rewardNpc() == null ? "" : entry.rewardNpc())).npcIds();
			String talkNpcName = entry.talkNpc() == null ? "" : entry.talkNpc();
			Set<Integer> briefing = npcIndex.resolveAll(List.of(talkNpcName)).npcIds();
			var plan = new RetailSimpleHuntPlan(questId, List.of(), acquired, reward, 0, List.of(),
				entry.acquiredNpc() == null ? "" : entry.acquiredNpc(),
				entry.rewardNpc() == null ? "" : entry.rewardNpc());
			var outcome = RetailSimpleHuntDefinitionCompiler.compileSerialChain(plan, clientHuntStages, metadata,
				briefing, talkNpcName);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** SimpleUseItem 行 → 定义（用物品接取 + 无目标对话 + 报告/完成；异形按稳定码拒绝留 XML）。 */
	private Optional<CompiledQuestDefinition> compileSimpleUseItem(int questId) {
		try {
			var row = useItemTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var outcome = RetailSimpleUseItemDefinitionCompiler.compile(row.orElseThrow(), itemIndex, npcIndex,
				metadata, clientSummaryRows, useItemReport, clientDialogExits);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** SimpleItemPlay 行 → 定义（接取发物 → 用物品演出 → 交付；异形按稳定码拒绝留 XML）。 */
	private Optional<CompiledQuestDefinition> compileSimpleItemPlay(int questId) {
		try {
			var row = itemPlayTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var outcome = RetailSimpleItemPlayDefinitionCompiler.compile(row.orElseThrow(), itemIndex, npcIndex,
				metadata);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** DataDriven 行 → 定义（P5-1：Talk 接取 + 单块 Hunt 进度复用 hunt 网格；异形按稳定码拒绝）。 */
	private Optional<CompiledQuestDefinition> compileDataDriven(int questId) {
		try {
			var row = dataDrivenTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var outcome = RetailDataDrivenDefinitionCompiler.compile(row.orElseThrow(), itemIndex, npcIndex,
				metadata, clientRewardNpcs, questAreas, clientDialogExits,
				clientSummaryRows, clientHandinPages,
				interactionObjects, clientTalkChainPages, clientKillTargets,
				clientTalkCollectChainPages, clientHuntProgressRows, enterAreaZoneResolution);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** SimpleTalk 行 → 定义（单步 / 交付检查形态；其余形态由编译器按稳定码拒绝）。 */
	private Optional<CompiledQuestDefinition> compileSimpleTalk(int questId) {
		try {
			var row = simpleTalkTable.find(questId);
			if (row.isEmpty()) {
				rejections.put(questId, "RETAIL_ROW_MISSING");
				return Optional.empty();
			}
			var metadata = retailMetadata(questId);
			if (metadata == null) {
				return Optional.empty();
			}
			var outcome = RetailSimpleTalkDefinitionCompiler.compile(row.orElseThrow(), npcIndex, metadata,
				clientDialogExits, clientSummaryRows, clientRewardNpcs, interactionObjects, chainSteps);
			if (outcome.accepted()) {
				return Optional.of(outcome.definition());
			}
			rejections.put(questId, outcome.rejectionCode());
			return Optional.empty();
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	/** 真端 quest.xml 元数据（缺行记 RETAIL_ROW_MISSING）。 / Retail metadata for a quest row. */
	private RetailQuestMetadataCompiler.Outcome retailMetadata(int questId) {
		var row = retailTable.find(questId);
		if (row.isEmpty()) {
			rejections.put(questId, "RETAIL_ROW_MISSING");
			return null;
		}
		return RetailQuestMetadataCompiler.compile(row.orElseThrow(), npcIndex, itemIndex, randomRewards, nameIds,
			RetailSpawnedNpcIds.load());
	}

	private Optional<CompiledQuestDefinition> compileSimpleHunt(int questId) {
		try {
			return catalog.simpleHuntPlan(questId).flatMap(plan -> {
				var row = retailTable.find(questId);
				if (row.isEmpty()) {
					rejections.put(questId, "RETAIL_ROW_MISSING");
					return Optional.empty();
				}
				var metadata = RetailQuestMetadataCompiler.compile(row.orElseThrow(), npcIndex, itemIndex,
					randomRewards, nameIds, RetailSpawnedNpcIds.load());
				var outcome = RetailSimpleHuntDefinitionCompiler.compile(plan, metadata, clientRewardNpcs,
					questAreas, clientDialogExits);
				if (outcome.accepted()) {
					return Optional.of(outcome.definition());
				}
				rejections.put(questId, outcome.rejectionCode());
				return Optional.empty();
			});
		} catch (RuntimeException e) {
			rejections.put(questId, "RUNTIME_FAILURE");
			return Optional.empty();
		}
	}

	// ---------------------------------------------------------------- 资源装载

	private static Map<String, Integer> randomRewardIds() throws IOException {
		Map<String, Integer> ids = new HashMap<>();
		try (InputStream input = open(RANDOM_REWARDS)) {
			var document = parse(input);
			var nodes = document.getDocumentElement().getElementsByTagName("quest_random_reward");
			for (int index = 0; index < nodes.getLength(); index++) {
				var element = (org.w3c.dom.Element) nodes.item(index);
				ids.put(element.getElementsByTagName("name").item(0).getTextContent().trim(),
					Integer.parseInt(element.getElementsByTagName("id").item(0).getTextContent().trim()));
			}
		}
		return ids;
	}

	private static Map<Integer, Integer> nameIds() throws IOException {
		Map<Integer, Integer> ids = new HashMap<>();
		for (String line : lines(open(NAME_IDS_TSV))) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			String[] parts = line.split("\t");
			ids.put(Integer.parseInt(parts[0].substring("STR_QUEST_NAME_Q".length())), Integer.parseInt(parts[1]));
		}
		return ids;
	}

	private static List<String> listXmlNames(String dir) throws IOException {
		var url = RetailQuestDriver.class.getResource(dir);
		if (url == null) {
			return List.of();
		}
		if ("file".equals(url.getProtocol())) {
			try {
				java.io.File[] files = new java.io.File(url.toURI()).listFiles(
					(directory, name) -> name.endsWith(".xml"));
				if (files == null) {
					return List.of();
				}
				return java.util.Arrays.stream(files).map(java.io.File::getName).sorted().toList();
			} catch (java.net.URISyntaxException e) {
				throw new IOException("bad resource dir " + dir, e);
			}
		}
		// jar 协议：按目录前缀枚举（与 QuestDefinitionDirectoryLoader 同思路）。
		if ("jar".equals(url.getProtocol())) {
			try {
				var connection = (java.net.JarURLConnection) url.openConnection();
				connection.setUseCaches(false);
				try (var jar = connection.getJarFile()) {
					String prefix = dir.substring(1);
					List<String> names = new java.util.ArrayList<>();
					var entries = jar.entries();
					while (entries.hasMoreElements()) {
						String name = entries.nextElement().getName();
						if (name.startsWith(prefix) && name.endsWith(".xml")
							&& name.indexOf('/', prefix.length()) < 0) {
							names.add(name.substring(prefix.length()));
						}
					}
					java.util.Collections.sort(names);
					return names;
				}
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException("jar listing failed for " + dir, e);
			}
		}
		throw new IOException("unsupported resource protocol " + url.getProtocol());
	}

	private static org.w3c.dom.Document parse(InputStream input) throws IOException {
		try (input) {
			var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			return factory.newDocumentBuilder().parse(input);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException("xml parse failed", e);
		}
	}

	private static List<InputStream> openAll(String dir, List<String> files) throws IOException {
		List<InputStream> inputs = new java.util.ArrayList<>();
		for (String file : files) {
			inputs.add(open(dir + file));
		}
		return inputs;
	}

	private static List<String> lines(InputStream input) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return reader.lines().toList();
		}
	}

	private static InputStream open(String resource) throws IOException {
		InputStream input = RetailQuestDriver.class.getResourceAsStream(resource);
		if (input == null) {
			throw new IOException("missing resource " + resource);
		}
		return input;
	}
}
