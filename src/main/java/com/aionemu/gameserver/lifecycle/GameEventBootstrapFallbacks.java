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
     * @return 露娜商店服务 / Luna-shop service
     */
    static LunaShopService lunaShopService() {
        return LunaShopService.getInstance();
    }

    /**
     * 返回宠物役从服务回退实例。
     * Return the minion service fallback instance.
     * @return 宠物役从服务 / Minion service
     */
    static MinionService minionService() {
        return MinionService.getInstance();
    }

    /**
     * 返回修勾扫荡服务回退实例。
     * Return the Shugo-sweep service fallback instance.
     * @return 修勾扫荡服务 / Shugo-sweep service
     */
    static ShugoSweepService shugoSweepService() {
        return ShugoSweepService.getInstance();
    }

    /**
     * 返回阿特里亚护照服务回退实例。
     * Return the Atreian-passport service fallback instance.
     * @return 阿特里亚护照服务 / Atreian-passport service
     */
    static AtreianPassportService atreianPassportService() {
        return AtreianPassportService.getInstance();
    }

    /**
     * 返回事件窗口服务回退实例。
     * Return the event-window service fallback instance.
     * @return 事件窗口服务 / Event-window service
     */
    static EventWindowService eventWindowService() {
        return EventWindowService.getInstance();
    }
}
