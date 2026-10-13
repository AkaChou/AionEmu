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
 * 创造力面板服务静态绑定：把 Spring ObjectProvider 喂入创造力面板域服务自持的 fail-fast 单例通道。
 * Creativity-panel static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Creativity-panel services.
 * <p>本组件取代已退役的创造力面板静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Creativity-panel static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class CreativityServiceBindings implements DisposableBean {

    /**
     * 构造并注册各创造力面板服务的实例提供者。
     * Construct and register instance providers for Creativity-panel services.
     * @param creativityEssenceServiceProvider CreativityEssenceService 提供者 / CreativityEssenceService provider
     * @param creativitySkillServiceProvider CreativitySkillService 提供者 / CreativitySkillService provider
     * @param creativityStatsServiceProvider CreativityStatsService 提供者 / CreativityStatsService provider
     * @param creativityTransfoServiceProvider CreativityTransfoService 提供者 / CreativityTransfoService provider
     * @param accuracyProvider Accuracy 提供者 / Accuracy provider
     * @param agilityProvider Agility 提供者 / Agility provider
     * @param healthProvider Health 提供者 / Health provider
     * @param knowledgeProvider Knowledge 提供者 / Knowledge provider
     * @param powerProvider Power 提供者 / Power provider
     * @param precisionProvider Precision 提供者 / Precision provider
     * @param willProvider Will 提供者 / Will provider
     */
    public CreativityServiceBindings(ObjectProvider<CreativityEssenceService> creativityEssenceServiceProvider,
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

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
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
