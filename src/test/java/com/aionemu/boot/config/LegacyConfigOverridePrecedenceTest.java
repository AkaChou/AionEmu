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
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.aionemu.commons.configuration.ConfigSourceResolverHolder;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.main.ThreadConfig;

/**
 * 单一权威验证：命令行/环境变量覆盖必须同时决定 Bean 绑定值与遗留静态字段值。
 * Single-authority check: a command-line/environment override must decide both the bean binding
 * value and the legacy static field value.
 */
class LegacyConfigOverridePrecedenceTest {

	@TempDir
	Path tempDir;

	@AfterEach
	void clearPublishedResolver() {
		ConfigSourceResolverHolder.publish(null);
	}

	/**
	 * 文件写 7、命令行写 9 时：遗留加载器必须采用 9，不得回退到文件值 7。
	 * With the file saying 7 and the command line saying 9, the legacy loader must take 9 and
	 * must not fall back to the file value 7.
	 */
	@Test
	void commandLineOverrideWinsOverLegacyFileForStaticFields() throws Exception {
		Path configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("main"));
		Files.writeString(configDir.resolve("main/main.properties"), """
			gameserver.thread.basepoolsize=7
			""");

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
			"commandLine",
			Map.of(
				"aion.config.dir", configDir.toString(),
				"gameserver.thread.basepoolsize", "9"
			)
		));
		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());
		// 解析器由启动层单例发布（生产启动路径）。 / Published by the bootstrap singleton, as in production.
		new BootConfigSourceResolver(environment).publishEnvironmentResolver();

		assertEquals("9", ConfigSourceResolverHolder.resolve("gameserver.thread.basepoolsize"),
			"the published resolver must expose the highest-precedence source");

		// Spring 侧绑定与静态字段必须收敛到同一个值。 / Bean binding and the static field must converge.
		ThreadConfig bound = new ThreadConfig();
		Binder.get(environment).bind("gameserver.thread", Bindable.ofInstance(bound));

		int savedBase = ThreadConfig.BASE_THREAD_POOL_SIZE;
		try {
			// 遗留侧组装：文件里是 7，但解析器给出 9，字段必须取 9。
			// Legacy assembly: the file says 7, the resolver says 9, so the field must end up 9.
			ConfigurableProcessor.process(ThreadConfig.class,
				propertiesOf("gameserver.thread.basepoolsize", "7"));

			assertEquals(9, ThreadConfig.BASE_THREAD_POOL_SIZE,
				"legacy loader must prefer the higher-precedence Spring source over the file");
			assertEquals(bound.getBasepoolsize(), ThreadConfig.BASE_THREAD_POOL_SIZE,
				"bean property and static field must not diverge");
		} finally {
			ThreadConfig.BASE_THREAD_POOL_SIZE = savedBase;
		}
	}

	/**
	 * 未发布解析器时必须沿用原有 `Properties` 顺序，行为保持不变。
	 * With no resolver published, the original {@code Properties} order must still apply.
	 */
	@Test
	void withoutResolverThePropertiesOrderIsUnchanged() {
		ConfigSourceResolverHolder.publish(null);
		assertNull(ConfigSourceResolverHolder.resolve("gameserver.thread.basepoolsize"));

		int savedBase = ThreadConfig.BASE_THREAD_POOL_SIZE;
		try {
			ConfigurableProcessor.process(ThreadConfig.class, propertiesOf("gameserver.thread.basepoolsize", "5"));
			assertEquals(5, ThreadConfig.BASE_THREAD_POOL_SIZE);
		} finally {
			ThreadConfig.BASE_THREAD_POOL_SIZE = savedBase;
		}
	}

	private static java.util.Properties propertiesOf(String key, String value) {
		java.util.Properties properties = new java.util.Properties();
		properties.setProperty(key, value);
		return properties;
	}
}
