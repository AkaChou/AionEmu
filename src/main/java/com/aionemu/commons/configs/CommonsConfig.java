package com.aionemu.commons.configs;

import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通用配置类，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * <p>
 * 管理应用程序公共配置项。
 * </p>
 * Commons configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * <p>
 * Manages common configuration settings for the application.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "gameserver.log")
public class CommonsConfig {

    /**
     * 是否启用 Runnable 执行统计
     * <p>
     * 启用后系统会收集可运行任务的执行统计信息
     * </p>
     * Enable/disable runnable execution statistics collection.
     * <p>
     * When enabled, the system collects execution statistics for runnable tasks
     * </p>
     */
    @Property(
        key = "gameserver.log.runnablestats",
        defaultValue = "false"
    )
    public static boolean RUNNABLESTATS_ENABLE;

    private boolean runnablestats;

    /**
     * 设置是否启用执行统计，并同步更新静态字段。
     * Sets whether runnable stats are enabled and synchronizes the static field.
     * @param runnablestats 是否启用 / whether enabled
     */
    public void setRunnablestats(boolean runnablestats) {
        this.runnablestats = runnablestats;
        RUNNABLESTATS_ENABLE = runnablestats;
    }

    public boolean isRunnablestats() {
        return RUNNABLESTATS_ENABLE;
    }
}
