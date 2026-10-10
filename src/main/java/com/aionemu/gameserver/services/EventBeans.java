package com.aionemu.gameserver.services;

import com.aionemu.gameserver.eventEngine.EventScheduler;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.dao.ServerVariablesDAO;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.dao.PlayerPassportsDAO;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.CrazyDaevaService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.player.PlayerEventService;
import com.aionemu.gameserver.services.player.PlayerLimitService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster;
import org.springframework.beans.factory.ObjectProvider;
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
    public AbyssRankUpdateService abyssRankUpdateService(ObjectProvider<AbyssRankDAO> abyssRankDaoProvider,
            ObjectProvider<ServerVariablesDAO> serverVariablesDaoProvider) {
        AbyssRankDAO abyssRankDao = abyssRankDaoProvider.getIfAvailable();
        ServerVariablesDAO serverVariablesDao = serverVariablesDaoProvider.getIfAvailable();
        return abyssRankDao != null && serverVariablesDao != null
                ? new AbyssRankUpdateService(abyssRankDao, serverVariablesDao)
                : new AbyssRankUpdateService();
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
    public LadderService ladderService() {
        return new LadderService();
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
    public LunaShopService lunaShopService(ObjectProvider<PlayerLunaShopDAO> lunaShopDaoProvider,
            ObjectProvider<PlayerWardrobeDAO> wardrobeDaoProvider) {
        PlayerLunaShopDAO lunaShopDao = lunaShopDaoProvider.getIfAvailable();
        PlayerWardrobeDAO wardrobeDao = wardrobeDaoProvider.getIfAvailable();
        return lunaShopDao != null && wardrobeDao != null ? new LunaShopService(lunaShopDao, wardrobeDao)
                : new LunaShopService();
    }

    @Bean
    @Lazy
    public MinionService minionService(ObjectProvider<PlayerMinionsDAO> minionsDaoProvider,
            ObjectProvider<PlayerDAO> playerDaoProvider) {
        PlayerMinionsDAO minionsDao = minionsDaoProvider.getIfAvailable();
        PlayerDAO playerDao = playerDaoProvider.getIfAvailable();
        return minionsDao != null && playerDao != null ? new MinionService(minionsDao, playerDao)
                : new MinionService();
    }

    @Bean
    @Lazy
    public ShugoSweepService shugoSweepService(ObjectProvider<PlayerShugoSweepDAO> shugoSweepDaoProvider,
            ObjectProvider<PlayerDAO> playerDaoProvider) {
        PlayerShugoSweepDAO shugoSweepDao = shugoSweepDaoProvider.getIfAvailable();
        PlayerDAO playerDao = playerDaoProvider.getIfAvailable();
        return shugoSweepDao != null && playerDao != null ? new ShugoSweepService(shugoSweepDao, playerDao)
                : new ShugoSweepService();
    }

    @Bean
    @Lazy
    public AtreianPassportService atreianPassportService(ObjectProvider<PlayerPassportsDAO> passportsDaoProvider) {
        PlayerPassportsDAO dao = passportsDaoProvider.getIfAvailable();
        return dao != null ? new AtreianPassportService(dao) : new AtreianPassportService();
    }

    @Bean
    @Lazy
    public EventWindowService eventWindowService(ObjectProvider<PlayerEventsWindowDAO> eventsWindowDaoProvider) {
        PlayerEventsWindowDAO dao = eventsWindowDaoProvider.getIfAvailable();
        return dao != null ? new EventWindowService(dao) : new EventWindowService();
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
    public MotionLoggingService motionLoggingService() {
        return new MotionLoggingService();
    }
}
