package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class LoggingPatternConfigurationTest {

	private static final String FILE_CONSOLE_PATTERN = "%date{MM-dd HH:mm:ss} %-5level [%thread] %logger{36} - %message%n";
	private static final String COLORED_CONSOLE_PATTERN = "%date{MM-dd HH:mm:ss} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %message%n";

	@Test
	void unifiedConsoleAppendersUseCompactTimestampAndAbbreviatedLoggerWithTerminalColors() throws IOException {
		String logbackXml = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

		assertTrue(logbackXml.contains("<property name=\"fileConsolePattern\" value=\"" + FILE_CONSOLE_PATTERN + "\"/>"));
		assertTrue(logbackXml.contains("<property name=\"coloredConsolePattern\" value=\"" + COLORED_CONSOLE_PATTERN + "\"/>"));
		assertTrue(logbackXml.contains("<Pattern>${fileConsolePattern}</Pattern>"));
		assertTrue(logbackXml.contains("<Pattern>${coloredConsolePattern}</Pattern>"));
	}
}
