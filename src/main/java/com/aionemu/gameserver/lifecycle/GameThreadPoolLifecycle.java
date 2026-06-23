package com.aionemu.gameserver.lifecycle;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameThreadPoolLifecycle {

    private final GameThreadPoolGateway threadPoolGateway;
    private boolean started;

    public synchronized void start() {
        if (started) {
            return;
        }
        threadPoolGateway.start();
        started = true;
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        try {
            threadPoolGateway.stop();
        } finally {
            started = false;
        }
    }

    public synchronized boolean isStarted() {
        return started;
    }
}
