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

@Component
public final class GameCreativityServices implements DisposableBean {

    private static volatile ObjectProvider<CreativityEssenceService> creativityEssenceServiceProvider;
    private static volatile ObjectProvider<CreativitySkillService> creativitySkillServiceProvider;
    private static volatile ObjectProvider<CreativityStatsService> creativityStatsServiceProvider;
    private static volatile ObjectProvider<CreativityTransfoService> creativityTransfoServiceProvider;
    private static volatile ObjectProvider<Accuracy> accuracyProvider;
    private static volatile ObjectProvider<Agility> agilityProvider;
    private static volatile ObjectProvider<Health> healthProvider;
    private static volatile ObjectProvider<Knowledge> knowledgeProvider;
    private static volatile ObjectProvider<Power> powerProvider;
    private static volatile ObjectProvider<Precision> precisionProvider;
    private static volatile ObjectProvider<Will> willProvider;

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

    public static CreativityEssenceService creativityEssenceService() {
        ObjectProvider<CreativityEssenceService> provider = creativityEssenceServiceProvider;
        if (provider == null) {
            return CreativityEssenceService.getInstance();
        }
        return provider.getIfAvailable(CreativityEssenceService::getInstance);
    }

    public static CreativitySkillService creativitySkillService() {
        ObjectProvider<CreativitySkillService> provider = creativitySkillServiceProvider;
        if (provider == null) {
            return CreativitySkillService.getInstance();
        }
        return provider.getIfAvailable(CreativitySkillService::getInstance);
    }

    public static CreativityStatsService creativityStatsService() {
        ObjectProvider<CreativityStatsService> provider = creativityStatsServiceProvider;
        if (provider == null) {
            return CreativityStatsService.getInstance();
        }
        return provider.getIfAvailable(CreativityStatsService::getInstance);
    }

    public static CreativityTransfoService creativityTransfoService() {
        ObjectProvider<CreativityTransfoService> provider = creativityTransfoServiceProvider;
        if (provider == null) {
            return CreativityTransfoService.getInstance();
        }
        return provider.getIfAvailable(CreativityTransfoService::getInstance);
    }

    public static Accuracy accuracy() {
        ObjectProvider<Accuracy> provider = accuracyProvider;
        if (provider == null) {
            return Accuracy.getInstance();
        }
        return provider.getIfAvailable(Accuracy::getInstance);
    }

    public static Agility agility() {
        ObjectProvider<Agility> provider = agilityProvider;
        if (provider == null) {
            return Agility.getInstance();
        }
        return provider.getIfAvailable(Agility::getInstance);
    }

    public static Health health() {
        ObjectProvider<Health> provider = healthProvider;
        if (provider == null) {
            return Health.getInstance();
        }
        return provider.getIfAvailable(Health::getInstance);
    }

    public static Knowledge knowledge() {
        ObjectProvider<Knowledge> provider = knowledgeProvider;
        if (provider == null) {
            return Knowledge.getInstance();
        }
        return provider.getIfAvailable(Knowledge::getInstance);
    }

    public static Power power() {
        ObjectProvider<Power> provider = powerProvider;
        if (provider == null) {
            return Power.getInstance();
        }
        return provider.getIfAvailable(Power::getInstance);
    }

    public static Precision precision() {
        ObjectProvider<Precision> provider = precisionProvider;
        if (provider == null) {
            return Precision.getInstance();
        }
        return provider.getIfAvailable(Precision::getInstance);
    }

    public static Will will() {
        ObjectProvider<Will> provider = willProvider;
        if (provider == null) {
            return Will.getInstance();
        }
        return provider.getIfAvailable(Will::getInstance);
    }

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
