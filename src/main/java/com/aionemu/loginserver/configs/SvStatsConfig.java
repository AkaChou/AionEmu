package com.aionemu.loginserver.configs;

import com.aionemu.commons.configuration.Property;

/**
 * SvStats 统计开关配置。
 * SvStats statistics feature flags.
 */
public class SvStatsConfig {

    /**
     * 是否启用 SvStats 在线统计。
     * Whether SvStats online stats are enabled.
     */
    @Property(key = "svstats.enable_svstats", defaultValue = "false")
    public static boolean SVSTATS_ENABLE;
}
