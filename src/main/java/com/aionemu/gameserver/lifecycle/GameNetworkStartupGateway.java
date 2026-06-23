package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.ShutdownHook;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameNetworkStartupGateway {

    public void printNetworkSection() {
        Util.printSection(" *** Network *** ");
    }

    public void printMiscSection() {
        Util.printSection(" *** Misc *** ");
    }

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
