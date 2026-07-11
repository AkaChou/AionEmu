package com.aionemu.commons.network;

import com.aionemu.commons.network.util.ThreadPoolManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Commons 网络模块的 Spring 配置。
 * Spring configuration for the commons network module.
 */
@Configuration(proxyBeanMethods = false)
public class CommonsNetworkSpringConfiguration {

    /**
     * 提供懒加载的网络线程池管理器 Bean。
     * Provide a lazy network thread-pool manager bean.
     *
     * @return 线程池管理器 / Thread pool manager
     */
    @Bean
    @Lazy
    public ThreadPoolManager commonsNetworkThreadPoolManager() {
        return new ThreadPoolManager();
    }
}
