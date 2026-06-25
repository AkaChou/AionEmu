package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.lifecycle.AionProcessRuntimeBridge;
import com.aionemu.commons.utils.AionEmbeddedShutdownHandler;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.commons.utils.ExitCode;
import com.aionemu.gameserver.GameServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class GameAdminPanelShutdownRequest implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GameAdminPanelShutdownRequest.class);
    private static volatile ObjectProvider<AionProcessRuntimeBridge> processRuntimeBridgeProvider;

    @Autowired
    public GameAdminPanelShutdownRequest(ObjectProvider<AionProcessRuntimeBridge> processRuntimeBridgeProvider) {
        setProcessRuntimeBridgeProvider(processRuntimeBridgeProvider);
    }

    public static void setProcessRuntimeBridgeProvider(ObjectProvider<AionProcessRuntimeBridge> processRuntimeBridgeProvider) {
        GameAdminPanelShutdownRequest.processRuntimeBridgeProvider = processRuntimeBridgeProvider;
    }

    public static void shutdown() {
        if (AionRuntimeMode.isBootEmbedded()) {
            if (!AionEmbeddedShutdownHandler.requestShutdown()) {
                log.warn("Embedded shutdown handler is not registered; stopping GameServer directly.");
                GameServer.stop();
            }
            return;
        }
        processRuntimeBridge().exit(ExitCode.CODE_NORMAL);
    }

    @Override
    public void destroy() {
        processRuntimeBridgeProvider = null;
    }

    private static AionProcessRuntimeBridge processRuntimeBridge() {
        ObjectProvider<AionProcessRuntimeBridge> provider = processRuntimeBridgeProvider;
        if (provider == null) {
            return new AionProcessRuntimeBridge();
        }
        return provider.getIfAvailable(AionProcessRuntimeBridge::new);
    }
}
