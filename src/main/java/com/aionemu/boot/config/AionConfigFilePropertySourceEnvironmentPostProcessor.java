package com.aionemu.boot.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 把磁盘配置目录注册为 {@code Environment} 的裸键来源，使 Spring Binder 直读配置文件。
 * Registers the disk-backed configuration directories as the raw-key source of the {@code Environment},
 * so the Spring Binder reads the configuration files directly.
 * <p>
 * 该属性源以最低优先级注册（{@code addLast}），因此命令行参数、操作系统环境变量与
 * {@code application.yml} 仍然压过文件值；带前缀的 {@code aion.legacy.*} 键由
 * {@link AionLegacyPropertySourceEnvironmentPostProcessor} 以同样的最低优先级另行注册。
 * <p>
 * The property source is registered at the lowest precedence ({@code addLast}), so command-line arguments,
 * operating-system environment variables and {@code application.yml} still outrank the file values; the
 * prefixed {@code aion.legacy.*} keys are registered separately at the same lowest precedence by
 * {@link AionLegacyPropertySourceEnvironmentPostProcessor}.
 */
public class AionConfigFilePropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * 注册磁盘配置源。
	 * Registers the disk-backed configuration source.
	 * @param environment 可配置环境 / configurable environment
	 * @param application Spring 应用实例 / Spring application instance
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Path configDir = configDir(environment);
		if (configDir == null) {
			return;
		}
		environment.getPropertySources().addLast(
			new AionConfigFilePropertySource(AionConfigFilePropertySource.readEntries(configDir)));
	}

	/**
	 * 返回本处理器顺序：最低优先级，确保在所有默认源之后生效。
	 * Returns the processor's order: the lowest precedence, so it applies after every default source.
	 * @return 处理器顺序值 / processor order value
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * 解析配置根目录；无法定位时不注册磁盘配置源。
	 * Resolves the configuration root directory; the disk-backed source is not registered when it cannot be
	 * located.
	 * @param environment 环境 / environment
	 * @return 配置根目录，或 {@code null} / configuration root directory, or {@code null}
	 */
	private Path configDir(ConfigurableEnvironment environment) {
		String configured = environment.getProperty("aion.config.dir");
		if (configured != null) {
			return Path.of(configured);
		}
		if (!environment.containsProperty("aion.home")) {
			Path sourceDirectory = Path.of("src/main/resources/aion/config");
			if (Files.isDirectory(sourceDirectory)) {
				return sourceDirectory;
			}
		}
		return Path.of(environment.getProperty("aion.home", "aion")).resolve("config");
	}
}
