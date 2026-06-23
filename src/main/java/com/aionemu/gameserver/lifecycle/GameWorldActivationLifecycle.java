package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameWorldActivationLifecycle {

    private final GameWorldActivationGateway worldActivationGateway;
    private GameServer activeServer;
    private boolean activated;
    private long activationTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized GameServer start() {
        if (activated) {
            return activeServer;
        }

        long start = System.currentTimeMillis();
        try {
            activeServer = worldActivationGateway.activate();
            activated = true;
            lastFailure = null;
            return activeServer;
        } catch (RuntimeException | Error e) {
            activeServer = null;
            activated = false;
            lastFailure = e;
            throw e;
        } finally {
            activationTimeMillis = System.currentTimeMillis() - start;
        }
    }

    public synchronized boolean isActivated() {
        return activated;
    }

    public synchronized long getActivationTimeMillis() {
        return activationTimeMillis;
    }

    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
