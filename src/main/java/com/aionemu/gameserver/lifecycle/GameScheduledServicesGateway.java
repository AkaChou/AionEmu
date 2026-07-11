package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.spawnengine.ShugoImperialTombSpawnManager;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时服务启动网关：按配置调度小猪波比、欧比斯活动与术古皇陵刷怪。
 * Gateway that starts scheduled services: Pig/Poppy, Abyss events, and Imperial Tomb spawns by config.
 */
@Component
public class GameScheduledServicesGateway {

    /**
     * 术古皇陵刷怪管理器提供者（可选）。
     * Optional provider for the Shugo Imperial Tomb spawn manager.
     */
    private ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider;

    /**
     * 维护类运行时桥接提供者（可选）。
     * Optional provider for the maintenance runtime bridge.
     */
    private ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入术古皇陵刷怪管理器提供者。
     * Inject the Shugo Imperial Tomb spawn manager provider.
     *
     * @param shugoImperialTombSpawnManagerProvider 刷怪管理器提供者 / Spawn manager provider
     */
    @Autowired(required = false)
    void setShugoImperialTombSpawnManagerProvider(ObjectProvider<ShugoImperialTombSpawnManager> shugoImperialTombSpawnManagerProvider) {
        this.shugoImperialTombSpawnManagerProvider = shugoImperialTombSpawnManagerProvider;
    }

    /**
     * 注入维护类运行时桥接提供者。
     * Inject the maintenance runtime bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 打印定时服务分区并按开关调度相关活动。
     * Print the scheduled-services section and schedule enabled events.
     */
    public void start() {
        Util.printSection(I18n.get("console.section.scheduled_services"));
        GameMaintenanceServicesRuntimeBridge runtimeBridge = runtimeBridge();
        if (runtimeBridge.isPigPoppyEventEnabled()) {
            runtimeBridge.schedulePigPoppyEvent();
        }
        if (runtimeBridge.isAbyssEventEnabled()) {
            runtimeBridge.scheduleAbyssEvent();
        }
        if (runtimeBridge.isImperialTombEnabled()) {
            shugoImperialTombSpawnManager(runtimeBridge).start();
        }
    }

    /**
     * 解析术古皇陵刷怪管理器：优先 Spring 提供，否则回退桥接。
     * Resolve the Imperial Tomb spawn manager: prefer Spring, otherwise the bridge.
     *
     * @param runtimeBridge 运行时桥接 / Runtime bridge
     * @return 刷怪管理器 / Spawn manager
     */
    private ShugoImperialTombSpawnManager shugoImperialTombSpawnManager(GameMaintenanceServicesRuntimeBridge runtimeBridge) {
        if (shugoImperialTombSpawnManagerProvider == null) {
            return runtimeBridge.shugoImperialTombSpawnManager();
        }
        return shugoImperialTombSpawnManagerProvider.getIfAvailable(runtimeBridge::shugoImperialTombSpawnManager);
    }

    /**
     * 解析维护类运行时桥接。
     * Resolve the maintenance runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameMaintenanceServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameMaintenanceServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameMaintenanceServicesRuntimeBridge::new);
    }
}
