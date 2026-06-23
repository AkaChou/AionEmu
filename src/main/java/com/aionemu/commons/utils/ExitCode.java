package com.aionemu.commons.utils;

/**
 * 系统退出代码常量类
 * System exit code constants
 */
public final class ExitCode {
    /**
     * 正常退出代码
     * Normal exit code
     */
    public static final int CODE_NORMAL = 0;

    /**
     * 重启退出代码
     * Restart exit code
     */
    public static final int CODE_RESTART = 2;

    /**
     * 错误退出代码
     * Error exit code
     */
    public static final int CODE_ERROR = 1;

    /**
     * 私有构造函数，防止实例化
     * Private constructor to prevent instantiation
     */
    private ExitCode() {
    }
}
