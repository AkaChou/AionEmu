package com.aionemu;

import com.aionemu.boot.config.AionGameProperties;
import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.LegacyChatProperties;
import com.aionemu.boot.config.LegacyGameProperties;
import com.aionemu.boot.config.LegacyLoginProperties;
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
 * 持久层走 Spring Boot 标准数据源（application.yml 的 spring.datasource 指向统一库 aion_emu），
 * MyBatis-Flex Mapper 以懒加载方式扫描注册，避免非 Boot 注解上下文在缺少 Flex 基建时提前实例化。
 * Persistence uses the standard Boot DataSource (spring.datasource in application.yml, unified
 * aion_emu database); MyBatis-Flex mappers are scanned lazily so non-Boot annotation contexts do
 * not instantiate them before the Flex infrastructure exists.
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
			.web(WebApplicationType.NONE)
			.run(args);
	}
}
