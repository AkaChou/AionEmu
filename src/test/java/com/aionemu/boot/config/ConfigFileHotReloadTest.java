package com.aionemu.boot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import com.aionemu.commons.configuration.ConfigSourceResolverHolder;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.testutil.ConfigSnapshot;

/**
 * 热重载回归测试：改盘后重载必须真正取到新值。
 * Hot-reload regression test: a reload after an on-disk change must actually see the new value.
 * <p>
 * 回归背景：启动期快照进 {@code Environment} 的裸键镜像会遮蔽 {@code Config.reload()} 重读磁盘后的值，
 * 于是手改配置文件再执行 {@code //reload config} 对任何 {@code gameserver.*} 键都无效。
 * 裸键改由磁盘配置源提供后，重读的值必须能到达静态字段。
 * <p>
 * Regression context: the raw-key mirror that snapshotted the configuration files into the {@code Environment}
 * at boot shadowed the values {@code Config.reload()} re-read from disk, so hand-editing a configuration file
 * and then running {@code //reload config} had no effect on any {@code gameserver.*} key. With the raw keys
 * served by the disk-backed property source, the re-read value must reach the static field.
 */
class ConfigFileHotReloadTest {

	@TempDir
	Path tempDir;

	private Path configDir;

	@BeforeEach
	void setUp() throws Exception {
		configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("administration"));
		Files.writeString(configDir.resolve("administration/admin.properties"), """
			gameserver.administration.gmlevel = 3
			""");
		ConfigSourceResolverHolder.publish(null);
	}

	@AfterEach
	void tearDown() {
		ConfigSourceResolverHolder.publish(null);
	}

	/**
	 * 改盘后重新装配，静态字段必须取到新值。
	 * After an on-disk change, the re-assembly must pick up the new value.
	 */
	@Test
	void reAssemblyAfterAnOnDiskChangeSeesTheNewValue() throws Exception {
		StandardEnvironment environment = environmentWithConfigDir();
		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());
		new AionConfigFilePropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());
		new BootConfigSourceResolver(environment).publishEnvironmentResolver();

		ConfigSnapshot snapshot = ConfigSnapshot.of(AdminConfig.class, "GM_LEVEL");
		try {
			ConfigurableProcessor.process(AdminConfig.class, administrationProperties());
			assertEquals(3, AdminConfig.GM_LEVEL, "the file value must reach the static field at boot");

			// 改盘：把等级从 3 改成 5。
			// Change the file: the level goes from 3 to 5.
			Files.writeString(configDir.resolve("administration/admin.properties"), """
				gameserver.administration.gmlevel = 5
				""");

			// 重建磁盘配置源，模拟控制台保存后执行的刷新。
			// Rebuild the disk-backed property source, mirroring the refresh the console performs after a save.
			environment.getPropertySources().remove(AionConfigFilePropertySource.NAME);
			environment.getPropertySources().addLast(
				new AionConfigFilePropertySource(AionConfigFilePropertySource.readEntries(configDir)));

			ConfigurableProcessor.process(AdminConfig.class, administrationProperties());
			assertEquals(5, AdminConfig.GM_LEVEL, "the re-read file value must reach the static field");
		} finally {
			snapshot.restore();
		}
	}

	/**
	 * 磁盘配置源必须提供裸键，且以最低优先级注册。
	 * The disk-backed property source must provide the raw keys and be registered at the lowest precedence.
	 */
	@Test
	void diskPropertySourceProvidesRawKeysAtTheLowestPrecedence() {
		StandardEnvironment environment = environmentWithConfigDir();
		new AionConfigFilePropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("3", environment.getProperty("gameserver.administration.gmlevel"),
			"the raw key must reach the environment");

		var source = environment.getPropertySources().get(AionConfigFilePropertySource.NAME);
		assertNotNull(source, "the disk-backed property source must be registered");
		String[] names = java.util.Arrays.stream(environment.getPropertySources().stream().toArray(
				org.springframework.core.env.PropertySource[]::new))
			.map(org.springframework.core.env.PropertySource::getName)
			.toArray(String[]::new);
		assertEquals(AionConfigFilePropertySource.NAME, names[names.length - 1],
			"the disk-backed property source must sit at the lowest precedence");
	}

	/**
	 * 遗留镜像不得再发布裸键：裸键由磁盘配置源承担。
	 * The legacy mirror must not publish raw keys any more: the disk-backed property source owns them.
	 */
	@Test
	void legacyMirrorNoLongerPublishesRawKeys() {
		StandardEnvironment environment = environmentWithConfigDir();
		new AionLegacyPropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertNull(environment.getPropertySources().get(AionLegacyPropertySourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)
				.getProperty("gameserver.administration.gmlevel"),
			"the legacy mirror must not publish raw keys");

		assertEquals("3", environment.getPropertySources()
				.get(AionLegacyPropertySourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)
				.getProperty("aion.legacy.game.property.gameserver.administration.gmlevel"),
			"the prefixed legacy key must still be published");
	}

	/**
	 * 命令行覆盖仍然压过文件值。
	 * A command-line override still outranks the file value.
	 */
	@Test
	void commandLineStillOutranksTheFileValue() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
			"commandLine",
			Map.of(
				"aion.config.dir", configDir.toString(),
				"gameserver.administration.gmlevel", "9")));
		new AionConfigFilePropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertEquals("9", environment.getProperty("gameserver.administration.gmlevel"),
			"the command line must outrank the file value");
	}

	/**
	 * 跨服务取值不一致的裸键必须被剔除：扁平命名空间无法同时表达两个服务的值。
	 * A raw key the services disagree on must be dropped: a flat namespace cannot express both values.
	 */
	@Test
	void rawKeysTheServicesDisagreeOnAreDropped() throws Exception {
		Files.createDirectories(configDir.resolve("login"));
		Files.createDirectories(configDir.resolve("network"));
		Files.writeString(configDir.resolve("network/database.properties"), """
			database.url=jdbc:mysql://127.0.0.1:3306/al_server_gs
			database.password=shared
			""");
		Files.writeString(configDir.resolve("login/database.properties"), """
			database.url=jdbc:mysql://127.0.0.1:3306/al_server_ls
			""");

		StandardEnvironment environment = environmentWithConfigDir();
		new AionConfigFilePropertySourceEnvironmentPostProcessor()
			.postProcessEnvironment(environment, new SpringApplication());

		assertNull(environment.getProperty("database.url"),
			"a raw key with two different service values must not be published");
		assertEquals("shared", environment.getProperty("database.password"),
			"a raw key the services agree on stays published");
	}

	/**
	 * 构造指向临时配置目录的环境。
	 * Builds an environment pointing at the temporary config directory.
	 * @return 环境 / environment
	 */
	private StandardEnvironment environmentWithConfigDir() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource(
			"commandLine",
			Map.of("aion.config.dir", configDir.toString())));
		return environment;
	}

	/**
	 * 读取 administration 目录下的属性。
	 * Reads the properties under the administration directory.
	 * @return 属性数组 / properties array
	 */
	private Properties[] administrationProperties() {
		try {
			return com.aionemu.commons.utils.PropertiesUtils
				.loadAllFromDirectory(configDir.resolve("administration").toFile());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
