package com.aionemu.chatserver.service;

import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChatRestartRequest {

    private static final Logger log = LoggerFactory.getLogger(ChatRestartRequest.class);

    private ChatRestartRequest() {
    }

    static void requestRestart() {
        requestRestart(new ChatProcessRuntimeBridge());
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
}
