package com.aionemu.loginserver.configs;

import com.aionemu.commons.configuration.Property;

/**
 * Independent account VIP configuration.
 */
public final class VipConfig {

    @Property(key = "loginserver.vip.auto-enable", defaultValue = "false")
    public static boolean AUTO_ENABLE;

    @Property(key = "loginserver.vip.auto-enable.level", defaultValue = "1")
    public static int AUTO_ENABLE_LEVEL;

    @Property(key = "loginserver.vip.sts.enable", defaultValue = "false")
    public static boolean STS_ENABLE;

    @Property(key = "loginserver.vip.sts.host", defaultValue = "*")
    public static String STS_HOST;

    @Property(key = "loginserver.vip.sts.port", defaultValue = "6600")
    public static int STS_PORT;

    private VipConfig() {
    }

    public static void validate() {
        if (AUTO_ENABLE && (AUTO_ENABLE_LEVEL < 1 || AUTO_ENABLE_LEVEL > 6)) {
            throw new IllegalArgumentException("loginserver.vip.auto-enable.level must be between 1 and 6");
        }
        if (STS_ENABLE && (STS_PORT <= 0 || STS_PORT > 65535)) {
            throw new IllegalArgumentException("loginserver.vip.sts.port must be between 1 and 65535");
        }
    }
}
