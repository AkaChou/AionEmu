package com.aionemu.boot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code aion.game} 前缀的游戏服务 Boot 配置。
 * Boot configuration properties bound to the {@code aion.game} prefix.
 */
@Getter
@ConfigurationProperties(prefix = "aion.game")
public class AionGameProperties {

    private final Startup startup = new Startup();

    /**
     * 启动相关配置分组。
     * Nested configuration group for startup behavior.
     */
    @Getter
    public static class Startup {

        private final Progress progress = new Progress();
    }

    /**
     * 启动进度展示开关。
     * Toggle for startup progress reporting.
     */
    @Getter
    @Setter
    public static class Progress {

        private Boolean enabled;
    }
}
