package com.aionemu.commons.utils;

import lombok.experimental.UtilityClass;

/**
 * 系统退出代码常量。
 * System exit code constants.
 */
@UtilityClass
public class ExitCode {

    /**
     * 正常退出代码。
     * Normal exit code.
     */
    public static final int CODE_NORMAL = 0;

    /**
     * 错误退出代码。
     * Error exit code.
     */
    public static final int CODE_ERROR = 1;

    /**
     * 重启退出代码。
     * Restart exit code.
     */
    public static final int CODE_RESTART = 2;
}
