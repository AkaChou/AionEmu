package com.aionemu.commons.utils;

public final class AionProcessExit {

    private AionProcessExit() {
    }

    public static void exit(int status) {
        System.exit(status);
    }
}
