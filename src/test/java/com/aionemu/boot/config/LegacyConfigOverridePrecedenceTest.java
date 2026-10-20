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
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.testutil.ConfigSnapshot;

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

		ConfigSnapshot snapshot = ConfigSnapshot.of(ThreadConfig.class, "BASE_THREAD_POOL_SIZE");
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
			snapshot.restore();
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

		ConfigSnapshot snapshot = ConfigSnapshot.of(ThreadConfig.class, "BASE_THREAD_POOL_SIZE");
		try {
			ConfigurableProcessor.process(ThreadConfig.class, propertiesOf("gameserver.thread.basepoolsize", "5"));
			assertEquals(5, ThreadConfig.BASE_THREAD_POOL_SIZE);
		} finally {
			snapshot.restore();
		}
	}

	/**
	 * 文件键采用 `success.rate` 子键后，遗留装配必须沿该子键取值，
	 * 不得让 @Property 的 defaultValue 覆盖 Binder 已绑定的文件值。
	 * When the file uses the `success.rate` sub-key, the legacy assembly must read through
	 * that sub-key instead of letting the @Property defaultValue overwrite the bound file value.
	 */
	@Test
	void legacyAssemblyReadsSuccessRateSubKey() throws Exception {
		Path configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("main"));
		Files.writeString(configDir.resolve("main/enchant.properties"), """
			gameserver.enchant.item.success.rate=90
			""");

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
			"commandLine",
			Map.of("aion.config.dir", configDir.toString())
		));
		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());
		new BootConfigSourceResolver(environment).publishEnvironmentResolver();

		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class, "ENCHANT_ITEM");
		try {
			// 生产形态：文件只带 `success.rate` 键，裸键在 Environment 与遗留 Properties 中均缺失。
			// Production shape: the file only carries the `success.rate` key, so the bare key is
			// missing from both the Environment and the legacy Properties.
			ConfigurableProcessor.process(EnchantsConfig.class, new java.util.Properties());

			assertEquals(90f, EnchantsConfig.ENCHANT_ITEM,
				"legacy assembly must read success.rate instead of falling back to the default");
		} finally {
			snapshot.restore();
		}
	}

	/**
	 * 旧 `.value` 子键仍须生效，避免现有部署在升级后被静默重置为默认值。
	 * The legacy `.value` sub-key must still work so existing deployments are not silently reset to defaults.
	 */
	@Test
	void legacyAssemblyKeepsValueSubKeyCompatibility() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class, "ENCHANT_ITEM");
		try {
			ConfigSourceResolverHolder.publish(null);
			ConfigurableProcessor.process(EnchantsConfig.class,
				propertiesOf("gameserver.enchant.item.success.rate.value", "91"));

			assertEquals(91f, EnchantsConfig.ENCHANT_ITEM,
				"a legacy .value suffix below the new success.rate key must remain readable");
		} finally {
			snapshot.restore();
		}
	}

	/**
	 * 配置键迁移后，生产配置文件中的每个强化键都必须能被遗留装配读到，不能静默退回默认值。
	 * After the key migration, every enchant key in the production file must be readable by the
	 * legacy assembly instead of silently falling back to defaults.
	 */
	@Test
	void productionEnchantKeysReachTheStaticFacade() throws Exception {
		Path configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("main"));
		Files.writeString(configDir.resolve("main/enchants.properties"), """
			gameserver.enchant.estima.success.rate=71
			gameserver.enchant.estima.cast.time=610
			gameserver.socket.manastone.success.rate=72
			gameserver.enchant.item.success.rate=73
			gameserver.enchant.plume.success.rate=74
			gameserver.enchant.bracelet.success.rate=75
			gameserver.enchant.accessory.success.rate=76
			gameserver.enchant.stigma.success.rate=77
			gameserver.enchant.stigma.max.level=35
			gameserver.enchant.stigma.cast.time=640
			gameserver.enchant.cast.time=620
			gameserver.enchant.authorize.cast.time=630
			""");

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
			"commandLine",
			Map.of("aion.config.dir", configDir.toString())
		));
		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());
		new BootConfigSourceResolver(environment).publishEnvironmentResolver();

		ConfigSnapshot snapshot = ConfigSnapshot.of(EnchantsConfig.class,
			"ENCHANT_ESTIMA", "ESTIMA_ENCHANT_SPEED",
			"SOCKET_MANASTONE", "ENCHANT_ITEM", "ENCHANT_PLUME", "ENCHANT_BRACELET", "ENCHANT_ACCESSORY",
			"ENCHANT_STIGMA", "MAX_STIGMA_ENCHANT_LEVEL", "STIGMA_ENCHANT_SPEED",
			"ENCHANT_SPEED", "AUTHORIZE_SPEED");
		try {
			ConfigurableProcessor.process(EnchantsConfig.class, new java.util.Properties());

			assertEquals(71f, EnchantsConfig.ENCHANT_ESTIMA);
			assertEquals(610, EnchantsConfig.ESTIMA_ENCHANT_SPEED);
			assertEquals(72f, EnchantsConfig.SOCKET_MANASTONE);
			assertEquals(73f, EnchantsConfig.ENCHANT_ITEM);
			assertEquals(74f, EnchantsConfig.ENCHANT_PLUME);
			assertEquals(75f, EnchantsConfig.ENCHANT_BRACELET);
			assertEquals(76f, EnchantsConfig.ENCHANT_ACCESSORY);
			assertEquals(77f, EnchantsConfig.ENCHANT_STIGMA);
			assertEquals(35, EnchantsConfig.MAX_STIGMA_ENCHANT_LEVEL);
			assertEquals(640, EnchantsConfig.STIGMA_ENCHANT_SPEED);
			assertEquals(620, EnchantsConfig.ENCHANT_SPEED);
			assertEquals(630, EnchantsConfig.AUTHORIZE_SPEED);
		} finally {
			snapshot.restore();
		}
	}

	private static java.util.Properties propertiesOf(String key, String value) {
		java.util.Properties properties = new java.util.Properties();
		properties.setProperty(key, value);
		return properties;
	}
}
