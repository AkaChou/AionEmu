package com.aionemu.gameserver.questEngine.retail;

import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.definition.QuestCatalogEntry;

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
import java.util.concurrent.ConcurrentHashMap;
import com.aionemu.gameserver.questEngine.tablelane.DataDrivenNativeRuntime;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCollectItemHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleCombineTaskHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleItemPlayHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleSerialHuntHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleTalkHandler;
import com.aionemu.gameserver.questEngine.tablelane.SimpleUseItemHandler;

/**
 * 生产装载链的"真端优先"驱动（提示词 §4.C 的 overlay 落点）。
 * <p>
 * P7 步 f 起七族（SimpleHunt/SimpleSerialHunt/SimpleTalk/SimpleCollectItem/SimpleUseItem/
 * SimpleItemPlay/CombineTask）与 DataDriven 1467 行全部由 tablelane 原生 handler/运行时直驱，
 * 旧 IR 编译车道已随切换原子退场；本类只剩三个职责：
 * <ol>
 *   <li><b>真端 quest.xml 元数据底座</b>（{@link #retailMetadataOf}，native 完成/领奖口的唯一事实来源）；</li>
 *   <li><b>生产覆盖校验</b>（{@link #overlayProduction}：保留清单 6224 行逐 id 核对 = 目录条目 ∨
 *       原生 owner ∨ DD 运行时 owned〔routed 或显式冻结〕，缺一即拒启）；</li>
 *   <li><b>直通 overlay</b>（目录原样返回；保留清单内任务一律维持 XML）。</li>
 * </ol>
 * The retail-first driver. Since P7 step f every family (SimpleHunt/SimpleSerialHunt/SimpleTalk/
 * SimpleCollectItem/SimpleUseItem/SimpleItemPlay/CombineTask) and the 1467 DataDriven rows are driven
 * natively by the tablelane handlers/runtime, and the old IR compile lane retired atomically with the
 * switch. Three duties remain: the retail quest.xml metadata base for the native completion port, the
 * fail-closed production coverage check (catalog entry ∨ native owner ∨ DD-runtime owned), and the
 * pass-through overlay.
 */
public final class RetailQuestDriver {

	/** 保留清单资源（quest_id, owner, family, reason, evidence）。 / The retention manifest resource. */
	private static final String RETENTION_RESOURCE =
		"/aion/data/static_data/quest/retail/retail-xml-retention.tsv";
	private static final String RETAIL_QUEST_XML = "/aion/data/static_data/quest/retail/quest.xml";
	private static final String NAME_IDS_TSV = "/aion/data/static_data/quest/retail/quest_name_string_ids.tsv";
	private static final String RANDOM_REWARDS = "/aion/data/static_data/quest/legacy/quest_random_rewards.xml";
	private static final String NPC_DIR = "/aion/data/static_data/npcs/";
	private static final String SWITCH_PROPERTY = "aion.quest.retailDriver";
	private static final int PRODUCTION_QUEST_COUNT = 6224;

	/** 与生产资源一致的 npc 模板文件清单。 / NPC template files, matching production resources. */
	private static final List<String> NPC_FILES = List.of("npc_template_200000_216188.xml",
		"npc_template_216189_235748.xml", "npc_template_235749_247606.xml", "npc_template_247607_270057.xml",
		"npc_template_270058_286320.xml", "npc_template_286321_800030.xml", "npc_template_800031_834289.xml",
		"npc_template_834290_885645.xml");

	private static volatile RetailQuestDriver instance;
	private static volatile boolean loadFailed;

	private final RetailQuestXmlTable retailTable;
	private final RetailNpcNameIndex npcIndex;
	private final RetailItemNameIndex itemIndex;
	private final Map<String, Integer> randomRewards;
	private final Map<Integer, Integer> nameIds;
	/** 真端 quest.xml 元数据缓存（native 完成口与目录构建共用同一条编译器）。 /
	 * Retail quest.xml metadata cache shared by the native completion port and the catalog build. */
	private final Map<Integer, Optional<RetailQuestMetadataCompiler.Outcome>> metadataCache =
		new ConcurrentHashMap<>();

	private RetailQuestDriver(RetailQuestXmlTable retailTable,
			RetailNpcNameIndex npcIndex, RetailItemNameIndex itemIndex, Map<String, Integer> randomRewards,
			Map<Integer, Integer> nameIds) {
		this.retailTable = retailTable;
		this.npcIndex = npcIndex;
		this.itemIndex = itemIndex;
		this.randomRewards = randomRewards;
		this.nameIds = nameIds;
	}

	/** 开关是否开启（默认 true；{@code -Daion.quest.retailDriver=false} 回退改造前）。 */
	public static boolean isEnabled() {
		return Boolean.parseBoolean(System.getProperty(SWITCH_PROPERTY, "true"));
	}

	/**
	 * 局部目录兼容入口：目录原样返回（P7 步 f 起无 retail 编译产物，保留任务一律维持 XML）。
	 * 不能用于生产启动：退役 XML 已删除，失败回退将丢失任务；生产必须调用 {@link #overlayProduction}。
	 * Compatibility overlay for partial catalogs; the catalog passes through untouched (no retail
	 * compile products remain). Production must use the fail-closed entry point.
	 */
	public static QuestCatalog overlay(QuestCatalog xmlCatalog) {
		Objects.requireNonNull(xmlCatalog, "xmlCatalog");
		if (!isEnabled()) {
			return xmlCatalog;
		}
		try {
			currentOrLoad();
		} catch (RuntimeException | IOException e) {
			loadFailed = true;
			return xmlCatalog;
		}
		return xmlCatalog;
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
		try {
			currentOrLoad();
		} catch (RuntimeException | IOException e) {
			loadFailed = true;
			throw new IllegalStateException("retail quest driver unavailable; retired XML cannot be restored", e);
		}
		verifyProductionCoverage(xmlCatalog, xmlCatalog, true);
		return xmlCatalog;
	}

	/**
	 * 对完整生产目录逐 ID 核对来源与执行定义；局部测试桩不走此入口。
	 * <p>
	 * 覆盖三分册（P7 步 f 起）：目录条目 ∨ 原生 handler owns ∨ DD 运行时 owned（可路由或**显式冻结**
	 * ——冻结行带 {@code FreezeReason} 登记 §10.3，非静默丢弃）。DataDriven 旧编译车道已退场，
	 * 其 1467 行全部由 {@link DataDrivenNativeRuntime} 接管。
	 * Verifies exact production ownership; partial test catalogs use the lenient overlay instead.
	 * Coverage since step f: catalog entry ∨ native handler owner ∨ DD-runtime owned (routed or
	 * explicitly frozen with a registered FreezeReason — never silently dropped).
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
					throw new IllegalStateException("nonpositive retail quest id in owner row: " + questId);
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
		int nativeCoveredCount = 0;
		for (Map.Entry<Integer, String> row : owners.entrySet()) {
			int questId = row.getKey();
			var xml = xmlCatalog.findEntry(questId);
			var entry = actual.findEntry(questId);
			if (entry.isEmpty()) {
				if ((SimpleHuntHandler.instance().owns(questId) || SimpleSerialHuntHandler.instance().owns(questId)
						|| SimpleTalkHandler.instance().owns(questId)
						|| SimpleCollectItemHandler.instance().owns(questId)
						|| SimpleUseItemHandler.instance().owns(questId)
						|| SimpleItemPlayHandler.instance().owns(questId)
						|| SimpleCombineTaskHandler.instance().owns(questId)
						|| DataDrivenNativeRuntime.instance().owns(questId))
						&& "RETAIL_TABLE".equals(row.getValue())) {
					nativeCoveredCount++;
					continue;
				}
				missing.add(questId);
			} else if (enabled && "RETAIL_TABLE".equals(row.getValue())) {
				if (xml.isPresent()) {
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
		int totalCovered = actual.entries().size() + nativeCoveredCount;
		if (!missing.isEmpty() || !wrongOwner.isEmpty() || totalCovered != owners.size()) {
			missing.sort(Integer::compareTo);
			wrongOwner.sort(Integer::compareTo);
			throw new IllegalStateException("retail production catalog incomplete: entries="
				+ totalCovered + "/" + owners.size() + ", missing="
				+ missing.stream().limit(10).toList() + " (" + missing.size() + "), wrongOwner="
				+ wrongOwner.stream().limit(10).toList() + " (" + wrongOwner.size() + ")");
		}
	}

	/** 测试与诊断用：当前驱动实例。 / The loaded driver for tests and diagnostics. */
	public static Optional<RetailQuestDriver> current() {
		return Optional.ofNullable(instance);
	}

	/**
	 * 真端 {@code quest.xml} 行的规范元数据（native 完成/领奖口的数据底座）。
	 * <p>
	 * 与生产目录走同一条 {@link RetailQuestMetadataCompiler}（同 npc/物品/随机奖励/name id 索引），
	 * 按任务缓存；缺行返回 empty。奖励面因此只有一个事实来源 = 真端表列本身，不引入 IR。
	 * <p>
	 * Canonical retail {@code quest.xml} metadata for the native completion port: the very compiler
	 * (and indexes) that build the production catalog, cached per quest id; empty when the row is
	 * absent. The reward face therefore has a single fact source — the retail columns — and no IR.
	 */
	public Optional<RetailQuestMetadataCompiler.Outcome> retailMetadataOf(int questId) {
		if (retailTable.find(questId).isEmpty()) {
			return Optional.empty();
		}
		return metadataCache.computeIfAbsent(questId, id -> Optional.of(RetailQuestMetadataCompiler.compile(
			retailTable.find(id).orElseThrow(), npcIndex, itemIndex, randomRewards, nameIds,
			RetailSpawnedNpcIds.load())));
	}

	/** 装载是否降级（真端资源不可用）。 / Whether the driver degraded on load. */
	public static boolean isDegraded() {
		return loadFailed;
	}

	/**
	 * 确保真端驱动已按类路径资源装载并返回（供 native 车道只读消费元数据，不触发目录覆盖校验）。
	 * Ensures the retail driver is loaded from classpath resources and returns it; the native lane
	 * consumes metadata read-only through this entry without running catalog overlay verification.
	 */
	public static RetailQuestDriver ensureLoaded() throws IOException {
		return currentOrLoad();
	}

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

	/**
	 * 装载真端表与索引（P7 步 f 后 = 元数据底座；家族切换史：SimpleHunt 939 / SimpleSerialHunt 16 /
	 * SimpleTalk 3152 / SimpleCollectItem 262 / SimpleUseItem 160 / SimpleItemPlay 43 / CombineTask 574
	 * 已随 P1-P6 原生直驱，DataDriven 1467 随 P7 步 f 由 {@link DataDrivenNativeRuntime} 接管，
	 * 旧 IR 编译车道原子退场）。归属核验见 {@link #verifyProductionCoverage}。
	 * Loads the retail tables and indexes (post step f: the metadata base). Family switch history:
	 * P1-P6 went native batch by batch; DataDriven's 1467 rows moved to the DD runtime in step f and
	 * the old IR compile lane retired atomically. Ownership is verified by verifyProductionCoverage.
	 */
	private static RetailQuestDriver load() throws IOException {
		RetailQuestXmlTable retailTable;
		try (InputStream input = open(RETAIL_QUEST_XML)) {
			retailTable = RetailQuestXmlTable.load(input);
		}
		// 真端对话名组表（守备队同组共用 ScriptDLL 对话名）：接取/交付字段可写组名的唯一展开通道。
		// The retail dialog-name group table: the only expansion channel for group names written in
		// the acquire/hand-in fields (one guard squad shares a ScriptDLL dialog name).
		RetailNpcNameIndex npcIndex = RetailNpcNameIndex.build(openAll(NPC_DIR, NPC_FILES),
			RetailQuestAiNameGroups.streams(), RetailNpcNameAliases.streams());
		RetailItemNameIndex itemIndex = RetailItemNameIndex.loadItemTemplates();
		return new RetailQuestDriver(retailTable, npcIndex, itemIndex, randomRewardIds(), nameIds());
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
