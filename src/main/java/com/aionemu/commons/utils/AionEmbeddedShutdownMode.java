package com.aionemu.commons.utils;

/**
 * 嵌入式关闭模式及对应退出码。
 * Embedded shutdown modes and their exit codes.
 */
public enum AionEmbeddedShutdownMode {
    /**
     * 正常关闭。
     * Normal shutdown.
     */
    SHUTDOWN(ExitCode.CODE_NORMAL),

    /**
     * 重启退出。
     * Restart exit.
     */
    RESTART(ExitCode.CODE_RESTART);

    private final int exitCode;

    AionEmbeddedShutdownMode(int exitCode) {
        this.exitCode = exitCode;
    }

    /**
     * 返回对应进程退出码。
     * Return the associated process exit code.
     *
     * Exit code
     */
    public int exitCode() {
        return exitCode;
    }
}
