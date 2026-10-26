package com.aionemu.boot.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 管理控制台配置读取测试。
 * Tests for reading the admin console configuration.
 */
class AdminConsoleSettingsTest {

	@TempDir
	Path tempDir;

	/**
	 * 缺失配置文件时控制台保持关闭，且不抛异常。
	 * A missing configuration file leaves the console disabled without throwing.
	 */
	@Test
	void missingFileLeavesTheConsoleDisabled() {
		AdminConsoleSettings settings = AdminConsoleSettings.read(tempDir);

		assertFalse(settings.enabled());
		assertFalse(settings.hasCredentials());
	}

	/**
	 * 默认凭据为 aion。
	 * The default credentials are aion.
	 */
	@Test
	void defaultCredentialsAreAion() throws Exception {
		Path web = tempDir.resolve("web");
		Files.createDirectories(web);
		Files.writeString(web.resolve("web.properties"), """
			web.enabled = true
			web.address = 127.0.0.1
			web.port = 8080
			web.username = aion
			web.password = aion
			web.session.timeout = 120
			""");

		AdminConsoleSettings settings = AdminConsoleSettings.read(tempDir);

		assertTrue(settings.enabled());
		assertEquals("127.0.0.1", settings.address());
		assertEquals(8080, settings.port());
		assertEquals("aion", settings.username());
		assertEquals("aion", settings.password());
		assertEquals(120, settings.sessionTimeoutMinutes());
		assertTrue(settings.hasCredentials());
	}

	/**
	 * 关闭开关时控制台不启用，即便凭据齐全。
	 * The console stays disabled by the switch even with complete credentials.
	 */
	@Test
	void disabledSwitchKeepsTheConsoleOff() throws Exception {
		Path web = tempDir.resolve("web");
		Files.createDirectories(web);
		Files.writeString(web.resolve("web.properties"), """
			web.enabled = false
			web.username = aion
			web.password = aion
			""");

		AdminConsoleSettings settings = AdminConsoleSettings.read(tempDir);

		assertFalse(settings.enabled());
	}

	/**
	 * 无法识别的端口回落到默认值，不得让控制台崩溃。
	 * An unrecognizable port falls back to the default instead of crashing the console.
	 */
	@Test
	void unrecognizableValuesFallBackToTheDefaults() throws Exception {
		Path web = tempDir.resolve("web");
		Files.createDirectories(web);
		Files.writeString(web.resolve("web.properties"), """
			web.enabled = true
			web.port = not-a-port
			""");

		AdminConsoleSettings settings = AdminConsoleSettings.read(tempDir);

		assertEquals(AdminConsoleSettings.DEFAULT_PORT, settings.port());
	}
}
