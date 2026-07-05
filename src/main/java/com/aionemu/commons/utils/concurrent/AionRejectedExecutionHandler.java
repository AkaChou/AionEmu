package com.aionemu.commons.utils.concurrent;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
/**
 * 线程池拒绝策略处理器（Thread Pool Rejected Execution Handler）
 * 
 * 当线程池无法接受新任务时，在调用线程中执行任务以形成背压。
 * When the pool cannot accept a task, run it in the calling thread to apply backpressure.
 */
@Slf4j
public final class AionRejectedExecutionHandler implements RejectedExecutionHandler {
    

    /**
     * 拒绝任务处理方法（Rejected task handling method）
     * @param r 被拒绝的任务（Rejected task）
     * @param executor 关联的线程池（Related thread pool）
     */
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 检查线程池是否已关闭（Check if executor is shutdown）
        if (!executor.isShutdown()) {
            // 记录拒绝警告（Log rejection warning）
            log.warn("Task {} rejected from {}", r, executor);
            
            r.run();
        }
    }
}
