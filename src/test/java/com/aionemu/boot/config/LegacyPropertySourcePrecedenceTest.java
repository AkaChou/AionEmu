package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import com.aionemu.commons.configuration.ConfigSourceResolverHolder;

/**
 * 遗留配置文件与外部覆盖来源之间的优先级契约。
 *
 * <p>锁定两件此前只在提交信息里声明、未被测试守住的行为：遗留文件必须以最低优先级注册（操作系统环境变量
 * 仍然覆盖它），且注册顺序不能依赖本处理器写入全局解析器持有者。</p>
 *
 * Contract between the legacy properties files and the external override sources.
 *
 * <p>Locks two properties that were previously only asserted in commit messages: legacy files must be
 * registered at the lowest precedence (operating-system environment variables still override them), and
 * the registration must not rely on this post-processor writing the global resolver holder.</p>
 */
class LegacyPropertySourcePrecedenceTest {

	@TempDir
	Path tempDir;

	/**
	 * 清理全局持有者，避免用例之间互相污染。
	 * Clears the global holder so cases cannot pollute each other.
	 */
	@AfterEach
	void clearPublishedResolver() {
		ConfigSourceResolverHolder.publish(null);
	}

	/**
	 * 本处理器只在 Environment 上注册属性源，不得写入全局解析器持有者。
	 *
	 * <p>回归背景：曾有一版把 {@code environment::getProperty} 发布到全局持有者，于是任何实例化
	 * {@code SpringApplication} 的测试都会改写全局状态，导致 {@code VipConfigPathTest} 读到上一个用例的
	 * {@code StandardEnvironment}。解析器只由 {@code BootConfigSourceResolver} 单例在上下文装配时发布。</p>
	 *
	 * The post-processor only registers a property source; it must not write the global resolver.
	 *
	 * <p>Regression context: an earlier revision published {@code environment::getProperty} globally, so
	 * any test instantiating a {@code SpringApplication} rewrote global state and made
	 * {@code VipConfigPathTest} read the previous case's {@code StandardEnvironment}. The resolver is
	 * published only by the {@code BootConfigSourceResolver} singleton during context wiring.</p>
	 */
	@Test
	void postProcessorLeavesTheGlobalResolverUntouched() throws Exception {
		Path configDir = writeMainConfig("gameserver.thread.basepoolsize=7\n");
		StandardEnvironment environment = environmentWithConfigDir(configDir);

		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("7", environment.getProperty("gameserver.thread.basepoolsize"),
			"the file value must still reach the environment");
		assertNull(ConfigSourceResolverHolder.resolve("gameserver.thread.basepoolsize"),
			"the post-processor must not publish a global resolver");
	}

	/**
	 * 操作系统环境变量必须高于遗留文件值，并且点号长键的大小写/下划线改写也要生效。
	 * Operating-system environment variables must outrank the legacy file values, including the
	 * uppercase/underscore mangling of the dotted keys.
	 */
	@Test
	void operatingSystemEnvironmentVariablesOutrankLegacyFileValues() throws Exception {
		Path configDir = writeMainConfig("gameserver.thread.basepoolsize=7\n");
		StandardEnvironment environment = environmentWithConfigDir(configDir);
		environment.getPropertySources().addAfter(
			StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
			new SystemEnvironmentPropertySource("testedSystemEnvironment", Map.of(
				"GAMESERVER_THREAD_BASEPOOLSIZE", "11",
				"AION_LEGACY_GAME_PROPERTY_GAMESERVER_THREAD_BASEPOOLSIZE", "11"
			))
		);

		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("11", environment.getProperty("gameserver.thread.basepoolsize"),
			"the environment variable must override the legacy file value");
		assertEquals("11", environment.getProperty("aion.legacy.game.property.gameserver.thread.basepoolsize"),
			"the low-precedence file source must not shadow the environment variable");
	}

	/**
	 * 没有更高优先级来源时，文件值仍然是有效值（源的加载基线）。
	 * With no higher-precedence source in play, the file value remains effective (loading baseline).
	 */
	@Test
	void fileValueAppliesWhenNoHigherPrecedenceSourceDefinesTheKey() throws Exception {
		Path configDir = writeMainConfig("gameserver.thread.basepoolsize=7\n");
		StandardEnvironment environment = environmentWithConfigDir(configDir);

		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("7", environment.getProperty("gameserver.thread.basepoolsize"));
		assertEquals("7", environment.getProperty("aion.legacy.game.property.gameserver.thread.basepoolsize"));
	}

	/**
	 * 写入一份最小 main.properties 并返回配置根目录。
	 * Writes a minimal main.properties and returns the configuration root.
	 *
	 * @param content 文件内容 / file content
	 * @return 配置根目录 / configuration root
	 * @throws Exception 写入失败 / when writing fails
	 */
	private Path writeMainConfig(String content) throws Exception {
		Path configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("main"));
		Files.createDirectories(configDir.resolve("network"));
		Files.createDirectories(configDir.resolve("login"));
		Files.createDirectories(configDir.resolve("chat"));
		Files.writeString(configDir.resolve("main/main.properties"), content);
		return configDir;
	}

	/**
	 * 构造指向临时配置目录的环境，命令行源模拟真实启动时的最高优先级来源。
	 * Builds an environment pointing at the temporary config directory; the command-line source
	 * stands in for the highest-precedence source of a real startup.
	 *
	 * @param configDir 配置根目录 / configuration root
	 * @return 环境 / environment
	 */
	private StandardEnvironment environmentWithConfigDir(Path configDir) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
			"commandLine",
			Map.of("aion.config.dir", configDir.toString())
		));
		return environment;
	}
}
