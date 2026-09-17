package com.aionemu.gameserver.services;

import com.aionemu.gameserver.configs.main.HousingConfig;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ProtectorConquerorService;
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

/**
 * SiegeBattlefieldBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * SiegeBattlefieldBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class SiegeBattlefieldBeans {

    @Bean
    @Lazy
    public HousingBidService housingBidService() throws ParseException {
        return new HousingBidService();
    }

    @Bean
    @Lazy
    public MaintenanceTask maintenanceTask() throws ParseException {
        return new MaintenanceTask(HousingConfig.HOUSE_MAINTENANCE_TIME);
    }

    @Bean
    @Lazy
    public TownService townService() {
        return new TownService();
    }

    @Bean
    @Lazy
    public ChallengeTaskService challengeTaskService() {
        return new ChallengeTaskService();
    }

    @Bean
    @Lazy
    public KamarBattlefieldService kamarBattlefieldService() {
        return new KamarBattlefieldService();
    }

    @Bean
    @Lazy
    public EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return new EngulfedOphidanBridgeService();
    }

    @Bean
    @Lazy
    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return new SuspiciousOphidanBridgeService();
    }

    @Bean
    @Lazy
    public IronWallWarfrontService ironWallWarfrontService() {
        return new IronWallWarfrontService();
    }

    @Bean
    @Lazy
    public IdgelDomeService idgelDomeService() {
        return new IdgelDomeService();
    }

    @Bean
    @Lazy
    public IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return new IdgelDomeLandmarkService();
    }

    @Bean
    @Lazy
    public HallOfTenacityService hallOfTenacityService() {
        return new HallOfTenacityService();
    }

    @Bean
    @Lazy
    public GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return new GrandArenaTrainingCampService();
    }

    @Bean
    @Lazy
    public IDRunService idRunService() {
        return new IDRunService();
    }

    @Bean
    @Lazy
    public DisputeLandService disputeLandService() {
        return new DisputeLandService();
    }

    @Bean
    @Lazy
    public OutpostService outpostService() {
        return new OutpostService();
    }

    @Bean
    @Lazy
    public DredgionService2 dredgionService() {
        return new DredgionService2();
    }

    @Bean
    @Lazy
    public AsyunatarService asyunatarService() {
        return new AsyunatarService();
    }

    @Bean
    @Lazy
    public ShugoImperialTombSpawnManager shugoImperialTombSpawnManager() {
        return new ShugoImperialTombSpawnManager();
    }

    @Bean
    @Lazy
    public SeasonRankingUpdateService seasonRankingUpdateService() {
        return new SeasonRankingUpdateService();
    }

    @Bean
    @Lazy
    public ProtectorConquerorService protectorConquerorService() {
        return new ProtectorConquerorService();
    }

    @Bean
    @Lazy
    public SiegeService siegeService() {
        return new SiegeService();
    }

    @Bean
    @Lazy
    public BaseService baseService() {
        return new BaseService();
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
    public AbyssLandingService abyssLandingService() {
        return new AbyssLandingService();
    }

    @Bean
    @Lazy
    public LandingUpdateService landingUpdateService() {
        return new LandingUpdateService();
    }

    @Bean
    @Lazy
    public AbyssLandingSpecialService abyssLandingSpecialService() {
        return new AbyssLandingSpecialService();
    }
}
