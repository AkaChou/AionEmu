package com.aionemu.boot.web;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 把管理控制台配置翻译成 Spring 认识的键，使 {@code web.*} 配置能驱动嵌入式 Web 层。
 * Translates the admin console configuration into keys Spring understands, so the {@code web.*} settings can
 * drive the embedded web layer.
 * <p>
 * {@code web.port} 与 {@code web.address} 分别翻译为 {@code server.port} 与 {@code server.address}。
 * 翻译结果以<b>低优先级</b>注册：命令行参数、操作系统环境变量与 {@code application.yml} 仍可覆盖，
 * 与其它配置来源的优先级契约一致。
 * <p>
 * {@code web.port} and {@code web.address} are translated into {@code server.port} and {@code server.address}
 * respectively. The translation is registered at a <b>low precedence</b>: command-line arguments,
 * operating-system environment variables and {@code application.yml} still override it, matching the
 * precedence contract of the other configuration sources.
 * <p>
 * 该处理器不读取 {@code web.password}：密码不会进入 Spring {@code Environment}，只在
 * {@link AdminConsoleSettings} 中使用。
 * <p>
 * This processor does not read {@code web.password}: the password never reaches the Spring
 * {@code Environment} and is only used through {@link AdminConsoleSettings}.
 */
public class AdminConsoleEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	/**
	 * 属性源名称。
	 * Property source name.
	 */
	public static final String PROPERTY_SOURCE_NAME = "aionAdminConsole";

	/**
	 * 注册翻译后的键值。
	 * Registers the translated entries.
	 * @param environment 可配置环境 / configurable environment
	 * @param application Spring 应用实例 / Spring application instance
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		AdminConsoleSettings settings = AdminConsoleSettings.read(AdminConsoleConfigDir.resolve(environment));
		if (!settings.enabled()) {
			return;
		}
		Map<String, Object> entries = new LinkedHashMap<>();
		entries.put("server.port", settings.port());
		entries.put("server.address", settings.address());
		environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, entries));
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
}
