package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameNetworkStartupRuntimeBridge {

    private ObjectProvider<ShutdownHook> shutdownHookProvider;

    @Autowired(required = false)
    void setShutdownHookProvider(ObjectProvider<ShutdownHook> shutdownHookProvider) {
        this.shutdownHookProvider = shutdownHookProvider;
    }

    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    public Thread shutdownHook() {
        if (shutdownHookProvider == null) {
            return ShutdownHook.getInstance();
        }
        return shutdownHookProvider.getIfAvailable(ShutdownHook::getInstance);
    }

    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
