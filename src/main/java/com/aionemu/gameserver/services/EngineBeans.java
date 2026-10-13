package com.aionemu.gameserver.services;

import com.aionemu.gameserver.services.drop.DropService;
import java.util.List;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
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
import com.aionemu.gameserver.persistence.dao.GuideDao;
import com.aionemu.gameserver.persistence.dao.HouseDao;
import com.aionemu.gameserver.persistence.dao.InventoryDao;
import com.aionemu.gameserver.persistence.dao.LegionDao;
import com.aionemu.gameserver.persistence.dao.MailDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.PlayerMinionDao;
import com.aionemu.gameserver.persistence.dao.PlayerRegisteredItemDao;
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

    /**
     * 技能引擎 Bean：由容器直接构造唯一实例。
     * Skill-engine bean: the container constructs the single instance directly.
     * <p>不得改写成 {@code SkillEngine.getInstance()}：该方法会回头向本容器索取
     * {@code SkillEngine}，构成 Bean 创建期循环引用。</p>
     * <p>Must not call {@code SkillEngine.getInstance()}: it would ask this very container for
     * {@code SkillEngine} and create a creation-time cycle.</p>
     * @return 技能引擎 / skill engine
     */
    @Bean
    @Lazy
    public SkillEngine skillEngine() {
        return new SkillEngine();
    }

    @Bean
    @Lazy
    public ChatProcessor chatProcessor() {
        return new ChatProcessor();
    }

    /**
     * 装配 ID 工厂：由容器解析各持久层的已用 ID 来源。
     * Wires the ID factory: resolves each persistence layer's used-ID source from the container.
     */
    @Bean
    @Lazy
    public IDFactory gameIdFactory(PlayerDao playerDao, InventoryDao inventoryDao,
            PlayerRegisteredItemDao playerRegisteredItemsDao, LegionDao legionDao, MailDao mailDao,
            GuideDao guideDao, HouseDao housesDao, PlayerMinionDao playerMinionsDao) {
        return new IDFactory(List.of(
            playerDao::getUsedIDs,
            inventoryDao::getUsedIDs,
            playerRegisteredItemsDao::getUsedIDs,
            legionDao::getUsedIDs,
            mailDao::getUsedIDs,
            guideDao::getUsedIDs,
            housesDao::getUsedIDs,
            playerMinionsDao::getUsedIDs));
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
    public DropRegistrationService dropRegistrationService(DropService dropService) {
        return new DropRegistrationService(dropService);
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
    public DropDistributionService dropDistributionService(DropService dropService) {
        return new DropDistributionService(dropService);
    }

    @Bean
    @Lazy
    public SystemMailService systemMailService(PlayerDao playerDao, InventoryDao inventoryDao, MailDao mailDao) {
        return new SystemMailService(playerDao, inventoryDao, mailDao);
    }
}
