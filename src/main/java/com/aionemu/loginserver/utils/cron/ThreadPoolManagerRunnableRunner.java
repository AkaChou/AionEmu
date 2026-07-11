package com.aionemu.loginserver.utils.cron;

import com.aionemu.commons.services.cron.RunnableRunner;
import com.aionemu.loginserver.service.LoginThreadPoolServices;

/**
 * Cron 任务的 RunnableRunner：把普通/长任务派发到登录服线程池。
 * RunnableRunner for cron jobs: dispatches normal and long-running tasks to the login-server thread pool.
 */
public class ThreadPoolManagerRunnableRunner extends RunnableRunner {

    /**
     * 在瞬时线程池中执行 cron 任务。
     * Executes a cron runnable on the instant pool.
     *
     * @param r 任务 / runnable
     */
    @Override
    public void executeRunnable(Runnable r) {
        LoginThreadPoolServices.threadPoolManager().execute(r);
    }

    /**
     * 在长任务线程池中执行 cron 任务。
     * Executes a long-running cron runnable on the long-running pool.
     *
     * @param r 任务 / runnable
     */
    @Override
    public void executeLongRunningRunnable(Runnable r) {
        LoginThreadPoolServices.threadPoolManager().executeLongRunning(r);
    }
}
