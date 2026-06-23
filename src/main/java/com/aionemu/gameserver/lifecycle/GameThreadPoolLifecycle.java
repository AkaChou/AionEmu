package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.stereotype.Component;

@Component
public class GameThreadPoolLifecycle {

    private final Runnable startAction;
    private final Runnable stopAction;
    private boolean started;

    public GameThreadPoolLifecycle() {
        this(() -> ThreadPoolManager.getInstance(), () -> ThreadPoolManager.getInstance().shutdown());
    }

    GameThreadPoolLifecycle(Runnable startAction, Runnable stopAction) {
        this.startAction = startAction;
        this.stopAction = stopAction;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        startAction.run();
        started = true;
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        try {
            stopAction.run();
        } finally {
            started = false;
        }
    }

    public synchronized boolean isStarted() {
        return started;
    }
}
