package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.player.CreativityPanel.CreativityEssenceService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativitySkillService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityStatsService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityTransfoService;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Accuracy;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Agility;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Health;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Knowledge;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Power;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Precision;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Will;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 创造力面板服务门面：将 ObjectProvider 写入静态访问器并在销毁时清空。
 * Creativity-panel services facade: wires ObjectProviders into static accessors and clears them on destroy.
 */
@Component
public final class GameCreativityServices implements DisposableBean {

    /**
     * 创造力精华服务提供者静态缓存。
     * Static cache of creativity essence service provider.
     */
    private static volatile ObjectProvider<CreativityEssenceService> creativityEssenceServiceProvider;

    /**
     * 创造力技能服务提供者静态缓存。
     * Static cache of creativity skill service provider.
     */
    private static volatile ObjectProvider<CreativitySkillService> creativitySkillServiceProvider;

    /**
     * 创造力属性服务提供者静态缓存。
     * Static cache of creativity stats service provider.
     */
    private static volatile ObjectProvider<CreativityStatsService> creativityStatsServiceProvider;

    /**
     * 创造力变身服务提供者静态缓存。
     * Static cache of creativity transfo service provider.
     */
    private static volatile ObjectProvider<CreativityTransfoService> creativityTransfoServiceProvider;

    /**
     * 精准属性提供者静态缓存。
     * Static cache of Accuracy provider.
     */
    private static volatile ObjectProvider<Accuracy> accuracyProvider;

    /**
     * 敏捷属性提供者静态缓存。
     * Static cache of Agility provider.
     */
    private static volatile ObjectProvider<Agility> agilityProvider;

    /**
     * 生命属性提供者静态缓存。
     * Static cache of Health provider.
     */
    private static volatile ObjectProvider<Health> healthProvider;

    /**
     * 知识属性提供者静态缓存。
     * Static cache of Knowledge provider.
     */
    private static volatile ObjectProvider<Knowledge> knowledgeProvider;

    /**
     * 力量属性提供者静态缓存。
     * Static cache of Power provider.
     */
    private static volatile ObjectProvider<Power> powerProvider;

    /**
     * 精密度属性提供者静态缓存。
     * Static cache of Precision provider.
     */
    private static volatile ObjectProvider<Precision> precisionProvider;

    /**
     * 意志属性提供者静态缓存。
     * Static cache of Will provider.
     */
    private static volatile ObjectProvider<Will> willProvider;

    /**
     * 构造并注册创造力面板相关服务的静态访问器。
     * Construct and register static accessors for creativity-panel services.
     *
     * @param creativityEssenceServiceProvider 创造力精华服务提供者 / Creativity essence service provider
     * @param creativitySkillServiceProvider 创造力技能服务提供者 / Creativity skill service provider
     * @param creativityStatsServiceProvider 创造力属性服务提供者 / Creativity stats service provider
     * @param creativityTransfoServiceProvider 创造力变身服务提供者 / Creativity transfo service provider
     * @param accuracyProvider 精准属性提供者 / Accuracy provider
     * @param agilityProvider 敏捷属性提供者 / Agility provider
     * @param healthProvider 生命属性提供者 / Health provider
     * @param knowledgeProvider 知识属性提供者 / Knowledge provider
     * @param powerProvider 力量属性提供者 / Power provider
     * @param precisionProvider 精密度属性提供者 / Precision provider
     * @param willProvider 意志属性提供者 / Will provider
     */
    public GameCreativityServices(ObjectProvider<CreativityEssenceService> creativityEssenceServiceProvider,
            ObjectProvider<CreativitySkillService> creativitySkillServiceProvider,
            ObjectProvider<CreativityStatsService> creativityStatsServiceProvider,
            ObjectProvider<CreativityTransfoService> creativityTransfoServiceProvider,
            ObjectProvider<Accuracy> accuracyProvider,
            ObjectProvider<Agility> agilityProvider,
            ObjectProvider<Health> healthProvider,
            ObjectProvider<Knowledge> knowledgeProvider,
            ObjectProvider<Power> powerProvider,
            ObjectProvider<Precision> precisionProvider,
            ObjectProvider<Will> willProvider) {
        GameCreativityServices.creativityEssenceServiceProvider = creativityEssenceServiceProvider;
        GameCreativityServices.creativitySkillServiceProvider = creativitySkillServiceProvider;
        GameCreativityServices.creativityStatsServiceProvider = creativityStatsServiceProvider;
        GameCreativityServices.creativityTransfoServiceProvider = creativityTransfoServiceProvider;
        GameCreativityServices.accuracyProvider = accuracyProvider;
        GameCreativityServices.agilityProvider = agilityProvider;
        GameCreativityServices.healthProvider = healthProvider;
        GameCreativityServices.knowledgeProvider = knowledgeProvider;
        GameCreativityServices.powerProvider = powerProvider;
        GameCreativityServices.precisionProvider = precisionProvider;
        GameCreativityServices.willProvider = willProvider;
        CreativityEssenceService.setInstanceProvider(creativityEssenceServiceProvider);
        CreativitySkillService.setInstanceProvider(creativitySkillServiceProvider);
        CreativityStatsService.setInstanceProvider(creativityStatsServiceProvider);
        CreativityTransfoService.setInstanceProvider(creativityTransfoServiceProvider);
        Accuracy.setInstanceProvider(accuracyProvider);
        Agility.setInstanceProvider(agilityProvider);
        Health.setInstanceProvider(healthProvider);
        Knowledge.setInstanceProvider(knowledgeProvider);
        Power.setInstanceProvider(powerProvider);
        Precision.setInstanceProvider(precisionProvider);
        Will.setInstanceProvider(willProvider);
    }

    /**
     * 获取创造力精华服务。
     * Obtain the creativity essence service.
     *
     * @return 服务实例 / Service instance
     */
    public static CreativityEssenceService creativityEssenceService() {
        ObjectProvider<CreativityEssenceService> provider = creativityEssenceServiceProvider;
        if (provider == null) {
            return CreativityEssenceService.getInstance();
        }
        return provider.getIfAvailable(CreativityEssenceService::getInstance);
    }

    /**
     * 获取创造力技能服务。
     * Obtain the creativity skill service.
     *
     * @return 服务实例 / Service instance
     */
    public static CreativitySkillService creativitySkillService() {
        ObjectProvider<CreativitySkillService> provider = creativitySkillServiceProvider;
        if (provider == null) {
            return CreativitySkillService.getInstance();
        }
        return provider.getIfAvailable(CreativitySkillService::getInstance);
    }

    /**
     * 获取创造力属性服务。
     * Obtain the creativity stats service.
     *
     * @return 服务实例 / Service instance
     */
    public static CreativityStatsService creativityStatsService() {
        ObjectProvider<CreativityStatsService> provider = creativityStatsServiceProvider;
        if (provider == null) {
            return CreativityStatsService.getInstance();
        }
        return provider.getIfAvailable(CreativityStatsService::getInstance);
    }

    /**
     * 获取创造力变身服务。
     * Obtain the creativity transfo service.
     *
     * @return 服务实例 / Service instance
     */
    public static CreativityTransfoService creativityTransfoService() {
        ObjectProvider<CreativityTransfoService> provider = creativityTransfoServiceProvider;
        if (provider == null) {
            return CreativityTransfoService.getInstance();
        }
        return provider.getIfAvailable(CreativityTransfoService::getInstance);
    }

    /**
     * 获取精准属性。
     * Obtain Accuracy.
     *
     * @return 属性实例 / Stat instance
     */
    public static Accuracy accuracy() {
        ObjectProvider<Accuracy> provider = accuracyProvider;
        if (provider == null) {
            return Accuracy.getInstance();
        }
        return provider.getIfAvailable(Accuracy::getInstance);
    }

    /**
     * 获取敏捷属性。
     * Obtain Agility.
     *
     * @return 属性实例 / Stat instance
     */
    public static Agility agility() {
        ObjectProvider<Agility> provider = agilityProvider;
        if (provider == null) {
            return Agility.getInstance();
        }
        return provider.getIfAvailable(Agility::getInstance);
    }

    /**
     * 获取生命属性。
     * Obtain Health.
     *
     * @return 属性实例 / Stat instance
     */
    public static Health health() {
        ObjectProvider<Health> provider = healthProvider;
        if (provider == null) {
            return Health.getInstance();
        }
        return provider.getIfAvailable(Health::getInstance);
    }

    /**
     * 获取知识属性。
     * Obtain Knowledge.
     *
     * @return 属性实例 / Stat instance
     */
    public static Knowledge knowledge() {
        ObjectProvider<Knowledge> provider = knowledgeProvider;
        if (provider == null) {
            return Knowledge.getInstance();
        }
        return provider.getIfAvailable(Knowledge::getInstance);
    }

    /**
     * 获取力量属性。
     * Obtain Power.
     *
     * @return 属性实例 / Stat instance
     */
    public static Power power() {
        ObjectProvider<Power> provider = powerProvider;
        if (provider == null) {
            return Power.getInstance();
        }
        return provider.getIfAvailable(Power::getInstance);
    }

    /**
     * 获取精密度属性。
     * Obtain Precision.
     *
     * @return 属性实例 / Stat instance
     */
    public static Precision precision() {
        ObjectProvider<Precision> provider = precisionProvider;
        if (provider == null) {
            return Precision.getInstance();
        }
        return provider.getIfAvailable(Precision::getInstance);
    }

    /**
     * 获取意志属性。
     * Obtain Will.
     *
     * @return 属性实例 / Stat instance
     */
    public static Will will() {
        ObjectProvider<Will> provider = willProvider;
        if (provider == null) {
            return Will.getInstance();
        }
        return provider.getIfAvailable(Will::getInstance);
    }

    /**
     * 销毁时清空静态提供者与领域服务实例提供者。
     * Clear static providers and domain-service instance providers on destroy.
     */
    @Override
    public void destroy() {
        creativityEssenceServiceProvider = null;
        CreativityEssenceService.setInstanceProvider(null);
        creativitySkillServiceProvider = null;
        CreativitySkillService.setInstanceProvider(null);
        creativityStatsServiceProvider = null;
        CreativityStatsService.setInstanceProvider(null);
        creativityTransfoServiceProvider = null;
        CreativityTransfoService.setInstanceProvider(null);
        accuracyProvider = null;
        Accuracy.setInstanceProvider(null);
        agilityProvider = null;
        Agility.setInstanceProvider(null);
        healthProvider = null;
        Health.setInstanceProvider(null);
        knowledgeProvider = null;
        Knowledge.setInstanceProvider(null);
        powerProvider = null;
        Power.setInstanceProvider(null);
        precisionProvider = null;
        Precision.setInstanceProvider(null);
        willProvider = null;
        Will.setInstanceProvider(null);
    }
}
