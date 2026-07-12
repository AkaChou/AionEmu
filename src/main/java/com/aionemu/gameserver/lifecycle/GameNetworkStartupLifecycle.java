package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 网络启动生命周期：在启动序列中驱动网关、执行 serverStarter，并记录加载状态。
 * Network-startup lifecycle: drives the gateway with a serverStarter Runnable during startup and tracks load state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameNetworkStartupLifecycle {

    /**
     * 网络启动网关。
     * Network-startup gateway.
     */
    private final GameNetworkStartupGateway networkStartupGateway;
    /**
     * 是否已加载。
     * Whether this stage is loaded.
     */
    private boolean loaded;
    /**
     * 加载耗时毫秒；未启动前为 -1。
     * Load time in milliseconds; {@code -1} before start.
     */
    private long loadTimeMillis = -1;
    /**
     * 最近一次失败。
     * Last failure, if any.
     */
    private Throwable lastFailure;

    /**
     * 启动网络阶段：打印段落、运行 serverStarter，非 boot-embedded 时注册关闭钩子。
     * Start the network stage: print sections, run serverStarter, and register the shutdown hook when not boot-embedded.
     *
     * @param serverStarter 网络服务器启动回调 / Network server starter callback
     */
    public synchronized void start(Runnable serverStarter) {
        if (loaded) {
            return;
        }

        long start = networkStartupGateway.currentTimeMillis();
        try {
            networkStartupGateway.printNetworkSection();
            serverStarter.run();
            networkStartupGateway.printMiscSection();
            boolean bootEmbeddedMode = networkStartupGateway.isBootEmbedded();
            log.info(I18n.get(bootEmbeddedMode ? "log.71d26b100f77" : "log.0a68de7d6fac"));
            if (!bootEmbeddedMode) {
                networkStartupGateway.registerShutdownHook(networkStartupGateway.shutdownHook());
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = networkStartupGateway.currentTimeMillis() - start;
        }
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     *
     * @return {@code true} if loaded。
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     *
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     *
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
