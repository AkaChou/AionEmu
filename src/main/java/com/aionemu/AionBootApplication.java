package com.aionemu;

import com.aionemu.boot.config.AionGameProperties;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyChatProperties;
import com.aionemu.boot.config.LegacyGameProperties;
import com.aionemu.boot.config.LegacyLoginProperties;
import com.aionemu.boot.web.AdminConsoleConfigDir;
import com.aionemu.boot.web.AdminConsoleSettings;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Aion 统一 Boot 启动入口，以非 Web 模式拉起内嵌游戏/登录/聊天服务。
 * Unified Boot entry that starts the embedded game/login/chat services without a web container.
 * <p>
 * 持久层走 Spring Boot 标准数据源（库参数唯一来源是 aion/config/network/database.properties，由遗留属性
 * 后处理器在启动期翻译成 spring.datasource 并指向统一库 aion_emu），MyBatis-Flex Mapper 以懒加载方式扫描
 * 注册，避免非 Boot 注解上下文在缺少 Flex 基建时提前实例化。
 * Persistence uses the standard Boot DataSource (the database settings have a single source,
 * aion/config/network/database.properties, translated into spring.datasource at startup by the legacy
 * property environment post-processor and pointing at the unified aion_emu database); MyBatis-Flex mappers
 * are scanned lazily so non-Boot annotation contexts do not instantiate them before the Flex infrastructure
 * exists.
 * </p>
 */
@SpringBootApplication
@MapperScan(basePackages = {
	"com.aionemu.loginserver.persistence.mapper",
	"com.aionemu.gameserver.persistence.mapper"
}, lazyInitialization = "true")
@EnableConfigurationProperties({
	AionServicesProperties.class,
	AionGameProperties.class,
	LegacyGameProperties.class,
	LegacyLoginProperties.class,
	LegacyChatProperties.class
})
public class AionBootApplication {

	/**
	 * 启用内嵌运行模式后启动 Spring 应用（失败/关闭回调由
	 * {@code AionServiceLauncher} 在 {@code ApplicationRunner.run()} 中注册，不依赖字节码织入）。
	 * Enables embedded runtime mode, then starts Spring (failure/shutdown callbacks are registered by
	 * {@code AionServiceLauncher} in {@code ApplicationRunner.run()}, with no bytecode weaving).
	 * <p>必须保持 {@code public}：JVM 启动器与 pom 的 mainClass 反射入口，且被
	 * {@code AionBootApplicationTest} 的反射与源码审计双重钉住，降级为包私有会直接无法启动。
	 * <p>Must stay {@code public}: the JVM launcher and the pom mainClass both need it, and
	 * {@code AionBootApplicationTest} pins it via reflection and source audit.
	 *
	 * @param args 命令行参数 / command-line arguments
	 */
	public static void main(String[] args) {
		AionRuntimeMode.enableBootEmbeddedMode();
		new SpringApplicationBuilder(AionBootApplication.class)
			.web(adminConsoleWebApplicationType())
			.run(args);
	}

	/**
	 * 依据管理控制台配置决定是否启动 Web 层。
	 * <p>
	 * 判定必须在 Spring 上下文创建之前完成：{@code SpringApplicationBuilder.web(...)} 的取值早于
	 * {@code Environment} 成形，因此这里直接读取 {@code config/web/web.properties}，不经 Spring。
	 * <p>
	 * Decides whether the web layer starts, based on the admin console configuration.
	 * <p>
	 * The decision must happen before the Spring context exists: the {@code SpringApplicationBuilder.web(...)}
	 * value is chosen before the {@code Environment} takes shape, so {@code config/web/web.properties} is read
	 * directly here instead of through Spring.
	 * @return 启用了控制台则为 {@link WebApplicationType#SERVLET}，否则为 {@link WebApplicationType#NONE}
	 *         / {@link WebApplicationType#SERVLET} when the console is enabled, otherwise
	 *         {@link WebApplicationType#NONE}
	 */
	private static WebApplicationType adminConsoleWebApplicationType() {
		AdminConsoleSettings settings = AdminConsoleSettings.read(AdminConsoleConfigDir.resolve());
		if (!settings.enabled()) {
			return WebApplicationType.NONE;
		}
		return WebApplicationType.SERVLET;
	}
}
