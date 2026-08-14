package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.house.MaintenanceTask;
import com.aionemu.gameserver.services.ChallengeTaskService;
import com.aionemu.gameserver.services.HousingService;
import com.aionemu.gameserver.services.HousingBidService;
import com.aionemu.gameserver.services.TownService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 房屋系统网关：启动竞拍、维护、城镇、房屋与挑战任务服务。
 * Housing gateway: starts bid, maintenance, town, housing, and challenge-task services.
 */
@Component
public class GameHousingGateway {

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
     * 运行时桥接提供者。
     * Runtime-bridge provider.
     */
    private ObjectProvider<GameHousingRuntimeBridge> runtimeBridgeProvider;

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
     * 可选注入运行时桥接提供者。
     * Optionally inject the runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameHousingRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 启动房屋相关服务。
     * Start housing-related services.
     */
    public void start() {
        runtimeBridge().printHousingSection();
        housingBidService().start();
        maintenanceTask();
        townService();
        housingService();
        challengeTaskService();
    }

    /**
     * 解析房屋竞拍服务。
     * Resolve the housing-bid service.
     *
     * @return 房屋竞拍服务 / Housing-bid service
     */
    private HousingBidService housingBidService() {
        if (housingBidServiceProvider == null) {
            return runtimeBridge().housingBidService();
        }
        return housingBidServiceProvider.getIfAvailable(() -> runtimeBridge().housingBidService());
    }

    /**
     * 解析房屋维护任务。
     * Resolve the maintenance task.
     *
     * @return 房屋维护任务 / Maintenance task
     */
    private MaintenanceTask maintenanceTask() {
        if (maintenanceTaskProvider == null) {
            return runtimeBridge().maintenanceTask();
        }
        return maintenanceTaskProvider.getIfAvailable(() -> runtimeBridge().maintenanceTask());
    }

    /**
     * 解析城镇服务。
     * Resolve the town service.
     *
     * @return 城镇服务 / Town service
     */
    private TownService townService() {
        if (townServiceProvider == null) {
            return runtimeBridge().townService();
        }
        return townServiceProvider.getIfAvailable(() -> runtimeBridge().townService());
    }

    /**
     * 解析房屋服务。
     * Resolve the housing service.
     *
     * @return 房屋服务 / Housing service
     */
    private HousingService housingService() {
        if (housingServiceProvider == null) {
            return runtimeBridge().housingService();
        }
        return housingServiceProvider.getIfAvailable(() -> runtimeBridge().housingService());
    }

    /**
     * 解析挑战任务服务。
     * Resolve the challenge-task service.
     *
     * @return 挑战任务服务 / Challenge-task service
     */
    private ChallengeTaskService challengeTaskService() {
        if (challengeTaskServiceProvider == null) {
            return runtimeBridge().challengeTaskService();
        }
        return challengeTaskServiceProvider.getIfAvailable(() -> runtimeBridge().challengeTaskService());
    }

    /**
     * 解析运行时桥接。
     * Resolve the runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameHousingRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameHousingRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameHousingRuntimeBridge::new);
    }
}
