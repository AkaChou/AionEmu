package com.aionemu.gameserver.services;

import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.persistence.dao.AbyssRankDao;
import com.aionemu.gameserver.persistence.dao.LadderPlayerDao;
import com.aionemu.gameserver.persistence.dao.PlayerMinionDao;
import com.aionemu.gameserver.persistence.dao.PlayerDao;
import com.aionemu.gameserver.persistence.dao.ServerVariableDao;
import com.aionemu.gameserver.persistence.dao.PlayerLunaShopDao;
import com.aionemu.gameserver.persistence.dao.PlayerPassportDao;
import com.aionemu.gameserver.persistence.dao.PlayerShugoSweepDao;
import com.aionemu.gameserver.persistence.dao.PlayerWardrobeDao;
import com.aionemu.gameserver.persistence.dao.SkillMotionDao;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * EventBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * EventBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class EventBeans {

    @Bean
    @Lazy
    public EventService eventService() {
        return new EventService();
    }

    @Bean
    @Lazy
    public PlayerEventService playerEventService() {
        return new PlayerEventService();
    }

    @Bean
    @Lazy
    public CrazyDaevaService crazyDaevaService() {
        return new CrazyDaevaService();
    }

    @Bean
    @Lazy
    public AbyssRankUpdateService abyssRankUpdateService(AbyssRankDao abyssRankDao,
            ServerVariableDao serverVariablesDao, AbyssRankingCache abyssRankingCache) {
        return new AbyssRankUpdateService(abyssRankDao, serverVariablesDao, abyssRankingCache);
    }

    @Bean
    @Lazy
    public PacketBroadcaster packetBroadcaster() {
        return new PacketBroadcaster();
    }

    @Bean
    @Lazy
    public EventScheduler eventScheduler() {
        return new EventScheduler();
    }

    @Bean
    @Lazy
    public FFAService ffaService() {
        return new FFAService();
    }

    @Bean
    @Lazy
    public LadderService ladderService(LadderPlayerDao dao) {
        return new LadderService(dao);
    }

    @Bean
    @Lazy
    public BGService bgService() {
        return new BGService();
    }

    @Bean
    @Lazy
    public BanditService banditService() {
        return new BanditService();
    }

    @Bean
    @Lazy
    public PlayerLimitService playerLimitService() {
        return new PlayerLimitService();
    }

    @Bean
    @Lazy
    public NpcShoutsService npcShoutsService() {
        return new NpcShoutsService();
    }

    @Bean
    @Lazy
    public ShieldService shieldService() {
        return new ShieldService();
    }

    @Bean
    @Lazy
    public LunaShopService lunaShopService(PlayerLunaShopDao lunaShopDao, PlayerWardrobeDao wardrobeDao) {
        return new LunaShopService(lunaShopDao, wardrobeDao);
    }

    @Bean
    @Lazy
    public MinionService minionService(PlayerMinionDao minionsDao, PlayerDao playerDao) {
        return new MinionService(minionsDao, playerDao);
    }

    @Bean
    @Lazy
    public ShugoSweepService shugoSweepService(PlayerShugoSweepDao shugoSweepDao, PlayerDao playerDao) {
        return new ShugoSweepService(shugoSweepDao, playerDao);
    }

    @Bean
    @Lazy
    public AtreianPassportService atreianPassportService(PlayerPassportDao dao) {
        return new AtreianPassportService(dao);
    }

    @Bean
    @Lazy
    public WindyGorgeService windyGorgeService() {
        return new WindyGorgeService();
    }

    @Bean
    @Lazy
    public AStationService aStationService() {
        return new AStationService();
    }

    @Bean
    @Lazy
    public F2pService f2pService() {
        return new F2pService();
    }

    @Bean
    @Lazy
    public MotionLoggingService motionLoggingService(SkillMotionDao skillMotionDao) {
        MotionLoggingService service = new MotionLoggingService();
        service.setSkillMotionDao(skillMotionDao);
        return service;
    }
}
