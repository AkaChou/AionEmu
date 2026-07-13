package com.aionemu.gameserver.dataholders;

import com.aionemu.boot.i18n.I18n;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.templates.mail.Mails;

import lombok.extern.slf4j.Slf4j;

/**
 * JAXB 根对象，聚合全部静态数据分区（NPC、物品、技能、刷怪、活动等）。
 * JAXB root that aggregates every static-data section (npcs, items, skills, spawns, events, etc.).
 */
@XmlRootElement(name = "ae_static_data")
@XmlAccessorType(XmlAccessType.NONE)
@Slf4j
public class StaticData {
	public NpcPathBehaviorData npcPathBehaviorData;
	public RetailAiData retailAiData;
	@XmlElement(name = "world_maps")
	public WorldMapsData worldMapsData;
	@XmlElement(name = "npc_trade_list")
	public TradeListData tradeListData;
	@XmlElement(name = "npc_teleporter")
	public TeleporterData teleporterData;
	@XmlElement(name = "teleport_location")
	public TeleLocationData teleLocationData;
	@XmlElement(name = "bind_points")
	public BindPointData bindPointData;
	@XmlElement(name = "quests")
	public QuestsData questData;
	@XmlElement(name = "quest_scripts")
	public XMLQuests questsScriptData;
	@XmlElement(name = "player_experience_table")
	public PlayerExperienceTable playerExperienceTable;
	@XmlElement(name = "player_stats_templates")
	public PlayerStatsData playerStatsData;
	@XmlElement(name = "summon_stats_templates")
	public SummonStatsData summonStatsData;
	@XmlElement(name = "item_templates")
	public ItemData itemData;
	@XmlElement(name = "random_bonuses")
	public ItemRandomBonusData itemRandomBonuses;
	@XmlElement(name = "npc_templates")
	public NpcData npcData;
	@XmlElement(name = "npc_shouts")
	public NpcShoutData npcShoutData;
	@XmlElement(name = "player_initial_data")
	public PlayerInitialData playerInitialData;
	@XmlElement(name = "skill_data")
	public SkillData skillData;
	@XmlElement(name = "motion_times")
	public MotionData motionData;
	@XmlElement(name = "skill_tree")
	public SkillTreeData skillTreeData;
	@XmlElement(name = "cube_expander")
	public CubeExpandData cubeExpandData;
	@XmlElement(name = "warehouse_expander")
	public WarehouseExpandData warehouseExpandData;
	@XmlElement(name = "player_titles")
	public TitleData titleData;
	@XmlElement(name = "gatherable_templates")
	public GatherableData gatherableData;
	@XmlElement(name = "npc_walker")
	public WalkerData walkerData;
	@XmlElement(name = "zones")
	public ZoneData zoneData;
	@XmlElement(name = "goodslists")
	public GoodsListData goodsListData;
	@XmlElement(name = "tribe_relations")
	public TribeRelationsData tribeRelationsData;
	@XmlElement(name = "recipe_templates")
	public RecipeData recipeData;
	@XmlElement(name = "luna_templates")
	public LunaData lunaData;
	@XmlElement(name = "chest_templates")
	public ChestData chestData;
	@XmlElement(name = "staticdoor_templates")
	public StaticDoorData staticDoorData;
	@XmlElement(name = "item_sets")
	public ItemSetData itemSetData;
	@XmlElement(name = "npc_factions")
	public NpcFactionsData npcFactionsData;
	@XmlElement(name = "npc_skill_templates")
	public NpcSkillData npcSkillData;
	@XmlElement(name = "pet_skill_templates")
	public PetSkillData petSkillData;
	@XmlElement(name = "siege_locations")
	public SiegeLocationData siegeLocationData;
	@XmlElement(name = "fly_rings")
	public FlyRingData flyRingData;
	@XmlElement(name = "shields")
	public ShieldData shieldData;
	@XmlElement(name = "pets")
	public PetData petData;
	@XmlElement(name = "pet_feed")
	public PetFeedData petFeedData;
	@XmlElement(name = "dopings")
	public PetDopingData petDopingData;
	@XmlElement(name = "merchands")
	public PetMerchandData petMerchandData;
	@XmlElement(name = "guides")
	public GuideHtmlData guideData;
	@XmlElement(name = "roads")
	public RoadData roadData;
	@XmlElement(name = "instance_cooltimes")
	public InstanceCooltimeData instanceCooltimeData;
	@XmlElement(name = "disassemblyitemsets")
	public DisassemblyItemSetsData disassemblyItemSetsData;
	@XmlElement(name = "ai_templates")
	public AIData aiData;
	@XmlElement(name = "flypath_template")
	public FlyPathData flyPath;
	@XmlElement(name = "windstreams")
	public WindstreamData windstreamsData;
	@XmlElement(name = "item_restriction_cleanups")
	public ItemRestrictionCleanupData itemCleanup;
	@XmlElement(name = "assembled_npcs")
	public AssembledNpcsData assembledNpcData;
	@XmlElement(name = "cosmetic_items")
	public CosmeticItemsData cosmeticItemsData;
	@XmlElement(name = "npc_drops")
	public NpcDropData npcDropData;
	@XmlElement(name = "auto_groups")
	public AutoGroupData autoGroupData;
	@XmlElement(name = "events_config")
	public EventData eventData;
	@XmlElement(name = "spawns")
	public SpawnsData2 spawnsData2;
	@XmlElement(name = "item_groups")
	public ItemGroupsData itemGroupsData;
	@XmlElement(name = "polymorph_panels")
	public PanelSkillsData panelSkillsData;
	@XmlElement(name = "instance_bonusattrs")
	public InstanceBuffData instanceBuffData;
	@XmlElement(name = "housing_objects")
	public HousingObjectData housingObjectData;
	@XmlElement(name = "rides")
	public RideData rideData;
	@XmlElement(name = "instance_exits")
	public InstanceExitData instanceExitData;
	@XmlElement(name = "portal_locs")
	PortalLocData portalLocData;
	@XmlElement(name = "portal_templates2")
	Portal2Data portalTemplate2;
	@XmlElement(name = "house_lands")
	public HouseData houseData;
	@XmlElement(name = "buildings")
	public HouseBuildingData houseBuildingData;
	@XmlElement(name = "house_parts")
	public HousePartsData housePartsData;
	@XmlElement(name = "curing_objects")
	public CuringObjectsData curingObjectsData;
	@XmlElement(name = "house_npcs")
	public HouseNpcsData houseNpcsData;
	@XmlElement(name = "assembly_items")
	public AssemblyItemsData assemblyItemData;
	@XmlElement(name = "multi_returns")
	public MultiReturnItemData multiReturnItemData;
	@XmlElement(name = "lboxes")
	public HouseScriptData houseScriptData;
	@XmlElement(name = "mails")
	public Mails systemMailTemplates;
	@XmlElement(name = "challenge_tasks")
	public ChallengeData challengeData;
	@XmlElement(name = "town_spawns_data")
	public TownSpawnsData townSpawnsData;
	@XmlElement(name = "charge_skills")
	public ChargeSkillData chargeSkillData;
	@XmlElement(name = "spring_objects")
	public SpringObjectsData springObjectsData;
	@XmlElement(name = "robots")
	public RobotData robotData;
	@XmlElement(name = "abyss_bonusattrs")
	public AbyssBuffData abyssBuffData;
	@XmlElement(name = "abyss_groupattrs")
	public AbyssGroupData abyssGroupData;
	@XmlElement(name = "absolute_stats")
	public AbsoluteStatsData absoluteStatsData;
	@XmlElement(name = "base_locations")
	public BaseData baseData;
	@XmlElement(name = "material_templates")
	public MaterialData materiaData;
	@XmlElement(name = "weather")
	public MapWeatherData mapWeatherData;
	@XmlElement(name = "dimensional_vortex")
	public VortexData vortexData;
	@XmlElement(name = "beritra_invasion")
	public BeritraData beritraData;
	@XmlElement(name = "agent_fight")
	public AgentData agentData;
	@XmlElement(name = "svs")
	public SvsData svsData;
	@XmlElement(name = "rvr")
	public RvrData rvrData;
	@XmlElement(name = "moltenus")
	public MoltenusData moltenusData;
	@XmlElement(name = "dynamic_rift")
	public DynamicRiftData dynamicRiftData;
	@XmlElement(name = "instance_rift")
	public InstanceRiftData instanceRiftData;
	@XmlElement(name = "nightmare_circus")
	public NightmareCircusData nightmareCircusData;
	@XmlElement(name = "zorshiv_dredgion")
	public ZorshivDredgionData zorshivDredgionData;
	@XmlElement(name = "dominion_locations")
	public LegionDominionData legionDominionData;
	@XmlElement(name = "idian_depths")
	public IdianDepthsData idianDepthsData;
	@XmlElement(name = "anoha")
	public AnohaData anohaData;
	@XmlElement(name = "iu")
	public IuData iuData;
	@XmlElement(name = "conquest")
	public ConquestData conquestData;
	@XmlElement(name = "serial_guards")
	public SerialGuardData serialGuardData;
	@XmlElement(name = "serial_killers")
	public SerialKillerData serialKillerData;
	@XmlElement(name = "rift_locations")
	public RiftData riftData;
	@XmlElement(name = "service_bonusattrs")
	public ServiceBuffData serviceBuffData;
	@XmlElement(name = "players_service_bonusattrs")
	public PlayersBonusData playersBonusData;
	@XmlElement(name = "enchant_templates")
	public ItemEnchantData itemEnchantData;
	@XmlElement(name = "hotspot_location")
	public HotspotLocationData hotspotLocationData;
	@XmlElement(name = "item_upgrades")
	public ItemUpgradeData itemUpgradeData;
	@XmlElement(name = "atreian_passports")
	public AtreianPassportData atreianPassportData;
	@XmlElement(name = "game_experience_items")
	public GameExperienceData gameExperienceData;
	@XmlElement(name = "abyss_ops")
	public AbyssOpData abyssOpData;
	@XmlElement(name = "panel_cps")
	public PanelCpData panelCpData;
	@XmlElement(name = "pet_bonusattrs")
	public PetBuffData petBuffData;
	@XmlElement(name = "landing")
	public LandingData landingLocationData;
	@XmlElement(name = "landing_special")
	public LandingSpecialData landingSpecialLocationData;
	@XmlElement(name = "luna_consume_rewards")
	public LunaConsumeRewardsData lunaConsumeRewardsData;
	@XmlElement(name = "item_custom_sets")
	public ItemCustomSetData itemCustomSet;
	@XmlElement(name = "minions")
	public MinionData minionData;
	@XmlElement(name = "f2p_bonus")
	public F2PBonusData f2pBonus;
	@XmlElement(name = "arcadelist")
	public ArcadeUpgradeData arcadeUpgradeData;
	@XmlElement(name = "global_rules")
	public GlobalDropData globalDropData;
	@XmlElement(name = "item_skill_enhances")
	public ItemSkillEnhanceData itemSkillEnhance;
	@XmlElement(name = "boost_events")
	public BoostEventdata boostEvents;
	@XmlElement(name = "monster_books")
	public AtreianBestiaryData atreianBestiary;
	@XmlElement(name = "events_window")
	public EventsWindowData eventsWindow;
	@XmlElement(name = "reward_mail_templates")
	public MailRewardData mailReward;
	@XmlElement(name = "luna_dice")
	public LunaDiceData lunaDice;
	@XmlElement(name = "revive_world_start_points")
	public ReviveWorldStartPointsData reviveWorldStartPoints;
	@XmlElement(name = "tower_of_eternity")
	public TowerOfEternityData towerOfEternity;
	@XmlElement(name = "instance_revive_start_points")
	public ReviveInstanceStartPointsData reviveInstanceStartPoints;
	@XmlElement(name = "outpost_locations")
	public OutpostData outpostLocation;
	@XmlElement(name = "stones_cp")
	public StoneCpData stoneCp;
	@XmlElement(name = "tower_reward_templates")
	public TowerRewardData towerReward;
	@XmlElement(name = "shugo_sweeps")
	public ShugoSweepRewardData shugoSweepsRewardData;
	@XmlElement(name = "skill_skins")
	public SkillSkinData skillSkinData;

	/**
	 * 全部静态定义加载完成后，按配置输出各分区加载数量摘要日志。
	 * Logs a size summary after all static definitions have been loaded when enabled.
	 */
	public void logSummary() {
		if (!GSConfig.STATIC_DATA_SUMMARY_LOG) {
			return;
		}
		log.info(I18n.get("log.d43419c8da6c", worldMapsData.size()));
		log.info(I18n.get("log.b223e19e2ce1", playerExperienceTable.getMaxLevel()));
		log.info(I18n.get("log.cd1d2fa1265d", playerStatsData.size()));
		log.info(I18n.get("log.b92082bc2aef", summonStatsData.size()));
		log.info(I18n.get("log.163e26cbbede", itemCleanup.size()));
		if (itemData != null) {
			log.info(I18n.get("log.a7639d832d45", itemData.size()));
		}
		log.info(I18n.get("log.cb813eb3067b", itemRandomBonuses.size()));
		log.info(I18n.get("log.d7990503f474", itemGroupsData.bonusSize()));
		log.info(I18n.get("log.b29f4c6a427b", itemGroupsData.petFoodSize()));
		log.info(I18n.get("log.7a39ab3cdda2", npcData.size()));
		log.info(I18n.get("log.02542267fe52", systemMailTemplates.size()));
		log.info(I18n.get("log.3af3de6f0421", npcShoutData.size()));
		log.info(I18n.get("log.58b469a0870e", petData.size()));
		log.info(I18n.get("log.90c0f7b5bcfc", petFeedData.size()));
		log.info(I18n.get("log.a64de9021b02", petDopingData.size()));
		log.info(I18n.get("log.260b9c13369e", petMerchandData.size()));
		log.info(I18n.get("log.9aba8952c181", playerInitialData.size()));
		log.info(I18n.get("log.ddef83f06fc8", goodsListData.size()));
		log.info(I18n.get("log.bb4da84ad4a0", tradeListData.size()));
		log.info(I18n.get("log.cb32c6ebe648", teleporterData.size()));
		log.info(I18n.get("log.b463de58d584", teleLocationData.size()));
		log.info(I18n.get("log.031bd5830a8c", hotspotLocationData.size()));
		log.info(I18n.get("log.b5f7ba1ed5cc", skillData.size()));
		log.info(I18n.get("log.80c433e8573e", motionData.size()));
		log.info(I18n.get("log.2897b432221b", skillTreeData.size()));
		log.info(I18n.get("log.a18f09254709", cubeExpandData.size()));
		log.info(I18n.get("log.56f737c52823", warehouseExpandData.size()));
		log.info(I18n.get("log.0da69f2a900a", bindPointData.size()));
		log.info(I18n.get("log.fe9338f00401", questData.size()));
		log.info(I18n.get("log.d41021554656", gatherableData.size()));
		log.info(I18n.get("log.f7402147d1c0", titleData.size()));
		log.info(I18n.get("log.926913cabd63", walkerData.size()));
		log.info(I18n.get("log.2a423e0f769e", zoneData.size()));
		log.info(I18n.get("log.68bda207beab", tribeRelationsData.size()));
		log.info(I18n.get("log.330854034f35", recipeData.size()));
		log.info(I18n.get("log.412d1d563a20", lunaData.size()));
		log.info(I18n.get("log.02db48cab2bc", chestData.size()));
		log.info(I18n.get("log.b56d2ee109f4", staticDoorData.size()));
		log.info(I18n.get("log.13792a5f89b7", itemSetData.size()));
		log.info(I18n.get("log.6be770e218ef", npcFactionsData.size()));
		log.info(I18n.get("log.05be76e45171", petSkillData.size()));
		log.info(I18n.get("log.817680ce4780", siegeLocationData.size()));
		log.info(I18n.get("log.6850322d6c37", flyRingData.size()));
		log.info(I18n.get("log.43a80a7acc11", shieldData.size()));
		log.info(I18n.get("log.861f6a3b8237", petData.size()));
		log.info(I18n.get("log.23acbd6ac8e3", guideData.size()));
		log.info(I18n.get("log.41cb4bc71597", roadData.size()));
		log.info(I18n.get("log.cc9cb867aece", instanceCooltimeData.size()));
		log.info(I18n.get("log.d35d8517e6f0", disassemblyItemSetsData.size()));
		log.info(I18n.get("log.7d0656872005", aiData.size()));
		log.info(I18n.get("log.b5ce3e0bbfb7", flyPath.size()));
		log.info(I18n.get("log.1b0dc7dfa0c2", assembledNpcData.size()));
		log.info(I18n.get("log.e4cbb71f003a", cosmeticItemsData.size()));
		log.info(I18n.get("log.4103f2b9b4db", npcDropData.size()));
		log.info(I18n.get("log.546e904b6600", autoGroupData.size()));
		log.info(I18n.get("log.2bcccf8f811f", spawnsData2.size()));
		log.info(I18n.get("log.a8fed52a5964", eventData.size()));
		log.info(I18n.get("log.c43241318695", panelSkillsData.size()));
		log.info(I18n.get("log.402505407463", instanceBuffData.size()));
		log.info(I18n.get("log.2b693aa87217", housingObjectData.size()));
		log.info(I18n.get("log.2b042d4f0fc3", rideData.size()));
		log.info(I18n.get("log.266dbc9bceaf", robotData.size()));
		log.info(I18n.get("log.2e04d52e4051", instanceExitData.size()));
		log.info(I18n.get("log.9f9da08faf46", portalLocData.size()));
		log.info(I18n.get("log.d9dce71ebe89", portalTemplate2.size()));
		log.info(I18n.get("log.636e11ec5bf8", houseData.size()));
		log.info(I18n.get("log.957eeef58141", houseBuildingData.size()));
		log.info(I18n.get("log.1d22db488ed3", housePartsData.size()));
		log.info(I18n.get("log.6c42b563e0d1", houseNpcsData.size()));
		log.info(I18n.get("log.c3903bb9b1f0", houseScriptData.size()));
		log.info(I18n.get("log.8d930393db12", curingObjectsData.size()));
		log.info(I18n.get("log.34055ef9a6f5", springObjectsData.size()));
		log.info(I18n.get("log.f6123674a81a", assemblyItemData.size()));
		log.info(I18n.get("log.5ed8f689c200", challengeData.size()));
		log.info(I18n.get("log.37e549ec154b", townSpawnsData.getSpawnsCount()));
		log.info(I18n.get("log.aed773a27e84", abyssBuffData.size()));
		log.info(I18n.get("log.d1d536af00c9", abyssGroupData.size()));
		log.info(I18n.get("log.947499cbb7c5", absoluteStatsData.size()));
		log.info(I18n.get("log.6df3c7419d07", baseData.size()));
		log.info(I18n.get("log.0823fea5acd1", agentData.size()));
		log.info(I18n.get("log.7a6e8b8c428e", beritraData.size()));
		log.info(I18n.get("log.852fae89332a", svsData.size()));
		log.info(I18n.get("log.c7b678034e04", rvrData.size()));
		log.info(I18n.get("log.e1dd74a05c05", moltenusData.size()));
		log.info(I18n.get("log.53914340ae4c", dynamicRiftData.size()));
		log.info(I18n.get("log.cdedc9f5368d", instanceRiftData.size()));
		log.info(I18n.get("log.b3b9de759aa5", nightmareCircusData.size()));
		log.info(I18n.get("log.b2c6484f8f15", zorshivDredgionData.size()));
		log.info(I18n.get("log.26ec849d3a7b", legionDominionData.size()));
		log.info(I18n.get("log.c6e6280cba15", anohaData.size()));
		log.info(I18n.get("log.5e2de94d7629", iuData.size()));
		log.info(I18n.get("log.bb8bdbb28263", conquestData.size()));
		log.info(I18n.get("log.47b4bb329c1b", idianDepthsData.size()));
		log.info(I18n.get("log.6b12d0b74f9a", materiaData.size()));
		log.info(I18n.get("log.99e6edb0d2ea", mapWeatherData.size()));
		log.info(I18n.get("log.06d8d0191964", vortexData.size()));
		log.info(I18n.get("log.0880d45e5fbf", serialGuardData.size()));
		log.info(I18n.get("log.3643e7b742fa", serialKillerData.size()));
		log.info(I18n.get("log.730296b805bd", riftData.size()));
		log.info(I18n.get("log.f579eaf1d48c", serviceBuffData.size()));
		log.info(I18n.get("log.75d9f8552130", playersBonusData.size()));
		log.info(I18n.get("log.e7ffb007152a", itemEnchantData.size()));
		log.info(I18n.get("log.0e2d6d2ba2d8", itemUpgradeData.size()));
		log.info(I18n.get("log.89b08ae8495b", atreianPassportData.size()));
		log.info(I18n.get("log.d8ad43b2c6cd", gameExperienceData.size()));
		log.info(I18n.get("log.b1b871aa2fc5", abyssOpData.size()));
		log.info(I18n.get("log.597ec87ca2b4", panelCpData.size()));
		log.info(I18n.get("log.5b9a7fbb1888", petBuffData.size()));
		log.info(I18n.get("log.5e32e9200de4", multiReturnItemData.size()));
		log.info(I18n.get("log.268f567c859d", landingLocationData.size()));
		log.info(I18n.get("log.ea87c9e412bb", landingSpecialLocationData.size()));
		log.info(I18n.get("log.00f73b61e2e7", lunaConsumeRewardsData.size()));
		log.info(I18n.get("log.73ecb1ad6e2b", itemCustomSet.size()));
		log.info(I18n.get("log.57a69e9812ae", minionData.size()));
		log.info(I18n.get("log.b9e669923cce", f2pBonus.size()));
		log.info(I18n.get("log.2f415c5394aa", arcadeUpgradeData.size()));
		log.info(I18n.get("log.c67bcbcd05de", globalDropData.size()));
		log.info(I18n.get("log.5635e0855764", itemSkillEnhance.size()));
		log.info(I18n.get("log.adfafb08b32a", boostEvents.size()));
		log.info(I18n.get("log.596d2257b5ed", atreianBestiary.size()));
		log.info(I18n.get("log.2e9957f776eb", skillData.sizeOfGroup()));
		log.info(I18n.get("log.274e3cdb640a", chargeSkillData.size()));
		log.info(I18n.get("log.6d91ec4699d0", eventsWindow.size()));
		log.info(I18n.get("log.9de8a898475b", mailReward.size()));
		log.info(I18n.get("log.ea3bbb559359", lunaDice.size()));
		log.info(I18n.get("log.f71810db7f3e", reviveWorldStartPoints.size()));
		log.info(I18n.get("log.530721e609d0", reviveInstanceStartPoints.size()));
		log.info(I18n.get("log.59300f3e2648", outpostLocation.size()));
		log.info(I18n.get("log.009450541e78", stoneCp.size()));
		log.info(I18n.get("log.b9b36be371d6", towerReward.size()));
		log.info(I18n.get("log.825d4d1861a2", shugoSweepsRewardData.size()));
		log.info(I18n.get("log.de747e09a243", skillSkinData.size()));
	}
}
