package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import org.springframework.stereotype.Component;

@Component
public class GameWorldActivationLifecycle {

    private final Runnable dropRegistration;
    private final Runnable playersOfflineMarker;
    private boolean activated;
    private long activationTimeMillis = -1;
    private Throwable lastFailure;

    public GameWorldActivationLifecycle() {
        this(
            () -> DropRegistrationService.getInstance(),
            () -> DAOManager.getDAO(PlayerDAO.class).setPlayersOffline(false)
        );
    }

    GameWorldActivationLifecycle(Runnable dropRegistration, Runnable playersOfflineMarker) {
        this.dropRegistration = dropRegistration;
        this.playersOfflineMarker = playersOfflineMarker;
    }

    public synchronized void start(Runnable activeServerSetter) {
        if (activated) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            dropRegistration.run();
            activeServerSetter.run();
            playersOfflineMarker.run();
            activated = true;
            lastFailure = null;
        } catch (RuntimeException | Error e) {
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
