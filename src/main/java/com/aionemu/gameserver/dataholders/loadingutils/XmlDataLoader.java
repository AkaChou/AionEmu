package com.aionemu.gameserver.dataholders.loadingutils;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.AssemblyItemsData;
import com.aionemu.gameserver.dataholders.ItemData;
import com.aionemu.gameserver.dataholders.ItemGroupsData;
import com.aionemu.gameserver.dataholders.ItemRandomBonusData;
import com.aionemu.gameserver.dataholders.ItemSetData;
import com.aionemu.gameserver.dataholders.AutoGroupData;
import com.aionemu.gameserver.dataholders.BindPointData;
import com.aionemu.gameserver.dataholders.ChestData;
import com.aionemu.gameserver.dataholders.CubeExpandData;
import com.aionemu.gameserver.dataholders.DynamicRiftData;
import com.aionemu.gameserver.dataholders.InstanceBuffData;
import com.aionemu.gameserver.dataholders.InstanceCooltimeData;
import com.aionemu.gameserver.dataholders.InstanceExitData;
import com.aionemu.gameserver.dataholders.InstanceRiftData;
import com.aionemu.gameserver.dataholders.FlyPathData;
import com.aionemu.gameserver.dataholders.FlyRingData;
import com.aionemu.gameserver.dataholders.GatherableData;
import com.aionemu.gameserver.dataholders.GoodsListData;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.NpcDropData;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.dataholders.Portal2Data;
import com.aionemu.gameserver.dataholders.PortalLocData;
import com.aionemu.gameserver.dataholders.QuestsData;
import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.dataholders.RiftData;
import com.aionemu.gameserver.dataholders.RoadData;
import com.aionemu.gameserver.dataholders.ReviveInstanceStartPointsData;
import com.aionemu.gameserver.dataholders.ReviveWorldStartPointsData;
import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.dataholders.StaticData;
import com.aionemu.gameserver.dataholders.TeleLocationData;
import com.aionemu.gameserver.dataholders.TeleporterData;
import com.aionemu.gameserver.dataholders.TradeListData;
import com.aionemu.gameserver.dataholders.WindstreamData;
import com.aionemu.gameserver.dataholders.WarehouseExpandData;
import com.aionemu.gameserver.dataholders.WorldMapsData;
import com.aionemu.gameserver.dataholders.XMLQuests;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.questEngine.handlers.models.XMLQuest;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

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
	private static final String ITEM_CACHE_XML_FILE = "./cache/item_templates.xml";
	private static final String ITEM_DEFINITIONS_DIR = "./definitions/items";
	private static final String ITEM_ASSEMBLY_DEFINITIONS_FILE = "./definitions/items/assembly/assembly_items.xml";
	private static final String ITEM_GROUP_DEFINITIONS_FILE = "./definitions/items/groups/item_groups.xml";
	private static final String ITEM_RANDOM_BONUS_DEFINITIONS_FILE = "./definitions/items/random_bonuses/item_random_bonuses.xml";
	private static final String ITEM_SET_DEFINITIONS_FILE = "./definitions/items/sets/item_sets.xml";
	private static final String AUTO_GROUP_DEFINITIONS_FILE = "./definitions/instances/auto_group/auto_group.xml";
	private static final String BIND_POINT_DEFINITIONS_FILE = "./definitions/world/transport/bind_points/bind_points.xml";
	private static final String CHEST_DEFINITIONS_FILE = "./definitions/world/resources/chests/chest_templates.xml";
	private static final String CUBE_EXPAND_DEFINITIONS_FILE = "./definitions/player/storage/cube_expander/cube_expander.xml";
	private static final String FLY_PATH_DEFINITIONS_FILE = "./definitions/world/transport/flypath_template.xml";
	private static final String FLY_RING_DEFINITIONS_FILE = "./definitions/world/movement/fly_rings/fly_rings.xml";
	private static final String GATHERABLE_DEFINITIONS_FILE = "./definitions/world/resources/gatherables/gatherable_templates.xml";
	private static final String GOODS_LIST_DEFINITIONS_FILE = "./definitions/commerce/npc_shops/goodslists.xml";
	private static final String DYNAMIC_RIFT_DEFINITIONS_FILE = "./definitions/locations/dynamic_rift/dynamic_rift.xml";
	private static final String INSTANCE_BUFF_DEFINITIONS_FILE = "./definitions/instances/instance_bonusattr/instance_bonusattr.xml";
	private static final String INSTANCE_COOLTIME_DEFINITIONS_FILE = "./definitions/instances/instance_cooltimes/instance_cooltimes.xml";
	private static final String INSTANCE_EXIT_DEFINITIONS_FILE = "./definitions/instances/instance_exit/instance_exit.xml";
	private static final String INSTANCE_RIFT_DEFINITIONS_FILE = "./definitions/locations/instance_rift/instance_rift.xml";
	private static final String NPC_DEFINITIONS_FILE = "./definitions/npcs/npc_template.xml";
	private static final String NPC_DROP_DEFINITIONS_DIR = "./definitions/npc_drops";
	private static final String NPC_SKILL_DEFINITIONS_FILE = "./definitions/compact/npc-skills.xml";
	private static final String PORTAL_LOC_DEFINITIONS_FILE = "./definitions/portals/portal_loc.xml";
	private static final String PORTAL_TEMPLATE_DEFINITIONS_FILE = "./definitions/portals/portal_template2.xml";
	private static final String QUEST_DEFINITIONS_FILE = "./definitions/quests/quest_data.xml";
	private static final String QUEST_SCRIPT_DEFINITIONS_DIR = "./definitions/quests/scripts";
	private static final String RECIPE_DEFINITIONS_FILE = "./definitions/recipes/recipe_templates.xml";
	private static final String RIFT_DEFINITIONS_FILE = "./definitions/locations/rift/rift_locations.xml";
	private static final String ROAD_DEFINITIONS_FILE = "./definitions/world/movement/roads/roads.xml";
	private static final String REVIVE_INSTANCE_DEFINITIONS_FILE = "./definitions/world/revive_start_points/instance_revive_start_points.xml";
	private static final String REVIVE_WORLD_DEFINITIONS_FILE = "./definitions/world/revive_start_points/revive_world_start_points.xml";
	private static final String SKILL_DEFINITIONS_FILE = "./definitions/skills/skill_templates.xml";
	private static final String TELEPORT_LOCATION_DEFINITIONS_FILE = "./definitions/world/transport/teleport_location.xml";
	private static final String TELEPORTER_DEFINITIONS_FILE = "./definitions/world/transport/npc_teleporter.xml";
	private static final String TRADE_LIST_DEFINITIONS_FILE = "./definitions/commerce/npc_shops/npc_trade_list.xml";
	private static final String WORLD_DEFINITIONS_FILE = "./definitions/compact/world.xml";
	private static final String WORLD_MAPS_DEFINITIONS_FILE = "./definitions/world/maps/world_maps.xml";
	private static final String WAREHOUSE_EXPAND_DEFINITIONS_FILE = "./definitions/player/storage/warehouse_expander/warehouse_expander.xml";
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
		return loadStaticData(ConsoleStaticDataProgressReporter.forCurrentConsole());
	}

	/**
	 * 带进度回调的静态数据加载：合并缓存、可选校验、JAXB 反序列化。
	 * Loads static data with a progress reporter: merge cache, optional validation, JAXB unmarshalling.
	 *
	 * @param progressReporter 进度报告器 / progress reporter
	 * @return 静态数据，失败则为 null / static data, or null on failure
	 */
	StaticData loadStaticData(StaticDataProgressReporter progressReporter) {
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
				data.npcData = loadNpcData();
				data.assemblyItemData = loadAssemblyItemsData();
				data.autoGroupData = loadAutoGroupData();
				data.bindPointData = loadBindPointData();
				data.chestData = loadChestData();
				data.cubeExpandData = loadCubeExpandData();
				data.flyPath = loadFlyPathData();
				data.flyRingData = loadFlyRingData();
				data.gatherableData = loadGatherableData();
				data.goodsListData = loadGoodsListData();
				data.dynamicRiftData = loadDynamicRiftData();
				data.npcDropData = loadNpcDropData();
				data.npcSkillData = loadNpcSkillData();
				data.instanceBuffData = loadInstanceBuffData();
				data.instanceCooltimeData = loadInstanceCooltimeData();
				data.instanceExitData = loadInstanceExitData();
				data.instanceRiftData = loadInstanceRiftData();
				data.itemGroupsData = loadItemGroupsData();
				data.itemRandomBonuses = loadItemRandomBonusData();
				data.itemSetData = loadItemSetData();
				data.portalLocData = loadPortalLocData();
				data.portalTemplate2 = loadPortal2Data();
				data.questData = loadQuestData();
				data.questsScriptData = loadQuestScripts();
				data.recipeData = loadRecipeData();
				data.riftData = loadRiftData();
				data.roadData = loadRoadData();
				data.reviveInstanceStartPoints = loadReviveInstanceStartPointsData();
				data.reviveWorldStartPoints = loadReviveWorldStartPointsData();
				data.skillData = loadSkillData();
				data.teleLocationData = loadTeleLocationData();
				data.teleporterData = loadTeleporterData();
				data.tradeListData = loadTradeListData();
				data.windstreamsData = loadWindstreamData();
				data.warehouseExpandData = loadWarehouseExpandData();
				data.worldMapsData = loadWorldMapsData();
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

	public NpcData loadNpcData() {
		File file = Config.definitionFile(NPC_DEFINITIONS_FILE);
		try (FileReader reader = new FileReader(file)) {
			NpcData data = (NpcData) createJaxbContext(StaticData.class).createUnmarshaller().unmarshal(reader);
			log.info(I18n.get("log.7a39ab3cdda2", data.size()));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load NPC definitions from " + file.getPath(), e);
		}
	}

	public AutoGroupData loadAutoGroupData() {
		return loadDefinition(AUTO_GROUP_DEFINITIONS_FILE, AutoGroupData.class);
	}

	public BindPointData loadBindPointData() {
		return loadDefinition(BIND_POINT_DEFINITIONS_FILE, BindPointData.class);
	}

	public ChestData loadChestData() {
		return loadDefinition(CHEST_DEFINITIONS_FILE, ChestData.class);
	}

	public CubeExpandData loadCubeExpandData() {
		return loadDefinition(CUBE_EXPAND_DEFINITIONS_FILE, CubeExpandData.class);
	}

	public FlyPathData loadFlyPathData() {
		return loadDefinition(FLY_PATH_DEFINITIONS_FILE, FlyPathData.class);
	}

	public FlyRingData loadFlyRingData() {
		return loadDefinition(FLY_RING_DEFINITIONS_FILE, FlyRingData.class);
	}

	public GatherableData loadGatherableData() {
		return loadDefinition(GATHERABLE_DEFINITIONS_FILE, GatherableData.class);
	}

	public GoodsListData loadGoodsListData() {
		return loadDefinition(GOODS_LIST_DEFINITIONS_FILE, GoodsListData.class);
	}

	public NpcDropData loadNpcDropData() {
		NpcDropData data = NpcDropData.loadEager(Config.definitionFile(NPC_DROP_DEFINITIONS_DIR));
		log.info(I18n.get("log.4103f2b9b4db", data.size()));
		return data;
	}

	public NpcSkillData loadNpcSkillData() {
		NpcSkillData data = NpcSkillDefinitionLoader.load(Config.definitionFile(NPC_SKILL_DEFINITIONS_FILE));
		log.info(I18n.get("log.b3e7ebfb7d92", data.size()));
		return data;
	}

	public DynamicRiftData loadDynamicRiftData() {
		return loadDefinition(DYNAMIC_RIFT_DEFINITIONS_FILE, DynamicRiftData.class);
	}

	public InstanceBuffData loadInstanceBuffData() {
		return loadDefinition(INSTANCE_BUFF_DEFINITIONS_FILE, InstanceBuffData.class);
	}

	public InstanceCooltimeData loadInstanceCooltimeData() {
		return loadDefinition(INSTANCE_COOLTIME_DEFINITIONS_FILE, InstanceCooltimeData.class);
	}

	public InstanceExitData loadInstanceExitData() {
		return loadDefinition(INSTANCE_EXIT_DEFINITIONS_FILE, InstanceExitData.class);
	}

	public InstanceRiftData loadInstanceRiftData() {
		return loadDefinition(INSTANCE_RIFT_DEFINITIONS_FILE, InstanceRiftData.class);
	}

	public AssemblyItemsData loadAssemblyItemsData() {
		return loadDefinition(ITEM_ASSEMBLY_DEFINITIONS_FILE, AssemblyItemsData.class);
	}

	public ItemGroupsData loadItemGroupsData() {
		return loadDefinition(ITEM_GROUP_DEFINITIONS_FILE, ItemGroupsData.class);
	}

	public ItemRandomBonusData loadItemRandomBonusData() {
		return loadDefinition(ITEM_RANDOM_BONUS_DEFINITIONS_FILE, ItemRandomBonusData.class);
	}

	public ItemSetData loadItemSetData() {
		return loadDefinition(ITEM_SET_DEFINITIONS_FILE, ItemSetData.class);
	}

	public RiftData loadRiftData() {
		return loadDefinition(RIFT_DEFINITIONS_FILE, RiftData.class);
	}

	public RoadData loadRoadData() {
		return loadDefinition(ROAD_DEFINITIONS_FILE, RoadData.class);
	}

	public ReviveInstanceStartPointsData loadReviveInstanceStartPointsData() {
		return loadDefinition(REVIVE_INSTANCE_DEFINITIONS_FILE, ReviveInstanceStartPointsData.class);
	}

	public ReviveWorldStartPointsData loadReviveWorldStartPointsData() {
		return loadDefinition(REVIVE_WORLD_DEFINITIONS_FILE, ReviveWorldStartPointsData.class);
	}

	public TeleLocationData loadTeleLocationData() {
		return loadDefinition(TELEPORT_LOCATION_DEFINITIONS_FILE, TeleLocationData.class);
	}

	public TeleporterData loadTeleporterData() {
		return loadDefinition(TELEPORTER_DEFINITIONS_FILE, TeleporterData.class);
	}

	public TradeListData loadTradeListData() {
		return loadDefinition(TRADE_LIST_DEFINITIONS_FILE, TradeListData.class);
	}

	public PortalLocData loadPortalLocData() {
		return loadDefinition(PORTAL_LOC_DEFINITIONS_FILE, PortalLocData.class);
	}

	public Portal2Data loadPortal2Data() {
		return loadDefinition(PORTAL_TEMPLATE_DEFINITIONS_FILE, Portal2Data.class);
	}

	private <T> T loadDefinition(String path, Class<T> type) {
		File file = Config.definitionFile(path);
		try (FileReader reader = new FileReader(file)) {
			return type.cast(createJaxbContext(type).createUnmarshaller().unmarshal(reader));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load definitions from " + file.getPath(), e);
		}
	}

	public QuestsData loadQuestData() {
		File file = Config.definitionFile(QUEST_DEFINITIONS_FILE);
		try (FileReader reader = new FileReader(file)) {
			QuestsData data = (QuestsData) createJaxbContext(StaticData.class).createUnmarshaller().unmarshal(reader);
			log.info(I18n.get("log.fe9338f00401", data.size()));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load quest definitions from " + file.getPath(), e);
		}
	}

	public XMLQuests loadQuestScripts() {
		File directory = Config.definitionFile(QUEST_SCRIPT_DEFINITIONS_DIR);
		List<XMLQuest> scripts = new ArrayList<>();
		try {
			Unmarshaller unmarshaller = createJaxbContext(StaticData.class).createUnmarshaller();
			try (var paths = Files.walk(directory.toPath())) {
				for (Path path : paths.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".xml"))
					.filter(p -> !p.getFileName().toString().startsWith("new") && !p.getFileName().toString().startsWith("."))
					.sorted().toList()) {
					XMLQuests data = (XMLQuests) unmarshaller.unmarshal(path.toFile());
					if (data.getQuest() != null) {
						scripts.addAll(data.getQuest());
					}
				}
			}
			XMLQuests data = new XMLQuests();
			data.setData(scripts);
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load quest scripts from " + directory.getPath(), e);
		}
	}

	public RecipeData loadRecipeData() {
		File file = Config.definitionFile(RECIPE_DEFINITIONS_FILE);
		try (FileReader reader = new FileReader(file)) {
			RecipeData data = (RecipeData) createJaxbContext(StaticData.class).createUnmarshaller().unmarshal(reader);
			log.info(I18n.get("log.330854034f35", data.size()));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load recipe definitions from " + file.getPath(), e);
		}
	}

	public SkillData loadSkillData() {
		File file = Config.definitionFile(SKILL_DEFINITIONS_FILE);
		try (FileReader reader = new FileReader(file)) {
			SkillData data = (SkillData) createJaxbContext(StaticData.class).createUnmarshaller().unmarshal(reader);
			data.initializeCooldownGroups();
			log.info(I18n.get("log.b5f7ba1ed5cc", data.size()));
			log.info(I18n.get("log.2e9957f776eb", data.sizeOfGroup()));
			return data;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load skill definitions from " + file.getPath(), e);
		}
	}

	public WindstreamData loadWindstreamData() {
		WindstreamData data = WindstreamDefinitionLoader.load(Config.definitionFile(WORLD_DEFINITIONS_FILE),
			Config.definitionFile(ID_DEFINITIONS_FILE));
		log.info(I18n.get("log.e7553a368e56", data.size()));
		return data;
	}

	public WorldMapsData loadWorldMapsData() {
		return loadDefinition(WORLD_MAPS_DEFINITIONS_FILE, WorldMapsData.class);
	}

	public WarehouseExpandData loadWarehouseExpandData() {
		return loadDefinition(WAREHOUSE_EXPAND_DEFINITIONS_FILE, WarehouseExpandData.class);
	}

	/**
	 * 单独加载物品模板数据（与主静态数据并行路径）。
	 * Loads item template data on a path parallel to main static data.
	 *
	 * item data
	 */
	public ItemData loadItemData() {
		return loadItemData(Config.cacheFile(ITEM_CACHE_XML_FILE), Config.definitionFile(ITEM_DEFINITIONS_DIR));
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

	private static JAXBContext createJaxbContext(Class<?> boundType) throws jakarta.xml.bind.JAXBException {
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
