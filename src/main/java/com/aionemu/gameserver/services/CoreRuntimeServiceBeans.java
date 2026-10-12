package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.model.templates.tasks.TaskFromDBHandler;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.player.PlayerEnterWorldService;
import com.aionemu.gameserver.services.player.PlayerService;
import com.aionemu.gameserver.services.player.PlayerServiceDaos;
import com.aionemu.gameserver.services.item.ItemSocketService;
import com.aionemu.gameserver.services.ranking.SeasonRankingService;
import com.aionemu.gameserver.services.reward.RewardService;
import com.aionemu.gameserver.services.rift.RiftManager;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;
import com.aionemu.gameserver.services.territory.TerritoryService;
import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import com.aionemu.gameserver.services.veteranreward.VeteranRewardsService;
import com.aionemu.gameserver.taskmanager.TaskManagerFromDB;
import com.aionemu.gameserver.utils.audit.GMService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.BrokerDAO;
import com.aionemu.gameserver.dao.CraftCooldownsDAO;
import com.aionemu.gameserver.dao.EventItemsDAO;
import com.aionemu.gameserver.dao.F2pDAO;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dao.HouseObjectCooldownsDAO;
import com.aionemu.gameserver.dao.HouseScriptsDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.InGameShopDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemCooldownsDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.LimitedQuestDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.MotionDAO;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dao.PlayerBindPointDAO;
import com.aionemu.gameserver.dao.PlayerCooldownsDAO;
import com.aionemu.gameserver.dao.PlayerCreativityPointsDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerEffectsDAO;
import com.aionemu.gameserver.dao.PlayerEmotionListDAO;
import com.aionemu.gameserver.dao.PlayerEquipmentSettingDAO;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.dao.PlayerLifeStatsDAO;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.dao.PlayerMacrossesDAO;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.dao.PlayerNpcFactionsDAO;
import com.aionemu.gameserver.dao.PlayerPasskeyDAO;
import com.aionemu.gameserver.dao.PlayerPetsDAO;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.dao.PlayerSettingsDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.dao.PlayerTransformDAO;
import com.aionemu.gameserver.dao.PlayerVarsDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.dao.PortalCooldownsDAO;
import com.aionemu.gameserver.dao.RewardServiceDAO;
import com.aionemu.gameserver.dao.SeasonRankingDAO;
import com.aionemu.gameserver.dao.SurveyControllerDAO;
import com.aionemu.gameserver.dao.TaskFromDBDAO;
import com.aionemu.gameserver.dao.VeteranRewardsDAO;

/**
 * CoreRuntimeServiceBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * CoreRuntimeServiceBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class CoreRuntimeServiceBeans {

    @Bean
    @Lazy
    public AdminService adminService() {
        return new AdminService();
    }

    @Bean
    @Lazy
    public PlayerTransferService gamePlayerTransferService(ObjectProvider<PlayerDAO> playerDaoProvider,
                                                           ObjectProvider<LegionMemberDAO> legionMemberDaoProvider) {
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        LegionMemberDAO legionMemberDAO = legionMemberDaoProvider.getIfAvailable();
        return playerDAO != null && legionMemberDAO != null ? new PlayerTransferService(playerDAO, legionMemberDAO)
            : new PlayerTransferService();
    }

    @Bean
    @Lazy
    public PeriodicSaveService periodicSaveService(ObjectProvider<AbyssRankDAO> abyssRankDaoProvider,
                                                   ObjectProvider<PlayerSkillListDAO> playerSkillListDaoProvider,
                                                   ObjectProvider<PlayerQuestListDAO> playerQuestListDaoProvider,
                                                   ObjectProvider<PlayerDAO> playerDaoProvider,
                                                   ObjectProvider<PlayerStigmasEquippedDAO> playerStigmasEquippedDaoProvider,
                                                   ObjectProvider<InventoryDAO> inventoryDaoProvider,
                                                   ObjectProvider<ItemStoneListDAO> itemStoneListDaoProvider) {
        AbyssRankDAO abyssRankDAO = abyssRankDaoProvider.getIfAvailable();
        PlayerSkillListDAO playerSkillListDAO = playerSkillListDaoProvider.getIfAvailable();
        PlayerQuestListDAO playerQuestListDAO = playerQuestListDaoProvider.getIfAvailable();
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        PlayerStigmasEquippedDAO playerStigmasEquippedDAO = playerStigmasEquippedDaoProvider.getIfAvailable();
        InventoryDAO inventoryDAO = inventoryDaoProvider.getIfAvailable();
        ItemStoneListDAO itemStoneListDAO = itemStoneListDaoProvider.getIfAvailable();
        if (abyssRankDAO != null && playerSkillListDAO != null && playerQuestListDAO != null
                && playerDAO != null && playerStigmasEquippedDAO != null
                && inventoryDAO != null && itemStoneListDAO != null) {
            return new PeriodicSaveService(abyssRankDAO, playerSkillListDAO, playerQuestListDAO,
                    playerDAO, playerStigmasEquippedDAO, inventoryDAO, itemStoneListDAO);
        }
        return new PeriodicSaveService();
    }

    @Bean
    @Lazy
    public TerritoryService territoryService(ObjectProvider<LegionDAO> legionDaoProvider) {
        LegionDAO dao = legionDaoProvider.getIfAvailable();
        return dao != null ? new TerritoryService(dao) : new TerritoryService();
    }

    @Bean
    @Lazy
    public GameTimeService gameTimeService() {
        return new GameTimeService();
    }

    @Bean
    @Lazy
    public AnnouncementService announcementService(ObjectProvider<AnnouncementsDAO> announcementsDaoProvider) {
        AnnouncementsDAO dao = announcementsDaoProvider.getIfAvailable();
        return dao != null ? new AnnouncementService(dao) : new AnnouncementService();
    }

    @Bean
    @Lazy
    public DebugService debugService() {
        return new DebugService();
    }

    @Bean
    @Lazy
    public WeatherService weatherService() {
        return new WeatherService();
    }

    @Bean
    @Lazy
    public BrokerService brokerService(ObjectProvider<BrokerDAO> brokerDaoProvider,
                                       ObjectProvider<InventoryDAO> inventoryDaoProvider) {
        BrokerDAO brokerDAO = brokerDaoProvider.getIfAvailable();
        InventoryDAO inventoryDAO = inventoryDaoProvider.getIfAvailable();
        if (brokerDAO != null && inventoryDAO != null) {
            return new BrokerService(brokerDAO, inventoryDAO);
        }
        return new BrokerService();
    }

    @Bean
    @Lazy
    public Influence influence() {
        return new Influence();
    }

    @Bean
    @Lazy
    public ExchangeService exchangeService(ObjectProvider<InventoryDAO> inventoryDaoProvider) {
        InventoryDAO dao = inventoryDaoProvider.getIfAvailable();
        return dao != null ? new ExchangeService(dao) : new ExchangeService();
    }

    @Bean
    @Lazy
    public PetitionService petitionService(ObjectProvider<PetitionDAO> petitionDaoProvider) {
        PetitionDAO dao = petitionDaoProvider.getIfAvailable();
        return dao != null ? new PetitionService(dao) : new PetitionService();
    }

    @Bean
    @Lazy
    public FlyRingService flyRingService() {
        return new FlyRingService();
    }

    @Bean
    @Lazy
    public CuringZoneService curingZoneService() {
        return new CuringZoneService();
    }

    @Bean
    @Lazy
    public SpringZoneService springZoneService() {
        return new SpringZoneService();
    }

    @Bean
    @Lazy
    public BoostEventService boostEventService() {
        return new BoostEventService();
    }

    /**
     * 装配数据库任务管理器：注入任务 DAO 并顺带绑定至 {@link TaskFromDBHandler} 静态门面
     * （反射创建的处理器无法构造注入，缺失时保留 safeDao 兜底，不触发整体降级）。
     * Wires the DB task manager: injects the task DAO and also binds it to the {@link TaskFromDBHandler}
     * static facade (reflectively created handlers cannot take constructor injection; keep the safeDao
     * fallback when absent, without triggering a full offline fallback).
     */
    @Bean
    @Lazy
    public TaskManagerFromDB taskManagerFromDB(ObjectProvider<TaskFromDBDAO> taskFromDBDaoProvider) {
        TaskFromDBDAO taskFromDBDAO = taskFromDBDaoProvider.getIfAvailable();
        if (taskFromDBDAO != null) {
            TaskFromDBHandler.bindTaskFromDBDAO(taskFromDBDAO);
            return new TaskManagerFromDB(taskFromDBDAO);
        }
        return new TaskManagerFromDB();
    }

    @Bean
    @Lazy
    public LimitedItemTradeService limitedItemTradeService() {
        return new LimitedItemTradeService();
    }

    @Bean
    @Lazy
    public GMService gmService() {
        return new GMService();
    }

    @Bean
    public GameRuntimeServiceBridge gameRuntimeServiceBridge() {
        return new GameRuntimeServiceBridge();
    }

    @Bean
    @Lazy
    public RewardService rewardService(ObjectProvider<RewardServiceDAO> rewardServiceDaoProvider) {
        RewardServiceDAO dao = rewardServiceDaoProvider.getIfAvailable();
        return dao != null ? new RewardService(dao) : new RewardService();
    }

    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService(ObjectProvider<VeteranRewardsDAO> veteranRewardsDaoProvider,
                                                       ObjectProvider<PlayerDAO> playerDaoProvider) {
        VeteranRewardsDAO veteranRewardsDao = veteranRewardsDaoProvider.getIfAvailable();
        PlayerDAO playerDao = playerDaoProvider.getIfAvailable();
        return veteranRewardsDao != null && playerDao != null ? new VeteranRewardsService(veteranRewardsDao, playerDao)
                : new VeteranRewardsService();
    }

    @Bean
    @Lazy
    public HousingService housingService(ObjectProvider<HousesDAO> housesDaoProvider) {
        HousesDAO dao = housesDaoProvider.getIfAvailable();
        return dao != null ? new HousingService(dao) : new HousingService();
    }

    @Bean
    @Lazy
    public SurveyService surveyService(ObjectProvider<SurveyControllerDAO> surveyControllerDaoProvider) {
        SurveyControllerDAO dao = surveyControllerDaoProvider.getIfAvailable();
        return dao != null ? new SurveyService(dao) : new SurveyService();
    }

    @Bean
    @Lazy
    public FindGroupService findGroupService() {
        return new FindGroupService();
    }

    @Bean
    @Lazy
    public WebshopService webshopService(ObjectProvider<RewardServiceDAO> rewardServiceDaoProvider) {
        RewardServiceDAO dao = rewardServiceDaoProvider.getIfAvailable();
        return dao != null ? new WebshopService(dao) : new WebshopService();
    }

    @Bean
    @Lazy
    public InGameShopEn inGameShopEn(ObjectProvider<InGameShopDAO> inGameShopDaoProvider) {
        InGameShopDAO inGameShopDAO = inGameShopDaoProvider.getIfAvailable();
        return inGameShopDAO != null ? new InGameShopEn(inGameShopDAO) : new InGameShopEn();
    }

    @Bean
    @Lazy
    public LegionService legionService(ObjectProvider<LegionDAO> legionDaoProvider,
                                       ObjectProvider<LegionMemberDAO> legionMemberDaoProvider,
                                       ObjectProvider<InventoryDAO> inventoryDaoProvider,
                                       ObjectProvider<ItemStoneListDAO> itemStoneListDaoProvider,
                                       ObjectProvider<PlayerDAO> playerDaoProvider) {
        LegionDAO legionDAO = legionDaoProvider.getIfAvailable();
        LegionMemberDAO legionMemberDAO = legionMemberDaoProvider.getIfAvailable();
        InventoryDAO inventoryDAO = inventoryDaoProvider.getIfAvailable();
        ItemStoneListDAO itemStoneListDAO = itemStoneListDaoProvider.getIfAvailable();
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        if (legionDAO != null && legionMemberDAO != null && inventoryDAO != null && itemStoneListDAO != null
            && playerDAO != null) {
            return new LegionService(legionDAO, legionMemberDAO, inventoryDAO, itemStoneListDAO, playerDAO);
        }
        return new LegionService();
    }

    /**
     * 装配玩家核心服务的持久层依赖聚合并在启动期绑定至 {@link PlayerService}；
     * 任一 DAO 缺失（离线/单测上下文）时降级为 safeDao 离线聚合，保持旧行为等价。
     * Wires the core player service persistence aggregate and binds it to {@link PlayerService}
     * at startup; falls back to the safeDao offline aggregate when any DAO is missing
     * (offline/unit-test contexts), preserving legacy behavior.
     */
	@Bean
	public PlayerServiceDaos playerServiceDaos(ConfigurableBeanFactory beanFactory) {
        try {
            PlayerServiceDaos daos = new PlayerServiceDaos(
                daoFrom(beanFactory, PlayerDAO.class),
                daoFrom(beanFactory, OldNamesDAO.class),
                daoFrom(beanFactory, PlayerAppearanceDAO.class),
                daoFrom(beanFactory, PlayerSkillListDAO.class),
                daoFrom(beanFactory, InventoryDAO.class),
                daoFrom(beanFactory, PlayerStigmasEquippedDAO.class),
                daoFrom(beanFactory, PlayerEquipmentSettingDAO.class),
                daoFrom(beanFactory, PlayerSettingsDAO.class),
                daoFrom(beanFactory, PlayerQuestListDAO.class),
                daoFrom(beanFactory, AbyssRankDAO.class),
                daoFrom(beanFactory, PlayerPunishmentsDAO.class),
                daoFrom(beanFactory, HousesDAO.class),
                daoFrom(beanFactory, PlayerRegisteredItemsDAO.class),
                daoFrom(beanFactory, ItemStoneListDAO.class),
                daoFrom(beanFactory, MailDAO.class),
                daoFrom(beanFactory, PortalCooldownsDAO.class),
                daoFrom(beanFactory, CraftCooldownsDAO.class),
                daoFrom(beanFactory, PlayerNpcFactionsDAO.class),
                daoFrom(beanFactory, PlayerLunaShopDAO.class),
                daoFrom(beanFactory, EventItemsDAO.class),
                daoFrom(beanFactory, PlayerMacrossesDAO.class),
                daoFrom(beanFactory, FriendListDAO.class),
                daoFrom(beanFactory, BlockListDAO.class),
                daoFrom(beanFactory, PlayerTitleListDAO.class),
                daoFrom(beanFactory, PlayerCreativityPointsDAO.class),
                daoFrom(beanFactory, PlayerEventsWindowDAO.class),
                daoFrom(beanFactory, PlayerABDAO.class),
                daoFrom(beanFactory, PlayerWardrobeDAO.class),
                daoFrom(beanFactory, F2pDAO.class),
                daoFrom(beanFactory, MotionDAO.class),
                daoFrom(beanFactory, PlayerVarsDAO.class),
                daoFrom(beanFactory, PlayerRecipesDAO.class),
                daoFrom(beanFactory, PlayerSkillSkinListDAO.class),
                daoFrom(beanFactory, PlayerEffectsDAO.class),
                daoFrom(beanFactory, PlayerCooldownsDAO.class),
                daoFrom(beanFactory, ItemCooldownsDAO.class),
                daoFrom(beanFactory, HouseObjectCooldownsDAO.class),
                daoFrom(beanFactory, PlayerBindPointDAO.class),
                daoFrom(beanFactory, PlayerLifeStatsDAO.class),
                daoFrom(beanFactory, PlayerEmotionListDAO.class),
                daoFrom(beanFactory, PlayerPetsDAO.class),
                daoFrom(beanFactory, PlayerMinionsDAO.class),
                daoFrom(beanFactory, PlayerShugoSweepDAO.class),
                daoFrom(beanFactory, HouseScriptsDAO.class),
                daoFrom(beanFactory, PlayerPasskeyDAO.class),
                daoFrom(beanFactory, PlayerTransformDAO.class));
            PlayerService.bindDaos(daos);
            // 顺带绑定 item 域静态工具类的魔石列表 DAO / also bind the item-domain static utility's DAO
            ItemSocketService.bindItemStoneListDAO(daos.itemStoneListDAO());
            // 顺带绑定 player 域静态门面的二级密码 DAO（缺失时保留 safeDao 兜底，不触发整体降级）
            // also bind the player-domain static facade's passkey DAO (keep safeDao fallback when absent,
            // without triggering a full offline fallback)
            PlayerPasskeyDAO playerPasskeyDAO = daoFrom(beanFactory, PlayerPasskeyDAO.class);
            if (playerPasskeyDAO != null) {
                PlayerEnterWorldService.bindPlayerPasskeyDAO(playerPasskeyDAO);
            }
            // 顺带绑定 teleport 域静态门面的玩家变身 DAO（缺失时保留 safeDao 兜底，不触发整体降级）
            // also bind the teleport-domain static facade's transform DAO (keep safeDao fallback when absent,
            // without triggering a full offline fallback)
            PlayerTransformDAO playerTransformDAO = daoFrom(beanFactory, PlayerTransformDAO.class);
            if (playerTransformDAO != null) {
                com.aionemu.gameserver.services.teleport.TeleportService2.bindPlayerTransformDAO(playerTransformDAO);
            }
            return daos;
        } catch (NullPointerException offline) {
            return PlayerService.daos();
        }
    }

    private static <T extends DAO> T daoFrom(ConfigurableBeanFactory beanFactory, Class<T> daoClass) {
        return beanFactory.getBeanProvider(daoClass).getIfAvailable();
    }

    @Bean
    public BalaurAssaultService balaurAssaultService() {
        return new BalaurAssaultService();
    }

    @Bean
    public BattlefieldUnionService battlefieldUnionService() {
        return new BattlefieldUnionService();
    }

    @Bean
    public PvpService pvpService() {
        return new PvpService();
    }

    @Bean
    public AutoGroupService autoGroupService() {
        return new AutoGroupService();
    }

    @Bean
    public AbyssRankingCache abyssRankingCache(ObjectProvider<AbyssRankDAO> abyssRankDaoProvider) {
        AbyssRankDAO dao = abyssRankDaoProvider.getIfAvailable();
        return dao != null ? new AbyssRankingCache(dao) : new AbyssRankingCache();
    }

    @Bean
    public MailService mailService(ObjectProvider<MailDAO> mailDaoProvider,
                                   ObjectProvider<InventoryDAO> inventoryDaoProvider,
                                   ObjectProvider<PlayerDAO> playerDaoProvider,
                                   ObjectProvider<AbyssRankDAO> abyssRankDaoProvider) {
        MailDAO mailDAO = mailDaoProvider.getIfAvailable();
        InventoryDAO inventoryDAO = inventoryDaoProvider.getIfAvailable();
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        AbyssRankDAO abyssRankDAO = abyssRankDaoProvider.getIfAvailable();
        if (mailDAO != null && inventoryDAO != null && playerDAO != null && abyssRankDAO != null) {
            return new MailService(mailDAO, inventoryDAO, playerDAO, abyssRankDAO);
        }
        return new MailService();
    }

    @Bean
    public DropService dropService() {
        return new DropService();
    }

    @Bean
    @Lazy
    public RiftManager riftManager() {
        return new RiftManager();
    }

    @Bean
    @Lazy
    public SeasonRankingService seasonRankingService(ObjectProvider<SeasonRankingDAO> seasonRankingDaoProvider) {
        SeasonRankingDAO seasonRankingDao = seasonRankingDaoProvider.getIfAvailable();
        return seasonRankingDao != null ? new SeasonRankingService(seasonRankingDao) : new SeasonRankingService();
    }

    @Bean
    public LifeStatsRestoreService lifeStatsRestoreService() {
        return new LifeStatsRestoreService();
    }

    @Bean
    @Lazy
    public DuelService duelService() {
        return new DuelService();
    }

    @Bean
    @Lazy
    public PunishmentService punishmentService(ObjectProvider<PlayerPunishmentsDAO> daoProvider) {
        PlayerPunishmentsDAO dao = daoProvider.getIfAvailable();
        return dao != null ? new PunishmentService(dao) : new PunishmentService();
    }

    @Bean
    @Lazy
    public RenameService renameService(ObjectProvider<OldNamesDAO> oldNamesDaoProvider,
                                       ObjectProvider<PlayerDAO> playerDaoProvider,
                                       ObjectProvider<LegionDAO> legionDaoProvider) {
        OldNamesDAO oldNamesDAO = oldNamesDaoProvider.getIfAvailable();
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        LegionDAO legionDAO = legionDaoProvider.getIfAvailable();
        if (oldNamesDAO != null && playerDAO != null && legionDAO != null) {
            return new RenameService(oldNamesDAO, playerDAO, legionDAO);
        }
        return new RenameService();
    }

    @Bean
    @Lazy
    public HTMLService htmlService(ObjectProvider<GuideDAO> guideDaoProvider) {
        GuideDAO dao = guideDaoProvider.getIfAvailable();
        return dao != null ? new HTMLService(dao) : new HTMLService();
    }

    @Bean
    @Lazy
    public AccountService accountService(ObjectProvider<InventoryDAO> inventoryDaoProvider,
                                         ObjectProvider<PlayerDAO> playerDaoProvider,
                                         ObjectProvider<PlayerAppearanceDAO> playerAppearanceDaoProvider,
                                         ObjectProvider<PlayerPunishmentsDAO> playerPunishmentsDaoProvider,
                                         ObjectProvider<LegionMemberDAO> legionMemberDaoProvider) {
        InventoryDAO inventoryDAO = inventoryDaoProvider.getIfAvailable();
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        PlayerAppearanceDAO playerAppearanceDAO = playerAppearanceDaoProvider.getIfAvailable();
        PlayerPunishmentsDAO playerPunishmentsDAO = playerPunishmentsDaoProvider.getIfAvailable();
        LegionMemberDAO legionMemberDAO = legionMemberDaoProvider.getIfAvailable();
        if (inventoryDAO != null && playerDAO != null && playerAppearanceDAO != null
                && playerPunishmentsDAO != null && legionMemberDAO != null) {
            return new AccountService(inventoryDAO, playerDAO, playerAppearanceDAO,
                    playerPunishmentsDAO, legionMemberDAO);
        }
        return new AccountService();
    }

    @Bean
    @Lazy
    public SocialService socialService(ObjectProvider<BlockListDAO> blockListDaoProvider,
                                       ObjectProvider<FriendListDAO> friendListDaoProvider,
                                       ObjectProvider<PlayerDAO> playerDaoProvider) {
        BlockListDAO blockListDAO = blockListDaoProvider.getIfAvailable();
        FriendListDAO friendListDAO = friendListDaoProvider.getIfAvailable();
        PlayerDAO playerDAO = playerDaoProvider.getIfAvailable();
        if (blockListDAO != null && friendListDAO != null && playerDAO != null) {
            return new SocialService(blockListDAO, friendListDAO, playerDAO);
        }
        return new SocialService();
    }

    @Bean
    @Lazy
    public LimitedQuestService limitedQuestService(ObjectProvider<LimitedQuestDAO> daoProvider) {
        LimitedQuestDAO dao = daoProvider.getIfAvailable();
        return dao != null ? new LimitedQuestService(dao) : new LimitedQuestService();
    }

    @Bean
    @Lazy
    public ArmsfusionService armsfusionService(ObjectProvider<InventoryDAO> inventoryDaoProvider) {
        InventoryDAO dao = inventoryDaoProvider.getIfAvailable();
        return dao != null ? new ArmsfusionService(dao) : new ArmsfusionService();
    }
}
