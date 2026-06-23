package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameNetworkStartupLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GameNetworkStartupLifecycle.class);

    private final GameNetworkStartupGateway networkStartupGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start(Runnable serverStarter) {
        if (loaded) {
            return;
        }

        long start = networkStartupGateway.currentTimeMillis();
        try {
            networkStartupGateway.printNetworkSection();
            serverStarter.run();
            networkStartupGateway.printMiscSection();
            boolean bootEmbeddedMode = networkStartupGateway.isBootEmbedded();
            log.info(bootEmbeddedMode ? "Network transport started and external server connections scheduled" : "All network servers started successfully");
            if (!bootEmbeddedMode) {
                networkStartupGateway.registerShutdownHook(networkStartupGateway.shutdownHook());
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = networkStartupGateway.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
