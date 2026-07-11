package com.aionemu.commons.network;

/**
 * 断开连接任务线程池接口。
 * Disconnection task thread pool interface.
 */
public interface DisconnectionThreadPool {

    /**
     * 调度断开连接任务。
     * Schedule a disconnection task.
     *
     * @param task 断开连接任务 / Disconnection task
     * @param delay 延迟毫秒 / Delay in milliseconds
     */
    void scheduleDisconnection(DisconnectionTask task, long delay);

    /**
     * 等待所有断开连接任务完成。
     * Wait for all disconnection tasks to complete.
     */
    void waitForDisconnectionTasks();
}
