package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;

/**
 * 事件引导服务的回退工厂：在 Spring 提供者不可用时返回各事件子系统单例。
 * Fallback factory for event-bootstrap services: returns each event-subsystem singleton when Spring providers are unavailable.
 */
final class GameEventBootstrapFallbacks {

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private GameEventBootstrapFallbacks() {
    }

    /**
     * 返回露娜商店服务回退实例。
     * Return the Luna-shop service fallback instance.
     *
     * @return 露娜商店服务 / Luna-shop service
     */
    static LunaShopService lunaShopService() {
        return LunaShopServiceFallback.INSTANCE;
    }

    /**
     * 返回宠物役从服务回退实例。
     * Return the minion service fallback instance.
     *
     * @return 宠物役从服务 / Minion service
     */
    static MinionService minionService() {
        return MinionServiceFallback.INSTANCE;
    }

    /**
     * 返回修勾扫荡服务回退实例。
     * Return the Shugo-sweep service fallback instance.
     *
     * @return 修勾扫荡服务 / Shugo-sweep service
     */
    static ShugoSweepService shugoSweepService() {
        return ShugoSweepServiceFallback.INSTANCE;
    }

    /**
     * 返回阿特里亚护照服务回退实例。
     * Return the Atreian-passport service fallback instance.
     *
     * @return 阿特里亚护照服务 / Atreian-passport service
     */
    static AtreianPassportService atreianPassportService() {
        return AtreianPassportServiceFallback.INSTANCE;
    }

    /**
     * 返回事件窗口服务回退实例。
     * Return the event-window service fallback instance.
     *
     * @return 事件窗口服务 / Event-window service
     */
    static EventWindowService eventWindowService() {
        return EventWindowServiceFallback.INSTANCE;
    }

    /**
     * 露娜商店服务懒加载回退持有者。
     * Lazy fallback holder for the Luna-shop service.
     */
    private static final class LunaShopServiceFallback {
        private static final LunaShopService INSTANCE = LunaShopService.getInstance();
    }

    /**
     * 宠物役从服务懒加载回退持有者。
     * Lazy fallback holder for the minion service.
     */
    private static final class MinionServiceFallback {
        private static final MinionService INSTANCE = MinionService.getInstance();
    }

    /**
     * 修勾扫荡服务懒加载回退持有者。
     * Lazy fallback holder for the Shugo-sweep service.
     */
    private static final class ShugoSweepServiceFallback {
        private static final ShugoSweepService INSTANCE = ShugoSweepService.getInstance();
    }

    /**
     * 阿特里亚护照服务懒加载回退持有者。
     * Lazy fallback holder for the Atreian-passport service.
     */
    private static final class AtreianPassportServiceFallback {
        private static final AtreianPassportService INSTANCE = AtreianPassportService.getInstance();
    }

    /**
     * 事件窗口服务懒加载回退持有者。
     * Lazy fallback holder for the event-window service.
     */
    private static final class EventWindowServiceFallback {
        private static final EventWindowService INSTANCE = EventWindowService.getInstance();
    }
}
