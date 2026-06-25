package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameNetworkStartupGateway {

    private ObjectProvider<ShutdownHook> shutdownHookProvider;
    private ObjectProvider<GameNetworkStartupRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameNetworkStartupRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void printNetworkSection() {
        Util.printSection(" *** Network *** ");
    }

    public void printMiscSection() {
        Util.printSection(" *** Misc *** ");
    }

    public boolean isBootEmbedded() {
        return runtimeBridge().isBootEmbedded();
    }

    public Thread shutdownHook() {
        if (shutdownHookProvider == null) {
            return runtimeBridge().shutdownHook();
        }
        ShutdownHook springShutdownHook = shutdownHookProvider.getIfAvailable();
        if (springShutdownHook != null) {
            return springShutdownHook;
        }
        return runtimeBridge().shutdownHook();
    }

    public void registerShutdownHook(Thread shutdownHook) {
        runtimeBridge().registerShutdownHook(shutdownHook);
    }

    public long currentTimeMillis() {
        return runtimeBridge().currentTimeMillis();
    }

    private GameNetworkStartupRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameNetworkStartupRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameNetworkStartupRuntimeBridge::new);
    }
}
