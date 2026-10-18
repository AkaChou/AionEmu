package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.events.AtreianPassportService;
import com.aionemu.gameserver.services.events.ShugoSweepService;
import com.aionemu.gameserver.services.player.LunaShopService;
import com.aionemu.gameserver.services.toypet.MinionService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 事件引导服务静态绑定：把 Spring ObjectProvider 喂入事件引导域服务自持的 fail-fast 单例通道。
 * Event-bootstrap static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Event-bootstrap services.
 * <p>本组件取代已退役的事件引导静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Event-bootstrap static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class EventBootstrapServiceBindings implements DisposableBean {

    /**
     * 构造并注册各事件引导服务的实例提供者。
     * Construct and register instance providers for Event-bootstrap services.
     * @param lunaShopServiceProvider LunaShopService 提供者 / LunaShopService provider
     * @param minionServiceProvider MinionService 提供者 / MinionService provider
     * @param shugoSweepServiceProvider ShugoSweepService 提供者 / ShugoSweepService provider
     * @param atreianPassportServiceProvider AtreianPassportService 提供者 / AtreianPassportService provider
     */
    public EventBootstrapServiceBindings(ObjectProvider<LunaShopService> lunaShopServiceProvider,
            ObjectProvider<MinionService> minionServiceProvider,
            ObjectProvider<ShugoSweepService> shugoSweepServiceProvider,
            ObjectProvider<AtreianPassportService> atreianPassportServiceProvider) {
        LunaShopService.setInstanceProvider(lunaShopServiceProvider);
        MinionService.setInstanceProvider(minionServiceProvider);
        ShugoSweepService.setInstanceProvider(shugoSweepServiceProvider);
        AtreianPassportService.setInstanceProvider(atreianPassportServiceProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        LunaShopService.setInstanceProvider(null);
        MinionService.setInstanceProvider(null);
        ShugoSweepService.setInstanceProvider(null);
        AtreianPassportService.setInstanceProvider(null);
    }
}
