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

    private static volatile ObjectProvider<DropService> dropServiceProvider;
    private static volatile ObjectProvider<MailService> mailServiceProvider;
    private static volatile ObjectProvider<PvpService> pvpServiceProvider;
    private static volatile ObjectProvider<AutoGroupService> autoGroupServiceProvider;
    private static volatile ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider;
    private static volatile ObjectProvider<LegionService> legionServiceProvider;
    private static volatile ObjectProvider<BalaurAssaultService> balaurAssaultServiceProvider;
    private static volatile ObjectProvider<BattlefieldUnionService> battlefieldUnionServiceProvider;

    public GameCoreGameplayServices(ObjectProvider<DropService> dropServiceProvider,
            ObjectProvider<MailService> mailServiceProvider,
            ObjectProvider<PvpService> pvpServiceProvider,
            ObjectProvider<AutoGroupService> autoGroupServiceProvider,
            ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider,
            ObjectProvider<LegionService> legionServiceProvider,
            ObjectProvider<ThievesGuildService> thievesGuildServiceProvider,
            ObjectProvider<BalaurAssaultService> balaurAssaultServiceProvider,
            ObjectProvider<BattlefieldUnionService> battlefieldUnionServiceProvider) {
        GameCoreGameplayServices.dropServiceProvider = dropServiceProvider;
        DropService.setInstanceProvider(dropServiceProvider);
        GameCoreGameplayServices.mailServiceProvider = mailServiceProvider;
        MailService.setInstanceProvider(mailServiceProvider);
        GameCoreGameplayServices.pvpServiceProvider = pvpServiceProvider;
        PvpService.setInstanceProvider(pvpServiceProvider);
        GameCoreGameplayServices.autoGroupServiceProvider = autoGroupServiceProvider;
        AutoGroupService.setInstanceProvider(autoGroupServiceProvider);
        GameCoreGameplayServices.abyssRankingCacheProvider = abyssRankingCacheProvider;
        AbyssRankingCache.setInstanceProvider(abyssRankingCacheProvider);
        GameCoreGameplayServices.legionServiceProvider = legionServiceProvider;
        LegionService.setInstanceProvider(legionServiceProvider);
        ThievesGuildService.setInstanceProvider(thievesGuildServiceProvider);
        GameCoreGameplayServices.balaurAssaultServiceProvider = balaurAssaultServiceProvider;
        BalaurAssaultService.setInstanceProvider(balaurAssaultServiceProvider);
        GameCoreGameplayServices.battlefieldUnionServiceProvider = battlefieldUnionServiceProvider;
        BattlefieldUnionService.setInstanceProvider(battlefieldUnionServiceProvider);
    }

    public static DropService dropService() {
        ObjectProvider<DropService> provider = dropServiceProvider;
        if (provider == null) {
            return DropService.getInstance();
        }
        return provider.getIfAvailable(DropService::getInstance);
    }

    public static MailService mailService() {
        ObjectProvider<MailService> provider = mailServiceProvider;
        if (provider == null) {
            return MailService.getInstance();
        }
        return provider.getIfAvailable(MailService::getInstance);
    }

    public static PvpService pvpService() {
        ObjectProvider<PvpService> provider = pvpServiceProvider;
        if (provider == null) {
            return PvpService.getInstance();
        }
        return provider.getIfAvailable(PvpService::getInstance);
    }

    public static AbyssRankingCache abyssRankingCache() {
        ObjectProvider<AbyssRankingCache> provider = abyssRankingCacheProvider;
        if (provider == null) {
            return AbyssRankingCache.getInstance();
        }
        return provider.getIfAvailable(AbyssRankingCache::getInstance);
    }

    public static LegionService legionService() {
        ObjectProvider<LegionService> provider = legionServiceProvider;
        if (provider == null) {
            return LegionService.getInstance();
        }
        return provider.getIfAvailable(LegionService::getInstance);
    }

    public static AutoGroupService autoGroupService() {
        ObjectProvider<AutoGroupService> provider = autoGroupServiceProvider;
        if (provider == null) {
            return AutoGroupService.getInstance();
        }
        return provider.getIfAvailable(AutoGroupService::getInstance);
    }

    public static BalaurAssaultService balaurAssaultService() {
        ObjectProvider<BalaurAssaultService> provider = balaurAssaultServiceProvider;
        if (provider == null) {
            return BalaurAssaultService.getInstance();
        }
        return provider.getIfAvailable(BalaurAssaultService::getInstance);
    }

    public static BattlefieldUnionService battlefieldUnionService() {
        ObjectProvider<BattlefieldUnionService> provider = battlefieldUnionServiceProvider;
        if (provider == null) {
            return BattlefieldUnionService.getInstance();
        }
        return provider.getIfAvailable(BattlefieldUnionService::getInstance);
    }

    @Override
    public void destroy() {
        dropServiceProvider = null;
        DropService.setInstanceProvider(null);
        mailServiceProvider = null;
        MailService.setInstanceProvider(null);
        pvpServiceProvider = null;
        PvpService.setInstanceProvider(null);
        autoGroupServiceProvider = null;
        AutoGroupService.setInstanceProvider(null);
        abyssRankingCacheProvider = null;
        AbyssRankingCache.setInstanceProvider(null);
        legionServiceProvider = null;
        LegionService.setInstanceProvider(null);
        ThievesGuildService.setInstanceProvider(null);
        balaurAssaultServiceProvider = null;
        BalaurAssaultService.setInstanceProvider(null);
        battlefieldUnionServiceProvider = null;
        BattlefieldUnionService.setInstanceProvider(null);
    }
}
