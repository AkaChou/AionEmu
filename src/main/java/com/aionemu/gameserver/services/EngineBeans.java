package com.aionemu.gameserver.services;

import java.util.List;
import java.util.function.Function;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dao.GuideDAO;
import com.aionemu.gameserver.dao.HousesDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.LegionDAO;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.dao.PlayerRegisteredItemsDAO;
import com.aionemu.gameserver.dao.MailDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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

    /**
     * 装配 ID 工厂：由容器解析各 IDFactoryAwareDAO 的已用 ID 来源，缺失时保持空来源。
     * Wires the ID factory: resolves each IDFactoryAwareDAO used-ID source from the container,
     * keeping an empty source list when absent.
     */
    @Bean
    @Lazy
    public IDFactory gameIdFactory(ConfigurableBeanFactory beanFactory) {
        return new IDFactory(List.of(
            () -> usedIds(beanFactory, PlayerDAO.class, PlayerDAO::getUsedIDs),
            () -> usedIds(beanFactory, InventoryDAO.class, InventoryDAO::getUsedIDs),
            () -> usedIds(beanFactory, PlayerRegisteredItemsDAO.class, PlayerRegisteredItemsDAO::getUsedIDs),
            () -> usedIds(beanFactory, LegionDAO.class, LegionDAO::getUsedIDs),
            () -> usedIds(beanFactory, MailDAO.class, MailDAO::getUsedIDs),
            () -> usedIds(beanFactory, GuideDAO.class, GuideDAO::getUsedIDs),
            () -> usedIds(beanFactory, HousesDAO.class, HousesDAO::getUsedIDs),
            () -> usedIds(beanFactory, PlayerMinionsDAO.class, PlayerMinionsDAO::getUsedIDs)));
    }

    /**
     * 解析单个 DAO 的已用 ID 集合，DAO 缺失（离线/单测上下文）时返回空集合。
     * Resolves one DAO's used-ID set; returns an empty set when the DAO is absent (offline/unit tests).
     */
    private static <T extends com.aionemu.commons.database.dao.DAO> int[] usedIds(
            ConfigurableBeanFactory beanFactory, Class<T> daoClass, Function<T, int[]> usedIds) {
        T dao = beanFactory.getBeanProvider(daoClass).getIfAvailable();
        return dao != null ? usedIds.apply(dao) : new int[0];
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
    public SystemMailService systemMailService(ObjectProvider<PlayerDAO> playerDaoProvider,
            ObjectProvider<InventoryDAO> inventoryDaoProvider, ObjectProvider<MailDAO> mailDaoProvider) {
        PlayerDAO playerDao = playerDaoProvider.getIfAvailable();
        InventoryDAO inventoryDao = inventoryDaoProvider.getIfAvailable();
        MailDAO mailDao = mailDaoProvider.getIfAvailable();
        return playerDao != null && inventoryDao != null && mailDao != null
                ? new SystemMailService(playerDao, inventoryDao, mailDao)
                : new SystemMailService();
    }
}
