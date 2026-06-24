package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.model.GameEngine;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameEnginesLifecycle {

    private final GameEnginesGateway enginesGateway;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            enginesGateway.printSection();
            List<GameEngine> engines = List.copyOf(enginesGateway.engines());
            CountDownLatch progressLatch = new CountDownLatch(engines.size());
            for (GameEngine engine : engines) {
                enginesGateway.execute(() -> engine.load(progressLatch));
            }
            await(progressLatch);
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

    private void await(CountDownLatch progressLatch) {
        try {
            progressLatch.await();
        } catch (InterruptedException e) {
            log.warn("Main thread interrupted while waiting for engines", e);
            Thread.currentThread().interrupt();
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
