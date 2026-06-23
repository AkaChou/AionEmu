package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameEnginesLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GameEnginesLifecycle.class);

    private final Supplier<List<GameEngine>> enginesSupplier;
    private final Consumer<Runnable> taskExecutor;
    private boolean loaded;
    private long loadTimeMillis = -1;
    private Throwable lastFailure;

    public GameEnginesLifecycle() {
        this(
            () -> List.of(
                QuestEngine.getInstance(),
                InstanceEngine.getInstance(),
                AI2Engine.getInstance(),
                ChatProcessor.getInstance()
            ),
            runnable -> ThreadPoolManager.getInstance().execute(runnable)
        );
    }

    GameEnginesLifecycle(Supplier<List<GameEngine>> enginesSupplier, Consumer<Runnable> taskExecutor) {
        this.enginesSupplier = enginesSupplier;
        this.taskExecutor = taskExecutor;
    }

    public synchronized void start() {
        if (loaded) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            Util.printSection(" *** Engines *** ");
            List<GameEngine> engines = List.copyOf(enginesSupplier.get());
            CountDownLatch progressLatch = new CountDownLatch(engines.size());
            for (GameEngine engine : engines) {
                taskExecutor.accept(() -> engine.load(progressLatch));
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
