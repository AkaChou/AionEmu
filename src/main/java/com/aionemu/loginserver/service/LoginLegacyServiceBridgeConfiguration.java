package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
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
    public ThreadPoolManager loginThreadPoolManager() {
        return ThreadPoolManager.getInstance();
    }

    @Bean
    @Lazy
    public BannedMacManager loginBannedMacManager() {
        return new BannedMacManager();
    }

    @Bean
    @Lazy
    public LoginBannedIpService loginBannedIpService() {
        return new LoginBannedIpService();
    }

    @Bean
    @Lazy
    public BruteForceProtector loginBruteForceProtector() {
        return new BruteForceProtector();
    }

    @Bean
    @Lazy
    public FloodProtector loginFloodProtector() {
        return new FloodProtector();
    }

    @Bean
    @Lazy
    public Shutdown loginShutdown() {
        return new Shutdown();
    }
}
