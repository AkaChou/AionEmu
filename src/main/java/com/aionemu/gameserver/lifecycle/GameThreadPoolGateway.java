package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameThreadPoolGateway {

    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameCoreServicesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        threadPoolManager();
    }

    public void stop() {
        threadPoolManager().shutdown();
    }

    private ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return GameThreadPoolServices.rememberThreadPoolManager(runtimeBridge().threadPoolManager());
        }
        return GameThreadPoolServices.rememberThreadPoolManager(
            threadPoolManagerProvider.getIfAvailable(() -> runtimeBridge().threadPoolManager())
        );
    }

    private GameCoreServicesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameCoreServicesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameCoreServicesRuntimeBridge::new);
    }
}
