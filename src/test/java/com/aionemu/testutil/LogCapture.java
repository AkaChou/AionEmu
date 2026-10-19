package com.aionemu.testutil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作用域内捕获指定 logger 的全部级别，关闭后恢复原级别；支持并发写入。
 * Captures all levels for one logger within a scope, restoring its level on close and accepting concurrent writes.
 */
public final class LogCapture implements AutoCloseable {

	private final Logger logger;
	private final Level previousLevel;
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

	/** 捕获目标类型的日志。 / Captures logs for the target type. */
	public LogCapture(Class<?> type) {
		this((Logger) LoggerFactory.getLogger(type.getName()));
	}

	/** 捕获具名日志器的日志。 / Captures logs for a named logger. */
	public LogCapture(String name) {
		this((Logger) LoggerFactory.getLogger(name));
	}

	/** 捕获指定 logger 的日志，使用其所属上下文。 / Captures one logger using its own context. */
	public LogCapture(Logger logger) {
		this.logger = logger;
		previousLevel = logger.getLevel();
		appender.list = Collections.synchronizedList(new ArrayList<>());
		appender.setContext(logger.getLoggerContext());
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.DEBUG);
	}

	public List<ILoggingEvent> events() {
		synchronized (appender.list) {
			return List.copyOf(appender.list);
		}
	}

	@Override
	public void close() {
		logger.detachAppender(appender);
		appender.stop();
		logger.setLevel(previousLevel);
	}

}
