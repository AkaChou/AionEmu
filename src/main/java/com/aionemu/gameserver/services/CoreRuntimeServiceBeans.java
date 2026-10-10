package com.aionemu.gameserver.services;

import com.aionemu.gameserver.lifecycle.GameRuntimeServiceBridge;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.siege.Influence;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.events.BoostEventService;
import com.aionemu.gameserver.services.events.ThievesGuildService;
import com.aionemu.gameserver.services.mail.MailService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.AnnouncementsDAO;
import com.aionemu.gameserver.dao.BlockListDAO;
import com.aionemu.gameserver.dao.BrokerDAO;
import com.aionemu.gameserver.dao.FriendListDAO;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.LegionMemberDAO;
import com.aionemu.gameserver.dao.LimitedQuestDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.OldNamesDAO;
import com.aionemu.gameserver.dao.PetitionDAO;
import com.aionemu.gameserver.dao.PlayerAppearanceDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.PlayerPunishmentsDAO;
import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.dao.PlayerSkillListDAO;
import com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO;
import com.aionemu.gameserver.dao.RewardServiceDAO;
import com.aionemu.gameserver.dao.SurveyControllerDAO;

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
    public PlayerTransferService gamePlayerTransferService() {
        return new PlayerTransferService();
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
    public TerritoryService territoryService() {
        return new TerritoryService();
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

    @Bean
    @Lazy
    public TaskManagerFromDB taskManagerFromDB() {
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
    public RewardService rewardService() {
        return new RewardService();
    }

    @Bean
    @Lazy
    public VeteranRewardsService veteranRewardsService() {
        return new VeteranRewardsService();
    }

    @Bean
    @Lazy
    public HousingService housingService() {
        return new HousingService();
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
    public InGameShopEn inGameShopEn() {
        return new InGameShopEn();
    }

    @Bean
    @Lazy
    public LegionService legionService() {
        return new LegionService();
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
    public ThievesGuildService thievesGuildService() {
        return new ThievesGuildService();
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
    public AbyssRankingCache abyssRankingCache() {
        return new AbyssRankingCache();
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
    public SeasonRankingService seasonRankingService() {
        return new SeasonRankingService();
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
}
