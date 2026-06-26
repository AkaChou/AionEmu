package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameCraftServices implements DisposableBean {

    public GameCraftServices(ObjectProvider<CraftSkillUpdateService> craftSkillUpdateServiceProvider,
            ObjectProvider<RelinquishCraftStatus> relinquishCraftStatusProvider) {
        CraftSkillUpdateService.setInstanceProvider(craftSkillUpdateServiceProvider);
        RelinquishCraftStatus.setInstanceProvider(relinquishCraftStatusProvider);
    }

    @Override
    public void destroy() {
        CraftSkillUpdateService.setInstanceProvider(null);
        RelinquishCraftStatus.setInstanceProvider(null);
    }
}
