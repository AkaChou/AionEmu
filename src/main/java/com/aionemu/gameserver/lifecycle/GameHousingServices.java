package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 房屋系统 Spring 服务门面 / 静态访问桥：注册房屋相关实例提供者。
 * static access bridge: registers housing-related instance providers.
 */
@Component
public final class GameHousingServices implements DisposableBean {

    /**
     * 房屋竞拍服务的 Spring 提供者。
     * Spring provider for the housing-bid service.
     */
    private static volatile ObjectProvider<HousingBidService> housingBidServiceProvider;
    /**
     * 房屋维护任务的 Spring 提供者。
     * Spring provider for the maintenance task.
     */
    private static volatile ObjectProvider<MaintenanceTask> maintenanceTaskProvider;
    /**
     * 城镇服务的 Spring 提供者。
     * Spring provider for the town service.
     */
    private static volatile ObjectProvider<TownService> townServiceProvider;
    /**
     * 房屋服务的 Spring 提供者。
     * Spring provider for the housing service.
     */
    private static volatile ObjectProvider<HousingService> housingServiceProvider;
    /**
     * 挑战任务服务的 Spring 提供者。
     * Spring provider for the challenge-task service.
     */
    private static volatile ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider;

    /**
     * 构造并注册各房屋相关实例提供者。
     * Construct and register instance providers for housing-related services.
     *
     * @param housingBidServiceProvider 房屋竞拍服务提供者 / Housing-bid service provider
     * @param maintenanceTaskProvider 房屋维护任务提供者 / Maintenance-task provider
     * @param townServiceProvider 城镇服务提供者 / Town-service provider
     * @param housingServiceProvider 房屋服务提供者 / Housing-service provider
     * @param challengeTaskServiceProvider 挑战任务服务提供者 / Challenge-task service provider
     */
    public GameHousingServices(ObjectProvider<HousingBidService> housingBidServiceProvider,
            ObjectProvider<MaintenanceTask> maintenanceTaskProvider, ObjectProvider<TownService> townServiceProvider,
            ObjectProvider<HousingService> housingServiceProvider,
            ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        GameHousingServices.housingBidServiceProvider = housingBidServiceProvider;
        GameHousingServices.maintenanceTaskProvider = maintenanceTaskProvider;
        GameHousingServices.townServiceProvider = townServiceProvider;
        HousingBidService.setInstanceProvider(housingBidServiceProvider);
        MaintenanceTask.setInstanceProvider(maintenanceTaskProvider);
        TownService.setInstanceProvider(townServiceProvider);
        GameHousingServices.housingServiceProvider = housingServiceProvider;
        HousingService.setInstanceProvider(housingServiceProvider);
        GameHousingServices.challengeTaskServiceProvider = challengeTaskServiceProvider;
        ChallengeTaskService.setInstanceProvider(challengeTaskServiceProvider);
    }

    /**
     * 解析房屋竞拍服务。
     * Resolve the housing-bid service.
     *
     * @return 房屋竞拍服务 / Housing-bid service
     */
    public static HousingBidService housingBidService() {
        ObjectProvider<HousingBidService> provider = housingBidServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.housingBidService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::housingBidService);
    }

    /**
     * 解析房屋维护任务。
     * Resolve the maintenance task.
     *
     * @return 房屋维护任务 / Maintenance task
     */
    public static MaintenanceTask maintenanceTask() {
        ObjectProvider<MaintenanceTask> provider = maintenanceTaskProvider;
        if (provider == null) {
            return GameHousingFallbacks.maintenanceTask();
        }
        return provider.getIfAvailable(GameHousingFallbacks::maintenanceTask);
    }

    /**
     * 解析城镇服务。
     * Resolve the town service.
     *
     * @return 城镇服务 / Town service
     */
    public static TownService townService() {
        ObjectProvider<TownService> provider = townServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.townService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::townService);
    }

    /**
     * 解析房屋服务。
     * Resolve the housing service.
     *
     * @return 房屋服务 / Housing service
     */
    public static HousingService housingService() {
        ObjectProvider<HousingService> provider = housingServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.housingService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::housingService);
    }

    /**
     * 解析挑战任务服务。
     * Resolve the challenge-task service.
     *
     * @return 挑战任务服务 / Challenge-task service
     */
    public static ChallengeTaskService challengeTaskService() {
        ObjectProvider<ChallengeTaskService> provider = challengeTaskServiceProvider;
        if (provider == null) {
            return GameHousingFallbacks.challengeTaskService();
        }
        return provider.getIfAvailable(GameHousingFallbacks::challengeTaskService);
    }

    /**
     * 销毁时清理静态提供者与服务实例桥。
     * Clear static providers and service instance bridges on destroy.
     */
    @Override
    public void destroy() {
        housingBidServiceProvider = null;
        HousingBidService.setInstanceProvider(null);
        maintenanceTaskProvider = null;
        MaintenanceTask.setInstanceProvider(null);
        townServiceProvider = null;
        TownService.setInstanceProvider(null);
        housingServiceProvider = null;
        HousingService.setInstanceProvider(null);
        challengeTaskServiceProvider = null;
        ChallengeTaskService.setInstanceProvider(null);
    }
}
