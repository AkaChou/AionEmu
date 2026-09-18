package com.aionemu.loginserver.configs;

import com.aionemu.commons.configuration.Property;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SvStats 统计开关配置，支持 Spring Boot ConfigurationProperties 与静态委托。
 * SvStats statistics feature flags supporting Spring Boot ConfigurationProperties and static delegation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "svstats")
public class SvStatsConfig {

    /**
     * 是否启用 SvStats 在线统计。
     * Whether SvStats online stats are enabled.
     */
    @Property(key = "svstats.enable_svstats", defaultValue = "false")
    public static boolean SVSTATS_ENABLE;

    private boolean enableSvstats;

    /**
     * 设置是否启用 SvStats，并同步更新静态字段。
     * Sets whether SvStats is enabled and synchronizes the static field.
     *
     * @param enableSvstats 是否启用 / whether enabled
     */
    public void setEnableSvstats(boolean enableSvstats) {
        this.enableSvstats = enableSvstats;
        SVSTATS_ENABLE = enableSvstats;
    }

    public boolean isEnableSvstats() {
        return SVSTATS_ENABLE;
    }
}
