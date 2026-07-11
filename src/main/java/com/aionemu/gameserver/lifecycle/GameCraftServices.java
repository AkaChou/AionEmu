package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 制作服务门面：将 ObjectProvider 写入静态访问器并在销毁时清空。
 * Craft services facade: wires ObjectProviders into static accessors and clears them on destroy.
 */
@Component
public final class GameCraftServices implements DisposableBean {

    /**
     * 制作技能更新服务提供者静态缓存。
     * Static cache of craft-skill update service provider.
     */
    private static volatile ObjectProvider<CraftSkillUpdateService> craftSkillUpdateServiceProvider;

    /**
     * 放弃制作状态服务提供者静态缓存。
     * Static cache of relinquish-craft-status service provider.
     */
    private static volatile ObjectProvider<RelinquishCraftStatus> relinquishCraftStatusProvider;

    /**
     * 构造并注册制作相关服务的静态访问器。
     * Construct and register static accessors for craft services.
     *
     * @param craftSkillUpdateServiceProvider 制作技能更新服务提供者 / Craft-skill update service provider
     * @param relinquishCraftStatusProvider 放弃制作状态服务提供者 / Relinquish-craft-status service provider
     */
    public GameCraftServices(ObjectProvider<CraftSkillUpdateService> craftSkillUpdateServiceProvider,
            ObjectProvider<RelinquishCraftStatus> relinquishCraftStatusProvider) {
        GameCraftServices.craftSkillUpdateServiceProvider = craftSkillUpdateServiceProvider;
        GameCraftServices.relinquishCraftStatusProvider = relinquishCraftStatusProvider;
        CraftSkillUpdateService.setInstanceProvider(craftSkillUpdateServiceProvider);
        RelinquishCraftStatus.setInstanceProvider(relinquishCraftStatusProvider);
    }

    /**
     * 获取制作技能更新服务。
     * Obtain the craft-skill update service.
     *
     * Service instance
     */
    public static CraftSkillUpdateService craftSkillUpdateService() {
        ObjectProvider<CraftSkillUpdateService> provider = craftSkillUpdateServiceProvider;
        if (provider == null) {
            return CraftSkillUpdateService.getInstance();
        }
        return provider.getIfAvailable(CraftSkillUpdateService::getInstance);
    }

    /**
     * 获取放弃制作状态服务。
     * Obtain the relinquish-craft-status service.
     *
     * Service instance
     */
    public static RelinquishCraftStatus relinquishCraftStatus() {
        ObjectProvider<RelinquishCraftStatus> provider = relinquishCraftStatusProvider;
        if (provider == null) {
            return RelinquishCraftStatus.getInstance();
        }
        return provider.getIfAvailable(RelinquishCraftStatus::getInstance);
    }

    /**
     * 销毁时清空静态提供者与领域服务实例提供者。
     * Clear static providers and domain-service instance providers on destroy.
     */
    @Override
    public void destroy() {
        craftSkillUpdateServiceProvider = null;
        relinquishCraftStatusProvider = null;
        CraftSkillUpdateService.setInstanceProvider(null);
        RelinquishCraftStatus.setInstanceProvider(null);
    }
}
