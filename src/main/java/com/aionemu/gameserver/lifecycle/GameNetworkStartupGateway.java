package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 网络启动网关：打印网络/杂项段落、解析并注册关闭钩子、检测 boot-embedded 模式。
 * Network-startup gateway: prints network/misc sections, resolves and registers the shutdown hook, and checks boot-embedded mode.
 */
@Component
public class GameNetworkStartupGateway {

    /**
     * 关闭钩子提供者。
     * Shutdown-hook provider.
     */
    private ObjectProvider<ShutdownHook> shutdownHookProvider;
    /**
     * 网络启动运行时桥提供者。
     * Network-startup runtime-bridge provider.
     */
    private ObjectProvider<GameNetworkStartupRuntimeBridge> runtimeBridgeProvider;

    /**
     * 可选注入关闭钩子提供者。
     * Optionally inject the shutdown-hook provider.
     *
     * @param shutdownHookProvider 关闭钩子提供者 / Shutdown-hook provider
     */
    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    /**
     * 可选注入运行时桥提供者。
     * Optionally inject the runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥提供者 / Runtime-bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameNetworkStartupRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 打印网络段落标题。
     * Print the network section header.
     */
    public void printNetworkSection() {
        Util.printSection(I18n.get("console.section.network"));
    }

    /**
     * 打印杂项段落标题。
     * Print the misc section header.
     */
    public void printMiscSection() {
        Util.printSection(I18n.get("console.section.misc"));
    }

    /**
     * 是否处于 boot-embedded 运行模式。
     * Whether the process is in boot-embedded mode.
     *
     * @return {@code true} if boot-embedded。
     */
    public boolean isBootEmbedded() {
        return runtimeBridge().isBootEmbedded();
    }

    /**
     * 解析关闭钩子线程：优先 Spring 提供者，否则运行时桥回退。
     * Resolve the shutdown-hook thread: prefer Spring provider, otherwise runtime-bridge fallback.
     *
     * @return 关闭钩子线程 / Shutdown-hook thread
     */
    public Thread shutdownHook() {
        if (shutdownHookProvider == null) {
            return runtimeBridge().shutdownHook();
        }
        ShutdownHook springShutdownHook = shutdownHookProvider.getIfAvailable();
        if (springShutdownHook != null) {
            return springShutdownHook;
        }
        return runtimeBridge().shutdownHook();
    }

    /**
     * 向 JVM 注册关闭钩子。
     * Register the shutdown hook with the JVM.
     *
     * @param shutdownHook 关闭钩子线程 / Shutdown-hook thread
     */
    public void registerShutdownHook(Thread shutdownHook) {
        runtimeBridge().registerShutdownHook(shutdownHook);
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     *
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return runtimeBridge().currentTimeMillis();
    }

    /**
     * 解析网络启动运行时桥：优先 Spring 提供者，否则新建。
     * Resolve the network-startup runtime bridge: prefer Spring provider, otherwise a new instance.
     *
     * Runtime bridge
     */
    private GameNetworkStartupRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameNetworkStartupRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameNetworkStartupRuntimeBridge::new);
    }
}
