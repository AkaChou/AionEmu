package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

class LoggingPatternConfigurationTest {

	private static final String FILE_CONSOLE_PATTERN = "%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger - %message%n";
	private static final String COLORED_CONSOLE_PATTERN = "%date{MM-dd HH:mm:ss} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %message%n";

	@Test
	void unifiedAppendersKeepFullLoggerThreadAndStackTrace() throws IOException {
		String logbackXml = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

		assertTrue(logbackXml.contains("<property name=\"fileConsolePattern\" value=\"" + FILE_CONSOLE_PATTERN + "\"/>"));
		assertTrue(logbackXml.contains("<property name=\"coloredConsolePattern\" value=\"" + COLORED_CONSOLE_PATTERN + "\"/>"));
		assertTrue(logbackXml.contains("<Pattern>${fileConsolePattern}</Pattern>"));
		assertTrue(logbackXml.contains("<Pattern>${coloredConsolePattern}</Pattern>"));
	}

	@Test
	void loggerAbbreviationDropsOnlyCommonPackageSegments() {
		LoggerContext context = new LoggerContext();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%logger{36} - %message");
		encoder.start();
		LoggingEvent event = new LoggingEvent();
		event.setLoggerName("com.aionemu.commons.network.util.ThreadPoolManager");
		event.setLevel(Level.INFO);
		event.setMessage("network-prefix");
		event.setThreadName("network-check");
		String rendered = new String(encoder.encode(event), StandardCharsets.UTF_8);

		assertTrue(rendered.contains("c.a.c.network.util.ThreadPoolManager"), rendered);
		assertTrue(rendered.contains(" - network-prefix"), rendered);
		encoder.stop();
		context.stop();
	}
}
