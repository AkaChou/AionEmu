package com.aionemu.commons.network;

import com.aionemu.commons.network.util.ThreadPoolManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class CommonsNetworkSpringConfiguration {

    @Bean
    @Lazy
    public ThreadPoolManager commonsNetworkThreadPoolManager() {
        return new ThreadPoolManager();
    }
}
