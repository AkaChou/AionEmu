package com.aionemu.commons.utils.concurrent;

import com.aionemu.boot.i18n.I18n;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * 线程池拒绝策略：在调用线程执行任务形成背压。
 * Pool rejection policy: run the task in the caller for backpressure.
 */
@Slf4j
public final class AionRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
     * 处理被拒绝的任务。
     * Handle a rejected task.
     *
     * @param r        被拒绝任务 / Rejected task
     * @param executor 线程池 / Thread pool
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (!executor.isShutdown()) {
            log.warn(I18n.get("log.5776856c1d75", r, executor));
            r.run();
        }
    }
}
