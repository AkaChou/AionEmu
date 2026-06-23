package com.aionemu.commons.services.cron;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 任务执行器抽象类，用于处理定时任务的执行
 * Abstract task executor class for handling scheduled task execution
 *
 * 该类实现了Quartz的Job接口，提供了任务执行的基础框架：
 * This class implements Quartz's Job interface, providing a basic framework for task execution:
 * 1. 支持长短任务的区分执行
 *    Supports differentiated execution of long and short running tasks
 * 2. 通过JobDataMap传递任务参数
 *    Passes task parameters through JobDataMap
 * 3. 提供抽象方法由子类实现具体执行逻辑
 *    Provides abstract methods for subclasses to implement specific execution logic
 */
public abstract class RunnableRunner implements Job {
    
    /** 
     * 任务实例的JobDataMap键名
     * JobDataMap key for runnable instance 
     */
    public static final String KEY_RUNNABLE_OBJECT = "cronservice.scheduled.runnable.instance";
    
    /** 
     * 是否为长时任务的JobDataMap键名
     * JobDataMap key for long-running task flag 
     */
    public static final String KEY_PROPERTY_IS_LONGRUNNING_TASK = "cronservice.scheduled.runnable.islognrunning";
    
    /** 
     * Cron表达式的JobDataMap键名
     * JobDataMap key for cron expression 
     */
    public static final String KEY_CRON_EXPRESSION = "cronservice.scheduled.runnable.cronexpression";

    /**
     * 执行定时任务
     * Execute scheduled task
     *
     * @param context 任务执行上下文 Task execution context
     * @throws JobExecutionException 任务执行异常 Task execution exception
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jdm = context.getJobDetail().getJobDataMap();
        Runnable r = (Runnable)jdm.get(KEY_RUNNABLE_OBJECT);
        boolean longRunning = jdm.getBoolean(KEY_PROPERTY_IS_LONGRUNNING_TASK);
        
        if (longRunning) {
            this.executeLongRunningRunnable(r);
        } else {
            this.executeRunnable(r);
        }
    }

    /**
     * 执行普通任务
     * Execute normal task
     *
     * @param r 要执行的任务 Task to execute
     */
    public abstract void executeRunnable(Runnable r);

    /**
     * 执行长时任务
     * Execute long-running task
     *
     * @param r 要执行的任务 Task to execute
     */
    public abstract void executeLongRunningRunnable(Runnable r);
}
