package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 事件引导运行时桥接：在 Spring 提供者与回退工厂之间解析事件子系统。
 * Event-bootstrap runtime bridge: resolves event subsystems between Spring providers and fallback factories.
 */
@Component
public class GameEventBootstrapRuntimeBridge {

    /**
     * 露娜商店服务提供者。
     * Luna-shop service provider.
     */
    private ObjectProvider<LunaShopService> lunaShopServiceProvider;
    /**
     * 宠物役从服务提供者。
     * Minion service provider.
     */
    private ObjectProvider<MinionService> minionServiceProvider;
    /**
     * 修勾扫荡服务提供者。
     * Shugo-sweep service provider.
     */
    private ObjectProvider<ShugoSweepService> shugoSweepServiceProvider;
    /**
     * 阿特里亚护照服务提供者。
     * Atreian-passport service provider.
     */
    private ObjectProvider<AtreianPassportService> atreianPassportServiceProvider;
    /**
     * 事件窗口服务提供者。
     * Event-window service provider.
     */
    private ObjectProvider<EventWindowService> eventWindowServiceProvider;

    /**
     * 可选注入露娜商店服务提供者。
     * Optionally inject the Luna-shop service provider.
     *
     * @param lunaShopServiceProvider 露娜商店服务提供者 / Luna-shop service provider
     */
    @Autowired(required = false)
    void setLunaShopServiceProvider(ObjectProvider<LunaShopService> lunaShopServiceProvider) {
        this.lunaShopServiceProvider = lunaShopServiceProvider;
    }

    /**
     * 可选注入宠物役从服务提供者。
     * Optionally inject the minion service provider.
     *
     * @param minionServiceProvider 宠物役从服务提供者 / Minion service provider
     */
    @Autowired(required = false)
    void setMinionServiceProvider(ObjectProvider<MinionService> minionServiceProvider) {
        this.minionServiceProvider = minionServiceProvider;
    }

    /**
     * 可选注入修勾扫荡服务提供者。
     * Optionally inject the Shugo-sweep service provider.
     *
     * @param shugoSweepServiceProvider 修勾扫荡服务提供者 / Shugo-sweep service provider
     */
    @Autowired(required = false)
    void setShugoSweepServiceProvider(ObjectProvider<ShugoSweepService> shugoSweepServiceProvider) {
        this.shugoSweepServiceProvider = shugoSweepServiceProvider;
    }

    /**
     * 可选注入阿特里亚护照服务提供者。
     * Optionally inject the Atreian-passport service provider.
     *
     * @param atreianPassportServiceProvider 阿特里亚护照服务提供者 / Atreian-passport service provider
     */
    @Autowired(required = false)
    void setAtreianPassportServiceProvider(ObjectProvider<AtreianPassportService> atreianPassportServiceProvider) {
        this.atreianPassportServiceProvider = atreianPassportServiceProvider;
    }

    /**
     * 可选注入事件窗口服务提供者。
     * Optionally inject the event-window service provider.
     *
     * @param eventWindowServiceProvider 事件窗口服务提供者 / Event-window service provider
     */
    @Autowired(required = false)
    void setEventWindowServiceProvider(ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        this.eventWindowServiceProvider = eventWindowServiceProvider;
    }

    /**
     * 解析露娜商店服务。
     * Resolve the Luna-shop service.
     *
     * @return 露娜商店服务 / Luna-shop service
     */
    public LunaShopService lunaShopService() {
        if (lunaShopServiceProvider == null) {
            return GameEventBootstrapFallbacks.lunaShopService();
        }
        return lunaShopServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::lunaShopService);
    }

    /**
     * 解析宠物役从服务。
     * Resolve the minion service.
     *
     * @return 宠物役从服务 / Minion service
     */
    public MinionService minionService() {
        if (minionServiceProvider == null) {
            return GameEventBootstrapFallbacks.minionService();
        }
        return minionServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::minionService);
    }

    /**
     * 解析修勾扫荡服务。
     * Resolve the Shugo-sweep service.
     *
     * @return 修勾扫荡服务 / Shugo-sweep service
     */
    public ShugoSweepService shugoSweepService() {
        if (shugoSweepServiceProvider == null) {
            return GameEventBootstrapFallbacks.shugoSweepService();
        }
        return shugoSweepServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::shugoSweepService);
    }

    /**
     * 解析阿特里亚护照服务。
     * Resolve the Atreian-passport service.
     *
     * @return 阿特里亚护照服务 / Atreian-passport service
     */
    public AtreianPassportService atreianPassportService() {
        if (atreianPassportServiceProvider == null) {
            return GameEventBootstrapFallbacks.atreianPassportService();
        }
        return atreianPassportServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::atreianPassportService);
    }

    /**
     * 解析事件窗口服务。
     * Resolve the event-window service.
     *
     * @return 事件窗口服务 / Event-window service
     */
    public EventWindowService eventWindowService() {
        if (eventWindowServiceProvider == null) {
            return GameEventBootstrapFallbacks.eventWindowService();
        }
        return eventWindowServiceProvider.getIfAvailable(GameEventBootstrapFallbacks::eventWindowService);
    }
}
