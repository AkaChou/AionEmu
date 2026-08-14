package com.aionemu.commons.network.util;

import com.aionemu.boot.i18n.I18n;
import java.lang.Thread.UncaughtExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 线程未捕获异常处理器，记录异常并预留 OOM 处理点。
 * Thread uncaught exception handler that logs exceptions and reserves OOM handling.
 *
 * @author AionEmu Project
 */
@Slf4j
public class ThreadUncaughtExceptionHandler implements UncaughtExceptionHandler {

    /**
     * 处理未捕获的线程异常。
     * Handle uncaught thread exception.
     *
     * @param t 发生异常的线程 / Thread where exception occurred
     * @param e 未捕获异常 / Uncaught exception
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error(I18n.get("log.cf34446ab3a6", t.getName(), e, e));
        if (e instanceof OutOfMemoryError) {
            // 内存溢出异常的特殊处理（当前为空，预留 OOM 处理点）
            // Special handling for OutOfMemoryError (currently empty, reserved OOM handling point)
        }
    }
}
