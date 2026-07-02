/*

 *
 *  Encom is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Encom is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser Public License
 *  along with Encom.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.dataholders;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.model.templates.mail.Mails;

import lombok.extern.slf4j.Slf4j;

@XmlRootElement(name = "ae_static_data")
@XmlAccessorType(XmlAccessType.NONE)
@Slf4j
public class StaticData {
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

	@SuppressWarnings("unused")
	private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		if (!GSConfig.STATIC_DATA_SUMMARY_LOG) {
			return;
		}
		log.info("Loaded " + worldMapsData.size() + " Map");
		log.info("Loaded " + playerExperienceTable.getMaxLevel() + " Level");
		log.info("Loaded " + playerStatsData.size() + " Player Stats Template");
		log.info("Loaded " + summonStatsData.size() + " Summon Stats Template");
		log.info("Loaded " + itemCleanup.size() + " Item Cleanup");
		if (itemData != null) {
			log.info("Loaded " + itemData.size() + " Item Template");
		}
		log.info("Loaded " + itemRandomBonuses.size() + " Random Bonus Template");
		log.info("Loaded " + itemGroupsData.bonusSize() + " Bonus Item Group Template");
		log.info("Loaded " + itemGroupsData.petFoodSize() + " Pet Food Item");
		log.info("Loaded " + npcData.size() + " Npc Templates");
		log.info("Loaded " + systemMailTemplates.size() + " System Mail Template");
		log.info("Loaded " + npcShoutData.size() + " Npc Shout Template");
		log.info("Loaded " + petData.size() + " Pet Template");
		log.info("Loaded " + petFeedData.size() + " Food Flavour");
		log.info("Loaded " + petDopingData.size() + " Pet Doping Template");
		log.info("Loaded " + petMerchandData.size() + " Pet Merchand Template");
		log.info("Loaded " + playerInitialData.size() + " Initial Player Template");
		log.info("Loaded " + goodsListData.size() + " Goods List");
		log.info("Loaded " + tradeListData.size() + " Npc Trade List");
		log.info("Loaded " + teleporterData.size() + " Npc Teleporter Template");
		log.info("Loaded " + teleLocationData.size() + " Teleport Location");
		log.info("Loaded " + hotspotLocationData.size() + " Hotspot Location");
		log.info("Loaded " + skillData.size() + " Skill Templates");
		log.info("Loaded " + motionData.size() + " Motion Times");
		log.info("Loaded " + skillTreeData.size() + " Skill Learn");
		log.info("Loaded " + cubeExpandData.size() + " Cube Expander");
		log.info("Loaded " + warehouseExpandData.size() + " Warehouse Expander");
		log.info("Loaded " + bindPointData.size() + " Bind Point");
		log.info("Loaded " + questData.size() + " Quest Data");
		log.info("Loaded " + gatherableData.size() + " Gatherable");
		log.info("Loaded " + titleData.size() + " Title");
		log.info("Loaded " + walkerData.size() + " Walker Road");
		log.info("Loaded " + zoneData.size() + " Zone");
		log.info("Loaded " + tribeRelationsData.size() + " Tribe Relation");
		log.info("Loaded " + recipeData.size() + " Recipe Template");
		log.info("Loaded " + lunaData.size() + " Luna Template");
		log.info("Loaded " + chestData.size() + " Chest");
		log.info("Loaded " + staticDoorData.size() + " Static Door");
		log.info("Loaded " + itemSetData.size() + " Item Set");
		log.info("Loaded " + npcFactionsData.size() + " Npc Faction");
		log.info("Loaded " + npcSkillData.size() + " Npc Skill List");
		log.info("Loaded " + petSkillData.size() + " Pet Skill List");
		log.info("Loaded " + siegeLocationData.size() + " Siege Location");
		log.info("Loaded " + flyRingData.size() + " Fly Ring");
		log.info("Loaded " + shieldData.size() + " Shield");
		log.info("Loaded " + petData.size() + " Pet");
		log.info("Loaded " + guideData.size() + " Guide");
		log.info("Loaded " + roadData.size() + " Road");
		log.info("Loaded " + instanceCooltimeData.size() + " Instance Cooltime");
		log.info("Loaded " + disassemblyItemSetsData.size() + " Decomposable Item");
		log.info("Loaded " + aiData.size() + " Ai2 Template");
		log.info("Loaded " + flyPath.size() + " Flypath Template");
		log.info("Loaded " + windstreamsData.size() + " Windstream");
		log.info("Loaded " + assembledNpcData.size() + " Assembled Npc");
		log.info("Loaded " + cosmeticItemsData.size() + " Cosmetics Item");
		log.info("Loaded " + npcDropData.size() + " Npc Drop");
		log.info("Loaded " + autoGroupData.size() + " Auto Group");
		log.info("Loaded " + spawnsData2.size() + " Spawn Map");
		log.info("Loaded " + eventData.size() + " Event");
		log.info("Loaded " + panelSkillsData.size() + " Polymorph Panel");
		log.info("Loaded " + instanceBuffData.size() + " Instance Bonus");
		log.info("Loaded " + housingObjectData.size() + " Housing Object");
		log.info("Loaded " + rideData.size() + " Ride");
		log.info("Loaded " + robotData.size() + " Aethertech Armor");
		log.info("Loaded " + instanceExitData.size() + " Instance Exit");
		log.info("Loaded " + portalLocData.size() + " Portal Location");
		log.info("Loaded " + portalTemplate2.size() + " Portal Template");
		log.info("Loaded " + houseData.size() + " Housing land");
		log.info("Loaded " + houseBuildingData.size() + " Housing Building Style");
		log.info("Loaded " + housePartsData.size() + " House Parts");
		log.info("Loaded " + houseNpcsData.size() + " House Spawn");
		log.info("Loaded " + houseScriptData.size() + " House Scripts");
		log.info("Loaded " + curingObjectsData.size() + " Curing Object");
		log.info("Loaded " + springObjectsData.size() + " Spring Object");
		log.info("Loaded " + assemblyItemData.size() + " Assembly Item");
		log.info("Loaded " + challengeData.size() + " Challenge Task");
		log.info("Loaded " + townSpawnsData.getSpawnsCount() + " Town Location");
		log.info("Loaded " + abyssBuffData.size() + " Abyss Bonus");
		log.info("Loaded " + abyssGroupData.size() + " Abyss Group");
		log.info("Loaded " + absoluteStatsData.size() + " Absolute Stats");
		log.info("Loaded " + baseData.size() + " Bases Location");
		log.info("Loaded " + agentData.size() + " Agent Fight");
		log.info("Loaded " + beritraData.size() + " Eresukigal/Beritra Invasion");
		log.info("Loaded " + svsData.size() + " S.v.S Location");
		log.info("Loaded " + rvrData.size() + " R.v.R Location");
		log.info("Loaded " + moltenusData.size() + " Moltenus Location");
		log.info("Loaded " + dynamicRiftData.size() + " Dynamic Rift Location");
		log.info("Loaded " + instanceRiftData.size() + " Instance Rift Location");
		log.info("Loaded " + nightmareCircusData.size() + " Nightmare Cirus Location");
		log.info("Loaded " + zorshivDredgionData.size() + " Zorshiv Dredgion Location");
		log.info("Loaded " + legionDominionData.size() + " Legion Dominion Location");
		log.info("Loaded " + anohaData.size() + " Anoha Location");
		log.info("Loaded " + iuData.size() + " Concert location");
		log.info("Loaded " + conquestData.size() + " Conquest/Offering Location");
		log.info("Loaded " + idianDepthsData.size() + " Idian Depths Location");
		log.info("Loaded " + materiaData.size() + " Materials");
		log.info("Loaded " + mapWeatherData.size() + " Weather Map");
		log.info("Loaded " + vortexData.size() + " Vortex");
		log.info("Loaded " + serialGuardData.size() + " Serial Guard");
		log.info("Loaded " + serialKillerData.size() + " Serial Killer");
		log.info("Loaded " + riftData.size() + " Rift/Volatile/Chaos Rift");
		log.info("Loaded " + serviceBuffData.size() + " Service Bonus");
		log.info("Loaded " + playersBonusData.size() + " Player Bonus");
		log.info("Loaded " + itemEnchantData.size() + " Item Enchant Table");
		log.info("Loaded " + itemUpgradeData.size() + " Item Upgrade");
		log.info("Loaded " + atreianPassportData.size() + " Atreian Passport");
		log.info("Loaded " + gameExperienceData.size() + " Game Experience Item");
		log.info("Loaded " + abyssOpData.size() + " Abyss Landing Table");
		log.info("Loaded " + panelCpData.size() + " Panel Cp");
		log.info("Loaded " + petBuffData.size() + " Pet Buff");
		log.info("Loaded " + multiReturnItemData.size() + " Multi Return Item");
		log.info("Loaded " + landingLocationData.size() + " Abyss Landing Location");
		log.info("Loaded " + landingSpecialLocationData.size() + " Monument Location");
		log.info("Loaded " + lunaConsumeRewardsData.size() + " Luna Consume Reward");
		log.info("Loaded " + itemCustomSet.size() + " Item Custom Set");
		log.info("Loaded " + minionData.size() + " Minion templates");
		log.info("Loaded " + f2pBonus.size() + " F2p Bonus Pack");
		log.info("Loaded " + arcadeUpgradeData.size() + " Upgrade Arcade");
		log.info("Loaded " + globalDropData.size() + " Global Drops");
		log.info("Loaded " + itemSkillEnhance.size() + " Skill Boost Reassignment");
		log.info("Loaded " + boostEvents.size() + " Boost Events");
		log.info("Loaded " + atreianBestiary.size() + " Atreian Bestiary");
		log.info("Loaded " + skillData.sizeOfGroup() + " Skill Templates Group");
		log.info("Loaded " + chargeSkillData.size() + " Charge Skill");
		log.info("Loaded " + eventsWindow.size() + " Event Window");
		log.info("Loaded " + mailReward.size() + " Reward Mail");
		log.info("Loaded " + lunaDice.size() + " Luna Dice Reward");
		log.info("Loaded " + reviveWorldStartPoints.size() + " Revive World Start Points");
		log.info("Loaded " + reviveInstanceStartPoints.size() + " Revive Instance Start Points");
		log.info("Loaded " + outpostLocation.size() + " Outpost Location");
		log.info("Loaded " + stoneCp.size() + " Estima Enchant");
		log.info("Loaded " + towerReward.size() + " Tower Data Reward");
		log.info("Loaded " + shugoSweepsRewardData.size() + " Shugo Sweep Reward");
		log.info("Loaded " + skillSkinData.size() + " Skill Animation Entries");
	}
}
