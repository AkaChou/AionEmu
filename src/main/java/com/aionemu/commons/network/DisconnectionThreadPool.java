package com.aionemu.commons.network;

/**
 * 断开连接线程池接口
 * Disconnection Thread Pool Interface
 *
 * 该接口定义了管理断开连接任务的线程池功能
 * This interface defines the functionality of a thread pool that manages disconnection tasks
 */
public interface DisconnectionThreadPool {

    /**
     * 调度一个断开连接任务
     * Schedule a disconnection task
     *
     * @param task 要执行的断开连接任务 / The disconnection task to execute
     * @param delay 延迟执行的时间（毫秒） / The delay in milliseconds before executing the task
     */
    void scheduleDisconnection(DisconnectionTask task, long delay);

    /**
     * 等待所有断开连接任务完成
     * Wait for all disconnection tasks to complete
     */
    void waitForDisconnectionTasks();
}
