package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.PvpService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
import com.aionemu.gameserver.services.events.ThievesGuildService;
import com.aionemu.gameserver.services.mail.MailService;
import com.aionemu.gameserver.services.siegeservice.BalaurAssaultService;
import com.aionemu.gameserver.services.siegeservice.BattlefieldUnionService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameCoreGameplayServices implements DisposableBean {

    private static volatile ObjectProvider<LegionService> legionServiceProvider;

    public GameCoreGameplayServices(ObjectProvider<DropService> dropServiceProvider,
            ObjectProvider<MailService> mailServiceProvider,
            ObjectProvider<PvpService> pvpServiceProvider,
            ObjectProvider<AutoGroupService> autoGroupServiceProvider,
            ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider,
            ObjectProvider<LegionService> legionServiceProvider,
            ObjectProvider<ThievesGuildService> thievesGuildServiceProvider,
            ObjectProvider<BalaurAssaultService> balaurAssaultServiceProvider,
            ObjectProvider<BattlefieldUnionService> battlefieldUnionServiceProvider) {
        DropService.setInstanceProvider(dropServiceProvider);
        MailService.setInstanceProvider(mailServiceProvider);
        PvpService.setInstanceProvider(pvpServiceProvider);
        AutoGroupService.setInstanceProvider(autoGroupServiceProvider);
        AbyssRankingCache.setInstanceProvider(abyssRankingCacheProvider);
        GameCoreGameplayServices.legionServiceProvider = legionServiceProvider;
        LegionService.setInstanceProvider(legionServiceProvider);
        ThievesGuildService.setInstanceProvider(thievesGuildServiceProvider);
        BalaurAssaultService.setInstanceProvider(balaurAssaultServiceProvider);
        BattlefieldUnionService.setInstanceProvider(battlefieldUnionServiceProvider);
    }

    public static LegionService legionService() {
        ObjectProvider<LegionService> provider = legionServiceProvider;
        if (provider == null) {
            return LegionService.getInstance();
        }
        return provider.getIfAvailable(LegionService::getInstance);
    }

    @Override
    public void destroy() {
        DropService.setInstanceProvider(null);
        MailService.setInstanceProvider(null);
        PvpService.setInstanceProvider(null);
        AutoGroupService.setInstanceProvider(null);
        AbyssRankingCache.setInstanceProvider(null);
        legionServiceProvider = null;
        LegionService.setInstanceProvider(null);
        ThievesGuildService.setInstanceProvider(null);
        BalaurAssaultService.setInstanceProvider(null);
        BattlefieldUnionService.setInstanceProvider(null);
    }
}
