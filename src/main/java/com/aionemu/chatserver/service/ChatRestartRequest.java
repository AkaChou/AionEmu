package com.aionemu.chatserver.service;

import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChatRestartRequest {

    private static final Logger log = LoggerFactory.getLogger(ChatRestartRequest.class);
    private static volatile ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider;

    private ChatRestartRequest() {
    }

    public static void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        ChatRestartRequest.processBridgeProvider = processBridgeProvider;
    }

    static void requestRestart() {
        requestRestart(processBridge());
    }

    static void requestRestart(ChatProcessRuntimeBridge processBridge) {
        ShutdownHook.setRestartOnly(true);
        if (!AionRuntimeMode.isBootEmbedded()) {
            processBridge.shutdownHook().start();
            return;
        }
        if (!AionEmbeddedShutdownHandler.requestShutdown(AionEmbeddedShutdownMode.RESTART)) {
            log.warn("Embedded shutdown handler is not registered; stopping ChatServer directly.");
            processBridge.shutdown(false);
        }
    }

    private static ChatProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new ChatProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
    }
}
