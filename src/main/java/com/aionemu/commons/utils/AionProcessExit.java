package com.aionemu.commons.utils;

import lombok.experimental.UtilityClass;

/**
 * 进程退出封装，便于测试替换与统一调用。
 * Process exit helpers for consistent invocation and testability.
 */
@UtilityClass
public class AionProcessExit {

    /**
     * 正常退出 JVM（可执行关闭钩子）。
     * Exit the JVM normally (shutdown hooks may run).
     *
     * @param status 退出状态码 / Exit status
     */
    public void exit(int status) {
        System.exit(status);
    }

    /**
     * 强制终止 JVM（不执行关闭钩子）。
     * Halt the JVM immediately (shutdown hooks are skipped).
     *
     * @param status 退出状态码 / Exit status
     */
    public void halt(int status) {
        Runtime.getRuntime().halt(status);
    }
}
