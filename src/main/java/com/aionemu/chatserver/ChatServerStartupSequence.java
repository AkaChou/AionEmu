package com.aionemu.chatserver;


import com.aionemu.boot.i18n.I18n;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天服务器启动序列：日志、配置、核心服务与关停钩子。
 * Chat-server startup sequence: logging, config, core services, and shutdown hook.
 */
@Slf4j
@UtilityClass
final class ChatServerStartupSequence {

    /**
     * 按依赖契约执行完整启动步骤并记录耗时。
     * Run the full startup steps via the dependency contract and log elapsed time.
     *
     * @param dependencies 启动依赖 / Startup dependencies
     */
    static void start(ChatServerDependencies dependencies) {
        ChatServerStartupBridge startupBridge = dependencies.startupBridge();
        long start = startupBridge.currentTimeMillis();

        startupBridge.initializeLogger();

        startupBridge.loadConfig();
        startupBridge.printInfos();
        dependencies.idFactory();
        dependencies.gameServerService();
        dependencies.broadcastService();
        dependencies.chatService();
        dependencies.nettyServer();
        dependencies.restartService();

        if (!startupBridge.isBootEmbedded()) {
            startupBridge.registerShutdownHook();
        }
        log.info(I18n.get("log.e8ea76d87691", (startupBridge.currentTimeMillis() - start) / 1000));
    }
}
