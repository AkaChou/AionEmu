package com.aionemu.commons.utils.concurrent;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 线程池拒绝策略处理器（Thread Pool Rejected Execution Handler）
 * 
 * 当线程池无法接受新任务时，根据当前线程优先级决定执行方式：
 * - 高优先级线程(>5)创建新线程执行
 * - 低优先级线程直接在当前线程执行
 */
public final class AionRejectedExecutionHandler implements RejectedExecutionHandler {
    
    // 日志记录器（Logger instance）
    private static final Logger log = LoggerFactory.getLogger(AionRejectedExecutionHandler.class);

    /**
     * 拒绝任务处理方法（Rejected task handling method）
     * @param r 被拒绝的任务（Rejected task）
     * @param executor 关联的线程池（Related thread pool）
     */
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 检查线程池是否已关闭（Check if executor is shutdown）
        if (!executor.isShutdown()) {
            // 记录拒绝警告（Log rejection warning）
            log.warn("Task {} rejected from {}", r, executor, new RejectedExecutionException());
            
            /*
             * 根据当前线程优先级选择执行方式：
             * - 优先级>5：创建新线程执行
             * - 优先级≤5：直接在当前线程执行
             * 
             * Execution strategy based on current thread priority:
             * - Priority >5: Execute in new thread
             * - Priority ≤5: Execute in current thread
             */
            if (Thread.currentThread().getPriority() > 5) {
                new Thread(r).start();
            } else {
                r.run();
            }
        }
    }
}
