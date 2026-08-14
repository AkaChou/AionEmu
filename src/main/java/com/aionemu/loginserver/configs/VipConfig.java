package com.aionemu.loginserver.configs;

import com.aionemu.commons.configuration.Property;

/**
 * 独立账号 VIP 配置。
 * Independent account VIP configuration.
 */
public final class VipConfig {

    /** 是否自动启用账号 VIP。 / Whether to auto-enable account VIP. */
    @Property(key = "loginserver.vip.auto-enable", defaultValue = "false")
    public static boolean AUTO_ENABLE;

    /** 自动启用 VIP 的等级。 / Level granted when auto-enabling VIP. */
    @Property(key = "loginserver.vip.auto-enable.level", defaultValue = "1")
    public static int AUTO_ENABLE_LEVEL;

    /** 是否启用 STS 服务。 / Whether the STS service is enabled. */
    @Property(key = "loginserver.vip.sts.enable", defaultValue = "false")
    public static boolean STS_ENABLE;

    /** STS 服务主机。 / STS service host. */
    @Property(key = "loginserver.vip.sts.host", defaultValue = "*")
    public static String STS_HOST;

    /** STS 服务端口。 / STS service port. */
    @Property(key = "loginserver.vip.sts.port", defaultValue = "6600")
    public static int STS_PORT;

    private VipConfig() {
    }

    /**
     * 校验配置值范围（VIP 等级与 STS 端口）。
     * Validates configuration value ranges (VIP level and STS port).
     */
    public static void validate() {
        if (AUTO_ENABLE && (AUTO_ENABLE_LEVEL < 1 || AUTO_ENABLE_LEVEL > 6)) {
            throw new IllegalArgumentException("loginserver.vip.auto-enable.level must be between 1 and 6");
        }
        if (STS_ENABLE && (STS_PORT <= 0 || STS_PORT > 65535)) {
            throw new IllegalArgumentException("loginserver.vip.sts.port must be between 1 and 65535");
        }
    }
}
