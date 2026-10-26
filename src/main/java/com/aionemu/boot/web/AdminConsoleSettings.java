package com.aionemu.boot.web;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 管理控制台配置：是否启用、监听地址与端口、登录凭据、会话有效期。
 * Admin console settings: enablement, bind address and port, login credentials and session lifetime.
 * <p>
 * 该配置<b>刻意不注册为 Spring Bean</b>：Web 层是否启动必须在 Spring 上下文创建之前决定
 * （{@code SpringApplicationBuilder.web(...)} 的取值早于 {@code Environment} 成形），因此这里提供
 * 一个不依赖容器的静态读取入口。
 * <p>
 * These settings are deliberately <b>not</b> a Spring bean: whether the web layer starts must be decided
 * before the Spring context exists (the {@code SpringApplicationBuilder.web(...)} value is chosen before the
 * {@code Environment} takes shape), so a container-free static entry point is provided here.
 * <p>
 * 配置文件位于 {@code <aion.config.dir>/web/web.properties}，不被遗留属性镜像收录，
 * 因此密码不会进入 Spring {@code Environment}。
 * <p>
 * The configuration file lives at {@code <aion.config.dir>/web/web.properties} and is not covered by the
 * legacy property mirror, so the password never reaches the Spring {@code Environment}.
 *
 * @param enabled 是否启用控制台 / whether the console is enabled
 * @param address 监听地址 / bind address
 * @param port 监听端口 / bind port
 * @param username 登录用户名 / login user name
 * @param password 登录密码 / login password
 * @param sessionTimeoutMinutes 会话有效期（分钟）/ session lifetime in minutes
 */
public record AdminConsoleSettings(
	boolean enabled,
	String address,
	int port,
	String username,
	String password,
	int sessionTimeoutMinutes) {

	/**
	 * 默认端口。
	 * Default port.
	 */
	public static final int DEFAULT_PORT = 8080;

	/**
	 * 默认监听地址。
	 * Default bind address.
	 */
	public static final String DEFAULT_ADDRESS = "127.0.0.1";

	/**
	 * 默认会话有效期（分钟）。
	 * Default session lifetime in minutes.
	 */
	public static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 120;

	/**
	 * 从配置目录读取控制台设置。
	 * <p>
	 * 文件缺失或不可读时返回「控制台关闭」的默认设置，绝不因为配置问题而阻断游戏服启动。
	 * <p>
	 * Reads the console settings from the configuration directory.
	 * <p>
	 * A missing or unreadable file yields the "console disabled" default; a configuration problem must
	 * never block the game server startup.
	 * @param configDir 配置根目录 / configuration root directory
	 * @return 控制台设置 / console settings
	 */
	public static AdminConsoleSettings read(Path configDir) {
		Path file = configDir.resolve("web/web.properties");
		if (!Files.isRegularFile(file)) {
			return disabled();
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException e) {
			return disabled();
		}
		return new AdminConsoleSettings(
			parseBoolean(properties.getProperty("web.enabled"), true),
			properties.getProperty("web.address", DEFAULT_ADDRESS).trim(),
			parseInt(properties.getProperty("web.port"), DEFAULT_PORT),
			properties.getProperty("web.username", "aion").trim(),
			properties.getProperty("web.password", "aion"),
			parseInt(properties.getProperty("web.session.timeout"), DEFAULT_SESSION_TIMEOUT_MINUTES));
	}

	/**
	 * 返回「控制台关闭」的默认设置。
	 * Returns the "console disabled" default settings.
	 * @return 控制台设置 / console settings
	 */
	public static AdminConsoleSettings disabled() {
		return new AdminConsoleSettings(false, DEFAULT_ADDRESS, DEFAULT_PORT, "", "", DEFAULT_SESSION_TIMEOUT_MINUTES);
	}

	/**
	 * 解析布尔值；无法识别时返回默认值。
	 * Parses a boolean value, falling back to the default when it is not recognized.
	 * @param raw 原始值 / raw value
	 * @param fallback 无法识别时的默认值 / fallback when unrecognized
	 * @return 布尔值 / boolean value
	 */
	private static boolean parseBoolean(String raw, boolean fallback) {
		if (raw == null) {
			return fallback;
		}
		String trimmed = raw.trim();
		if (trimmed.equalsIgnoreCase("true")) {
			return true;
		}
		if (trimmed.equalsIgnoreCase("false")) {
			return false;
		}
		return fallback;
	}

	/**
	 * 解析整数；无法识别时返回默认值。
	 * Parses an integer value, falling back to the default when it is not recognized.
	 * @param raw 原始值 / raw value
	 * @param fallback 无法识别时的默认值 / fallback when unrecognized
	 * @return 整数值 / integer value
	 */
	private static int parseInt(String raw, int fallback) {
		if (raw == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	/**
	 * 是否具备可用的登录凭据。
	 * <p>
	 * 密码为空时控制台仍可启动，但拒绝一切登录（fail-closed）：宁可无人能登录，也不放行无凭据访问。
	 * <p>
	 * Whether usable login credentials are present.
	 * <p>
	 * A blank password still lets the console start but rejects every login (fail-closed): locking
	 * everyone out is preferable to granting credential-less access.
	 * @return 用户名与密码均非空则为 true / true when both the user name and the password are non-blank
	 */
	public boolean hasCredentials() {
		return !username.isBlank() && !password.isBlank();
	}
}
