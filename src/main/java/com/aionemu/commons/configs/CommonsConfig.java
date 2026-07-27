package com.aionemu.commons.configs;

import com.aionemu.commons.configuration.Property;

/**
 * 通用配置类
 * Commons configuration class
 * <p>
 * 管理应用程序公共配置项
 * Manages common configuration settings for the application
 * </p>
 */
public class CommonsConfig {

    /**
     * 是否启用 Runnable 执行统计
     * Enable/disable runnable execution statistics collection
     * <p>
     * 启用后系统会收集可运行任务的执行统计信息
     * When enabled, the system collects execution statistics for runnable tasks
     * </p>
     */
    @Property(
        key = "gameserver.log.runnablestats",
        defaultValue = "false"
    )
    public static boolean RUNNABLESTATS_ENABLE;
}
