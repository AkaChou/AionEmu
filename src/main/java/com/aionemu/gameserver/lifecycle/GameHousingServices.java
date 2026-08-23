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

    // 解析结果缓存：JFR 显示并行刷怪期间每个地图反复经 getIfAvailable 穿过 Spring 单例
    // 注册表的全局锁，成片停车；解析一次后直接返回可消除该串行点。
    // Resolved-instance caches: JFR showed parallel spawning repeatedly passing through the Spring
    // singleton-registry global lock via getIfAvailable per map, parking threads in droves; resolve
    // once and return directly to remove that serialization point.
    private static volatile HousingBidService resolvedHousingBidService;
    private static volatile MaintenanceTask resolvedMaintenanceTask;
    private static volatile TownService resolvedTownService;
    private static volatile HousingService resolvedHousingService;
    private static volatile ChallengeTaskService resolvedChallengeTaskService;

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
     * 解析房屋竞拍服务（结果缓存）。
     * Resolve the housing-bid service (cached).
     *
     * @return 房屋竞拍服务 / Housing-bid service
     */
    public static HousingBidService housingBidService() {
        HousingBidService resolved = resolvedHousingBidService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<HousingBidService> provider = housingBidServiceProvider;
        resolved = provider == null ? GameHousingFallbacks.housingBidService()
                : provider.getIfAvailable(GameHousingFallbacks::housingBidService);
        resolvedHousingBidService = resolved;
        return resolved;
    }

    /**
     * 解析房屋维护任务（结果缓存）。
     * Resolve the maintenance task (cached).
     *
     * @return 房屋维护任务 / Maintenance task
     */
    public static MaintenanceTask maintenanceTask() {
        MaintenanceTask resolved = resolvedMaintenanceTask;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<MaintenanceTask> provider = maintenanceTaskProvider;
        resolved = provider == null ? GameHousingFallbacks.maintenanceTask()
                : provider.getIfAvailable(GameHousingFallbacks::maintenanceTask);
        resolvedMaintenanceTask = resolved;
        return resolved;
    }

    /**
     * 解析城镇服务（结果缓存）。
     * Resolve the town service (cached).
     *
     * @return 城镇服务 / Town service
     */
    public static TownService townService() {
        TownService resolved = resolvedTownService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<TownService> provider = townServiceProvider;
        resolved = provider == null ? GameHousingFallbacks.townService()
                : provider.getIfAvailable(GameHousingFallbacks::townService);
        resolvedTownService = resolved;
        return resolved;
    }

    /**
     * 解析房屋服务（结果缓存）。
     * Resolve the housing service (cached).
     *
     * @return 房屋服务 / Housing service
     */
    public static HousingService housingService() {
        HousingService resolved = resolvedHousingService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<HousingService> provider = housingServiceProvider;
        resolved = provider == null ? GameHousingFallbacks.housingService()
                : provider.getIfAvailable(GameHousingFallbacks::housingService);
        resolvedHousingService = resolved;
        return resolved;
    }

    /**
     * 解析挑战任务服务（结果缓存）。
     * Resolve the challenge-task service (cached).
     *
     * @return 挑战任务服务 / Challenge-task service
     */
    public static ChallengeTaskService challengeTaskService() {
        ChallengeTaskService resolved = resolvedChallengeTaskService;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<ChallengeTaskService> provider = challengeTaskServiceProvider;
        resolved = provider == null ? GameHousingFallbacks.challengeTaskService()
                : provider.getIfAvailable(GameHousingFallbacks::challengeTaskService);
        resolvedChallengeTaskService = resolved;
        return resolved;
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
        resolvedHousingBidService = null;
        resolvedMaintenanceTask = null;
        resolvedTownService = null;
        resolvedHousingService = null;
        resolvedChallengeTaskService = null;
    }
}
