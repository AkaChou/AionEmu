package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class LoginLegacyServiceBridgeConfiguration {

    @Bean
    @Lazy
    public PlayerTransferService loginPlayerTransferService() {
        return new PlayerTransferService();
    }

    @Bean
    @Lazy
    public PremiumController loginPremiumController() {
        return new PremiumController();
    }

    @Bean
    @Lazy
    public TaskFromDBManager loginTaskFromDBManager() {
        return new TaskFromDBManager();
    }

    @Bean
    @Lazy
    public Shutdown loginShutdown() {
        return new Shutdown();
    }
}
