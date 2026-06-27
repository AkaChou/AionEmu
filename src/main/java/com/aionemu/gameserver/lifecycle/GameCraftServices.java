package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameCraftServices implements DisposableBean {

    private static volatile ObjectProvider<CraftSkillUpdateService> craftSkillUpdateServiceProvider;
    private static volatile ObjectProvider<RelinquishCraftStatus> relinquishCraftStatusProvider;

    public GameCraftServices(ObjectProvider<CraftSkillUpdateService> craftSkillUpdateServiceProvider,
            ObjectProvider<RelinquishCraftStatus> relinquishCraftStatusProvider) {
        GameCraftServices.craftSkillUpdateServiceProvider = craftSkillUpdateServiceProvider;
        GameCraftServices.relinquishCraftStatusProvider = relinquishCraftStatusProvider;
        CraftSkillUpdateService.setInstanceProvider(craftSkillUpdateServiceProvider);
        RelinquishCraftStatus.setInstanceProvider(relinquishCraftStatusProvider);
    }

    public static CraftSkillUpdateService craftSkillUpdateService() {
        ObjectProvider<CraftSkillUpdateService> provider = craftSkillUpdateServiceProvider;
        if (provider == null) {
            return CraftSkillUpdateService.getInstance();
        }
        return provider.getIfAvailable(CraftSkillUpdateService::getInstance);
    }

    public static RelinquishCraftStatus relinquishCraftStatus() {
        ObjectProvider<RelinquishCraftStatus> provider = relinquishCraftStatusProvider;
        if (provider == null) {
            return RelinquishCraftStatus.getInstance();
        }
        return provider.getIfAvailable(RelinquishCraftStatus::getInstance);
    }

    @Override
    public void destroy() {
        craftSkillUpdateServiceProvider = null;
        relinquishCraftStatusProvider = null;
        CraftSkillUpdateService.setInstanceProvider(null);
        RelinquishCraftStatus.setInstanceProvider(null);
    }
}
