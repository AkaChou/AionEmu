package com.aionemu.commons.logging.slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public final class LogbackConfiguration {

	private static final String CONFIG_FILE_NAME = "logback-spring.xml";
	private static final String CONFIG_PROPERTY = "aion.logging.config";

	private LogbackConfiguration() {
	}

	public static void configure(LoggerContext context) throws JoranException {
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset();
		configurator.doConfigure(resolveConfigUrl());
	}

	public static URL resolveConfigUrl() {
		String configuredPath = System.getProperty(CONFIG_PROPERTY);
		if (configuredPath != null && !configuredPath.isBlank()) {
			return existingFileUrl(Path.of(configuredPath), "Configured logback file does not exist: " + configuredPath);
		}

		Path workingDirectoryConfig = Path.of(CONFIG_FILE_NAME);
		if (Files.isRegularFile(workingDirectoryConfig)) {
			return fileUrl(workingDirectoryConfig);
		}

		String aionHome = System.getProperty("aion.home");
		if (aionHome != null && !aionHome.isBlank()) {
			Path homeConfig = Path.of(aionHome).resolve(CONFIG_FILE_NAME);
			if (Files.isRegularFile(homeConfig)) {
				return fileUrl(homeConfig);
			}
		}

		URL classpathConfig = LogbackConfiguration.class.getClassLoader().getResource(CONFIG_FILE_NAME);
		if (classpathConfig != null) {
			return classpathConfig;
		}

		throw new IllegalStateException("Unable to find " + CONFIG_FILE_NAME);
	}

	private static URL existingFileUrl(Path path, String missingMessage) {
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException(missingMessage);
		}
		return fileUrl(path);
	}

	private static URL fileUrl(Path path) {
		try {
			return path.toAbsolutePath().normalize().toUri().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid logback configuration path: " + path, e);
		}
	}
}
