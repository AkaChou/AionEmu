package com.aionemu.boot.web;

import java.nio.file.Path;
import com.aionemu.boot.config.AionConfigSourceRefresher;
import com.aionemu.boot.web.config.AdminConfigService;
import com.aionemu.boot.web.config.EffectiveValueResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 管理控制台的 Spring 装配。
 * Spring wiring for the admin console.
 * <p>
 * 控制台设置以<b>普通 bean</b>（而非 {@code @ConfigurationProperties}）形式注入：这些键刻意不进入
 * Spring {@code Environment}，密码因此不会泄漏给任何读取环境的组件。
 * <p>
 * The console settings are injected as a plain bean rather than through {@code @ConfigurationProperties}:
 * these keys are deliberately kept out of the Spring {@code Environment}, so the password never leaks to any
 * component that reads the environment.
 *
 * @author AionEmu
 */
@Configuration
@RequiredArgsConstructor
public class AdminConsoleConfiguration {

	/**
	 * 应用环境。
	 * Application environment.
	 */
	private final ConfigurableEnvironment environment;

	/**
	 * 控制台设置。
	 * Console settings.
	 * @return 控制台设置 / console settings
	 */
	@Bean
	public AdminConsoleSettings adminConsoleSettings() {
		return AdminConsoleSettings.read(AdminConsoleConfigDir.resolve(environment));
	}

	/**
	 * 配置服务。
	 * Configuration service.
	 * @param sourceRefresher 配置源刷新器 / configuration source refresher
	 * @return 配置服务 / configuration service
	 */
	@Bean
	public AdminConfigService adminConfigService(AionConfigSourceRefresher sourceRefresher,
			EffectiveValueResolver valueResolver) {
		return new AdminConfigService(AdminConsoleConfigDir.resolve(environment), sourceRefresher, valueResolver);
	}

	/**
	 * 生效值解析器。
	 * Effective value resolver.
	 * @return 生效值解析器 / effective value resolver
	 */
	@Bean
	public EffectiveValueResolver effectiveValueResolver() {
		Path configDir = AdminConsoleConfigDir.resolve(environment);
		return new EffectiveValueResolver(environment, configDir);
	}
}
