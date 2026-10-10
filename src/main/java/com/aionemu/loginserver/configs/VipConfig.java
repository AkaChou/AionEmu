package com.aionemu.loginserver.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

/**
 * 独立账号 VIP 配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Independent account VIP configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "loginserver.vip")
public class VipConfig {

    /** 是否自动启用账号 VIP。 / Whether to auto-enable account VIP. */
    @Property(key = "loginserver.vip.auto-enable", defaultValue = "false")
    public static boolean AUTO_ENABLE;

    /** 自动启用 VIP 的等级。 / Level granted when auto-enabling VIP. */
    @Property(key = "loginserver.vip.auto-enable.level", defaultValue = "1")
    public static int AUTO_ENABLE_LEVEL;

    private final AutoEnableProperties autoEnable = new AutoEnableProperties();

    public AutoEnableProperties getAutoEnable() {
        return autoEnable;
    }

    /**
     * no-op：绑定值经 {@code valueOf} 或嵌套属性直接写入静态字段，防止重复绑定覆盖。
     * no-op: bound values reach the static fields via {@code valueOf} or the nested properties,
     * so re-binding must not overwrite them.
     * @param autoEnable 绑定产生的嵌套实例 / nested instance produced by binding
     */
    public void setAutoEnable(AutoEnableProperties autoEnable) {
    }

    /**
     * 校验配置值范围（VIP 等级）。
     * Validates configuration value ranges (VIP level).
     */
    public static void validate() {
        if (AUTO_ENABLE && (AUTO_ENABLE_LEVEL < 1 || AUTO_ENABLE_LEVEL > 6)) {
            throw new IllegalArgumentException("loginserver.vip.auto-enable.level must be between 1 and 6");
        }
    }

    /**
     * 「标量+子键」共存节点：{@code value} 承载规范化后的布尔标量，{@code level} 承载子键。
     * Scalar-plus-sub-key node: {@code value} carries the normalized boolean scalar, {@code level} the sub-key.
     */
    public static class AutoEnableProperties {

        /**
         * 裸键值形态兼容入口：直接解析布尔并写入静态字段。
         * Bare-key value-shape entry: parses the boolean and writes the static field directly.
         * @param value 裸键布尔值 / bare-key boolean value
         * @return 嵌套实例 / nested instance
         */
        public static AutoEnableProperties valueOf(String value) {
            AUTO_ENABLE = Boolean.parseBoolean(value);
            return new AutoEnableProperties();
        }

        public boolean isValue() {
            return AUTO_ENABLE;
        }

        public void setValue(boolean value) {
            AUTO_ENABLE = value;
        }

        public int getLevel() {
            return AUTO_ENABLE_LEVEL;
        }

        public void setLevel(int level) {
            AUTO_ENABLE_LEVEL = level;
        }
    }
}
