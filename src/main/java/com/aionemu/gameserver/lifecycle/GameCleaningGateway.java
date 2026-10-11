package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DatabaseCleaningService;
import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 清理网关：触发数据库与欧比斯排名清理服务。
 * Cleaning gateway: triggers database and abyss-rank cleaning services.
 */
@Component
public class GameCleaningGateway {

    /**
     * 数据库清理服务提供者。
     * Database cleaning service provider.
     */
    private ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider;

    /**
     * 欧比斯排名清理服务提供者。
     * Abyss-rank cleaning service provider.
     */
    private ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider;

    /**
     * 运维服务运行时桥提供者。
     * Maintenance-services runtime-bridge provider.
     */
    private ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入数据库清理服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of database cleaning service.
     * @param databaseCleaningServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setDatabaseCleaningServiceProvider(ObjectProvider<DatabaseCleaningService> databaseCleaningServiceProvider) {
        this.databaseCleaningServiceProvider = databaseCleaningServiceProvider;
    }

    /**
     * 可选注入欧比斯排名清理服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of abyss-rank cleaning service.
     * @param abyssRankCleaningServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setAbyssRankCleaningServiceProvider(ObjectProvider<AbyssRankCleaningService> abyssRankCleaningServiceProvider) {
        this.abyssRankCleaningServiceProvider = abyssRankCleaningServiceProvider;
    }

    /**
     * 可选注入运维服务运行时桥 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of maintenance-services runtime bridge.
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameMaintenanceServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 执行清理：解析并触发数据库与欧比斯排名清理服务。
     * Run cleaning: resolve and trigger database and abyss-rank cleaning services.
     */
    public void clean() {
        databaseCleaningService();
        abyssRankCleaningService();
    }

    /**
     * 触发数据库清理服务解析：优先 Spring，否则经运行时桥回退。
     * Trigger database cleaning service resolution: prefer Spring, otherwise fall back via runtime bridge.
     */
    private void databaseCleaningService() {
        if (databaseCleaningServiceProvider == null) {
            runtimeBridge().databaseCleaningService();
            return;
        }
        databaseCleaningServiceProvider.getIfAvailable(() -> runtimeBridge().databaseCleaningService());
    }

    /**
     * 触发欧比斯排名清理服务解析。
     * Trigger abyss-rank cleaning service resolution.
     */
    private void abyssRankCleaningService() {
        if (abyssRankCleaningServiceProvider == null) {
            runtimeBridge().abyssRankCleaningService();
            return;
        }
        abyssRankCleaningServiceProvider.getIfAvailable(() -> runtimeBridge().abyssRankCleaningService());
    }

    /**
     * 解析运维服务运行时桥：优先 Spring，否则新建。
     * Resolve maintenance-services runtime bridge: prefer Spring, otherwise create new.
     * @return 运行时桥 / Runtime bridge
     */
    private GameMaintenanceServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameMaintenanceServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameMaintenanceServicesRuntimeBridge::new);
    }
}
