package com.aionemu.chatserver.service;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionEmbeddedShutdownMode;
import com.aionemu.commons.utils.AionRuntimeMode;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 聊天服重启请求工具：按独立/嵌入式运行模式触发重启流程。
 * Chat-server restart request utility: triggers restart according to standalone/embedded runtime mode.
 */
@Slf4j
@UtilityClass
public class ChatRestartRequest {

    private static volatile ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider;

    /**
     * 设置进程运行时桥接的 Spring 提供者。
     * Set the Spring provider for the process runtime bridge.
     *
     * @param processBridgeProvider 进程桥接提供者 / Process bridge provider
     */
    public void setProcessBridgeProvider(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        ChatRestartRequest.processBridgeProvider = processBridgeProvider;
    }

    /**
     * 使用当前进程桥接请求重启。
     * Request restart using the current process bridge.
     */
    static void requestRestart() {
        requestRestart(processBridge());
    }

    /**
     * 使用指定进程桥接请求重启。
     * Request restart using the given process bridge.
     *
     * @param processBridge 进程运行时桥接 / Process runtime bridge
     */
    static void requestRestart(ChatProcessRuntimeBridge processBridge) {
        ShutdownHook.setRestartOnly(true);
        if (!AionRuntimeMode.isBootEmbedded()) {
            processBridge.shutdownHook().start();
            return;
        }
        if (!AionEmbeddedShutdownHandler.requestShutdown(AionEmbeddedShutdownMode.RESTART)) {
            log.warn(I18n.get("log.7803d2357e2b"));
            processBridge.shutdown(false);
        }
    }

    /**
     * 解析可用的进程运行时桥接。
     * Resolve an available process runtime bridge.
     *
     * @return 进程桥接实例 / Process bridge instance
     */
    private ChatProcessRuntimeBridge processBridge() {
        if (processBridgeProvider == null) {
            return new ChatProcessRuntimeBridge();
        }
        return processBridgeProvider.getIfAvailable(ChatProcessRuntimeBridge::new);
    }
}
