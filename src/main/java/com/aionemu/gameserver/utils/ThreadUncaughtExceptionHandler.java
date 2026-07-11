package com.aionemu.gameserver.utils;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * 线程未捕获异常的默认处理器。
 * Default handler for uncaught exceptions on threads.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class ThreadUncaughtExceptionHandler implements UncaughtExceptionHandler {


	/**
	 * 记录线程未捕获异常；若为内存不足则额外告警。
	 * Logs uncaught exceptions; emits an extra alert for out-of-memory errors.
	 *
	 * @param t 发生异常的线程 / Thread that threw the exception
	 * @param e 未捕获的异常 / Uncaught throwable
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		log.error(I18n.get("log.cf34446ab3a6", t.getName(), e, e));
		if (e instanceof OutOfMemoryError) {
			log.error(I18n.get("log.375696c74c58"));
		}
	}
}
