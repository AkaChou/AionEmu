package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class LoginLegacyServiceBridgeConfiguration {

    @Bean
    @Lazy
    public PlayerTransferService loginPlayerTransferService() {
        return PlayerTransferService.getInstance();
    }

    @Bean
    @Lazy
    public Shutdown loginShutdown() {
        return new Shutdown();
    }
}
