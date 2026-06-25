package com.aionemu.commons.utils;

public final class AionProcessExit {

    private AionProcessExit() {
    }

    public static void exit(int status) {
        System.exit(status);
    }

    public static void halt(int status) {
        Runtime.getRuntime().halt(status);
    }
}
