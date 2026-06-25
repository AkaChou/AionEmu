package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameNetworkStartupRuntimeBridge {

    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    public Thread shutdownHook() {
        return ShutdownHook.getInstance();
    }

    public void registerShutdownHook(Thread shutdownHook) {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
