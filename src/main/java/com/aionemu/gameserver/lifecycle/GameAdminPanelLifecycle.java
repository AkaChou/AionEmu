package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameAdminPanelLifecycle {

    private final GameAdminPanelGateway adminPanelGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = adminPanelGateway.currentTimeMillis();
        try {
            if (adminPanelGateway.isAdminPanelEnabled()) {
                adminPanelGateway.startAdminPanel();
            }
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = adminPanelGateway.currentTimeMillis() - start;
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
