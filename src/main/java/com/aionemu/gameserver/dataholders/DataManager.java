package com.aionemu.gameserver.dataholders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.model.templates.mail.Mails;
import com.aionemu.gameserver.utils.Util;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class DataManager {
    static Logger log = LoggerFactory.getLogger(DataManager.class);
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
    public static XMLQuests XML_QUESTS;
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

    // XmlDataLoader实例
    // XmlDataLoader instance
    private XmlDataLoader loader;
    // 获取DataManager的唯一实例
    // Get the singleton instance of DataManager
    public static final DataManager getInstance() {
        return SingletonHolder.instance;
    }

    // 私有构造函数，确保单例模式
    // Private constructor to ensure singleton pattern
    private DataManager() {
		Util.printSection(" *** Static Data *** ");
		log.info("##### Start Loading Static Data 5.8 #####");
        this.loader = XmlDataLoader.getInstance();
        long start = System.currentTimeMillis();
        // 加载静态数据
        // Load static data
        StaticData data = loader.loadStaticData();
        // 启动异步任务加载各数据对象
        // Start asynchronous task to load data objects
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            // 分配加载的数据到静态字段
            // Assign loaded data to static fields
            WORLD_MAPS_DATA = data.worldMapsData;
            PLAYER_EXPERIENCE_TABLE = data.playerExperienceTable;
            PLAYER_STATS_DATA = data.playerStatsData;
            SUMMON_STATS_DATA = data.summonStatsData;
            ITEM_CLEAN_UP = data.itemCleanup;
            ITEM_DATA = data.itemData;
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
            XML_QUESTS = data.questsScriptData;
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
        });

        try {
            // 等待异步任务完成
            // Wait for async task to complete
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            // 记录加载错误日志
            // Log loading error
            log.error("Error loading data", e);
        }

        long timeMillis = System.currentTimeMillis() - start;
        // 使用TimeUnit进行时间转换
        // Convert time using TimeUnit
        log.info("##### End Loading Static Data #####");
        log.info("##### [Loading Time: {} seconds] #####", TimeUnit.MILLISECONDS.toSeconds(timeMillis));
    }

    @SuppressWarnings("synthetic-access")
    private static class SingletonHolder {
        protected static final DataManager instance = new DataManager();
    }
}