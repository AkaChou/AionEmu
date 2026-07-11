package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 事件引导 Spring 服务门面 / 静态访问桥：注册事件子系统实例提供者。
 * static access bridge for event bootstrap: registers event-subsystem instance providers. / static access bridge for event bootstrap: registers event-subsystem instance providers.
 */
@Component
public final class GameEventBootstrapServices implements DisposableBean {

    /**
     * 露娜商店服务的 Spring 提供者。
     * Spring provider for the Luna-shop service.
     */
    private static volatile ObjectProvider<LunaShopService> lunaShopServiceProvider;
    /**
     * 宠物役从服务的 Spring 提供者。
     * Spring provider for the minion service.
     */
    private static volatile ObjectProvider<MinionService> minionServiceProvider;
    /**
     * 修勾扫荡服务的 Spring 提供者。
     * Spring provider for the Shugo-sweep service.
     */
    private static volatile ObjectProvider<ShugoSweepService> shugoSweepServiceProvider;
    /**
     * 阿特里亚护照服务的 Spring 提供者。
     * Spring provider for the Atreian-passport service.
     */
    private static volatile ObjectProvider<AtreianPassportService> atreianPassportServiceProvider;
    /**
     * 事件窗口服务的 Spring 提供者。
     * Spring provider for the event-window service.
     */
    private static volatile ObjectProvider<EventWindowService> eventWindowServiceProvider;

    /**
     * 构造并注册各事件子系统实例提供者。
     * Construct and register instance providers for each event subsystem.
     *
     * @param lunaShopServiceProvider 露娜商店服务提供者 / Luna-shop service provider
     * @param minionServiceProvider 宠物役从服务提供者 / Minion service provider
     * @param shugoSweepServiceProvider 修勾扫荡服务提供者 / Shugo-sweep service provider
     * @param atreianPassportServiceProvider 阿特里亚护照服务提供者 / Atreian-passport service provider
     * @param eventWindowServiceProvider 事件窗口服务提供者 / Event-window service provider
     */
    public GameEventBootstrapServices(ObjectProvider<LunaShopService> lunaShopServiceProvider,
            ObjectProvider<MinionService> minionServiceProvider,
            ObjectProvider<ShugoSweepService> shugoSweepServiceProvider,
            ObjectProvider<AtreianPassportService> atreianPassportServiceProvider,
            ObjectProvider<EventWindowService> eventWindowServiceProvider) {
        GameEventBootstrapServices.lunaShopServiceProvider = lunaShopServiceProvider;
        GameEventBootstrapServices.minionServiceProvider = minionServiceProvider;
        GameEventBootstrapServices.shugoSweepServiceProvider = shugoSweepServiceProvider;
        GameEventBootstrapServices.atreianPassportServiceProvider = atreianPassportServiceProvider;
        GameEventBootstrapServices.eventWindowServiceProvider = eventWindowServiceProvider;
        LunaShopService.setInstanceProvider(lunaShopServiceProvider);
        MinionService.setInstanceProvider(minionServiceProvider);
        ShugoSweepService.setInstanceProvider(shugoSweepServiceProvider);
        AtreianPassportService.setInstanceProvider(atreianPassportServiceProvider);
        EventWindowService.setInstanceProvider(eventWindowServiceProvider);
    }

    /**
     * 解析露娜商店服务。
     * Resolve the Luna-shop service.
     *
     * @return 露娜商店服务 / Luna-shop service
     */
    public static LunaShopService lunaShopService() {
        ObjectProvider<LunaShopService> provider = lunaShopServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.lunaShopService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::lunaShopService);
    }

    /**
     * 解析宠物役从服务。
     * Resolve the minion service.
     *
     * @return 宠物役从服务 / Minion service
     */
    public static MinionService minionService() {
        ObjectProvider<MinionService> provider = minionServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.minionService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::minionService);
    }

    /**
     * 解析修勾扫荡服务。
     * Resolve the Shugo-sweep service.
     *
     * @return 修勾扫荡服务 / Shugo-sweep service
     */
    public static ShugoSweepService shugoSweepService() {
        ObjectProvider<ShugoSweepService> provider = shugoSweepServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.shugoSweepService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::shugoSweepService);
    }

    /**
     * 解析阿特里亚护照服务。
     * Resolve the Atreian-passport service.
     *
     * @return 阿特里亚护照服务 / Atreian-passport service
     */
    public static AtreianPassportService atreianPassportService() {
        ObjectProvider<AtreianPassportService> provider = atreianPassportServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.atreianPassportService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::atreianPassportService);
    }

    /**
     * 解析事件窗口服务。
     * Resolve the event-window service.
     *
     * @return 事件窗口服务 / Event-window service
     */
    public static EventWindowService eventWindowService() {
        ObjectProvider<EventWindowService> provider = eventWindowServiceProvider;
        if (provider == null) {
            return GameEventBootstrapFallbacks.eventWindowService();
        }
        return provider.getIfAvailable(GameEventBootstrapFallbacks::eventWindowService);
    }

    /**
     * 销毁时清理静态提供者与服务实例桥。
     * Clear static providers and service instance bridges on destroy.
     */
    @Override
    public void destroy() {
        lunaShopServiceProvider = null;
        minionServiceProvider = null;
        shugoSweepServiceProvider = null;
        atreianPassportServiceProvider = null;
        eventWindowServiceProvider = null;
        LunaShopService.setInstanceProvider(null);
        MinionService.setInstanceProvider(null);
        ShugoSweepService.setInstanceProvider(null);
        AtreianPassportService.setInstanceProvider(null);
        EventWindowService.setInstanceProvider(null);
    }
}
