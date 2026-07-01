package com.aionemu.chatserver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class ChatServerStartupSequence {

    private ChatServerStartupSequence() {
    }

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
        log.info("AL Chat Server started in " + (startupBridge.currentTimeMillis() - start) / 1000 + " seconds.");
    }
}
