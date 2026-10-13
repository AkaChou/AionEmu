package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AutoGroupService;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.PvpService;
import com.aionemu.gameserver.services.abyss.AbyssRankingCache;
import com.aionemu.gameserver.services.drop.DropService;
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

    private static volatile DropService resolvedDropService;
    private static volatile MailService resolvedMailService;
    private static volatile PvpService resolvedPvpService;
    private static volatile AutoGroupService resolvedAutoGroupService;
    private static volatile AbyssRankingCache resolvedAbyssRankingCache;
    private static volatile LegionService resolvedLegionService;
    private static volatile BalaurAssaultService resolvedBalaurAssaultService;
    private static volatile BattlefieldUnionService resolvedBattlefieldUnionService;

    /**
     * 构造并注册各核心玩法服务的静态访问器。
     * Construct and register static accessors for core gameplay services.
     * @param dropServiceProvider 掉落服务提供者 / Drop service provider
     * @param mailServiceProvider 邮件服务提供者 / Mail service provider
     * @param pvpServiceProvider PvP 服务提供者 / PvP service provider
     * @param autoGroupServiceProvider 自动组队服务提供者 / Auto-group service provider
     * @param abyssRankingCacheProvider 欧比斯排名缓存提供者 / Abyss ranking cache provider
     * @param legionServiceProvider 军团服务提供者 / Legion service provider
     * @param balaurAssaultServiceProvider 龙族袭击服务提供者 / Balaur assault service provider
     * @param battlefieldUnionServiceProvider 战场联盟服务提供者 / Battlefield union service provider
     */
    public GameCoreGameplayServices(ObjectProvider<DropService> dropServiceProvider,
            ObjectProvider<MailService> mailServiceProvider,
            ObjectProvider<PvpService> pvpServiceProvider,
            ObjectProvider<AutoGroupService> autoGroupServiceProvider,
            ObjectProvider<AbyssRankingCache> abyssRankingCacheProvider,
            ObjectProvider<LegionService> legionServiceProvider,
            ObjectProvider<BalaurAssaultService> balaurAssaultServiceProvider,
            ObjectProvider<BattlefieldUnionService> battlefieldUnionServiceProvider) {
        GameCoreGameplayServices.dropServiceProvider = dropServiceProvider;
        DropService.setInstanceProvider(dropServiceProvider);
        clearResolvedServices();
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
        GameCoreGameplayServices.balaurAssaultServiceProvider = balaurAssaultServiceProvider;
        BalaurAssaultService.setInstanceProvider(balaurAssaultServiceProvider);
        GameCoreGameplayServices.battlefieldUnionServiceProvider = battlefieldUnionServiceProvider;
        BattlefieldUnionService.setInstanceProvider(battlefieldUnionServiceProvider);
    }

    /**
     * 获取掉落服务。
     * Obtain the drop service.
     * @return 服务实例 / Service instance
     */
    public static DropService dropService() {
        DropService resolved = resolvedDropService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<DropService> provider = dropServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "DropService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedDropService = resolved;
        return resolved;
    }

    /**
     * 获取邮件服务。
     * Obtain the mail service.
     * @return 服务实例 / Service instance
     */
    public static MailService mailService() {
        MailService resolved = resolvedMailService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<MailService> provider = mailServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "MailService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedMailService = resolved;
        return resolved;
    }

    /**
     * 获取 PvP 服务。
     * Obtain the PvP service.
     * @return 服务实例 / Service instance
     */
    public static PvpService pvpService() {
        PvpService resolved = resolvedPvpService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<PvpService> provider = pvpServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "PvpService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedPvpService = resolved;
        return resolved;
    }

    /**
     * 获取欧比斯排名缓存。
     * Obtain the abyss ranking cache.
     * @return 缓存实例 / Cache instance
     */
    public static AbyssRankingCache abyssRankingCache() {
        AbyssRankingCache resolved = resolvedAbyssRankingCache;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<AbyssRankingCache> provider = abyssRankingCacheProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "AbyssRankingCache is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedAbyssRankingCache = resolved;
        return resolved;
    }

    /**
     * 获取军团服务。
     * Obtain the legion service.
     * @return 服务实例 / Service instance
     */
    public static LegionService legionService() {
        LegionService resolved = resolvedLegionService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<LegionService> provider = legionServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "LegionService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedLegionService = resolved;
        return resolved;
    }

    /**
     * 获取自动组队服务。
     * Obtain the auto-group service.
     * @return 服务实例 / Service instance
     */
    public static AutoGroupService autoGroupService() {
        AutoGroupService resolved = resolvedAutoGroupService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<AutoGroupService> provider = autoGroupServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "AutoGroupService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedAutoGroupService = resolved;
        return resolved;
    }

    /**
     * 获取龙族袭击服务。
     * Obtain the Balaur assault service.
     * @return 服务实例 / Service instance
     */
    public static BalaurAssaultService balaurAssaultService() {
        BalaurAssaultService resolved = resolvedBalaurAssaultService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<BalaurAssaultService> provider = balaurAssaultServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "BalaurAssaultService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedBalaurAssaultService = resolved;
        return resolved;
    }

    /**
     * 获取战场联盟服务。
     * Obtain the battlefield union service.
     * @return 服务实例 / Service instance
     */
    public static BattlefieldUnionService battlefieldUnionService() {
        BattlefieldUnionService resolved = resolvedBattlefieldUnionService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<BattlefieldUnionService> provider = battlefieldUnionServiceProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "BattlefieldUnionService is only available inside the Spring container; "
                    + "offline construction must not touch core gameplay services");
        }
        resolved = provider.getObject();
        resolvedBattlefieldUnionService = resolved;
        return resolved;
    }

    /**
     * 清空已解析的服务单例缓存。
     * Clear resolved service-singleton caches.
     */
    static void clearResolvedServices() {
        resolvedDropService = null;
        resolvedMailService = null;
        resolvedPvpService = null;
        resolvedAutoGroupService = null;
        resolvedAbyssRankingCache = null;
        resolvedLegionService = null;
        resolvedBalaurAssaultService = null;
        resolvedBattlefieldUnionService = null;
    }

    /**
     * 销毁时清空静态提供者与领域服务实例提供者。
     * Clear static providers and domain-service instance providers on destroy.
     */
    @Override
    public void destroy() {
        dropServiceProvider = null;
        clearResolvedServices();
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
        balaurAssaultServiceProvider = null;
        BalaurAssaultService.setInstanceProvider(null);
        battlefieldUnionServiceProvider = null;
        BattlefieldUnionService.setInstanceProvider(null);
    }
}
