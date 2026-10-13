package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.TownService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 房屋服务静态绑定：把 Spring ObjectProvider 喂入房屋域服务自持的 fail-fast 单例通道。
 * Housing static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Housing services.
 * <p>本组件取代已退役的房屋静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Housing static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class HousingServiceBindings implements DisposableBean {

    /**
     * 构造并注册各房屋服务的实例提供者。
     * Construct and register instance providers for Housing services.
     * @param housingBidServiceProvider HousingBidService 提供者 / HousingBidService provider
     * @param maintenanceTaskProvider MaintenanceTask 提供者 / MaintenanceTask provider
     * @param townServiceProvider TownService 提供者 / TownService provider
     * @param housingServiceProvider HousingService 提供者 / HousingService provider
     * @param challengeTaskServiceProvider ChallengeTaskService 提供者 / ChallengeTaskService provider
     */
    public HousingServiceBindings(ObjectProvider<HousingBidService> housingBidServiceProvider,
            ObjectProvider<MaintenanceTask> maintenanceTaskProvider, ObjectProvider<TownService> townServiceProvider,
            ObjectProvider<HousingService> housingServiceProvider,
            ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        HousingBidService.setInstanceProvider(housingBidServiceProvider);
        MaintenanceTask.setInstanceProvider(maintenanceTaskProvider);
        TownService.setInstanceProvider(townServiceProvider);
        HousingService.setInstanceProvider(housingServiceProvider);
        ChallengeTaskService.setInstanceProvider(challengeTaskServiceProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        HousingBidService.setInstanceProvider(null);
        MaintenanceTask.setInstanceProvider(null);
        TownService.setInstanceProvider(null);
        HousingService.setInstanceProvider(null);
        ChallengeTaskService.setInstanceProvider(null);
    }
}
