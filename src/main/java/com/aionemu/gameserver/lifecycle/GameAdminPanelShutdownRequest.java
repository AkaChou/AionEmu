package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.boot.lifecycle.AionProcessRuntimeBridge;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.gameserver.GameServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 管理面板关停请求：嵌入式走嵌入式关停，否则经进程运行时桥正常退出。
 * Admin-panel shutdown request: embedded path uses embedded handler, otherwise normal exit via process runtime bridge.
 */
@Component
@Slf4j
public final class GameAdminPanelShutdownRequest implements DisposableBean {

    /**
     * 进程运行时桥 {@link ObjectProvider} 的静态缓存。
     * Static cache of the process-runtime-bridge {@link ObjectProvider}.
     */
    private static volatile ObjectProvider<AionProcessRuntimeBridge> processRuntimeBridgeProvider;

    /**
     * 构造并注入进程运行时桥提供者。
     * Construct and inject the process-runtime-bridge provider.
     *
     * @param processRuntimeBridgeProvider 进程运行时桥提供者 / Process-runtime-bridge provider
     */
    @Autowired
    public GameAdminPanelShutdownRequest(ObjectProvider<AionProcessRuntimeBridge> processRuntimeBridgeProvider) {
        setProcessRuntimeBridgeProvider(processRuntimeBridgeProvider);
    }

    /**
     * 设置进程运行时桥提供者（静态访问）。
     * Set the process-runtime-bridge provider (static access).
     *
     * @param processRuntimeBridgeProvider 进程运行时桥提供者 / Process-runtime-bridge provider
     */
    public static void setProcessRuntimeBridgeProvider(ObjectProvider<AionProcessRuntimeBridge> processRuntimeBridgeProvider) {
        GameAdminPanelShutdownRequest.processRuntimeBridgeProvider = processRuntimeBridgeProvider;
    }

    /**
     * 请求关停：嵌入式模式优先嵌入式处理器，否则正常退出。
     * Request shutdown: prefer embedded handler in embedded mode, otherwise normal exit.
     */
    public static void shutdown() {
        if (AionRuntimeMode.isBootEmbedded()) {
            if (!AionEmbeddedShutdownHandler.requestShutdown()) {
                log.warn(I18n.get("shutdown.embedded_handler_missing"));
                GameServer.stop();
            }
            return;
        }
        processRuntimeBridge().exit(ExitCode.CODE_NORMAL);
    }

    /**
     * 销毁时清空静态提供者。
     * Clear the static provider on destroy.
     */
    @Override
    public void destroy() {
        processRuntimeBridgeProvider = null;
    }

    /**
     * 解析进程运行时桥：优先 Spring 提供，否则新建。
     * Resolve the process runtime bridge: prefer Spring provider, otherwise create new.
     *
     * @return 进程运行时桥 / Process runtime bridge
     */
    private static AionProcessRuntimeBridge processRuntimeBridge() {
        ObjectProvider<AionProcessRuntimeBridge> provider = processRuntimeBridgeProvider;
        if (provider == null) {
            return new AionProcessRuntimeBridge();
        }
        return provider.getIfAvailable(AionProcessRuntimeBridge::new);
    }
}
