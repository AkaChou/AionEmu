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

/**
 * 核心玩法服务门面：将 ObjectProvider 写入静态访问器并在销毁时清空。
 * Core gameplay services facade: wires ObjectProviders into static accessors and clears them on destroy.
 */
@Component
public final class GameCoreGameplayServices implements DisposableBean {

    /**
     * 掉落服务提供者静态缓存。
     * Static cache of drop service provider.
     */
    private static volatile ObjectProvider<DropService> dropServiceProvider;

    /**
     * 邮件服务提供者静态缓存。
     * Static cache of mail service provider.
     */
    private static volatile ObjectProvider<MailService> mailServiceProvider;

    /**
     * PvP 服务提供者静态缓存。
     * Static cache of PvP service provider.
     */
    private static volatile ObjectProvider<PvpService> pvpServiceProvider;

    /**
     * 自动组队服务提供者静态缓存。
     * Static cache of auto-group service provider.
     */
    private static volatile ObjectProvider<AutoGroupService> autoGroupServiceProvider;

    /**
     * 欧比斯排名缓存提供者静态缓存。
     * Static cache of abyss ranking cache provider.
     */
    private static volatile ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider;

    /**
     * 军团服务提供者静态缓存。
     * Static cache of legion service provider.
     */
    private static volatile ObjectProvider<LegionService> legionServiceProvider;

    /**
     * 龙族袭击服务提供者静态缓存。
     * Static cache of Balaur assault service provider.
     */
    private static volatile ObjectProvider<BalaurAssaultService> balaurAssaultServiceProvider;

    /**
     * 战场联盟服务提供者静态缓存。
     * Static cache of battlefield union service provider.
     */
    private static volatile ObjectProvider<BattlefieldUnionService> battlefieldUnionServiceProvider;

    /**
     * 构造并注册各核心玩法服务的静态访问器。
     * Construct and register static accessors for core gameplay services.
     *
     * @param dropServiceProvider 掉落服务提供者 / Drop service provider
     * @param mailServiceProvider 邮件服务提供者 / Mail service provider
     * @param pvpServiceProvider PvP 服务提供者 / PvP service provider
     * @param autoGroupServiceProvider 自动组队服务提供者 / Auto-group service provider
     * @param abyssRankingCacheProvider 欧比斯排名缓存提供者 / Abyss ranking cache provider
     * @param legionServiceProvider 军团服务提供者 / Legion service provider
     * @param thievesGuildServiceProvider 盗贼公会服务提供者 / Thieves guild service provider
     * @param balaurAssaultServiceProvider 龙族袭击服务提供者 / Balaur assault service provider
     * @param battlefieldUnionServiceProvider 战场联盟服务提供者 / Battlefield union service provider
     */
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

    /**
     * 获取掉落服务。
     * Obtain the drop service.
     *
     * Service instance
     */
    public static DropService dropService() {
        ObjectProvider<DropService> provider = dropServiceProvider;
        if (provider == null) {
            return DropService.getInstance();
        }
        return provider.getIfAvailable(DropService::getInstance);
    }

    /**
     * 获取邮件服务。
     * Obtain the mail service.
     *
     * Service instance
     */
    public static MailService mailService() {
        ObjectProvider<MailService> provider = mailServiceProvider;
        if (provider == null) {
            return MailService.getInstance();
        }
        return provider.getIfAvailable(MailService::getInstance);
    }

    /**
     * 获取 PvP 服务。
     * Obtain the PvP service.
     *
     * Service instance
     */
    public static PvpService pvpService() {
        ObjectProvider<PvpService> provider = pvpServiceProvider;
        if (provider == null) {
            return PvpService.getInstance();
        }
        return provider.getIfAvailable(PvpService::getInstance);
    }

    /**
     * 获取欧比斯排名缓存。
     * Obtain the abyss ranking cache.
     *
     * Cache instance
     */
    public static AbyssRankingCache abyssRankingCache() {
        ObjectProvider<AbyssRankingCache> provider = abyssRankingCacheProvider;
        if (provider == null) {
            return AbyssRankingCache.getInstance();
        }
        return provider.getIfAvailable(AbyssRankingCache::getInstance);
    }

    /**
     * 获取军团服务。
     * Obtain the legion service.
     *
     * Service instance
     */
    public static LegionService legionService() {
        ObjectProvider<LegionService> provider = legionServiceProvider;
        if (provider == null) {
            return LegionService.getInstance();
        }
        return provider.getIfAvailable(LegionService::getInstance);
    }

    /**
     * 获取自动组队服务。
     * Obtain the auto-group service.
     *
     * Service instance
     */
    public static AutoGroupService autoGroupService() {
        ObjectProvider<AutoGroupService> provider = autoGroupServiceProvider;
        if (provider == null) {
            return AutoGroupService.getInstance();
        }
        return provider.getIfAvailable(AutoGroupService::getInstance);
    }

    /**
     * 获取龙族袭击服务。
     * Obtain the Balaur assault service.
     *
     * Service instance
     */
    public static BalaurAssaultService balaurAssaultService() {
        ObjectProvider<BalaurAssaultService> provider = balaurAssaultServiceProvider;
        if (provider == null) {
            return BalaurAssaultService.getInstance();
        }
        return provider.getIfAvailable(BalaurAssaultService::getInstance);
    }

    /**
     * 获取战场联盟服务。
     * Obtain the battlefield union service.
     *
     * Service instance
     */
    public static BattlefieldUnionService battlefieldUnionService() {
        ObjectProvider<BattlefieldUnionService> provider = battlefieldUnionServiceProvider;
        if (provider == null) {
            return BattlefieldUnionService.getInstance();
        }
        return provider.getIfAvailable(BattlefieldUnionService::getInstance);
    }

    /**
     * 销毁时清空静态提供者与领域服务实例提供者。
     * Clear static providers and domain-service instance providers on destroy.
     */
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
