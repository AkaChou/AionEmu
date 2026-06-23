package com.aionemu.commons.scripting.metadata;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定时任务注解，用于标记需要按照指定时间计划执行的类
 * Scheduling annotation, used to mark classes that need to be executed according to specified time schedules
 *
 * 该注解只能应用于类级别，用于配置定时任务的执行计划和行为
 * This annotation can only be applied at class level and is used to configure
 * the execution schedule and behavior of scheduled tasks
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Scheduled {
    /**
     * 定时任务的Cron表达式数组
     * Array of Cron expressions for the scheduled task
     */
    String[] value();

    /**
     * 是否为每个Cron表达式创建单独的实例
     * Whether to create separate instances for each Cron expression
     */
    boolean instancePerCronExpression() default false;

    /**
     * 是否禁用该定时任务
     * Whether this scheduled task is disabled
     */
    boolean disabled() default false;

    /**
     * 是否为长时间运行的任务
     * Whether this is a long-running task
     */
    boolean longRunningTask() default false;
}
