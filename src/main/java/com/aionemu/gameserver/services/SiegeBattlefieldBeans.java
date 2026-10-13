package com.aionemu.gameserver.services;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.abysslandingservice.LandingUpdateService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import com.aionemu.gameserver.services.ranking.SeasonRankingUpdateService;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import java.text.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import com.aionemu.gameserver.persistence.dao.AbyssLandingDao;
import com.aionemu.gameserver.persistence.dao.SpecialLandingDao;
import com.aionemu.gameserver.persistence.dao.BaseLocationDao;
import com.aionemu.gameserver.persistence.dao.ChallengeTaskDao;
import com.aionemu.gameserver.persistence.dao.HouseBidDao;
import com.aionemu.gameserver.persistence.dao.LegionMemberDao;
import com.aionemu.gameserver.persistence.dao.OutpostLocationDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.CompetitionRankingDao;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.persistence.dao.SiegeLocationDao;
import com.aionemu.gameserver.persistence.dao.TownDao;

/**
 * SiegeBattlefieldBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * SiegeBattlefieldBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class SiegeBattlefieldBeans {

    @Bean
    @Lazy
    public HousingBidService housingBidService(HouseBidDao houseBidsDAO, ServerVariableDao serverVariablesDAO,
                                               PlayerDao playerDAO) throws ParseException {
        return new HousingBidService(houseBidsDAO, serverVariablesDAO, playerDAO);
    }

    @Bean
    @Lazy
    public MaintenanceTask maintenanceTask(ServerVariableDao serverVariablesDao) throws ParseException {
        return new MaintenanceTask(HousingConfig.HOUSE_MAINTENANCE_TIME, serverVariablesDao);
    }

    @Bean
    @Lazy
    public TownService townService(TownDao dao) {
        return new TownService(dao);
    }

    @Bean
    @Lazy
    public ChallengeTaskService challengeTaskService(ChallengeTaskDao challengeTasksDAO, TownDao townDAO,
                                                     LegionMemberDao legionMemberDAO, PlayerDao playerDAO) {
        return new ChallengeTaskService(challengeTasksDAO, townDAO, legionMemberDAO, playerDAO);
    }

    @Bean
    @Lazy
    public KamarBattlefieldService kamarBattlefieldService(AutoGroupService autoGroupService) {
        return new KamarBattlefieldService(autoGroupService);
    }

    @Bean
    @Lazy
    public EngulfedOphidanBridgeService engulfedOphidanBridgeService(AutoGroupService autoGroupService) {
        return new EngulfedOphidanBridgeService(autoGroupService);
    }

    @Bean
    @Lazy
    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService(AutoGroupService autoGroupService) {
        return new SuspiciousOphidanBridgeService(autoGroupService);
    }

    @Bean
    @Lazy
    public IronWallWarfrontService ironWallWarfrontService(AutoGroupService autoGroupService) {
        return new IronWallWarfrontService(autoGroupService);
    }

    @Bean
    @Lazy
    public IdgelDomeService idgelDomeService(AutoGroupService autoGroupService) {
        return new IdgelDomeService(autoGroupService);
    }

    @Bean
    @Lazy
    public IdgelDomeLandmarkService idgelDomeLandmarkService(AutoGroupService autoGroupService) {
        return new IdgelDomeLandmarkService(autoGroupService);
    }

    @Bean
    @Lazy
    public HallOfTenacityService hallOfTenacityService(AutoGroupService autoGroupService) {
        return new HallOfTenacityService(autoGroupService);
    }

    @Bean
    @Lazy
    public GrandArenaTrainingCampService grandArenaTrainingCampService(AutoGroupService autoGroupService) {
        return new GrandArenaTrainingCampService(autoGroupService);
    }

    @Bean
    @Lazy
    public IDRunService idRunService(AutoGroupService autoGroupService) {
        return new IDRunService(autoGroupService);
    }

    @Bean
    @Lazy
    public DisputeLandService disputeLandService() {
        return new DisputeLandService();
    }

    @Bean
    @Lazy
    public OutpostService outpostService(OutpostLocationDao outpostDao, SiegeLocationDao siegeDao) {
        return new OutpostService(outpostDao, siegeDao);
    }

    @Bean
    @Lazy
    public DredgionService2 dredgionService(AutoGroupService autoGroupService) {
        return new DredgionService2(autoGroupService);
    }

    @Bean
    @Lazy
    public AsyunatarService asyunatarService(AutoGroupService autoGroupService) {
        return new AsyunatarService(autoGroupService);
    }

    @Bean
    @Lazy
    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return new ShugoImperialTombSpawnManager();
    }

    @Bean
    @Lazy
    public SeasonRankingUpdateService seasonRankingUpdateService(CompetitionRankingDao dao) {
        return new SeasonRankingUpdateService(dao);
    }

    @Bean
    @Lazy
    public ProtectorConquerorService protectorConquerorService() {
        return new ProtectorConquerorService();
    }

    @Bean
    @Lazy
    public SiegeService siegeService(SiegeLocationDao dao, LegionService legionService) {
        return new SiegeService(dao, legionService);
    }

    @Bean
    @Lazy
    public BaseService baseService(BaseLocationDao dao) {
        return new BaseService(dao);
    }

    @Bean
    @Lazy
    public VortexService vortexService() {
        return new VortexService();
    }

    @Bean
    @Lazy
    public BeritraService beritraService() {
        return new BeritraService();
    }

    @Bean
    @Lazy
    public AgentService agentService() {
        return new AgentService();
    }

    @Bean
    @Lazy
    public AnohaService anohaService() {
        return new AnohaService();
    }

    @Bean
    @Lazy
    public SvsService svsService() {
        return new SvsService();
    }

    @Bean
    @Lazy
    public RvrService rvrService() {
        return new RvrService();
    }

    @Bean
    @Lazy
    public IuService iuService() {
        return new IuService();
    }

    @Bean
    @Lazy
    public NightmareCircusService nightmareCircusService() {
        return new NightmareCircusService();
    }

    @Bean
    @Lazy
    public DynamicRiftService dynamicRiftService() {
        return new DynamicRiftService();
    }

    @Bean
    @Lazy
    public InstanceRiftService instanceRiftService() {
        return new InstanceRiftService();
    }

    @Bean
    @Lazy
    public ZorshivDredgionService zorshivDredgionService() {
        return new ZorshivDredgionService();
    }

    @Bean
    @Lazy
    public MoltenusService moltenusService() {
        return new MoltenusService();
    }

    @Bean
    @Lazy
    public RiftService riftService() {
        return new RiftService();
    }

    @Bean
    @Lazy
    public ConquestService conquestService() {
        return new ConquestService();
    }

    @Bean
    @Lazy
    public IdianDepthsService idianDepthsService() {
        return new IdianDepthsService();
    }

    @Bean
    @Lazy
    public TowerOfEternityService towerOfEternityService() {
        return new TowerOfEternityService();
    }

    @Bean
    @Lazy
    public AbyssLandingService abyssLandingService(AbyssLandingDao dao,
                                                   AbyssLandingSpecialService abyssLandingSpecialService) {
        return new AbyssLandingService(dao, abyssLandingSpecialService);
    }

    @Bean
    @Lazy
    public LandingUpdateService landingUpdateService(AbyssLandingService abyssLandingService) {
        return new LandingUpdateService(abyssLandingService);
    }

    @Bean
    @Lazy
    public AbyssLandingSpecialService abyssLandingSpecialService(SpecialLandingDao dao) {
        return new AbyssLandingSpecialService(dao);
    }
}
