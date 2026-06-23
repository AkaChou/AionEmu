package com.aionemu.commons.configs;

import com.aionemu.commons.configuration.Property;

/**
 * Commons Configuration Class
 * 通用配置类
 * <p>
 * This class manages common configuration settings for the application.
 * 该类管理应用程序的通用配置设置。
 * </p>
 */
public class CommonsConfig {
    /**
     * Enable/disable runnable statistics collection
     * 启用/禁用可运行统计信息收集
     * <p>
     * When enabled, the system will collect execution statistics for runnable tasks.
     * 启用后，系统将收集可运行任务的执行统计信息。
     * </p>
     */
    @Property(
        key = "commons.runnablestats.enable",
        defaultValue = "false"
    )
    public static boolean RUNNABLESTATS_ENABLE;
}
