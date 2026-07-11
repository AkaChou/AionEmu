package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;

/**
 * 房屋系统服务的回退工厂：在 Spring 提供者不可用时返回房屋相关单例。
 * Fallback factory for housing services: returns housing-related singletons when Spring providers are unavailable.
 */
final class GameHousingFallbacks {

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private GameHousingFallbacks() {
    }

    /**
     * 返回房屋竞拍服务回退实例。
     * Return the housing-bid service fallback instance.
     *
     * @return 房屋竞拍服务 / Housing-bid service
     */
    static HousingBidService housingBidService() {
        return HousingBidServiceFallback.INSTANCE;
    }

    /**
     * 返回房屋维护任务回退实例。
     * Return the maintenance-task fallback instance.
     *
     * @return 房屋维护任务 / Maintenance task
     */
    static MaintenanceTask maintenanceTask() {
        return MaintenanceTaskFallback.INSTANCE;
    }

    /**
     * 返回城镇服务回退实例。
     * Return the town-service fallback instance.
     *
     * Town service
     */
    static TownService townService() {
        return TownServiceFallback.INSTANCE;
    }

    /**
     * 返回房屋服务回退实例。
     * Return the housing-service fallback instance.
     *
     * Housing service
     */
    static HousingService housingService() {
        return HousingServiceFallback.INSTANCE;
    }

    /**
     * 返回挑战任务服务回退实例。
     * Return the challenge-task service fallback instance.
     *
     * @return 挑战任务服务 / Challenge-task service
     */
    static ChallengeTaskService challengeTaskService() {
        return ChallengeTaskServiceFallback.INSTANCE;
    }

    /**
     * 房屋竞拍服务懒加载回退持有者。
     * Lazy fallback holder for the housing-bid service.
     */
    private static final class HousingBidServiceFallback {
        private static final HousingBidService INSTANCE = HousingBidService.getInstance();
    }

    /**
     * 房屋维护任务懒加载回退持有者。
     * Lazy fallback holder for the maintenance task.
     */
    private static final class MaintenanceTaskFallback {
        private static final MaintenanceTask INSTANCE = MaintenanceTask.getInstance();
    }

    /**
     * 城镇服务懒加载回退持有者。
     * Lazy fallback holder for the town service.
     */
    private static final class TownServiceFallback {
        private static final TownService INSTANCE = TownService.getInstance();
    }

    /**
     * 房屋服务懒加载回退持有者。
     * Lazy fallback holder for the housing service.
     */
    private static final class HousingServiceFallback {
        private static final HousingService INSTANCE = HousingService.getInstance();
    }

    /**
     * 挑战任务服务懒加载回退持有者。
     * Lazy fallback holder for the challenge-task service.
     */
    private static final class ChallengeTaskServiceFallback {
        private static final ChallengeTaskService INSTANCE = ChallengeTaskService.getInstance();
    }
}
