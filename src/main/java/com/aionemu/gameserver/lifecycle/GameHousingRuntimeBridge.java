package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 房屋系统运行时桥接：在 Spring 提供者与回退工厂之间解析房屋相关服务。
 * Housing runtime bridge: resolves housing-related services between Spring providers and fallback factories.
 */
@Component
public class GameHousingRuntimeBridge {

    /**
     * 房屋竞拍服务提供者。
     * Housing-bid service provider.
     */
    private ObjectProvider<HousingBidService> housingBidServiceProvider;
    /**
     * 房屋维护任务提供者。
     * Maintenance-task provider.
     */
    private ObjectProvider<MaintenanceTask> maintenanceTaskProvider;
    /**
     * 城镇服务提供者。
     * Town-service provider.
     */
    private ObjectProvider<TownService> townServiceProvider;
    /**
     * 房屋服务提供者。
     * Housing-service provider.
     */
    private ObjectProvider<HousingService> housingServiceProvider;
    /**
     * 挑战任务服务提供者。
     * Challenge-task service provider.
     */
    private ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider;

    /**
     * 可选注入房屋竞拍服务提供者。
     * Optionally inject the housing-bid service provider.
     *
     * @param housingBidServiceProvider 房屋竞拍服务提供者 / Housing-bid service provider
     */
    @Autowired(required = false)
    void setHousingBidServiceProvider(ObjectProvider<HousingBidService> housingBidServiceProvider) {
        this.housingBidServiceProvider = housingBidServiceProvider;
    }

    /**
     * 可选注入房屋维护任务提供者。
     * Optionally inject the maintenance-task provider.
     *
     * @param maintenanceTaskProvider 房屋维护任务提供者 / Maintenance-task provider
     */
    @Autowired(required = false)
    void setMaintenanceTaskProvider(ObjectProvider<MaintenanceTask> maintenanceTaskProvider) {
        this.maintenanceTaskProvider = maintenanceTaskProvider;
    }

    /**
     * 可选注入城镇服务提供者。
     * Optionally inject the town-service provider.
     *
     * @param townServiceProvider 城镇服务提供者 / Town-service provider
     */
    @Autowired(required = false)
    void setTownServiceProvider(ObjectProvider<TownService> townServiceProvider) {
        this.townServiceProvider = townServiceProvider;
    }

    /**
     * 可选注入房屋服务提供者。
     * Optionally inject the housing-service provider.
     *
     * @param housingServiceProvider 房屋服务提供者 / Housing-service provider
     */
    @Autowired(required = false)
    void setHousingServiceProvider(ObjectProvider<HousingService> housingServiceProvider) {
        this.housingServiceProvider = housingServiceProvider;
    }

    /**
     * 可选注入挑战任务服务提供者。
     * Optionally inject the challenge-task service provider.
     *
     * @param challengeTaskServiceProvider 挑战任务服务提供者 / Challenge-task service provider
     */
    @Autowired(required = false)
    void setChallengeTaskServiceProvider(ObjectProvider<ChallengeTaskService> challengeTaskServiceProvider) {
        this.challengeTaskServiceProvider = challengeTaskServiceProvider;
    }

    /**
     * 打印房屋分区标题。
     * Print the housing section header.
     */
    public void printHousingSection() {
        Util.printSection(I18n.get("console.section.housing"));
    }

    /**
     * 解析房屋竞拍服务。
     * Resolve the housing-bid service.
     *
     * @return 房屋竞拍服务 / Housing-bid service
     */
    public HousingBidService housingBidService() {
        if (housingBidServiceProvider == null) {
            return GameHousingFallbacks.housingBidService();
        }
        return housingBidServiceProvider.getIfAvailable(GameHousingFallbacks::housingBidService);
    }

    /**
     * 解析房屋维护任务。
     * Resolve the maintenance task.
     *
     * @return 房屋维护任务 / Maintenance task
     */
    public MaintenanceTask maintenanceTask() {
        if (maintenanceTaskProvider == null) {
            return GameHousingFallbacks.maintenanceTask();
        }
        return maintenanceTaskProvider.getIfAvailable(GameHousingFallbacks::maintenanceTask);
    }

    /**
     * 解析城镇服务。
     * Resolve the town service.
     *
     * @return 城镇服务 / Town service
     */
    public TownService townService() {
        if (townServiceProvider == null) {
            return GameHousingFallbacks.townService();
        }
        return townServiceProvider.getIfAvailable(GameHousingFallbacks::townService);
    }

    /**
     * 解析房屋服务。
     * Resolve the housing service.
     *
     * @return 房屋服务 / Housing service
     */
    public HousingService housingService() {
        if (housingServiceProvider == null) {
            return GameHousingFallbacks.housingService();
        }
        return housingServiceProvider.getIfAvailable(GameHousingFallbacks::housingService);
    }

    /**
     * 解析挑战任务服务。
     * Resolve the challenge-task service.
     *
     * @return 挑战任务服务 / Challenge-task service
     */
    public ChallengeTaskService challengeTaskService() {
        if (challengeTaskServiceProvider == null) {
            return GameHousingFallbacks.challengeTaskService();
        }
        return challengeTaskServiceProvider.getIfAvailable(GameHousingFallbacks::challengeTaskService);
    }
}
