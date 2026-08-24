package com.aionemu.gameserver.dataholders;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.lifecycle.GameStaticDataServices;
import com.aionemu.gameserver.model.templates.mail.Mails;
import com.aionemu.gameserver.utils.Util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 静态数据总装入口：通过 {@link XmlDataLoader} 加载全部 XML 模板，并暴露全局静态访问点。
 * Static-data facade that loads all XML templates via {@link XmlDataLoader} and exposes global accessors.
 */
@Slf4j
public final class DataManager {
    private static volatile ObjectProvider<DataManager> instanceProvider;
    private static final AtomicBoolean CONSTRUCTED = new AtomicBoolean(false);
    /** 静态数据是否已加载完成 / whether static data has been loaded */
    private static final AtomicBoolean LOADED = new AtomicBoolean(false);
    /**
     * 加载互斥锁：并发首调时后到者等待而非重复加载 /
     * Load mutex: concurrent first callers wait instead of reloading.
     */
    private static final Object LOAD_LOCK = new Object();
    public static NpcData NPC_DATA;
    public static NpcDropData NPC_DROP_DATA;
    public static NpcShoutData NPC_SHOUT_DATA;
    public static GatherableData GATHERABLE_DATA;
    public static WorldMapsData WORLD_MAPS_DATA;
    public static TradeListData TRADE_LIST_DATA;
    public static PlayerExperienceTable PLAYER_EXPERIENCE_TABLE;
    public static TeleporterData TELEPORTER_DATA;
    public static TeleLocationData TELELOCATION_DATA;
    public static CubeExpandData CUBEEXPANDER_DATA;
    public static WarehouseExpandData WAREHOUSEEXPANDER_DATA;
    public static BindPointData BIND_POINT_DATA;
    public static QuestsData QUEST_DATA;
    public static QuestRandomRewardsData QUEST_RANDOM_REWARDS;
    public static PlayerStatsData PLAYER_STATS_DATA;
    public static SummonStatsData SUMMON_STATS_DATA;
    public static ItemData ITEM_DATA;
    public static ItemRandomBonusData ITEM_RANDOM_BONUSES;
    public static TitleData TITLE_DATA;
    public static PlayerInitialData PLAYER_INITIAL_DATA;
    public static SkillData SKILL_DATA;
    public static MotionData MOTION_DATA;
    public static SkillTreeData SKILL_TREE_DATA;
    public static GuideHtmlData GUIDE_HTML_DATA;
    public static WalkerData WALKER_DATA;
    public static ZoneData ZONE_DATA;
    public static GoodsListData GOODSLIST_DATA;
    public static TribeRelationsData TRIBE_RELATIONS_DATA;
    public static RecipeData RECIPE_DATA;
    public static LunaData LUNA_DATA;
    public static ChestData CHEST_DATA;
    public static StaticDoorData STATICDOOR_DATA;
    public static ItemSetData ITEM_SET_DATA;
    public static NpcFactionsData NPC_FACTIONS_DATA;
    public static NpcFactionQuestData NPC_FACTIONS_QUEST_DATA;
    public static NpcSkillData NPC_SKILL_DATA;
    public static PetSkillData PET_SKILL_DATA;
    public static SiegeLocationData SIEGE_LOCATION_DATA;
    public static FlyRingData FLY_RING_DATA;
    public static ShieldData SHIELD_DATA;
    public static PetData PET_DATA;
    public static PetFeedData PET_FEED_DATA;
    public static PetDopingData PET_DOPING_DATA;
    public static PetMerchandData PET_MERCHAND_DATA;
    public static RoadData ROAD_DATA;
    public static InstanceCooltimeData INSTANCE_COOLTIME_DATA;
    public static DisassemblyItemSetsData DISASSEMBLY_ITEMS_DATA;
    public static AIData AI_DATA;
    public static NpcPathBehaviorData NPC_PATH_BEHAVIOR_DATA;
    public static RetailAiData RETAIL_AI_DATA;
    public static FlyPathData FLY_PATH;
    public static WindstreamData WINDSTREAM_DATA;
    public static ItemRestrictionCleanupData ITEM_CLEAN_UP;
    public static AssembledNpcsData ASSEMBLED_NPC_DATA;
    public static CosmeticItemsData COSMETIC_ITEMS_DATA;
    public static ItemGroupsData ITEM_GROUPS_DATA;
    public static AssemblyItemsData ASSEMBLY_ITEM_DATA;
    public static SpawnsData2 SPAWNS_DATA2;
    public static AutoGroupData AUTO_GROUP;
    public static EventData EVENT_DATA;
    public static PanelSkillsData PANEL_SKILL_DATA;
    public static InstanceBuffData INSTANCE_BUFF_DATA;
    public static HousingObjectData HOUSING_OBJECT_DATA;
    public static RideData RIDE_DATA;
    public static InstanceExitData INSTANCE_EXIT_DATA;
    public static PortalLocData PORTAL_LOC_DATA;
    public static Portal2Data PORTAL2_DATA;
    public static HouseData HOUSE_DATA;
    public static HouseBuildingData HOUSE_BUILDING_DATA;
    public static HousePartsData HOUSE_PARTS_DATA;
    public static CuringObjectsData CURING_OBJECTS_DATA;
    public static HouseNpcsData HOUSE_NPCS_DATA;
    public static HouseScriptData HOUSE_SCRIPT_DATA;
    public static Mails SYSTEM_MAIL_TEMPLATES;
    public static ChallengeData CHALLENGE_DATA;
    public static TownSpawnsData TOWN_SPAWNS_DATA;
    public static ChargeSkillData CHARGE_SKILL_DATA;
    public static SpringObjectsData SPRING_OBJECTS_DATA;
    public static RobotData ROBOT_DATA;
    public static AbyssBuffData ABYSS_BUFF_DATA;
    public static AbyssGroupData ABYSS_GROUP_DATA;
    public static AbsoluteStatsData ABSOLUTE_STATS_DATA;
    public static BaseData BASE_DATA;
    public static MaterialData MATERIAL_DATA;
    public static MapWeatherData MAP_WEATHER_DATA;
    public static VortexData VORTEX_DATA;
    public static BeritraData BERITRA_DATA;
    public static AgentData AGENT_DATA;
    public static SvsData SVS_DATA;
    public static RvrData RVR_DATA;
    public static MoltenusData MOLTENUS_DATA;
    public static DynamicRiftData DYNAMIC_RIFT_DATA;
    public static InstanceRiftData INSTANCE_RIFT_DATA;
    public static NightmareCircusData NIGHTMARE_CIRCUS_DATA;
    public static ZorshivDredgionData ZORSHIV_DREDGION_DATA;
    public static LegionDominionData LEGION_DOMINION_DATA;
    public static IdianDepthsData IDIAN_DEPTHS_DATA;
    public static AnohaData ANOHA_DATA;
    public static IuData IU_DATA;
    public static ConquestData CONQUEST_DATA;
    public static SerialGuardData SERIAL_GUARD_DATA;
    public static SerialKillerData SERIAL_KILLER_DATA;
    public static RiftData RIFT_DATA;
    public static ServiceBuffData SERVICE_BUFF_DATA;
    public static PlayersBonusData PLAYERS_BONUS_DATA;
    public static ItemEnchantData ITEM_ENCHANT_DATA;
    public static HotspotLocationData HOTSPOT_LOCATION_DATA;
    public static ItemUpgradeData ITEM_UPGRADE_DATA;
    public static AtreianPassportData ATREIAN_PASSPORT_DATA;
    public static GameExperienceData GAME_EXPERIENCE_DATA;
    public static AbyssOpData ABYSS_OP_DATA;
    public static PanelCpData PANEL_CP_DATA;
    public static PetBuffData PET_BUFF_DATA;
    public static MultiReturnItemData MULTI_RETURN_ITEM_DATA;
    public static LandingData LANDING_LOCATION_DATA;
    public static LandingSpecialData LANDING_SPECIAL_LOCATION_DATA;
    public static LunaConsumeRewardsData LUNA_CONSUME_REWARDS_DATA;
    public static ItemCustomSetData ITEM_CUSTOM_SET_DATA;
    public static MinionData MINION_DATA;
    public static F2PBonusData F2P_BONUS_DATA;
    public static ArcadeUpgradeData ARCADE_UPGRADE_DATA;
    public static GlobalDropData GLOBAL_DROP_DATA;
    public static ItemSkillEnhanceData ITEM_SKILL_ENHANCE_DATA;
    public static BoostEventdata BOOST_EVENT_DATA;
    public static AtreianBestiaryData ATREIAN_BESTIARY;
    public static EventsWindowData EVENTS_WINDOW;
    public static MailRewardData MAIL_REWARD;
    public static LunaDiceData LUNA_DICE;
    public static ReviveWorldStartPointsData REVIVE_WORLD_START_POINTS;
    public static TowerOfEternityData TOWER_OF_ETERNITY_DATA;
    public static ReviveInstanceStartPointsData REVIVE_INSTANCE_START_POINTS;
    public static OutpostData OUTPOST_DATA;
    public static StoneCpData STONE_CP_DATA;
    public static TowerRewardData TOWER_REWARD_DATA;
    public static ShugoSweepRewardData SHUGO_SWEEP_REWARD_DATA;
    public static SkillSkinData SKILL_SKIN_DATA;

    /** XML 加载器实例 / XML data loader instance */
    private XmlDataLoader loader;

    /**
     * 获取 DataManager 单例（优先 Spring 提供的实例，否则懒加载内部单例），
     * 并保证返回前静态数据已加载完成。
     * Returns the DataManager singleton (Spring-provided if available, otherwise the internal holder),
     * guaranteeing static data is loaded before returning.
     *
     * <p>构造已与加载分离：此处显式补齐加载（幂等）。加载已在进行中时阻塞等待，
     * 与旧版"构造期间解析将等待单例锁"的语义保持一致。
     * Construction and loading are decoupled: loading is ensured here (idempotent). While a load is
     * in progress callers block, matching the legacy semantics where resolving during construction
     * waited on the singleton lock.
     *
     * @return  DataManager 单例（已就绪）/ Returns the ready-to-use DataManager singleton.
     */
    public static final DataManager getInstance() {
        ObjectProvider<DataManager> provider = instanceProvider;
        DataManager manager = provider == null ? SingletonHolder.instance
                : provider.getIfAvailable(() -> SingletonHolder.instance);
        // 快路径：已加载时免锁直接返回 / Fast path: skip locking when already loaded.
        if (!LOADED.get()) {
            manager.load();
        }
        return manager;
    }

    /**
     * 注入 Spring 侧实例提供者，供容器接管单例解析。
     * Sets the Spring ObjectProvider used to resolve the singleton.
     *
     * @param instanceProvider 实例提供者 / instance provider
     */
    public static void setInstanceProvider(ObjectProvider<DataManager> instanceProvider) {
        DataManager.instanceProvider = instanceProvider;
    }

    /**
     * 构造 DataManager（轻量操作，不加载任何数据）。
     * Constructs the manager (cheap; loads nothing).
     *
     * <p>重要：构造必须保持轻量。Spring 在创建单例 Bean 期间持有全局 singletonLock，
     * 若在构造中执行耗时的静态数据加载，加载期间任何后台线程解析其他懒加载 Bean 都会
     * 永久阻塞在该锁上，而主线程又在等待这些线程的加载结果，形成死锁（启动卡死的
     * 根因）。
     * 实际加载由 {@link #load()} 在生命周期阶段显式触发，此时不再持有任何 Spring 锁。
     *
     * <p>Important: construction must stay cheap. Spring holds its global singleton lock while
     * creating a singleton bean. Loading static data inside the constructor blocks any background
     * thread resolving other lazy beans on that lock for the whole multi-minute load, while the
     * main thread waits for those same threads' results - a deadlock (the startup hang's root
     * cause). Actual loading is triggered explicitly via {@link #load()} from lifecycle code that
     * holds no Spring locks.
     *
     * @throws IllegalStateException 重复构造时抛出 / on duplicate construction
     */
    public DataManager() {
        if (!CONSTRUCTED.compareAndSet(false, true)) {
            throw new IllegalStateException("Duplicate DataManager construction detected");
        }
    }

    /**
     * 加载全部静态数据并分配到各公共静态字段；幂等，重复调用直接返回。
     * Loads all static data into the public static fields; idempotent, later calls return immediately.
     *
     * <p>必须在不持有任何 Spring 单例锁的上下文中调用（如启动生命周期的主流程），
     * 以便并行加载线程在此期间仍可正常解析懒加载 Bean。
     * Must be called from a context holding no Spring singleton locks (e.g. the main startup
     * lifecycle flow) so parallel loader threads can still resolve lazy beans during loading.
     */
    public void load() {
        synchronized (LOAD_LOCK) {
            if (LOADED.get()) {
                return;
            }
            CountDownLatch phaseDone = new CountDownLatch(1);
            startStallWatchdog(phaseDone);
            try {
                Util.printSection(I18n.get("console.section.static_data"));
                log.info(I18n.get("log.821c9082e891"));
                this.loader = GameStaticDataServices.xmlDataLoader();
                long start = System.currentTimeMillis();
                LoadedStaticData loadedData = loadStaticData(loader);
                StaticData data = loadedData.staticData();
                ItemData itemData = loadedData.itemData();
                // 静态字段赋值改为主线程内联执行：原实现提交到 commonPool 后立即
                // get() 等待，
                // 既无并行收益，又曾在 IDEA 环境出现无错误日志的静默挂起。
                // Static-field assignment runs inline on the loading thread: the former commonPool
                // runAsync followed by an immediate get() had no parallel benefit and produced silent,
                // error-free hangs in the IDEA environment.
                assignStaticData(data, itemData);

                long timeMillis = System.currentTimeMillis() - start;
                log.info(I18n.get("log.07c1c49f4c8b"));
                log.info(I18n.get("log.c7dc526fc9f6", TimeUnit.MILLISECONDS.toSeconds(timeMillis)));
                LOADED.set(true);
            } finally {
                phaseDone.countDown();
            }
        }
    }

    /**
     * 将加载完成的静态数据分配到各公共静态字段，并应用物品清理规则。
     * Assigns the loaded static data to the public static fields and applies item cleanup rules.
     *
     * @param data 主静态数据 / main static data
     * @param itemData 物品数据 / item data
     */
    private static void assignStaticData(StaticData data, ItemData itemData) {
        WORLD_MAPS_DATA = data.worldMapsData;
        PLAYER_EXPERIENCE_TABLE = data.playerExperienceTable;
        PLAYER_STATS_DATA = data.playerStatsData;
        SUMMON_STATS_DATA = data.summonStatsData;
        ITEM_CLEAN_UP = data.itemCleanup;
        ITEM_DATA = itemData;
        ITEM_RANDOM_BONUSES = data.itemRandomBonuses;
        NPC_DATA = data.npcData;
        NPC_SHOUT_DATA = data.npcShoutData;
        GATHERABLE_DATA = data.gatherableData;
        PLAYER_INITIAL_DATA = data.playerInitialData;
        SKILL_DATA = data.skillData;
        MOTION_DATA = data.motionData;
        SKILL_TREE_DATA = data.skillTreeData;
        TITLE_DATA = data.titleData;
        TRADE_LIST_DATA = data.tradeListData;
        TELEPORTER_DATA = data.teleporterData;
        TELELOCATION_DATA = data.teleLocationData;
        CUBEEXPANDER_DATA = data.cubeExpandData;
        WAREHOUSEEXPANDER_DATA = data.warehouseExpandData;
        BIND_POINT_DATA = data.bindPointData;
        QUEST_DATA = data.questData;
        QUEST_RANDOM_REWARDS = data.questRandomRewardsData;
        ZONE_DATA = data.zoneData;
        WALKER_DATA = data.walkerData;
        GOODSLIST_DATA = data.goodsListData;
        TRIBE_RELATIONS_DATA = data.tribeRelationsData;
        RECIPE_DATA = data.recipeData;
        LUNA_DATA = data.lunaData;
        CHEST_DATA = data.chestData;
        STATICDOOR_DATA = data.staticDoorData;
        ITEM_SET_DATA = data.itemSetData;
        NPC_FACTIONS_DATA = data.npcFactionsData;
        NPC_FACTIONS_QUEST_DATA = data.npcFactionQuestData;
        NPC_SKILL_DATA = data.npcSkillData;
        PET_SKILL_DATA = data.petSkillData;
        SIEGE_LOCATION_DATA = data.siegeLocationData;
        FLY_RING_DATA = data.flyRingData;
        SHIELD_DATA = data.shieldData;
        PET_DATA = data.petData;
        PET_FEED_DATA = data.petFeedData;
        PET_DOPING_DATA = data.petDopingData;
        PET_MERCHAND_DATA = data.petMerchandData;
        GUIDE_HTML_DATA = data.guideData;
        ROAD_DATA = data.roadData;
        INSTANCE_COOLTIME_DATA = data.instanceCooltimeData;
        DISASSEMBLY_ITEMS_DATA = data.disassemblyItemSetsData;
        AI_DATA = data.aiData;
        NPC_PATH_BEHAVIOR_DATA = data.npcPathBehaviorData;
        RETAIL_AI_DATA = data.retailAiData;
        FLY_PATH = data.flyPath;
        WINDSTREAM_DATA = data.windstreamsData;
        ASSEMBLED_NPC_DATA = data.assembledNpcData;
        COSMETIC_ITEMS_DATA = data.cosmeticItemsData;
        SPAWNS_DATA2 = data.spawnsData2;
        ITEM_GROUPS_DATA = data.itemGroupsData;
        ASSEMBLY_ITEM_DATA = data.assemblyItemData;
        AUTO_GROUP = data.autoGroupData;
        EVENT_DATA = data.eventData;
        PANEL_SKILL_DATA = data.panelSkillsData;
        INSTANCE_BUFF_DATA = data.instanceBuffData;
        HOUSING_OBJECT_DATA = data.housingObjectData;
        RIDE_DATA = data.rideData;
        INSTANCE_EXIT_DATA = data.instanceExitData;
        PORTAL_LOC_DATA = data.portalLocData;
        PORTAL2_DATA = data.portalTemplate2;
        HOUSE_DATA = data.houseData;
        HOUSE_BUILDING_DATA = data.houseBuildingData;
        HOUSE_PARTS_DATA = data.housePartsData;
        CURING_OBJECTS_DATA = data.curingObjectsData;
        HOUSE_NPCS_DATA = data.houseNpcsData;
        HOUSE_SCRIPT_DATA = data.houseScriptData;
        SYSTEM_MAIL_TEMPLATES = data.systemMailTemplates;
        ITEM_DATA.cleanup();
        NPC_DROP_DATA = data.npcDropData;
        CHALLENGE_DATA = data.challengeData;
        TOWN_SPAWNS_DATA = data.townSpawnsData;
        CHARGE_SKILL_DATA = data.chargeSkillData;
        SPRING_OBJECTS_DATA = data.springObjectsData;
        ROBOT_DATA = data.robotData;
        ABYSS_BUFF_DATA = data.abyssBuffData;
        ABYSS_GROUP_DATA = data.abyssGroupData;
        ABSOLUTE_STATS_DATA = data.absoluteStatsData;
        BASE_DATA = data.baseData;
        MATERIAL_DATA = data.materiaData;
        MAP_WEATHER_DATA = data.mapWeatherData;
        VORTEX_DATA = data.vortexData;
        BERITRA_DATA = data.beritraData;
        AGENT_DATA = data.agentData;
        SVS_DATA = data.svsData;
        RVR_DATA = data.rvrData;
        MOLTENUS_DATA = data.moltenusData;
        DYNAMIC_RIFT_DATA = data.dynamicRiftData;
        INSTANCE_RIFT_DATA = data.instanceRiftData;
        NIGHTMARE_CIRCUS_DATA = data.nightmareCircusData;
        ZORSHIV_DREDGION_DATA = data.zorshivDredgionData;
        LEGION_DOMINION_DATA = data.legionDominionData;
        IDIAN_DEPTHS_DATA = data.idianDepthsData;
        ANOHA_DATA = data.anohaData;
        IU_DATA = data.iuData;
        CONQUEST_DATA = data.conquestData;
        SERIAL_GUARD_DATA = data.serialGuardData;
        SERIAL_KILLER_DATA = data.serialKillerData;
        RIFT_DATA = data.riftData;
        SERVICE_BUFF_DATA = data.serviceBuffData;
        PLAYERS_BONUS_DATA = data.playersBonusData;
        ITEM_ENCHANT_DATA = data.itemEnchantData;
        HOTSPOT_LOCATION_DATA = data.hotspotLocationData;
        ITEM_UPGRADE_DATA = data.itemUpgradeData;
        ATREIAN_PASSPORT_DATA = data.atreianPassportData;
        GAME_EXPERIENCE_DATA = data.gameExperienceData;
        ABYSS_OP_DATA = data.abyssOpData;
        PANEL_CP_DATA = data.panelCpData;
        PET_BUFF_DATA = data.petBuffData;
        MULTI_RETURN_ITEM_DATA = data.multiReturnItemData;
        LANDING_LOCATION_DATA = data.landingLocationData;
        LANDING_SPECIAL_LOCATION_DATA = data.landingSpecialLocationData;
        LUNA_CONSUME_REWARDS_DATA = data.lunaConsumeRewardsData;
        ITEM_CUSTOM_SET_DATA = data.itemCustomSet;
        MINION_DATA = data.minionData;
        F2P_BONUS_DATA = data.f2pBonus;
        ARCADE_UPGRADE_DATA = data.arcadeUpgradeData;
        GLOBAL_DROP_DATA = data.globalDropData;
        ITEM_SKILL_ENHANCE_DATA = data.itemSkillEnhance;
        BOOST_EVENT_DATA = data.boostEvents;
        ATREIAN_BESTIARY = data.atreianBestiary;
        EVENTS_WINDOW = data.eventsWindow;
        MAIL_REWARD = data.mailReward;
        LUNA_DICE = data.lunaDice;
        REVIVE_WORLD_START_POINTS = data.reviveWorldStartPoints;
        TOWER_OF_ETERNITY_DATA = data.towerOfEternity;
        REVIVE_INSTANCE_START_POINTS = data.reviveInstanceStartPoints;
        OUTPOST_DATA = data.outpostLocation;
        TOWER_REWARD_DATA = data.towerReward;
        SHUGO_SWEEP_REWARD_DATA = data.shugoSweepsRewardData;
        SKILL_SKIN_DATA = data.skillSkinData;
    }

    /** 首次卡住告警前的等待时长 / Delay before the first stall report */
    private static final long STALL_WATCHDOG_INITIAL_DELAY_MILLIS = 60_000;
    /** 卡住告警间隔 / Interval between stall reports */
    private static final long STALL_WATCHDOG_REPORT_INTERVAL_MILLIS = 30_000;
    /** 卡住告警最大次数，避免无限刷屏 / Maximum stall reports to avoid endless spam */
    private static final int STALL_WATCHDOG_MAX_REPORTS = 5;
    /** 每个线程转储的最大栈帧数 / Maximum frames per dumped thread */
    private static final int STALL_WATCHDOG_MAX_FRAMES = 40;

    /**
     * 启动静态数据阶段看门狗：加载迟迟不完成时，周期性把加载线程与静态数据工作线程的
     * 栈
     * （含锁等待信息）以 ERROR 输出，使无日志卡住能自证卡点。
     * Starts the static-data phase watchdog: when loading stalls, periodically dumps the stacks
     * (including lock waits) of the loading and static-data worker threads at ERROR level so a
     * silent hang identifies itself.
     *
     * @param phaseDone 加载完成信号 / signal that loading finished
     */
    private static void startStallWatchdog(CountDownLatch phaseDone) {
        Thread loadingThread = Thread.currentThread();
        Thread watchdog = new Thread(() -> awaitAndReportStalls(phaseDone, loadingThread),
                "static-data-stall-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void awaitAndReportStalls(CountDownLatch phaseDone, Thread loadingThread) {
        long start = System.currentTimeMillis();
        try {
            if (phaseDone.await(STALL_WATCHDOG_INITIAL_DELAY_MILLIS, TimeUnit.MILLISECONDS)) {
                return;
            }
            for (int reported = 0; reported < STALL_WATCHDOG_MAX_REPORTS
                    && !phaseDone.await(STALL_WATCHDOG_REPORT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS); reported++) {
                log.error(I18n.get("log.static_data.phase_stall_detected",
                    System.currentTimeMillis() - start, describeStaticDataThreads(loadingThread)));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 汇总加载线程、静态数据加载线程与 quest 目錄预加载线程的当前栈。
     * Summarizes the current stacks of the loading, static-data loader, and quest preload threads.
     */
    private static String describeStaticDataThreads(Thread loadingThread) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        StringBuilder report = new StringBuilder();
        appendThreadDump(report, threads, loadingThread);
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            String name = thread.getName();
            if (thread != loadingThread && (name.equals("static-data-loader") || name.equals("quest-catalog-preload")
                    || name.startsWith("ForkJoinPool.commonPool"))) {
                appendThreadDump(report, threads, thread);
            }
        }
        return report.toString();
    }

    private static void appendThreadDump(StringBuilder report, ThreadMXBean threads, Thread thread) {
        ThreadInfo info = threads.getThreadInfo(thread.getId(), STALL_WATCHDOG_MAX_FRAMES);
        if (info != null) {
            report.append(info.toString()).append(System.lineSeparator());
        }
    }

    /**
     * 并行加载主静态数据、物品数据与技能数据。
     * Loads main static data, item data, and skill data in parallel.
     *
     * @param loader XML 数据加载器 / XML data loader
     * @return 已加载的静态数据与物品数据 / loaded static data and item data
     */
    static LoadedStaticData loadStaticData(XmlDataLoader loader) {
        // 使用静态数据专用线程池：避免 commonPool 嵌套 join 的丢失唤醒竞态（见
        // XmlDataLoader.STATIC_DATA_POOL）。
        // Use the dedicated static-data pool: avoids the commonPool nested-join lost-wakeup race
        // (see XmlDataLoader.STATIC_DATA_POOL).
        java.util.concurrent.Executor executor = XmlDataLoader.staticDataExecutor();
        ConcurrentMap<String, Long> phaseTimings = new ConcurrentHashMap<>();
        CompletableFuture<ItemData> itemDataFuture = timedDataLoad("ItemData", loader::loadItemData, executor,
                phaseTimings);
        CompletableFuture<SkillData> skillDataFuture = timedDataLoad("SkillData",
                () -> loader.loadSkillData(phaseTimings), executor, phaseTimings);
        try {
            StaticData staticData = loader.loadStaticData(skillDataFuture::join, phaseTimings);
            ItemData itemData = itemDataFuture.join();
            loader.logStaticDataPhaseTimings(phaseTimings);
            return new LoadedStaticData(staticData, itemData);
        } catch (CompletionException e) {
            itemDataFuture.cancel(true);
            skillDataFuture.cancel(true);
            Throwable cause = e.getCause();
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        } catch (RuntimeException | Error e) {
            itemDataFuture.cancel(true);
            skillDataFuture.cancel(true);
            throw e;
        }
    }

    /**
     * 在线程池任务内部记录物品或技能阶段的实际执行耗时。
     * Records the actual execution time of the item or skill phase inside its pool task.
     *
     * @param phaseName 阶段名称 / phase name
     * @param supplier 阶段加载任务 / phase loader
     * @param executor 执行器 / executor
     * @param phaseTimings 线程安全的阶段计时表 / thread-safe phase timing map
     * @return 带计时的 Future / timed future
     */
    private static <T> CompletableFuture<T> timedDataLoad(String phaseName, Supplier<T> supplier,
            java.util.concurrent.Executor executor, ConcurrentMap<String, Long> phaseTimings) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            try {
                return supplier.get();
            } finally {
                phaseTimings.put(phaseName, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            }
        }, executor);
    }

    /**
     * 并行加载结果：主静态数据 + 物品数据。
     * Parallel-load result holding main static data and item data.
     */
    record LoadedStaticData(StaticData staticData, ItemData itemData) {
    }

    /**
     * 内部懒加载单例持有者。
     * Lazy-init holder for the internal singleton.
     */
    @SuppressWarnings("synthetic-access")
    private static class SingletonHolder {
        protected static final DataManager instance = new DataManager();
    }
}
