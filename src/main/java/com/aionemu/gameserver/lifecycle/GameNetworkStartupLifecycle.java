package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 网络启动生命周期：打印网络/杂项段落、执行 serverStarter、解析并注册关闭钩子，并记录加载状态。
 * Network-startup lifecycle: prints network/misc sections, runs serverStarter, resolves and
 * registers the shutdown hook, and tracks load state.
 */
@Component
@Slf4j
public class GameNetworkStartupLifecycle {

    /**
     * 关闭钩子提供者。
     * Shutdown-hook provider.
     */
    private ObjectProvider<ShutdownHook> shutdownHookProvider;

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
     * 可选注入关闭钩子提供者。
     * Optionally inject the shutdown-hook provider.
     * @param shutdownHookProvider 关闭钩子提供者 / Shutdown-hook provider
     */
    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    /**
     * 启动网络阶段：打印段落、运行 serverStarter，非 boot-embedded 时注册关闭钩子。
     * Start the network stage: print sections, run serverStarter, and register the shutdown hook when not boot-embedded.
     * @param serverStarter 网络服务器启动回调 / Network server starter callback
     */
    public synchronized void start(Runnable serverStarter) {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            printNetworkSection();
            serverStarter.run();
            printMiscSection();
            boolean bootEmbeddedMode = isBootEmbedded();
            log.info(I18n.get(bootEmbeddedMode ? "log.71d26b100f77" : "log.0a68de7d6fac"));
            if (!bootEmbeddedMode) {
                registerShutdownHook(shutdownHook());
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
        }
    }

    /**
     * 打印网络段落标题。
     * Print the network section header.
     */
    protected void printNetworkSection() {
        Util.printSection(I18n.get("console.section.network"));
    }

    /**
     * 打印杂项段落标题。
     * Print the misc section header.
     */
    protected void printMiscSection() {
        Util.printSection(I18n.get("console.section.misc"));
    }

    /**
     * 是否处于 boot-embedded 运行模式。
     * Whether the process is in boot-embedded mode.
     * @return 处于 boot-embedded 模式为 {@code true} / {@code true} if boot-embedded
     */
    protected boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    /**
     * 解析关闭钩子线程。
     * Resolve the shutdown-hook thread.
     * @return 关闭钩子线程 / Shutdown-hook thread
     */
    protected Thread shutdownHook() {
        return shutdownHookProvider.getObject();
    }

    /**
     * 向 JVM 注册关闭钩子。
     * Register the shutdown hook with the JVM.
     * @param shutdownHook 关闭钩子线程 / Shutdown-hook thread
     */
    protected void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * 是否已加载。
     * Whether this stage is loaded.
     * @return 已加载为 {@code true} / {@code true} if loaded
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 加载耗时毫秒。
     * Load time in milliseconds.
     * @return 耗时毫秒，未启动为 -1 / Elapsed millis, or {@code -1} if not started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近失败。
     * Last failure.
     * @return 最近异常，无则为 null / Last throwable, or {@code null}
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
