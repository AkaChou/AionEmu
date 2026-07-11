package com.aionemu.commons.logging.slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Logback 配置加载工具，按系统属性/工作目录/aion.home/classpath 解析配置文件
 * Logback configuration loader that resolves the config file from system property, working dir, aion.home, or classpath
 */
public final class LogbackConfiguration {

	private static final String CONFIG_FILE_NAME = "logback-spring.xml";
	private static final String CONFIG_PROPERTY = "aion.logging.config";

	private LogbackConfiguration() {
	}

	/**
	 * 使用解析到的配置 URL 初始化 LoggerContext
	 * Initialize the LoggerContext with the resolved configuration URL
	 *
	 * Logback LoggerContext
	 *
	 * @param context @throws JoranException 配置解析失败时 / When configuration parsing fails
	 */
	public static void configure(LoggerContext context) throws JoranException {
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);
		context.reset();
		configurator.doConfigure(resolveConfigUrl());
	}

	/**
	 * 按优先级解析 logback 配置文件 URL
	 * Resolve the logback configuration file URL by priority
	 *
	 * Configuration file URL
	 *
	 * @return @throws IllegalStateException 找不到配置文件时 / When no configuration file can be found
	 */
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

	/**
	 * 校验路径存在后转换为 URL
	 * Convert path to URL after verifying it exists
	 *
	 * @param path 配置文件路径 / Configuration file path
	 * @param missingMessage 文件不存在时的错误消息 / Error message when file is missing
	 * File URL
	 */
	private static URL existingFileUrl(Path path, String missingMessage) {
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException(missingMessage);
		}
		return fileUrl(path);
	}

	/**
	 * 将本地路径转换为 URL
	 * Convert a local path to a URL
	 *
	 * @param path 文件路径 / File path
	 * File URL
	 */
	private static URL fileUrl(Path path) {
		try {
			return path.toAbsolutePath().normalize().toUri().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid logback configuration path: " + path, e);
		}
	}
}
