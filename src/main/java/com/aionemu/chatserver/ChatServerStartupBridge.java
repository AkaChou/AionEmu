package com.aionemu.chatserver;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.commons.utils.AEInfos;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 聊天服启动阶段对配置、日志、进程桥与环境信息的桥接。
 * Bridge for chat-server startup steps: config, logging, process bridge, and environment info.
 */
@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatServerStartupBridge {

    private ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider;

    /**
     * 可选注入进程运行时桥的 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of the process runtime bridge.
     *
     * @param processBridgeProvider 进程桥提供者 / Process-bridge provider
     */
    @Autowired(required = false)
    void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        this.processBridgeProvider = processBridgeProvider;
    }

    /**
     * 初始化日志系统。
     * Initialize the logging system.
     */
    public void initializeLogger() {
        ChatServer.initializeLogger();
    }

    /**
     * 加载聊天服配置。
     * Load chat-server configuration.
     */
    public void loadConfig() {
        Config.load();
    }

    /**
     * 打印运行时环境信息。
     * Print runtime environment information.
     */
    public void printInfos() {
        AEInfos.printAllInfos();
    }

    /**
     * 是否处于 boot 嵌入式运行模式。
     * Whether running in boot-embedded mode.
     *
     * @return 嵌入式则为 true / {@code true} if boot-embedded
     */
    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    /**
     * 通过进程桥注册 JVM 关停钩子。
     * Register the JVM shutdown hook via the process bridge.
     */
    public void registerShutdownHook() {
        ChatProcessRuntimeBridge processBridge = processBridge();
        processBridge.registerShutdownHook(processBridge.shutdownHook());
    }

    /**
     * 当前系统时间毫秒。
     * Current system time in milliseconds.
     *
     * @return 毫秒时间戳 / Epoch millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 解析进程桥：优先 Spring 提供，否则新建。
     * Resolve the process bridge: prefer Spring provider, otherwise create a new one.
     *
     * @return 进程运行时桥 / Process runtime bridge
     */
    private ChatProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new ChatProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
    }
}
