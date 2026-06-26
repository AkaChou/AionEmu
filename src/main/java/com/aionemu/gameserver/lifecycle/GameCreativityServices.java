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

    @Override
    public void destroy() {
        CreativityEssenceService.setInstanceProvider(null);
        CreativitySkillService.setInstanceProvider(null);
        CreativityStatsService.setInstanceProvider(null);
        CreativityTransfoService.setInstanceProvider(null);
        Accuracy.setInstanceProvider(null);
        Agility.setInstanceProvider(null);
        Health.setInstanceProvider(null);
        Knowledge.setInstanceProvider(null);
        Power.setInstanceProvider(null);
        Precision.setInstanceProvider(null);
        Will.setInstanceProvider(null);
    }
}
