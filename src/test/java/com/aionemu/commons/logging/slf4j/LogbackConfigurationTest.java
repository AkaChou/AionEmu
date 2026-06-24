package com.aionemu.commons.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.LoggerContext;
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
		} finally {
			context.stop();
		}
	}
}
