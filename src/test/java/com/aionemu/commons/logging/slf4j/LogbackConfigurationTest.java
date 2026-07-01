package com.aionemu.commons.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.aionemu.commons.logging.slf4j.filters.ExactLevelFilter;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.spi.FilterReply;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
		} finally {
			context.stop();
		}
	}

	private void assertLevelFilter(Appender<ILoggingEvent> appender, Level acceptedLevel, Level deniedLevel) {
		ExactLevelFilter filter = (ExactLevelFilter) appender.getCopyOfAttachedFiltersList().get(0);

		assertEquals(FilterReply.ACCEPT, filter.decide(loggingEvent(acceptedLevel)));
		assertEquals(FilterReply.DENY, filter.decide(loggingEvent(deniedLevel)));
	}

	private LoggingEvent loggingEvent(Level level) {
		LoggingEvent event = new LoggingEvent();
		event.setLevel(level);
		return event;
	}
}
