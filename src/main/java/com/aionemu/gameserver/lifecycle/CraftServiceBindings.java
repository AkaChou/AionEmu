package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 制作服务静态绑定：把 Spring ObjectProvider 喂入制作域服务自持的 fail-fast 单例通道。
 * Craft static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Craft services.
 * <p>本组件取代已退役的制作静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Craft static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class CraftServiceBindings implements DisposableBean {

    /**
     * 构造并注册各制作服务的实例提供者。
     * Construct and register instance providers for Craft services.
     * @param craftSkillUpdateServiceProvider CraftSkillUpdateService 提供者 / CraftSkillUpdateService provider
     * @param relinquishCraftStatusProvider RelinquishCraftStatus 提供者 / RelinquishCraftStatus provider
     */
    public CraftServiceBindings(ObjectProvider<CraftSkillUpdateService> craftSkillUpdateServiceProvider,
            ObjectProvider<RelinquishCraftStatus> relinquishCraftStatusProvider) {
        CraftSkillUpdateService.setInstanceProvider(craftSkillUpdateServiceProvider);
        RelinquishCraftStatus.setInstanceProvider(relinquishCraftStatusProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        CraftSkillUpdateService.setInstanceProvider(null);
        RelinquishCraftStatus.setInstanceProvider(null);
    }
}
