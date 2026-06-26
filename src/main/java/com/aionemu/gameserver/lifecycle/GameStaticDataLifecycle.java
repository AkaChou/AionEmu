package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameStaticDataLifecycle {

    private final GameStaticDataGateway staticDataGateway;
    private ObjectProvider<GameMovementLoopGateway> movementLoopGatewayProvider;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            staticDataGateway.load();
            movementLoopGateway().initialize();
            loaded = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
            loaded = false;
            lastFailure = e;
            throw e;
        } finally {
            loadTimeMillis = System.currentTimeMillis() - start;
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

    @Autowired(required = false)
    void setMovementLoopGatewayProvider(ObjectProvider<GameMovementLoopGateway> movementLoopGatewayProvider) {
        this.movementLoopGatewayProvider = movementLoopGatewayProvider;
    }

    private GameMovementLoopGateway movementLoopGateway() {
        if (movementLoopGatewayProvider == null) {
            return new GameMovementLoopGateway();
        }
        return movementLoopGatewayProvider.getIfAvailable(GameMovementLoopGateway::new);
    }
}
