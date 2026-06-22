package com.aionemu.commons.utils;

public final class AionRuntimeMode {

    public static final String BOOT_EMBEDDED_PROPERTY = "aion.boot.embedded";

    private AionRuntimeMode() {
    }

    public static void enableBootEmbeddedMode() {
        System.setProperty(BOOT_EMBEDDED_PROPERTY, "true");
    }

    public static boolean isBootEmbedded() {
        return Boolean.getBoolean(BOOT_EMBEDDED_PROPERTY);
    }
}
