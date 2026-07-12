package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.GameEngine;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 游戏引擎生命周期：并行加载任务 / 副本 / AI2 / 聊天等引擎。
 * instance / AI2 / chat engines in parallel.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameEnginesLifecycle {

    /**
     * 引擎网关委托入口。
     * Engines gateway delegation entry.
     */
    private final GameEnginesGateway enginesGateway;
    /**
     * 是否已成功加载。
     * Whether loading has completed successfully.
     */
    private boolean loaded;
    /**
     * 最近一次加载耗时（毫秒）；未启动为 -1。
     * Last load duration in milliseconds; -1 if never started.
     */
    private long loadTimeMillis = -1;
    /**
     * 最近一次失败原因；成功时为 {@code null}。
     * Last failure cause; {@code null} on success.
     */
    private Throwable lastFailure;

    /**
     * 启动并行加载全部游戏引擎（幂等）。
     * Start and load all game engines in parallel (idempotent).
     */
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

    /**
     * 等待全部引擎加载完成；中断时记录警告并恢复中断标志。
     * Await completion of all engine loads; on interrupt log a warning and restore the interrupt flag.
     *
     * Progress latch
     */
    private void await(CountDownLatch progressLatch) {
        try {
            progressLatch.await();
        } catch (InterruptedException e) {
            log.warn(I18n.get("console.startup.engines_interrupted"), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 是否已成功加载。
     * Whether loading has completed successfully.
     *
     * @return {@code true} if loaded。
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

    /**
     * 最近一次加载耗时（毫秒）。
     * Last load duration in milliseconds.
     *
     * @return 耗时毫秒数；未启动为 -1 / Duration ms; -1 if never started
     */
    public synchronized long getLoadTimeMillis() {
        return loadTimeMillis;
    }

    /**
     * 最近一次失败原因。
     * Last failure cause.
     *
     * @return 失败异常；成功为 {@code null} / Failure throwable; {@code null} on success
     */
    public synchronized Throwable getLastFailure() {
        return lastFailure;
    }
}
