package com.aionemu.loginserver.utils;



import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;

/**
 * 线程未捕获异常处理器：记录错误，预留 OOM 与线程重启扩展点。
 * Uncaught-exception handler: logs the error, reserving extension points for OOM and thread-restart handling.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class ThreadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {


    /**
     * {@inheritDoc}
     * 记录未捕获异常；对 OOM 等场景预留处理。
     * Logs the uncaught exception; reserves handling for scenarios such as OOM.
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error(I18n.get("log.cf34446ab3a6", t.getName(), e, e));
    }
}
