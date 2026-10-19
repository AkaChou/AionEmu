package com.aionemu.commons.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.aionemu.commons.logging.slf4j.filters.ExactLevelFilter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.spi.FilterReply;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 验证日志配置加载、诊断前缀、异常和专用日志分流。
 * Verifies configuration loading, diagnostic prefixes, exceptions and dedicated log routing.
 */
class LogbackConfigurationTest {

	@TempDir
	Path tempDir;

	@BeforeEach
	@AfterEach
	void restoreProperties() {
		System.clearProperty("aion.home");
		System.clearProperty("aion.log.dir");
		System.clearProperty("aion.logging.config");
	}

	@Test
	void configuredPathTakesPrecedence() throws Exception {
		Path configuredLogback = tempDir.resolve("custom-logback.xml");
		Files.writeString(configuredLogback, "<configuration/>");
		System.setProperty("aion.logging.config", configuredLogback.toString());

		URL configUrl = LogbackConfiguration.resolveConfigUrl();

		assertEquals(configuredLogback.toUri().toURL(), configUrl);
	}

	@Test
	void aionHomeLogbackIsUsedBeforeClasspathResource() throws Exception {
		Path homeLogback = tempDir.resolve("logback-spring.xml");
		Files.writeString(homeLogback, "<configuration/>");
		System.setProperty("aion.home", tempDir.toString());

		URL configUrl = LogbackConfiguration.resolveConfigUrl();

		assertEquals(homeLogback.toUri().toURL(), configUrl);
	}

	@Test
	void missingConfiguredPathFailsFast() {
		Path missingLogback = tempDir.resolve("missing-logback.xml");
		System.setProperty("aion.logging.config", missingLogback.toString());

		assertThrows(IllegalStateException.class, LogbackConfiguration::resolveConfigUrl);
	}

	@Test
	void unifiedLogbackConfigurationCanBeLoaded() {
		System.setProperty("aion.logging.config", "src/main/resources/logback-spring.xml");
		System.setProperty("aion.log.dir", tempDir.resolve("log").toString());
		LoggerContext context = new LoggerContext();

		try {
			assertDoesNotThrow(() -> LogbackConfiguration.configure(context));
			assertEquals(Level.INFO, context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel());
			assertLevelFilter(context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("out_error"), Level.ERROR,
				Level.WARN);
			assertLevelFilter(context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender("out_warn"), Level.WARN,
				Level.ERROR);
			Logger questLogger = context.getLogger("quest");
			assertTrue(questLogger.isAdditive());
			assertNotNull(questLogger.getAppender("app_quest"));
		} finally {
			context.stop();
		}
	}

	private void assertLevelFilter(Appender<ILoggingEvent> appender, Level acceptedLevel, Level deniedLevel) {
		ExactLevelFilter filter = (ExactLevelFilter) appender.getCopyOfAttachedFiltersList().getFirst();

		assertEquals(FilterReply.ACCEPT, filter.decide(loggingEvent(acceptedLevel)));
		assertEquals(FilterReply.DENY, filter.decide(loggingEvent(deniedLevel)));
	}

	@Test
	void diagnosticAppendersKeepFullLoggerThreadAndStackTrace() throws Exception {
		// File appenders reject duplicate console.log destinations, so isolate this context's log directory.
		System.setProperty("aion.logging.config", "src/main/resources/logback-spring.xml");
		Path logs = tempDir.resolve("diagnostics");
		System.setProperty("aion.log.dir", logs.toString());
		LoggerContext context = new LoggerContext();
		try {
			LogbackConfiguration.configure(context);
			Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
			Logger logger = context.getLogger("com.aionemu.gameserver.network.loginserver.LoginServer");
			for (Level level : new Level[] { Level.INFO, Level.WARN, Level.ERROR }) {
				LoggingEvent event = new LoggingEvent(getClass().getName(), logger, level,
					"diagnostic-" + level, new IllegalStateException("logging failure"), null);
				event.setThreadName("shutdown-test-thread");
				event.setMDCPropertyMap(Map.of());
				for (String name : new String[] { "out_console", "app_console", "out_warn", "out_error" }) {
					OutputStreamAppender<?> appender = (OutputStreamAppender<?>) root.getAppender(name);
					PatternLayoutEncoder encoder = (PatternLayoutEncoder) appender.getEncoder();
					String rendered = new String(encoder.encode(event), StandardCharsets.UTF_8)
						.replaceAll("\u001B\\[[;\\d]*m", "");
					assertTrue(rendered.contains("[shutdown-test-thread] " + logger.getName() + " - "));
					assertTrue(rendered.contains("IllegalStateException: logging failure"));
					assertTrue(rendered.contains("\tat "));
					String date = name.equals("out_console") ? "\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"
						: "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}";
					assertTrue(rendered.matches("(?s)^" + date + " .*"), rendered);
				}
				root.callAppenders(event);
			}
			String console = Files.readString(logs.resolve("console.log"));
			String warn = Files.readString(logs.resolve("warn.log"));
			String error = Files.readString(logs.resolve("error.log"));
			assertTrue(console.contains("diagnostic-INFO"));
			assertTrue(console.contains("diagnostic-WARN"));
			assertTrue(console.contains("diagnostic-ERROR"));
			assertTrue(warn.contains("diagnostic-WARN"));
			assertFalse(warn.contains("diagnostic-ERROR"));
			assertFalse(warn.contains("diagnostic-INFO"));
			assertTrue(error.contains("diagnostic-ERROR"));
			assertFalse(error.contains("diagnostic-WARN"));
			assertFalse(error.contains("diagnostic-INFO"));
			for (String name : new String[] { "CHAT_LOG", "ADMINAUDIT_LOG", "aion.console" }) {
				assertFalse(context.getLogger(name).isAdditive());
			}
			OutputStreamAppender<?> banner = (OutputStreamAppender<?>) context.getLogger("aion.console")
				.getAppender("out_banner");
			assertEquals("%message%n", ((PatternLayoutEncoder) banner.getEncoder()).getPattern());
		} finally {
			context.stop();
		}
	}

	private LoggingEvent loggingEvent(Level level) {
		LoggingEvent event = new LoggingEvent();
		event.setLevel(level);
		return event;
	}
}
