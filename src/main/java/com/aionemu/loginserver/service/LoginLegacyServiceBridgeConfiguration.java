package com.aionemu.loginserver.service;

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
}
