package com.aionemu.commons.utils;

public enum AionEmbeddedShutdownMode {
    SHUTDOWN(ExitCode.CODE_NORMAL),
    RESTART(ExitCode.CODE_RESTART);

    private final int exitCode;

    AionEmbeddedShutdownMode(int exitCode) {
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
