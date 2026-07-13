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

    /** Enable the STS VIP stage endpoint used by China client UI. */
    @Property(key = "loginserver.vip.sts.enable", defaultValue = "false")
    public static boolean STS_ENABLE;

    /** Bind host for STS VIP endpoint ("*" = all interfaces). */
    @Property(key = "loginserver.vip.sts.host", defaultValue = "*")
    public static String STS_HOST;

    /** Bind port for STS VIP endpoint. */
    @Property(key = "loginserver.vip.sts.port", defaultValue = "6600")
    public static int STS_PORT;

    /** AppGroupCode returned in Level/GetLevel replies. */
    @Property(key = "loginserver.vip.sts.app-group", defaultValue = "AION")
    public static String STS_APP_GROUP;

    /**
     * Fallback AccumulateGradeScore when account VIP is missing.
     * 0 means "no membership stage".
     */
    @Property(key = "loginserver.vip.sts.default-score", defaultValue = "0")
    public static long STS_DEFAULT_SCORE;

    private VipConfig() {
    }

    public static void validate() {
        if (AUTO_ENABLE && (AUTO_ENABLE_LEVEL < 1 || AUTO_ENABLE_LEVEL > 6)) {
            throw new IllegalArgumentException("loginserver.vip.auto-enable.level must be between 1 and 6");
        }
        if (STS_ENABLE) {
            if (STS_PORT <= 0 || STS_PORT > 65535) {
                throw new IllegalArgumentException("loginserver.vip.sts.port must be between 1 and 65535");
            }
            if (STS_DEFAULT_SCORE < 0) {
                throw new IllegalArgumentException("loginserver.vip.sts.default-score must be >= 0");
            }
            if (STS_APP_GROUP == null || STS_APP_GROUP.isBlank()) {
                throw new IllegalArgumentException("loginserver.vip.sts.app-group must not be blank");
            }
        }
    }
}
