package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.siege.Influence;
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
import com.aionemu.gameserver.persistence.dao.AnnouncementDao;
import com.aionemu.gameserver.persistence.dao.BrokerDao;
import com.aionemu.gameserver.persistence.dao.CompetitionRankingDao;
import com.aionemu.gameserver.persistence.dao.GuideDao;
import com.aionemu.gameserver.persistence.dao.IngameshopDao;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.persistence.dao.LegionMemberDao;
import com.aionemu.gameserver.persistence.dao.LimitedQuestCounterDao;
import com.aionemu.gameserver.persistence.dao.PetitionDao;
import com.aionemu.gameserver.persistence.dao.SurveyDao;
import com.aionemu.gameserver.persistence.dao.TaskDao;
import com.aionemu.gameserver.persistence.dao.VeteranRewardDao;
import com.aionemu.gameserver.persistence.dao.WebRewardDao;
import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.dao.BlockDao;
import com.aionemu.gameserver.persistence.dao.CraftCooldownDao;
import com.aionemu.gameserver.persistence.dao.EventItemDao;
import com.aionemu.gameserver.persistence.dao.F2paccountDao;
import com.aionemu.gameserver.persistence.dao.FriendDao;
import com.aionemu.gameserver.persistence.dao.HouseObjectCooldownDao;
import com.aionemu.gameserver.persistence.dao.HouseScriptDao;
import com.aionemu.gameserver.persistence.dao.HouseDao;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.persistence.dao.ItemCooldownDao;
import com.aionemu.gameserver.persistence.dao.ItemStoneDao;
import com.aionemu.gameserver.persistence.dao.MailDao;
import com.aionemu.gameserver.persistence.dao.PlayerMotionDao;
import com.aionemu.gameserver.persistence.dao.OldNameDao;
import com.aionemu.gameserver.persistence.dao.PlayerAtreianBestiaryDao;
import com.aionemu.gameserver.persistence.dao.PlayerAppearanceDao;
import com.aionemu.gameserver.persistence.dao.PlayerBindPointDao;
import com.aionemu.gameserver.persistence.dao.PlayerCooldownDao;
import com.aionemu.gameserver.persistence.dao.PlayerCpDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.PlayerEffectDao;
import com.aionemu.gameserver.persistence.dao.PlayerEmotionDao;
import com.aionemu.gameserver.persistence.dao.PlayerEquipmentSettingDao;
import com.aionemu.gameserver.persistence.dao.PlayerEventsWindowDao;
import com.aionemu.gameserver.persistence.dao.PlayerLifeStatDao;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.dao.PlayerMacrosseDao;
import com.aionemu.gameserver.persistence.dao.PlayerMinionDao;
import com.aionemu.gameserver.persistence.dao.PlayerNpcFactionDao;
import com.aionemu.gameserver.persistence.dao.PlayerPasskeyDao;
import com.aionemu.gameserver.persistence.dao.PlayerPetDao;
import com.aionemu.gameserver.persistence.dao.PlayerPunishmentDao;
import com.aionemu.gameserver.persistence.dao.PlayerQuestDao;
import com.aionemu.gameserver.persistence.dao.PlayerShugoSweepDao;
import com.aionemu.gameserver.persistence.dao.PlayerRecipeDao;
import com.aionemu.gameserver.persistence.dao.PlayerRegisteredItemDao;
import com.aionemu.gameserver.persistence.dao.PlayerSettingDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillDao;
import com.aionemu.gameserver.persistence.dao.PlayerSkillSkinDao;
import com.aionemu.gameserver.persistence.dao.PlayerStigmasEquippedDao;
import com.aionemu.gameserver.persistence.dao.PlayerTitleDao;
import com.aionemu.gameserver.persistence.dao.PlayerTransformDao;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.persistence.dao.PortalCooldownDao;
import com.aionemu.gameserver.persistence.dao.PlayerVarDao;

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
    public PlayerTransferService gamePlayerTransferService(PlayerDao playerDAO, LegionMemberDao legionMemberDAO) {
        return new PlayerTransferService(playerDAO, legionMemberDAO);
    }

    @Bean
    @Lazy
    public PeriodicSaveService periodicSaveService(AbyssRankDao abyssRankDAO, PlayerSkillDao playerSkillListDAO,
                                                   PlayerQuestDao playerQuestListDAO, PlayerDao playerDAO,
                                                   PlayerStigmasEquippedDao playerStigmasEquippedDAO,
                                                   InventoryDao inventoryDAO, ItemStoneDao itemStoneListDAO) {
        return new PeriodicSaveService(abyssRankDAO, playerSkillListDAO, playerQuestListDAO,
                playerDAO, playerStigmasEquippedDAO, inventoryDAO, itemStoneListDAO);
    }

    @Bean
    @Lazy
    public TerritoryService territoryService(LegionDao dao) {
        return new TerritoryService(dao);
    }

    @Bean
    @Lazy
    public GameTimeService gameTimeService() {
        return new GameTimeService();
    }

    @Bean
    @Lazy
    public AnnouncementService announcementService(AnnouncementDao dao) {
        return new AnnouncementService(dao);
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
    public BrokerService brokerService(BrokerDao brokerDAO, InventoryDao inventoryDAO) {
        return new BrokerService(brokerDAO, inventoryDAO);
    }

    @Bean
    @Lazy
    public Influence influence() {
        return new Influence();
    }

    @Bean
    @Lazy
    public ExchangeService exchangeService(InventoryDao dao) {
        return new ExchangeService(dao);
    }

    @Bean
    @Lazy
    public PetitionService petitionService(PetitionDao dao) {
        return new PetitionService(dao);
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
     * 装配数据库任务管理器：注入任务 DAO，由管理器加载并调度库中的关机/重启任务。
     * Wires the DB task manager: injects the task DAO, which the manager uses to load and schedule the
     * shutdown/restart tasks stored in the database.
     */
    @Bean
    @Lazy
    public TaskManagerFromDB taskManagerFromDB(TaskDao taskDao) {
        return new TaskManagerFromDB(taskDao);
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
    public RewardService rewardService(WebRewardDao dao) {
        return new RewardService(dao);
    }

    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService(VeteranRewardDao veteranRewardsDao, PlayerDao playerDao) {
        return new VeteranRewardsService(veteranRewardsDao, playerDao);
    }

    @Bean
    @Lazy
    public HousingService housingService(HouseDao dao) {
        return new HousingService(dao);
    }

    @Bean
    @Lazy
    public SurveyService surveyService(SurveyDao dao) {
        return new SurveyService(dao);
    }

    @Bean
    @Lazy
    public FindGroupService findGroupService() {
        return new FindGroupService();
    }

    @Bean
    @Lazy
    public WebshopService webshopService(WebRewardDao dao) {
        return new WebshopService(dao);
    }

    @Bean
    @Lazy
    public InGameShopEn inGameShopEn(IngameshopDao inGameShopDAO) {
        return new InGameShopEn(inGameShopDAO);
    }

    @Bean
    @Lazy
    public LegionService legionService(LegionDao legionDAO, LegionMemberDao legionMemberDAO,
                                       InventoryDao inventoryDAO, ItemStoneDao itemStoneListDAO,
                                       PlayerDao playerDAO) {
        return new LegionService(legionDAO, legionMemberDAO, inventoryDAO, itemStoneListDAO, playerDAO);
    }

    /**
     * 装配玩家核心服务的持久层依赖聚合并在启动期绑定至 {@link PlayerService}；
     * 任一 DAO 缺失即失败（fail-fast），不再保留离线兜底聚合。
     * Wires the core player service persistence aggregate and binds it to {@link PlayerService}
     * at startup; a missing DAO fails fast instead of falling back to an offline aggregate.
     */
	@Bean
	public PlayerServiceDaos playerServiceDaos(ConfigurableBeanFactory beanFactory) {
        PlayerServiceDaos daos = new PlayerServiceDaos(
                daoFrom(beanFactory, PlayerDao.class),
                daoFrom(beanFactory, OldNameDao.class),
                daoFrom(beanFactory, PlayerAppearanceDao.class),
                daoFrom(beanFactory, PlayerSkillDao.class),
                daoFrom(beanFactory, InventoryDao.class),
                daoFrom(beanFactory, PlayerStigmasEquippedDao.class),
                daoFrom(beanFactory, PlayerEquipmentSettingDao.class),
                daoFrom(beanFactory, PlayerSettingDao.class),
                daoFrom(beanFactory, PlayerQuestDao.class),
                daoFrom(beanFactory, AbyssRankDao.class),
                daoFrom(beanFactory, PlayerPunishmentDao.class),
                daoFrom(beanFactory, HouseDao.class),
                daoFrom(beanFactory, PlayerRegisteredItemDao.class),
                daoFrom(beanFactory, ItemStoneDao.class),
                daoFrom(beanFactory, MailDao.class),
                daoFrom(beanFactory, PortalCooldownDao.class),
                daoFrom(beanFactory, CraftCooldownDao.class),
                daoFrom(beanFactory, PlayerNpcFactionDao.class),
                daoFrom(beanFactory, PlayerLunaShopDao.class),
                daoFrom(beanFactory, EventItemDao.class),
                daoFrom(beanFactory, PlayerMacrosseDao.class),
                daoFrom(beanFactory, FriendDao.class),
                daoFrom(beanFactory, BlockDao.class),
                daoFrom(beanFactory, PlayerTitleDao.class),
                daoFrom(beanFactory, PlayerCpDao.class),
                daoFrom(beanFactory, PlayerEventsWindowDao.class),
                daoFrom(beanFactory, PlayerAtreianBestiaryDao.class),
                daoFrom(beanFactory, PlayerWardrobeDao.class),
                daoFrom(beanFactory, F2paccountDao.class),
                daoFrom(beanFactory, PlayerMotionDao.class),
                daoFrom(beanFactory, PlayerVarDao.class),
                daoFrom(beanFactory, PlayerRecipeDao.class),
                daoFrom(beanFactory, PlayerSkillSkinDao.class),
                daoFrom(beanFactory, PlayerEffectDao.class),
                daoFrom(beanFactory, PlayerCooldownDao.class),
                daoFrom(beanFactory, ItemCooldownDao.class),
                daoFrom(beanFactory, HouseObjectCooldownDao.class),
                daoFrom(beanFactory, PlayerBindPointDao.class),
                daoFrom(beanFactory, PlayerLifeStatDao.class),
                daoFrom(beanFactory, PlayerEmotionDao.class),
                daoFrom(beanFactory, PlayerPetDao.class),
                daoFrom(beanFactory, PlayerMinionDao.class),
                daoFrom(beanFactory, PlayerShugoSweepDao.class),
                daoFrom(beanFactory, HouseScriptDao.class),
                daoFrom(beanFactory, PlayerPasskeyDao.class),
                daoFrom(beanFactory, PlayerTransformDao.class));
        PlayerService.bindDaos(daos);
        // 顺带绑定 item 域静态工具类的魔石列表 DAO / also bind the item-domain static utility's DAO
        ItemSocketService.bindItemStoneListDAO(daos.itemStoneListDAO());
        // 顺带绑定 player 域静态门面的二级密码 DAO（缺失时门面保持未绑定，不触发整体降级）
        // also bind the player-domain static facade's passkey DAO (when absent the facade stays
        // unbound, without triggering a full offline fallback)
        PlayerPasskeyDao playerPasskeyDAO = daoFrom(beanFactory, PlayerPasskeyDao.class);
        if (playerPasskeyDAO != null) {
            PlayerEnterWorldService.bindPlayerPasskeyDAO(playerPasskeyDAO);
        }
        // 顺带绑定 teleport 域静态门面的玩家变身 DAO（缺失时门面保持未绑定，不触发整体降级）
        // also bind the teleport-domain static facade's transform DAO (when absent the facade stays
        // unbound, without triggering a full offline fallback)
        PlayerTransformDao playerTransformDAO = daoFrom(beanFactory, PlayerTransformDao.class);
        if (playerTransformDAO != null) {
            com.aionemu.gameserver.services.teleport.TeleportService2.bindPlayerTransformDAO(playerTransformDAO);
        }
        return daos;
    }

    private static <T> T daoFrom(ConfigurableBeanFactory beanFactory, Class<T> daoClass) {
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
    public AbyssRankingCache abyssRankingCache(AbyssRankDao dao) {
        return new AbyssRankingCache(dao);
    }

    @Bean
    public MailService mailService(MailDao mailDAO, InventoryDao inventoryDAO, PlayerDao playerDAO,
                                   AbyssRankDao abyssRankDAO) {
        return new MailService(mailDAO, inventoryDAO, playerDAO, abyssRankDAO);
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
    public SeasonRankingService seasonRankingService(CompetitionRankingDao seasonRankingDao) {
        return new SeasonRankingService(seasonRankingDao);
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
    public PunishmentService punishmentService(PlayerPunishmentDao dao) {
        return new PunishmentService(dao);
    }

    @Bean
    @Lazy
    public RenameService renameService(OldNameDao oldNamesDAO, PlayerDao playerDAO, LegionDao legionDAO) {
        return new RenameService(oldNamesDAO, playerDAO, legionDAO);
    }

    @Bean
    @Lazy
    public HTMLService htmlService(GuideDao dao) {
        return new HTMLService(dao);
    }

    @Bean
    @Lazy
    public AccountService accountService(InventoryDao inventoryDAO, PlayerDao playerDAO,
                                         PlayerAppearanceDao playerAppearanceDAO,
                                         PlayerPunishmentDao playerPunishmentsDAO,
                                         LegionMemberDao legionMemberDAO) {
        return new AccountService(inventoryDAO, playerDAO, playerAppearanceDAO,
                playerPunishmentsDAO, legionMemberDAO);
    }

    @Bean
    @Lazy
    public SocialService socialService(BlockDao blockListDAO, FriendDao friendListDAO, PlayerDao playerDAO) {
        return new SocialService(blockListDAO, friendListDAO, playerDAO);
    }

    @Bean
    @Lazy
    public LimitedQuestService limitedQuestService(LimitedQuestCounterDao dao) {
        return new LimitedQuestService(dao);
    }

    @Bean
    @Lazy
    public ArmsfusionService armsfusionService(InventoryDao dao) {
        return new ArmsfusionService(dao);
    }
}
