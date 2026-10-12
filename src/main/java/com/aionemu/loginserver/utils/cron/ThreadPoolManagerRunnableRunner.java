package com.aionemu.loginserver.utils.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.aionemu.commons.services.cron.RunnableRunner;
import com.aionemu.loginserver.utils.ThreadPoolManager;

import lombok.RequiredArgsConstructor;

/**
 * Cron 任务的 RunnableRunner：把普通/长任务派发到登录服线程池。
 * RunnableRunner for cron jobs: dispatches normal and long-running tasks to the login-server thread pool.
 */
@Component
@Lazy
@RequiredArgsConstructor
public class ThreadPoolManagerRunnableRunner extends RunnableRunner {

    private final ThreadPoolManager threadPoolManager;

    /**
     * 在瞬时线程池中执行 cron 任务。
     * Executes a cron runnable on the instant pool.
     * @param r 任务 / runnable
     */
    @Override
    public void executeRunnable(Runnable r) {
        threadPoolManager.execute(r);
    }

    /**
     * 在长任务线程池中执行 cron 任务。
     * Executes a long-running cron runnable on the long-running pool.
     * @param r 任务 / runnable
     */
    @Override
    public void executeLongRunningRunnable(Runnable r) {
        threadPoolManager.executeLongRunning(r);
    }
}
