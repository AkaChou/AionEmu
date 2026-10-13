package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.AionRuntimeMode;
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
     * 可选注入关闭钩子提供者。
     * Optionally inject the shutdown-hook provider.
     * @param shutdownHookProvider 关闭钩子提供者 / Shutdown-hook provider
     */
    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
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
     * @return 处于 boot-embedded 模式为 {@code true} / {@code true} if boot-embedded
     */
    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    /**
     * 解析关闭钩子线程。
     * Resolve the shutdown-hook thread.
     * @return 关闭钩子线程 / Shutdown-hook thread
     */
    public Thread shutdownHook() {
        return shutdownHookProvider.getObject();
    }

    /**
     * 向 JVM 注册关闭钩子。
     * Register the shutdown hook with the JVM.
     * @param shutdownHook 关闭钩子线程 / Shutdown-hook thread
     */
    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * 返回当前时间毫秒数。
     * Return the current time in milliseconds.
     * @return 当前时间毫秒 / Current time millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
