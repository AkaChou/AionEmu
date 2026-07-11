package com.aionemu.commons.utils;

import lombok.experimental.UtilityClass;

/**
 * 运行模式标记（是否嵌入式启动）。
 * Runtime mode flags (whether boot is embedded).
 */
@UtilityClass
public class AionRuntimeMode {

    /**
     * 嵌入式启动系统属性键。
     * System property key for embedded boot mode.
     */
    public static final String BOOT_EMBEDDED_PROPERTY = "aion.boot.embedded";

    /**
     * 启用嵌入式启动模式。
     * Enable embedded boot mode.
     */
    public void enableBootEmbeddedMode() {
        System.setProperty(BOOT_EMBEDDED_PROPERTY, "true");
    }

    /**
     * 判断当前是否为嵌入式启动。
     * Whether the process is in embedded boot mode.
     *
     * @return 是否嵌入式 / Whether embedded
     */
    public boolean isBootEmbedded() {
        return Boolean.getBoolean(BOOT_EMBEDDED_PROPERTY);
    }
}
