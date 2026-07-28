package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.AIData;
import com.aionemu.gameserver.dataholders.ChargeSkillData;
import com.aionemu.gameserver.dataholders.HotspotLocationData;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.MotionData;
import com.aionemu.gameserver.dataholders.NpcDropData;
import com.aionemu.gameserver.dataholders.NpcPathBehaviorData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.PetDopingData;
import com.aionemu.gameserver.dataholders.PetMerchandData;
import com.aionemu.gameserver.dataholders.RetailAiData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.dataholders.WalkerData;
import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraphData;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler;
import com.aionemu.gameserver.questEngine.graph.QuestGraphCompiler.References;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * 负责通过 JAXB 加载静态数据 XML；配合 {@link XmlMerger} 将多文件合并为缓存后反序列化。
 * Loads static-data XML via JAXB; uses {@link XmlMerger} to merge source files into a cache before unmarshalling.
 *
 * @author Luno
 */
@Slf4j
public class XmlDataLoader {

	private static volatile ObjectProvider<XmlDataLoader> instanceProvider;
	/** XML Schema 声明文件路径 / path to the XML schema declaration file */
	private final static String XML_SCHEMA_FILE = "./data/static_data/static_data.xsd";
	private static final String CACHE_XML_FILE = "./cache/static_data.xml";
	private static final String MAIN_XML_FILE = "./data/static_data/static_data.xml";
	private static final String QUEST_GRAPH_CACHE_XML_FILE = "./cache/quest_graph_data.xml";
	private static final String QUEST_GRAPH_MAIN_XML_FILE = "./data/static_data/quest_graph_data/quest_graph_data.xml";
	private static final String QUEST_GRAPH_SCHEMA_FILE = "./data/static_data/quest_graph_data/quest_graph_data.xsd";
	private static final String ITEM_CACHE_XML_FILE = "./cache/item_templates.xml";
	private static final String ITEM_DATA_DIR = "./data/static_data/items";
	private static final String NPC_DROP_DEFINITIONS_DIR = "./definitions/compact/npc_drops";
	private static final String NPC_COMBAT_DEFINITIONS_FILE = "./definitions/compact/npc-combat.xml";
	private static final String HOTSPOT_LOCATION_DEFINITIONS_FILE = "./definitions/compact/hotspot_location/hotspot_location.xml";
	private static final String SKILL_DEFINITIONS_DIR = "./definitions/compact/skills";
	private static final String NPC_SKILL_DEFINITIONS_FILE = SKILL_DEFINITIONS_DIR + "/npc-skills.xml";
	private static final String MOTION_DEFINITIONS_FILE = SKILL_DEFINITIONS_DIR + "/motion_times.xml";
	private static final String CHARGE_SKILL_DEFINITIONS_FILE = SKILL_DEFINITIONS_DIR + "/charge_skills.xml";
	private static final String AI_DEFINITIONS_DIR = "./definitions/compact/ai";
	private static final String AI_BOMBS_FILE = AI_DEFINITIONS_DIR + "/bombs.xml";
	private static final String AI_SPAWN_HELPERS_FILE = AI_DEFINITIONS_DIR + "/spawn_helpers.xml";
	private static final String NPC_PATH_BEHAVIOR_FILE = "./definitions/compact/ai/npc-ai.xml";
	private static final String RETAIL_NPC_AI_MAPPINGS_FILE = AI_DEFINITIONS_DIR + "/npc-ai.xml";
	private static final String RETAIL_AI_STRINGS_FILE = AI_DEFINITIONS_DIR + "/ai-strings.xml";
	private static final String RETAIL_AI_AREAS_FILE = AI_DEFINITIONS_DIR + "/ai-areas.xml";
	private static final String RETAIL_AI_LOCATION_ALIASES_FILE = AI_DEFINITIONS_DIR + "/ai-location-aliases.xml";
	private static final String RETAIL_DIRECT_PORTALS_FILE = AI_DEFINITIONS_DIR + "/direct-portals.xml";
	private static final String RETAIL_CONDITION_SPAWNS_FILE = AI_DEFINITIONS_DIR + "/condition-spawns.xml";
	private static final String RETAIL_SKILL_CATEGORIES_FILE = SKILL_DEFINITIONS_DIR + "/skill-categories.xml";
	private static final String RETAIL_NPC_SCORES_FILE = AI_DEFINITIONS_DIR + "/npc-scores.xml";
	private static final String RETAIL_GROUP_CONTROLLERS_FILE = AI_DEFINITIONS_DIR + "/group-controllers.xml";
	private static final String RETAIL_NPC_PARTIES_FILE = AI_DEFINITIONS_DIR + "/npc-parties.xml";
	private static final String RETAIL_DYNAMIC_AREAS_FILE = AI_DEFINITIONS_DIR + "/dynamic-areas.xml";
	private static final String RETAIL_AI_WAYPOINTS_FILE = AI_DEFINITIONS_DIR + "/ai-waypoints.xml";
	private static final String RETAIL_AI_WAYPOINTS_SCHEMA = "./definitions/schemas/ai-waypoints.xsd";
	private static final String PET_RIDES_DEFINITIONS_FILE = "./definitions/compact/pets-rides.xml";
	private static final String FLY_PATH_DEFINITIONS_FILE = "./definitions/compact/world/fly_path.xml";
	private static final String WIND_DEFINITIONS_FILE = "./definitions/compact/wind.xml";
	private static final String ID_DEFINITIONS_FILE = "./definitions/compact/id-mappings.xml";
	private static final String ITEM_SOURCE_XML = "<item_templates><import file=\"item\" skipRoot=\"true\"/></item_templates>";

	/**
	 * 获取 XmlDataLoader 单例（优先 Spring 提供的实例）。
	 * Returns the XmlDataLoader singleton (Spring-provided if available).
	 *
	 * XmlDataLoader instance
	 */
	public static final XmlDataLoader getInstance() {
		ObjectProvider<XmlDataLoader> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/**
	 * 注入 Spring 侧实例提供者，供容器接管单例解析。
	 * Sets the Spring ObjectProvider used to resolve the singleton.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<XmlDataLoader> provider) {
		instanceProvider = provider;
	}

	private static volatile Future<JAXBContext> preloadedContext;

	/**
	 * 异步预加载 StaticData 的 JAXBContext，以便反序列化开始时上下文已就绪。
	 * Asynchronously preloads the StaticData JAXBContext so it is ready when unmarshalling begins.
	 * <p>参考 aion-server {@code JAXBUtil.preLoadContextAsync}，应在启动早期调用。
	 * Modeled after aion-server JAXBUtil.preLoadContextAsync; call as early as possible during startup.
	 */
	public static void preloadContextAsync() {
		if (preloadedContext == null) {
			preloadedContext = ForkJoinPool.commonPool().submit(() -> createJaxbContext(StaticData.class));
		}
	}

	/** 默认构造函数 / default constructor */
	public XmlDataLoader() {

	}

	/**
	 * 从 static_data.xml 出发加载并合并 XML，返回 {@link StaticData}。
	 * Loads and merges XML starting from static_data.xml and returns {@link StaticData}.
	 *
	 * @return 包含全部游戏静态数据的对象 / object containing all game static data from XML
	 */
	public StaticData loadStaticData() {
		return loadStaticData(this::loadSkillData);
	}

	/**
	 * 合并、校验并编译配置目录中的任务图数据。
	 * Merges, validates, and compiles quest graph data from the configured data directory.
	 *
	 * @param references 可引用的任务与 NPC / allowed quest and NPC references
	 * @return 已编译任务图数据 / compiled quest graph data
	 */
	public CompiledQuestGraphData loadQuestGraphData(References references) {
		return loadQuestGraphData(Config.dataFile(QUEST_GRAPH_MAIN_XML_FILE), Config.cacheFile(QUEST_GRAPH_CACHE_XML_FILE),
			Config.dataFile(QUEST_GRAPH_SCHEMA_FILE), references);
	}

	/**
	 * 使用显式源文件、缓存和 XSD 加载任务图，供启动流程和聚焦测试复用。
	 * Loads quest graphs from explicit source, cache, and XSD files for startup and focused tests.
	 */
	static CompiledQuestGraphData loadQuestGraphData(File sourceFile, File cacheFile, File schemaFile, References references) {
		try {
			File cacheDirectory = cacheFile.getParentFile();
			if (cacheDirectory != null && !cacheDirectory.exists() && !cacheDirectory.mkdirs()) {
				throw new IOException("Could not create cache directory " + cacheDirectory);
			}
			new XmlMerger(sourceFile, cacheFile).process();
			return QuestGraphCompiler.load(cacheFile.toPath(), schemaFile.toPath(), references);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load quest graph data from " + sourceFile, e);
		}
	}

	/**
	 * 使用可预先启动的技能数据提供器加载静态数据。
	 * Loads static data with a skill-data supplier that callers may start in advance.
	 */
	public StaticData loadStaticData(Supplier<SkillData> skillDataSupplier) {
		return loadStaticData(ConsoleStaticDataProgressReporter.forCurrentConsole(), skillDataSupplier);
	}

	/**
	 * 带进度回调的静态数据加载：合并缓存、可选校验、JAXB 反序列化。
	 * Loads static data with a progress reporter: merge cache, optional validation, JAXB unmarshalling.
	 *
	 * @param progressReporter 进度报告器 / progress reporter
	 * @return 静态数据，失败则为 null / static data, or null on failure
	 */
	StaticData loadStaticData(StaticDataProgressReporter progressReporter) {
		return loadStaticData(progressReporter, this::loadSkillData);
	}

	private StaticData loadStaticData(StaticDataProgressReporter progressReporter, Supplier<SkillData> skillDataSupplier) {
		File cachedXml = Config.cacheFile(CACHE_XML_FILE);
		makeCacheDirectory(cachedXml.getParentFile());
		File cleanMainXml = Config.dataFile(MAIN_XML_FILE);
		long cacheStart = System.currentTimeMillis();
		log.info(I18n.get("log.51b6cf8ee647", cachedXml.getPath()));
		boolean cacheRebuilt = mergeXmlFiles(cachedXml, cleanMainXml);
		log.info(I18n.get("log.402b7307998c", System.currentTimeMillis() - cacheStart));
		if (cacheRebuilt) {
			validateCacheAsync(cachedXml);
		}

		try {
			long unmarshalStart = System.currentTimeMillis();
			Map<String, Integer> sectionEntryCounts = loadSectionEntryCounts(cachedXml);
			int totalSections = sectionEntryCounts.size();
			progressReporter.start(totalSections);
			log.info(I18n.get("log.113840e671fd", cachedXml.getPath()));
			StaticDataProgressListener progressListener = new StaticDataProgressListener(progressReporter, totalSections, sectionEntryCounts);
			Unmarshaller un = createStaticDataUnmarshaller(progressListener);
			try (FileReader reader = new FileReader(cachedXml)) {
				StaticData data = (StaticData) un.unmarshal(reader);
				NpcCombatDefinitionLoader.apply(Config.definitionFile(NPC_COMBAT_DEFINITIONS_FILE), data.npcData);
				PetDefinitionLoader.Result petDefinitions = loadPetDefinitions();
				data.npcDropData = loadNpcDropData();
				data.hotspotLocationData = loadHotspotLocationData();
				data.skillData = skillDataSupplier.get();
				data.motionData = loadMotionData();
				data.chargeSkillData = loadChargeSkillData();
				data.npcSkillData = loadNpcSkillData();
				data.aiData = loadAiData();
				data.npcPathBehaviorData = loadNpcPathBehaviorData();
				data.retailAiData = loadRetailAiData();
				data.walkerData.merge(loadRetailAiWaypointData());
				data.petDopingData = petDefinitions.doping();
				data.petMerchandData = petDefinitions.merchant();
				data.windstreamsData = loadWindstreamData();
				data.logSummary();
				long elapsed = System.currentTimeMillis() - unmarshalStart;
				progressReporter.finish(totalSections, elapsed);
				logSlowSectionTimings(progressListener.sectionElapsedTimes());
				log.info(I18n.get("log.20818547282f", elapsed));
				return data;
			}
		}
		/*
		 * catch (IllegalAnnotationsException e) {
		 * log.error(I18n.get("log.a30b9e9db6fa", e)); throw new
		 * Error("Error while loading static data", e); } catch (FileNotFoundException
		 * e) { log.error(I18n.get("log.a30b9e9db6fa", e)); throw new
		 * Error("Error while loading static data", e); } catch (JAXBException e) {
		 * log.error(I18n.get("log.a30b9e9db6fa", e)); throw new
		 * Error("Error while loading static data", e); }
		 */
		catch (Exception e) {
			progressReporter.failed();
			log.error(I18n.get("log.a30b9e9db6fa", e));
		}
		return null;
	}

	public NpcDropData loadNpcDropData() {
		return NpcDropData.loadEager(Config.definitionFile(NPC_DROP_DEFINITIONS_DIR));
	}

	public HotspotLocationData loadHotspotLocationData() {
		return HotspotLocationData.load(Config.definitionFile(HOTSPOT_LOCATION_DEFINITIONS_FILE));
	}

	public NpcSkillData loadNpcSkillData() {
		NpcSkillData data = NpcSkillDefinitionLoader.load(Config.definitionFile(NPC_SKILL_DEFINITIONS_FILE));
		log.info(I18n.get("log.b3e7ebfb7d92", data.size()));
		return data;
	}

	public SkillData loadSkillData() {
		SkillData data = SkillDefinitionLoader.load(Config.definitionFile(SKILL_DEFINITIONS_DIR));
		log.info(I18n.get("log.static_data.compact_skills_loaded", data.size()));
		return data;
	}

	MotionData loadMotionData() {
		return loadDefinition(MotionData.class, MOTION_DEFINITIONS_FILE);
	}

	ChargeSkillData loadChargeSkillData() {
		return loadDefinition(ChargeSkillData.class, CHARGE_SKILL_DEFINITIONS_FILE);
	}

	private static <T> T loadDefinition(Class<T> type, String relativePath) {
		File file = Config.definitionFile(relativePath);
		try {
			return type.cast(createJaxbContext(type).createUnmarshaller().unmarshal(file));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + file.getPath(), e);
		}
	}

	public AIData loadAiData() {
		AIData data = loadDefinition(AIData.class, AI_BOMBS_FILE);
		data.merge(loadDefinition(AIData.class, AI_SPAWN_HELPERS_FILE));
		return data;
	}

	public NpcPathBehaviorData loadNpcPathBehaviorData() {
		NpcPathBehaviorData data = NpcPathBehaviorDefinitionLoader.load(Config.definitionFile(NPC_PATH_BEHAVIOR_FILE));
		log.info(I18n.get("log.static_data.npc_paths_loaded", data.size()));
		return data;
	}

	public RetailAiData loadRetailAiData() {
		RetailAiData data = RetailAiDefinitionLoader.load(Config.definitionFile(AI_DEFINITIONS_DIR),
			Config.definitionFile(RETAIL_NPC_AI_MAPPINGS_FILE), Config.definitionFile(RETAIL_AI_STRINGS_FILE),
			Config.definitionFile(RETAIL_AI_AREAS_FILE), Config.definitionFile(RETAIL_CONDITION_SPAWNS_FILE),
			Config.definitionFile(RETAIL_SKILL_CATEGORIES_FILE), Config.definitionFile(RETAIL_AI_LOCATION_ALIASES_FILE),
			Config.definitionFile(RETAIL_DIRECT_PORTALS_FILE), Config.definitionFile(RETAIL_NPC_SCORES_FILE),
			Config.definitionFile(RETAIL_GROUP_CONTROLLERS_FILE), Config.definitionFile(RETAIL_NPC_PARTIES_FILE),
			Config.definitionFile(RETAIL_DYNAMIC_AREAS_FILE));
		log.info(I18n.get("log.static_data.condition_spawns_loaded", data.conditionSpawnCount()));
		log.info(I18n.get("log.static_data.npc_parties_loaded", data.npcPartyCount(), data.npcPartyMemberCount()));
		log.info(I18n.get("log.static_data.dynamic_areas_loaded", data.dynamicAreaCount()));
		return data;
	}

	WalkerData loadRetailAiWaypointData() {
		return loadRetailAiWaypointData(Config.definitionFile(RETAIL_AI_WAYPOINTS_FILE),
			Config.definitionFile(RETAIL_AI_WAYPOINTS_SCHEMA));
	}

	static WalkerData loadRetailAiWaypointData(File file, File schemaFile) {
		try {
			Unmarshaller unmarshaller = createJaxbContext(WalkerData.class).createUnmarshaller();
			unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(schemaFile));
			WalkerData data = (WalkerData) unmarshaller.unmarshal(file);
			log.info(I18n.get("log.static_data.ai_waypoints_loaded", data.size()));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load retail AI waypoints from " + file.getPath(), e);
		}
	}

	public PetDopingData loadPetDopingData() {
		return loadPetDefinitions().doping();
	}

	public PetMerchandData loadPetMerchandData() {
		return loadPetDefinitions().merchant();
	}

	private PetDefinitionLoader.Result loadPetDefinitions() {
		return PetDefinitionLoader.load(Config.definitionFile(PET_RIDES_DEFINITIONS_FILE));
	}

	public WindstreamData loadWindstreamData() {
		WindstreamData data = WindstreamDefinitionLoader.load(Config.definitionFile(FLY_PATH_DEFINITIONS_FILE),
			Config.definitionFile(WIND_DEFINITIONS_FILE), Config.definitionFile(ID_DEFINITIONS_FILE));
		log.info(I18n.get("log.e7553a368e56", data.size()));
		return data;
	}

	/**
	 * 单独加载物品模板数据（与主静态数据并行路径）。
	 * Loads item template data on a path parallel to main static data.
	 *
	 * item data
	 */
	public ItemData loadItemData() {
		return loadItemData(Config.cacheFile(ITEM_CACHE_XML_FILE), Config.dataFile(ITEM_DATA_DIR));
	}

	/**
	 * 指定缓存与源目录加载物品数据。
	 * Loads item data using the given cache file and source directory.
	 *
	 * cache file
	 * item XML directory
	 * item data
	 */
	ItemData loadItemData(File cachedXml, File itemDataDir) {
		makeCacheDirectory(cachedXml.getParentFile());
		File sourceXml = itemDataSourceXml(cachedXml.getParentFile());
		prepareItemDataSource(sourceXml);
		long cacheStart = System.currentTimeMillis();
		log.info(I18n.get("log.b4b0d6d5a04a", cachedXml.getPath()));
		mergeXmlFiles(cachedXml, sourceXml, itemDataDir);
		log.info(I18n.get("log.df03fc6e2d49", System.currentTimeMillis() - cacheStart));

		long unmarshalStart = System.currentTimeMillis();
		log.info(I18n.get("log.578d31223fd3", cachedXml.getPath()));
		try (FileReader reader = new FileReader(cachedXml)) {
			JAXBContext jc = createJaxbContext(ItemData.class);
			Unmarshaller un = jc.createUnmarshaller();
			un.setEventHandler(new XmlValidationHandler());
			ItemData data = (ItemData) un.unmarshal(reader);
			log.info(I18n.get("log.9ca73c5c206f", System.currentTimeMillis() - unmarshalStart));
			return data;
		} catch (Exception e) {
			log.error(I18n.get("log.ffb975771b9c", e));
			throw new Error("Error while loading item data", e);
		}
	}

	private File itemDataSourceXml(File cacheDir) {
		return new File(cacheDir, "item_templates.source.xml");
	}

	private void prepareItemDataSource(File sourceXml) {
		if (sourceXml.exists()) {
			return;
		}
		try (FileWriter writer = new FileWriter(sourceXml)) {
			writer.write(ITEM_SOURCE_XML);
		} catch (IOException e) {
			throw new Error("Error while preparing item data source", e);
		}
		sourceXml.setLastModified(0L);
	}

	/**
	 * 创建绑定进度监听的 StaticData Unmarshaller。
	 * Creates a StaticData Unmarshaller wired with progress reporting.
	 *
	 * @param progressReporter 进度报告器 / progress reporter
	 * total section count
	 * @param sectionEntryCounts 各分区条目数 / per-section entry counts
	 * configured unmarshaller
	 */
	Unmarshaller createStaticDataUnmarshaller(StaticDataProgressReporter progressReporter, int totalSections, Map<String, Integer> sectionEntryCounts)
			throws Exception {
		return createStaticDataUnmarshaller(new StaticDataProgressListener(progressReporter, totalSections, sectionEntryCounts));
	}

	private Unmarshaller createStaticDataUnmarshaller(StaticDataProgressListener progressListener) throws Exception {
		Future<JAXBContext> task = preloadedContext;
		JAXBContext jc = task != null ? task.get() : createJaxbContext(StaticData.class);
		Unmarshaller un = jc.createUnmarshaller();
		un.setEventHandler(new XmlValidationHandler());
		// 有意不在 JAXB 反序列化中接入 Schema 校验；过慢，仅在重建缓存时运行。 / Schema validation is intentionally not wired into JAXB unmarshalling; it is slow and is run only for rebuilt caches.
		un.setListener(progressListener);
		return un;
	}

	static JAXBContext createJaxbContext(Class<?> boundType) throws jakarta.xml.bind.JAXBException {
		Thread thread = Thread.currentThread();
		ClassLoader originalClassLoader = thread.getContextClassLoader();
		ClassLoader boundTypeClassLoader = boundType.getClassLoader();
		try {
			if (boundTypeClassLoader != null) {
				thread.setContextClassLoader(boundTypeClassLoader);
			}
			return JAXBContext.newInstance(boundType);
		} finally {
			thread.setContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * 异步校验已重建的静态数据缓存（XSD）。
	 * Asynchronously validates a rebuilt static-data cache against the XSD.
	 *
	 * cache file
	 * validation future
	 */
	Future<?> validateCacheAsync(File cachedXml) {
		return submitValidationTask(() -> validateCache(cachedXml));
	}

	/**
	 * 将校验任务提交到长时线程池。
	 * Submits a validation task to the long-running thread pool.
	 *
	 * @param task 校验任务 / validation runnable
	 * task future
	 */
	Future<?> submitValidationTask(Runnable task) {
		return GameThreadPoolServices.threadPoolManager().submitLongRunning(task);
	}

	private void validateCache(File cachedXml) {
		long validationStart = System.currentTimeMillis();
		log.info(I18n.get("log.5788d19f80e5", cachedXml.getPath()));
		try (Reader reader = new FileReader(cachedXml)) {
			getSchema().newValidator().validate(new SAXSource(new InputSource(reader)));
			log.info(I18n.get("log.1ae1bb91733a", System.currentTimeMillis() - validationStart));
		} catch (Throwable t) {
			cachedXml.setLastModified(0);
			log.error(I18n.get("log.9f39c9471c04", cachedXml.getPath(), t));
			throw new Error("Error validating static data cache", t);
		}
	}

	static String staticDataSectionName(Object target, Object parent) {
		if (target == null || !(parent instanceof StaticData)) {
			return null;
		}
		return target.getClass().getSimpleName();
	}

	static int staticDataSectionCount() {
		int count = 0;
		for (Field field : StaticData.class.getFields()) {
			if (field.getAnnotation(XmlElement.class) != null) {
				count++;
			}
		}
		return count;
	}

	static Map<String, Integer> staticDataSectionEntryCounts(File staticDataXml) throws Exception {
		Map<String, String> sectionNamesByXmlElement = staticDataSectionNamesByXmlElement();
		Map<String, Integer> counts = new HashMap<>();
		XMLInputFactory inputFactory = XMLInputFactory.newFactory();
		try (FileInputStream stream = new FileInputStream(staticDataXml)) {
			XMLStreamReader reader = inputFactory.createXMLStreamReader(stream);
			int depth = 0;
			String currentSectionName = null;
			try {
				while (reader.hasNext()) {
					int event = reader.next();
					if (event == XMLStreamConstants.START_ELEMENT) {
						depth++;
						if (depth == 2) {
							currentSectionName = sectionNamesByXmlElement.get(reader.getLocalName());
							if (currentSectionName != null) {
								counts.putIfAbsent(currentSectionName, 0);
							}
						} else if (depth == 3 && currentSectionName != null) {
							counts.merge(currentSectionName, 1, Integer::sum);
						}
					} else if (event == XMLStreamConstants.END_ELEMENT) {
						if (depth == 2) {
							currentSectionName = null;
						}
						depth--;
					}
				}
			} finally {
				reader.close();
			}
		}
		counts.replaceAll((sectionName, count) -> Math.max(1, count));
		return counts;
	}

	/**
	 * 返回各分区条目数；旁路缓存文件避免每次热启动扫描超大 XML。
	 * Returns section entry counts, cached in a sidecar file to avoid re-scanning the large XML on every warm start.
	 * <p>仅当 XML 缓存新于 counts 文件时重建。
	 * Cache is rebuilt only when the XML cache file is newer than the counts file.
	 *
	 * @param cachedXml 合并后的静态数据缓存 / merged static-data cache
	 * @return 分区名到条目数映射 / map of section name to entry count
	 */
	Map<String, Integer> loadSectionEntryCounts(File cachedXml) throws Exception {
		if (!GSConfig.STATIC_DATA_PROGRESS_ENTRY_COUNTS_ENABLE) {
			return defaultSectionEntryCounts();
		}
		File countsFile = new File(cachedXml.getParentFile(), "static_data.counts");
		if (countsFile.exists() && countsFile.lastModified() >= cachedXml.lastModified()) {
			return loadCountsFile(countsFile);
		}
		Map<String, Integer> counts = staticDataSectionEntryCounts(cachedXml);
		saveCountsFile(countsFile, counts);
		return counts;
	}

	private static Map<String, Integer> loadCountsFile(File countsFile) throws IOException {
		Properties props = new Properties();
		try (FileReader reader = new FileReader(countsFile)) {
			props.load(reader);
		}
		Map<String, Integer> counts = new HashMap<>();
		for (String name : props.stringPropertyNames()) {
			counts.put(name, Integer.valueOf(props.getProperty(name)));
		}
		return counts;
	}

	private static void saveCountsFile(File countsFile, Map<String, Integer> counts) {
		Properties props = new Properties();
		counts.forEach((name, count) -> props.setProperty(name, count.toString()));
		try (FileWriter writer = new FileWriter(countsFile)) {
			props.store(writer, "static_data section entry counts (avoids re-scanning XML on warm start)");
		} catch (IOException e) {
			log.warn(I18n.get("log.62f6cc254c59", countsFile.getPath(), e));
		}
	}

	private static Map<String, Integer> defaultSectionEntryCounts() {
		Map<String, Integer> counts = new HashMap<>();
		for (String sectionName : staticDataSectionNamesByXmlElement().values()) {
			counts.put(sectionName, 1);
		}
		return counts;
	}

	private static Map<String, String> staticDataSectionNamesByXmlElement() {
		Map<String, String> sectionNamesByXmlElement = new HashMap<>();
		for (Field field : StaticData.class.getFields()) {
			XmlElement element = field.getAnnotation(XmlElement.class);
			if (element != null) {
				sectionNamesByXmlElement.put(element.name(), field.getType().getSimpleName());
			}
		}
		return sectionNamesByXmlElement;
	}

	static List<Map.Entry<String, Long>> slowestSectionTimings(Map<String, Long> timings, int limit) {
		return timings.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.limit(limit)
			.toList();
	}

	private static void logSlowSectionTimings(Map<String, Long> timings) {
		List<Map.Entry<String, Long>> slowest = slowestSectionTimings(timings, 5);
		if (slowest.isEmpty()) {
			return;
		}
		log.info(I18n.get("log.6a77df723c2e"));
		for (Map.Entry<String, Long> timing : slowest) {
			log.info(I18n.get("log.b6609c40aca7", String.format("%-32s", timing.getKey()), timing.getValue()));
		}
	}

	private static final class StaticDataProgressListener extends Unmarshaller.Listener {

		private final StaticDataProgressReporter progressReporter;
		private final int totalSections;
		private final Map<String, Integer> sectionEntryCounts;
		private final Set<String> sectionNames;
		private final Map<String, Integer> sectionEntriesLoaded = new HashMap<>();
		private final Map<String, Long> sectionStartTimes = new HashMap<>();
		private final Map<String, Long> sectionElapsedTimes = new HashMap<>();
		private int sectionIndex;

		private StaticDataProgressListener(StaticDataProgressReporter progressReporter, int totalSections, Map<String, Integer> sectionEntryCounts) {
			this.progressReporter = progressReporter;
			this.totalSections = totalSections;
			this.sectionEntryCounts = sectionEntryCounts;
			this.sectionNames = sectionEntryCounts.keySet();
		}

		@Override
		public void beforeUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName == null) {
				return;
			}
			sectionEntriesLoaded.put(sectionName, 0);
			sectionStartTimes.put(sectionName, System.currentTimeMillis());
			progressReporter.sectionStarted(++sectionIndex, totalSections, sectionName, sectionEntryCounts.getOrDefault(sectionName, 1));
		}

		@Override
		public void afterUnmarshal(Object target, Object parent) {
			String sectionName = staticDataSectionName(target, parent);
			if (sectionName != null) {
				Long startTime = sectionStartTimes.remove(sectionName);
				if (startTime != null) {
					sectionElapsedTimes.put(sectionName, System.currentTimeMillis() - startTime);
				}
				progressReporter.sectionFinished(sectionIndex, totalSections, sectionName, sectionEntryCounts.getOrDefault(sectionName, 1));
				return;
			}
			String parentSectionName = staticDataChildSectionName(parent, sectionNames);
			if (parentSectionName == null) {
				return;
			}
			int totalEntries = sectionEntryCounts.getOrDefault(parentSectionName, 1);
			int currentEntries = Math.min(totalEntries, sectionEntriesLoaded.merge(parentSectionName, 1, Integer::sum));
			progressReporter.sectionProgress(sectionIndex, totalSections, parentSectionName, currentEntries, totalEntries);
		}

		private Map<String, Long> sectionElapsedTimes() {
			return sectionElapsedTimes;
		}
	}

	private static String staticDataChildSectionName(Object parent, Set<String> sectionNames) {
		if (parent == null) {
			return null;
		}
		String sectionName = parent.getClass().getSimpleName();
		return sectionNames.contains(sectionName) ? sectionName : null;
	}

	/**
	 * 创建并返回描述静态数据 XML 的 {@link Schema}。
	 * Creates and returns the {@link Schema} for static-data XML files.
	 *
	 * schema object
	 */
	private Schema getSchema() {
		Schema schema = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			schema = sf.newSchema(Config.dataFile(XML_SCHEMA_FILE));
		} catch (SAXException saxe) {
			log.error(I18n.get("log.b9e774d9c3cf", saxe));
			throw new Error("Error while getting schema", saxe);
		}
		return schema;
	}

	/** 若缓存目录不存在则创建 / creates the cache directory if missing */
	private void makeCacheDirectory(File cacheDir) {
		if (cacheDir != null && !cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}

	/**
	 * 若源文件新于缓存则合并 XML 并写入缓存。
	 * Merges XML sources into the cache file when sources are newer than the cache.
	 *
	 * @see XmlMerger
	 * @param cachedXml 缓存输出文件 / cache output file
	 * main entry XML
	 * @return 是否实际重建了缓存 / whether the cache was rebuilt
	 * if merge fails
	 */
	private boolean mergeXmlFiles(File cachedXml, File cleanMainXml) throws Error {
		return mergeXmlFiles(cachedXml, cleanMainXml, cleanMainXml.getParentFile());
	}

	private boolean mergeXmlFiles(File cachedXml, File cleanMainXml, File baseDir) throws Error {
		XmlMerger merger = new XmlMerger(cleanMainXml, cachedXml, baseDir);
		try {
			return merger.process();
		} catch (Exception e) {
			log.error(I18n.get("log.f0ac59daadde", e));
			throw new Error("Error while merging xml files", e);
		}
	}

	/** 内部懒加载单例持有者 / lazy-init holder for the internal singleton */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {

		protected static final XmlDataLoader instance = new XmlDataLoader();
	}
}
