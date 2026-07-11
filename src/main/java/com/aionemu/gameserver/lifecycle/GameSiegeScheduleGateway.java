package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 攻城日程启动网关：初始化攻城与基地日程。
 * Gateway that starts the siege schedule: initializes sieges and bases.
 */
@Component
public class GameSiegeScheduleGateway {

    /**
     * 攻城服务提供者（可选）。
     * Optional provider for the siege service.
     */
    private ObjectProvider<SiegeService> siegeServiceProvider;

    /**
     * 基地服务提供者（可选）。
     * Optional provider for the base service.
     */
    private ObjectProvider<BaseService> baseServiceProvider;

    /**
     * 功能服务运行时桥接提供者（可选）。
     * Optional provider for the feature-services runtime bridge.
     */
    private ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入攻城服务提供者。
     * Inject the siege service provider.
     *
     * @param siegeServiceProvider 攻城服务提供者 / Siege service provider
     */
    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    /**
     * 注入基地服务提供者。
     * Inject the base service provider.
     *
     * @param baseServiceProvider 基地服务提供者 / Base service provider
     */
    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    /**
     * 注入功能服务运行时桥接提供者。
     * Inject the feature-services runtime bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameFeatureServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 打印攻城分区并初始化攻城与基地。
     * Print the sieges section and initialize sieges and bases.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.sieges"));
        siegeService().initSieges();
        baseService().initBases();
    }

    /**
     * 解析攻城服务。
     * Resolve the siege service.
     *
     * Siege service
     */
    private SiegeService siegeService() {
        if (siegeServiceProvider == null) {
            return runtimeBridge().siegeService();
        }
        return siegeServiceProvider.getIfAvailable(() -> runtimeBridge().siegeService());
    }

    /**
     * 解析基地服务。
     * Resolve the base service.
     *
     * Base service
     */
    private BaseService baseService() {
        if (baseServiceProvider == null) {
            return runtimeBridge().baseService();
        }
        return baseServiceProvider.getIfAvailable(() -> runtimeBridge().baseService());
    }

    /**
     * 解析功能服务运行时桥接。
     * Resolve the feature-services runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameFeatureServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameFeatureServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameFeatureServicesRuntimeBridge::new);
    }
}
