package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class GameWorldActivationLifecycle {

    private final Runnable dropRegistration;
    private final Supplier<GameServer> serverSupplier;
    private final Consumer<GameServer> activeServerSetter;
    private final Runnable playersOfflineMarker;
    private GameServer activeServer;
    private boolean activated;
    private long activationTimeMillis = -1;
    private Throwable lastFailure;

    public GameWorldActivationLifecycle() {
        this(
            () -> DropRegistrationService.getInstance(),
            GameServer::new,
            GameServer::activateServer,
            () -> DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false)
        );
    }

    GameWorldActivationLifecycle(
        Runnable dropRegistration,
        Supplier<GameServer> serverSupplier,
        Consumer<GameServer> activeServerSetter,
        Runnable playersOfflineMarker
    ) {
        this.dropRegistration = dropRegistration;
        this.serverSupplier = serverSupplier;
        this.activeServerSetter = activeServerSetter;
        this.playersOfflineMarker = playersOfflineMarker;
    }

    public synchronized GameServer start() {
        if (activated) {
            return activeServer;
        }

        long start = System.currentTimeMillis();
        try {
            dropRegistration.run();
            GameServer server = serverSupplier.get();
            activeServerSetter.accept(server);
            playersOfflineMarker.run();
            activeServer = server;
            activated = true;
            lastFailure = null;
            return server;
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
