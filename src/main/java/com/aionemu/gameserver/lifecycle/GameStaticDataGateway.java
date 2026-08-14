package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 静态数据网关：触发 DataManager 解析与加载。
 * Static-data gateway: triggers DataManager resolution and load.
 */
@Component
public class GameStaticDataGateway {

    /**
     * DataManager 的可选 Spring 提供者。
     * Optional Spring provider for {@link DataManager}.
     */
    private ObjectProvider<DataManager> dataManagerProvider;

    /**
     * 核心服务运行时桥的可选提供者。
     * Optional provider for the core-services runtime bridge.
     */
    private ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入 DataManager 提供者。
     * Inject the DataManager provider.
     *
     * @param dataManagerProvider DataManager 提供者 / DataManager provider
     */
    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }

    /**
     * 注入核心服务运行时桥提供者。
     * Inject the core-services runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 加载静态数据（解析 DataManager 以触发初始化）。
     * Load static data (resolve DataManager to trigger initialization).
     */
    public void load() {
        dataManager();
    }

    /**
     * 解析 DataManager：优先 Spring，否则走运行时桥。
     * Resolve DataManager: prefer Spring, otherwise the runtime bridge.
     *
     * @return DataManager 实例 / DataManager instance
     */
    private DataManager dataManager() {
        if (dataManagerProvider == null) {
            return runtimeBridge().dataManager();
        }
        return dataManagerProvider.getIfAvailable(() -> runtimeBridge().dataManager());
    }

    /**
     * 解析核心服务运行时桥：优先 Spring，否则新建。
     * Resolve the core-services runtime bridge: prefer Spring, otherwise create new.
     *
     * @return 运行时桥 / Runtime bridge
     */
    private GameCoreServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameCoreServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameCoreServicesRuntimeBridge::new);
    }
}
