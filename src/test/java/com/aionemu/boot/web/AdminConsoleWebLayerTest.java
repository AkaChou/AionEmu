package com.aionemu.boot.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import com.aionemu.AionBootApplication;

/**
 * 管理控制台 Web 层测试：开关决定是否启动嵌入式 Tomcat，且启动后的服务端口必须真正可用。
 * Tests for the admin console web layer: the switch decides whether the embedded Tomcat starts, and the
 * resulting server port must actually be usable.
 */
class AdminConsoleWebLayerTest {

	@TempDir
	Path tempDir;

	@AfterEach
	void clearConfigDir() {
		System.clearProperty("aion.config.dir");
	}

	/**
	 * 控制台关闭时，Web 层完全不启动。
	 * With the console disabled, the web layer does not start at all.
	 */
	@Test
	void disabledConsoleStartsNoWebLayer() throws Exception {
		writeWebProperties("""
			web.enabled = false
			""");

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AionBootApplication.class)
			.web(WebApplicationType.NONE)
			.run("--boot-smoke=true",
				"--spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration,"
					+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
				"--spring.flyway.enabled=false",
				"--aion.services.login.enabled=false",
				"--aion.services.chat.enabled=false",
				"--aion.services.game.enabled=false")) {
			assertFalse(context instanceof WebServerApplicationContext,
				"a non-web application context must be created when the console is disabled");
		}
	}

	/**
	 * 控制台启用时，Web 层以 Servlet 上下文启动。
	 * With the console enabled, the web layer starts as a servlet context.
	 */
	@Test
	void enabledConsoleStartsTheServletWebLayer() throws Exception {
		writeWebProperties("""
			web.enabled = true
			web.address = 127.0.0.1
			web.port = 0
			web.username = aion
			web.password = aion
			""");

		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AionBootApplication.class)
			.web(WebApplicationType.SERVLET)
			.run("--boot-smoke=true",
				"--spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration,"
					+ "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
				"--spring.flyway.enabled=false",
				"--aion.services.login.enabled=false",
				"--aion.services.chat.enabled=false",
				"--aion.services.game.enabled=false")) {
			assertInstanceOf(WebServerApplicationContext.class, context,
				"a servlet web application context must be created when the console is enabled");
			int port = ((WebServerApplicationContext) context).getWebServer().getPort();
			assertTrue(port > 0,
				"the embedded web server must be listening on a real port, but got " + port);
			assertTrue(context.containsBean("adminConsoleController"),
				"the admin console controller must be registered");
			assertTrue(context.containsBean("adminConsoleAuthenticationFilter"),
				"the admin console authentication filter must be registered");
		}
	}

	/**
	 * 启动期的 Web 层判定由控制台开关唯一决定，{@code application.yml} 不得把
	 * {@code spring.main.web-application-type} 写死，否则会在环境绑定阶段覆盖
	 * {@code SpringApplicationBuilder.web(...)} 的取值，使嵌入式 Tomcat 永不启动。
	 * <p>
	 * The web layer decision belongs solely to the console switch; {@code application.yml} must not pin
	 * {@code spring.main.web-application-type}, because the environment binding stage would then override the
	 * {@code SpringApplicationBuilder.web(...)} value and the embedded Tomcat would never start.
	 * @throws Exception 读取失败 / when reading fails
	 */
	@Test
	void applicationYmlDoesNotPinTheWebApplicationType() throws Exception {
		String applicationYml = Files.readString(Path.of("src/main/resources/application.yml"));

		assertFalse(applicationYml.contains("web-application-type"),
			"application.yml must not pin spring.main.web-application-type: the console switch decides it");
	}

	/**
	 * 写入测试用控制台配置。
	 * Writes the admin console configuration for the test.
	 * @param content 配置内容 / configuration content
	 * @throws Exception 写入失败 / when writing fails
	 */
	private void writeWebProperties(String content) throws Exception {
		Path configDir = tempDir.resolve("config");
		Files.createDirectories(configDir.resolve("web"));
		Files.writeString(configDir.resolve("web/web.properties"), content);
		System.setProperty("aion.config.dir", configDir.toString());
	}
}
