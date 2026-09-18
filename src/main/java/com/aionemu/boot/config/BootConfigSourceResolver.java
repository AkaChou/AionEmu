package com.aionemu.boot.config;

import com.aionemu.commons.configuration.ConfigSourceResolverHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 启动层配置来源发布器：把 Spring {@code Environment} 暴露给遗留 {@link
 * com.aionemu.commons.configuration.ConfigurableProcessor}，使命令行参数、环境变量与 application.yml
 * 覆盖对 Bean 绑定和静态字段同时生效（权威唯一）。
 *
 * <p>刻意放在 Spring Bean 初始化阶段发布，而不是在 {@code EnvironmentPostProcessor} 里：后者会在
 * 任何 {@code SpringApplication} 实例化时触发，测试每次调用都会改到全局持有者，污染后续测试。
 * 作为普通单例，它只在真实应用上下文启动时发布一次，且必然早于服务的 {@code ApplicationRunner}
 * 配置装载。</p>
 *
 * Bootstrap publisher that exposes the Spring {@code Environment} to the legacy {@link
 * com.aionemu.commons.configuration.ConfigurableProcessor}, so command-line arguments, environment
 * variables and application.yml overrides apply to both bean binding and the static fields (one
 * authority).
 *
 * <p>Deliberately published from a singleton bean instead of the {@code EnvironmentPostProcessor}:
 * the post-processor runs for every {@code SpringApplication} instantiation, so tests calling it
 * would keep rewriting the global holder and leak into later tests. As a regular singleton it
 * publishes once for a real application context and always precedes the {@code ApplicationRunner}
 * configuration loading of any service.</p>
 */
@Component
@RequiredArgsConstructor
public class BootConfigSourceResolver {

    private final Environment environment;

    /**
     * 发布以 Spring {@code Environment} 为唯一来源的键解析器。
     * Publishes a key resolver backed solely by the Spring {@code Environment}.
     */
    @PostConstruct
    void publishEnvironmentResolver() {
        ConfigSourceResolverHolder.publish(environment::getProperty);
    }
}
