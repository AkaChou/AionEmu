package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线程池网关：解析并启动/关闭 ThreadPoolManager。
 * Thread-pool gateway: resolves and starts/shuts down ThreadPoolManager.
 */
@Component
public class GameThreadPoolGateway {

    /**
     * ThreadPoolManager 的可选 Spring 提供者。
     * Optional Spring provider for {@link ThreadPoolManager}.
     */
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    /**
     * 核心服务运行时桥的可选提供者。
     * Optional provider for the core-services runtime bridge.
     */
    private ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入 ThreadPoolManager 提供者。
     * Inject the ThreadPoolManager provider.
     *
     * @param threadPoolManagerProvider ThreadPoolManager 提供者 / ThreadPoolManager provider
     */
    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
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
     * 启动线程池（解析 ThreadPoolManager 以触发初始化）。
     * Start the thread pool (resolve ThreadPoolManager to trigger initialization).
     */
    public void start() {
        threadPoolManager();
    }

    /**
     * 关闭线程池。
     * Shut down the thread pool.
     */
    public void stop() {
        threadPoolManager().shutdown();
    }

    /**
     * 解析 ThreadPoolManager 并缓存解析结果。
     * Resolve ThreadPoolManager and remember the resolved instance.
     *
     * @return ThreadPoolManager 实例 / ThreadPoolManager instance
     */
    private ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return GameThreadPoolServices.rememberThreadPoolManager(runtimeBridge().threadPoolManager());
        }
        return GameThreadPoolServices.rememberThreadPoolManager(
            threadPoolManagerProvider.getIfAvailable(() -> runtimeBridge().threadPoolManager())
        );
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
