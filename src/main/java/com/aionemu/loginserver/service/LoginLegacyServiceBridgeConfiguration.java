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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
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
     * 装配登录服持久层依赖聚合并在启动期绑定至 {@link LoginDaosHolder}；
     * 任一 DAO 缺失（离线/单测上下文）时降级为 safeDao 离线聚合，保持旧行为等价。
     * Wires the login-server persistence aggregate and binds it to {@link LoginDaosHolder}
     * at startup; falls back to the safeDao offline aggregate when any DAO is missing
     * (offline/unit-test contexts), preserving legacy behavior.
     */
    @Bean
    public LoginDaos loginDaos(ConfigurableBeanFactory beanFactory) {
        try {
            LoginDaos daos = new LoginDaos(
                daoFrom(beanFactory, com.aionemu.loginserver.dao.AccountDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.AccountTimeDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.AccountPlayTimeDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.PremiumDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.VipDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.BannedIpDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.BannedMacDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.GameServersDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.PlayerTransferDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.SvStatsDAO.class),
                daoFrom(beanFactory, com.aionemu.loginserver.dao.TaskFromDBDAO.class));
            LoginDaosHolder.bindDaos(daos);
            return daos;
        } catch (NullPointerException offline) {
            return LoginDaosHolder.daos();
        }
    }

    /**
     * 角色转移服务 Bean。
     * Player transfer service bean.
     * @return 角色转移服务 / player transfer service
     */
    @Bean
    @Lazy
    public PlayerTransferService loginPlayerTransferService() {
        return new PlayerTransferService();
    }

    /**
     * 按类型从容器解析 DAO Bean，缺失（离线/单测上下文）时返回 null。
     * Resolves a DAO bean by type from the container; returns null when absent (offline/unit tests).
     */
    private static <T extends com.aionemu.commons.database.dao.DAO> T daoFrom(ConfigurableBeanFactory beanFactory,
            Class<T> daoClass) {
        return beanFactory.getBeanProvider(daoClass).getIfAvailable();
    }

    /**
     * 高级账号控制器 Bean。
     * Premium account controller bean.
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
     * @return IP 封禁服务 / banned IP service
     */
    @Bean
    @Lazy
    public LoginBannedIpService loginBannedIpService() {
        return new LoginBannedIpService();
    }

    /**
     * 暴力破解防护 Bean。
     * Brute-force protector bean.
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
     * @return 洪水防护 / flood protector
     */
    @Bean
    @Lazy
    public FloodProtector loginFloodProtector() {
        return new FloodProtector();
    }

    /**
     * 关机/重启协调器 Bean。
     * Shutdown/restart coordinator bean.
     * @return 关机协调器 / shutdown coordinator
     */
    @Bean
    @Lazy
    public Shutdown loginShutdown() {
        return new Shutdown();
    }
}
