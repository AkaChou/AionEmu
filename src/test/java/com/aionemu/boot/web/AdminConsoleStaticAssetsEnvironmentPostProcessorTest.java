package com.aionemu.boot.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * 管理控制台前端产物兜底测试：classpath 里没有已构建页面时，磁盘构建目录被加入静态资源位置。
 * Tests for the admin console frontend asset fallback: when the classpath carries no built page, the
 * on-disk build directory is added to the static resource locations.
 */
class AdminConsoleStaticAssetsEnvironmentPostProcessorTest {

	@TempDir
	Path tempDir;

	/**
	 * 原线程上下文 ClassLoader：测试结束后恢复。
	 * The original thread context class loader, restored after the test.
	 */
	private final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

	@AfterEach
	void clearAionHome() {
		System.clearProperty("aion.home");
		Thread.currentThread().setContextClassLoader(originalClassLoader);
	}

	/**
	 * 把线程上下文 ClassLoader 换成一个不含控制台产物的加载器。
	 * <p>
	 * 探测逻辑用的是线程上下文 ClassLoader：真实运行时它就是应用 classpath（含 {@code static/}），
	 * 而本用例要验证的正是「classpath 里没有产物」这条分支，因此换成一个空的加载器来隔离。
	 * <p>
	 * Replaces the thread context class loader with one that carries no console assets.
	 * <p>
	 * The probe uses the thread context class loader: at runtime that is the application classpath (with
	 * {@code static/}), while this case exercises the "the classpath carries no assets" branch, so an empty
	 * loader isolates it.
	 */
	private void useClasspathWithoutConsoleAssets() {
		Thread.currentThread().setContextClassLoader(new java.net.URLClassLoader(new java.net.URL[0], null));
	}

	/**
	 * 磁盘上存在构建产物时，构建目录被加入静态资源位置。
	 * When the build output exists on disk, the build directory is added to the static resource locations.
	 * @throws Exception 写入失败 / when writing fails
	 */
	@Test
	void buildDirectoryIsAddedWhenTheClasspathHasNoAssets() throws Exception {
		useClasspathWithoutConsoleAssets();
		Path buildDirectory = tempDir.resolve("web/dist");
		Files.createDirectories(buildDirectory);
		Files.writeString(buildDirectory.resolve("index.html"), "<!doctype html>");
		System.setProperty("aion.home", tempDir.toString());

		StandardEnvironment environment = new StandardEnvironment();
		new AdminConsoleStaticAssetsEnvironmentPostProcessor().postProcessEnvironment(environment,
			new SpringApplication());

		String locations = environment.getProperty(
			AdminConsoleStaticAssetsEnvironmentPostProcessor.STATIC_LOCATIONS_KEY);
		assertTrue(locations.contains(buildDirectory.toUri().toString()),
			"the build directory must be added to the static resource locations, but got " + locations);
		assertTrue(locations.endsWith("/"),
			"the static resource location must end with a slash, but got " + locations);
	}

	/**
	 * 磁盘上没有构建产物时，静态资源位置保持 Spring Boot 的默认值。
	 * When no build output exists on disk, the static resource locations keep the Spring Boot defaults.
	 * @throws Exception 写入失败 / when writing fails
	 */
	@Test
	void defaultsAreKeptWhenNoBuildDirectoryExists() throws Exception {
		useClasspathWithoutConsoleAssets();
		System.setProperty("aion.home", tempDir.toString());

		StandardEnvironment environment = new StandardEnvironment();
		new AdminConsoleStaticAssetsEnvironmentPostProcessor().postProcessEnvironment(environment,
			new SpringApplication());

		assertFalse(environment.getPropertySources().contains(
			AdminConsoleStaticAssetsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME),
			"no fallback property source may be registered without a build directory");
	}

	/**
	 * classpath 里已经有控制台产物时，不注册兜底位置。
	 * No fallback location is registered when the classpath already carries the console assets.
	 * @throws Exception 写入失败 / when writing fails
	 */
	@Test
	void classpathAssetsSuppressTheFallback() throws Exception {
		Path buildDirectory = tempDir.resolve("web/dist");
		Files.createDirectories(buildDirectory);
		Files.writeString(buildDirectory.resolve("index.html"), "<!doctype html>");
		System.setProperty("aion.home", tempDir.toString());
		useClasspathWithConsoleAssets();

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("classpathAssets",
			Map.of(AdminConsoleStaticAssetsEnvironmentPostProcessor.STATIC_LOCATIONS_KEY,
				"classpath:/static/")));
		new AdminConsoleStaticAssetsEnvironmentPostProcessor().postProcessEnvironment(environment,
			new SpringApplication());

		assertFalse(environment.getPropertySources().contains(
			AdminConsoleStaticAssetsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME),
			"no fallback property source may be registered when the classpath already has the assets");
	}

	/**
	 * 把线程上下文 ClassLoader 换成一个含控制台产物的加载器。
	 * Replaces the thread context class loader with one that carries the console assets.
	 * @throws Exception 目录准备失败 / when the directory cannot be prepared
	 */
	private void useClasspathWithConsoleAssets() throws Exception {
		Path classpathRoot = tempDir.resolve("classpath");
		Files.createDirectories(classpathRoot.resolve("static"));
		Files.writeString(classpathRoot.resolve("static/index.html"), "<!doctype html>");
		Thread.currentThread().setContextClassLoader(
			new java.net.URLClassLoader(new java.net.URL[] { classpathRoot.toUri().toURL() }, null));
	}

	/**
	 * 显式配置的静态资源位置被保留，兜底位置只是追加。
	 * An explicitly configured static resource location is kept; the fallback location is only appended.
	 * @throws Exception 写入失败 / when writing fails
	 */
	@Test
	void explicitlyConfiguredLocationsArePreserved() throws Exception {
		useClasspathWithoutConsoleAssets();
		Path buildDirectory = tempDir.resolve("web/dist");
		Files.createDirectories(buildDirectory);
		Files.writeString(buildDirectory.resolve("index.html"), "<!doctype html>");
		System.setProperty("aion.home", tempDir.toString());

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("customStaticLocations",
			Map.of(AdminConsoleStaticAssetsEnvironmentPostProcessor.STATIC_LOCATIONS_KEY,
				"classpath:/custom/")));
		new AdminConsoleStaticAssetsEnvironmentPostProcessor().postProcessEnvironment(environment,
			new SpringApplication());

		String locations = environment.getProperty(
			AdminConsoleStaticAssetsEnvironmentPostProcessor.STATIC_LOCATIONS_KEY);
		assertTrue(locations.contains("classpath:/custom/"),
			"the explicitly configured location must be kept, but got " + locations);
		assertTrue(locations.contains(buildDirectory.toUri().toString()),
			"the build directory must be added to the static resource locations, but got " + locations);
	}

	/**
	 * 兜底位置不会被重复注册。
	 * The fallback location is not registered twice.
	 * @throws Exception 写入失败 / when writing fails
	 */
	@Test
	void fallbackLocationIsRegisteredOnce() throws Exception {
		useClasspathWithoutConsoleAssets();
		Path buildDirectory = tempDir.resolve("web/dist");
		Files.createDirectories(buildDirectory);
		Files.writeString(buildDirectory.resolve("index.html"), "<!doctype html>");
		System.setProperty("aion.home", tempDir.toString());

		StandardEnvironment environment = new StandardEnvironment();
		AdminConsoleStaticAssetsEnvironmentPostProcessor processor =
			new AdminConsoleStaticAssetsEnvironmentPostProcessor();
		processor.postProcessEnvironment(environment, new SpringApplication());
		processor.postProcessEnvironment(environment, new SpringApplication());

		String locations = environment.getProperty(
			AdminConsoleStaticAssetsEnvironmentPostProcessor.STATIC_LOCATIONS_KEY);
		assertEquals(1, locations.split(Pattern.quote(buildDirectory.toUri().toString()), -1).length - 1,
			"the build directory must appear exactly once, but got " + locations);
	}
}
