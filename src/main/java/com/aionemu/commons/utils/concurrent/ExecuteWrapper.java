package com.aionemu.commons.utils.concurrent;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.configs.CommonsConfig;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务执行包装器：运行时统计与异常处理。
 * Task execution wrapper with runtime statistics and exception handling.
 *
 * @author NB4L1
 */
@Slf4j
public class ExecuteWrapper implements Executor {

    /**
     * 执行任务（默认无超时警告阈值）。
     * Execute a task with default no-warning threshold.
     *
     * @param runnable 待执行任务 / Task to execute
     */
    @Override
    public void execute(Runnable runnable) {
        execute(runnable, Long.MAX_VALUE);
    }

    /**
     * 执行任务并统计耗时；超过阈值时输出警告。
     * Execute a task with stats; warn when the threshold is exceeded.
     *
     * @param runnable 待执行任务 / Task to execute
     * @param maximumRuntimeInMillisecWithoutWarning 警告阈值（毫秒） / Warning threshold in milliseconds
     */
    public static void execute(Runnable runnable, long maximumRuntimeInMillisecWithoutWarning) {
        long begin = System.nanoTime();

        try {
            runnable.run();
        } catch (Throwable t) {
            log.warn(I18n.get("log.5a65654beccd"), t);
        } finally {
            long runtimeInNanosec = System.nanoTime() - begin;
            Class<? extends Runnable> clazz = runnable.getClass();

            if (CommonsConfig.RUNNABLESTATS_ENABLE) {
                RunnableStatsManager.handleStats(clazz, runtimeInNanosec);
            }

            long runtimeInMillisec = TimeUnit.NANOSECONDS.toMillis(runtimeInNanosec);
            if (runtimeInMillisec > maximumRuntimeInMillisecWithoutWarning) {
                log.warn(I18n.get("log.1f51677d7f54", clazz, runtimeInMillisec));
            }
        }
    }
}
