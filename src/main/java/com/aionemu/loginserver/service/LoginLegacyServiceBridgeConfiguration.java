package com.aionemu.loginserver.service;

import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.Shutdown;
import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.controller.PremiumController;
import com.aionemu.loginserver.network.NetConnector;
import com.aionemu.loginserver.taskmanager.TaskFromDBManager;
import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 登录服遗留单例到 Spring Bean 的桥接配置（懒加载）。
 * Bridge configuration exposing legacy login singletons as lazy Spring beans.
 */
@Configuration(proxyBeanMethods = false)
public class LoginLegacyServiceBridgeConfiguration {

    /**
     * 角色转移服务 Bean。
     * Player transfer service bean.
     *
     * @return 角色转移服务 / player transfer service
     */
    @Bean
    @Lazy
    public PlayerTransferService loginPlayerTransferService() {
        return new PlayerTransferService();
    }

    /**
     * 高级账号控制器 Bean。
     * Premium account controller bean.
     *
     * @return 高级账号控制器 / premium controller
     */
    @Bean
    @Lazy
    public PremiumController loginPremiumController() {
        return new PremiumController();
    }

    /**
     * 数据库任务管理器 Bean。
     * DB-driven task manager bean.
     *
     * @return 任务管理器 / task manager
     */
    @Bean
    @Lazy
    public TaskFromDBManager loginTaskFromDBManager() {
        return new TaskFromDBManager();
    }

    /**
     * 登录服线程池管理器 Bean。
     * Login thread-pool manager bean.
     *
     * @return 线程池管理器 / thread pool manager
     */
    @Bean
    @Lazy
    public ThreadPoolManager loginThreadPoolManager() {
        return new ThreadPoolManager();
    }

    /**
     * 当前网络传输层 Bean。
     * Current network transport bean.
     *
     * @return 服务器传输 / server transport
     */
    @Bean
    @Lazy
    public ServerTransport loginServerTransport() {
        return NetConnector.currentTransport();
    }

    /**
     * MAC 封禁管理器 Bean。
     * Banned MAC manager bean.
     *
     * @return MAC 封禁管理器 / banned MAC manager
     */
    @Bean
    @Lazy
    public BannedMacManager loginBannedMacManager() {
        return new BannedMacManager();
    }

    /**
     * IP 封禁服务 Bean。
     * Banned IP service bean.
     *
     * banned IP service
     */
    @Bean
    @Lazy
    public LoginBannedIpService loginBannedIpService() {
        return new LoginBannedIpService();
    }

    /**
     * 暴力破解防护 Bean。
     * Brute-force protector bean.
     *
     * @return 暴力破解防护 / brute-force protector
     */
    @Bean
    @Lazy
    public BruteForceProtector loginBruteForceProtector() {
        return new BruteForceProtector();
    }

    /**
     * 洪水攻击防护 Bean。
     * Flood protector bean.
     *
     * flood protector
     */
    @Bean
    @Lazy
    public FloodProtector loginFloodProtector() {
        return new FloodProtector();
    }

    /**
     * 关机/重启协调器 Bean。
     * Shutdown/restart coordinator bean.
     *
     * @return 关机协调器 / shutdown coordinator
     */
    @Bean
    @Lazy
    public Shutdown loginShutdown() {
        return new Shutdown();
    }
}
