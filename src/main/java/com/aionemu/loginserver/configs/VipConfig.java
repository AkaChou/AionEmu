package com.aionemu.loginserver.configs;

import com.aionemu.commons.configuration.Property;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

/**
 * 独立账号 VIP 配置。
 * Independent account VIP configuration.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VipConfig {

    /** 是否自动启用账号 VIP。 / Whether to auto-enable account VIP. */
    @Property(key = "loginserver.vip.auto-enable", defaultValue = "false")
    public static boolean AUTO_ENABLE;

    /** 自动启用 VIP 的等级。 / Level granted when auto-enabling VIP. */
    @Property(key = "loginserver.vip.auto-enable.level", defaultValue = "1")
    public static int AUTO_ENABLE_LEVEL;

    /**
     * 校验配置值范围（VIP 等级）。
     * Validates configuration value ranges (VIP level).
     */
    public static void validate() {
        if (AUTO_ENABLE && (AUTO_ENABLE_LEVEL < 1 || AUTO_ENABLE_LEVEL > 6)) {
            throw new IllegalArgumentException("loginserver.vip.auto-enable.level must be between 1 and 6");
        }
    }
}
