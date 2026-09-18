package com.aionemu.gameserver.services;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.drop.DropDistributionService;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.services.mail.SystemMailService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;
import com.aionemu.gameserver.world.zone.ZoneService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * EngineBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * EngineBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class EngineBeans {

    /**
     * 线程池管理器桥接 Bean，注入线程配置而不是读取静态门面。
     * Thread-pool manager bridge bean, wired with the thread configuration instead of the static facade.
     *
     * @param threadConfig 线程配置 / thread configuration
     * @return 线程池管理器 / thread-pool manager
     */
    @Bean
    @Lazy
    public ThreadPoolManager threadPoolManager(ThreadConfig threadConfig) {
        return new ThreadPoolManager(threadConfig);
    }

    @Bean
    @Lazy
    public QuestEngine questEngine() {
        return new QuestEngine();
    }

    @Bean
    @Lazy
    public InstanceEngine instanceEngine() {
        return new InstanceEngine();
    }

    @Bean
    @Lazy
    public AI2Engine ai2Engine() {
        return new AI2Engine();
    }

    @Bean
    @Lazy
    public ChatProcessor chatProcessor() {
        return new ChatProcessor();
    }

    @Bean
    @Lazy
    public IDFactory gameIdFactory() {
        return new IDFactory();
    }

    @Bean
    @Lazy
    public DataManager dataManager() {
        return new DataManager();
    }

    @Bean
    @Lazy
    public HTMLCache htmlCache() {
        return new HTMLCache();
    }

    @Bean
    @Lazy
    public XmlDataLoader xmlDataLoader() {
        return new XmlDataLoader();
    }

    @Bean
    @Lazy
    public World world() {
        return new World();
    }

    @Bean
    @Lazy
    public ZoneService zoneService() {
        return new ZoneService();
    }

    @Bean
    @Lazy
    public HotspotTeleportService hotspotTeleportService() {
        return new HotspotTeleportService();
    }

    @Bean
    @Lazy
    public RoadService roadService() {
        return new RoadService();
    }

    @Bean
    @Lazy
    public DropRegistrationService dropRegistrationService() {
        return new DropRegistrationService();
    }

    @Bean
    @Lazy
    public GeoService geoService() {
        return new GeoService();
    }

    @Bean
    @Lazy
    public PathService pathService() {
        return new PathService();
    }

    @Bean
    @Lazy
    public StaticDoorService staticDoorService() {
        return new StaticDoorService();
    }

    @Bean
    @Lazy
    public KiskService kiskService() {
        return new KiskService();
    }

    @Bean
    @Lazy
    public RepurchaseService repurchaseService() {
        return new RepurchaseService();
    }

    @Bean
    @Lazy
    public DropDistributionService dropDistributionService() {
        return new DropDistributionService();
    }

    @Bean
    @Lazy
    public SystemMailService systemMailService() {
        return new SystemMailService();
    }
}
