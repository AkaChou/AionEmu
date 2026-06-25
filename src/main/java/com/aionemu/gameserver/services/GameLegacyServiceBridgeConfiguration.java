package com.aionemu.gameserver.services;

import com.aionemu.gameserver.services.transfers.PlayerTransferService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class GameLegacyServiceBridgeConfiguration {

    @Bean
    @Lazy
    public AdminService adminService() {
        return AdminService.getInstance();
    }

    @Bean
    @Lazy
    public PlayerTransferService gamePlayerTransferService() {
        return PlayerTransferService.getInstance();
    }
}
