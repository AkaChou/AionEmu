package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.PvpService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.mail.MailService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameCoreGameplayServices implements DisposableBean {

    public GameCoreGameplayServices(ObjectProvider<DropService> dropServiceProvider,
            ObjectProvider<MailService> mailServiceProvider,
            ObjectProvider<PvpService> pvpServiceProvider,
            ObjectProvider<AutoGroupService> autoGroupServiceProvider,
            ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider) {
        DropService.setInstanceProvider(dropServiceProvider);
        MailService.setInstanceProvider(mailServiceProvider);
        PvpService.setInstanceProvider(pvpServiceProvider);
        AutoGroupService.setInstanceProvider(autoGroupServiceProvider);
        AbyssRankingCache.setInstanceProvider(abyssRankingCacheProvider);
    }

    @Override
    public void destroy() {
        DropService.setInstanceProvider(null);
        MailService.setInstanceProvider(null);
        PvpService.setInstanceProvider(null);
        AutoGroupService.setInstanceProvider(null);
        AbyssRankingCache.setInstanceProvider(null);
    }
}
