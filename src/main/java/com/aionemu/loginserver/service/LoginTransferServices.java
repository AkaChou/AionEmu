package com.aionemu.loginserver.service;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服玩家转移服务定位器，提供 {@link PlayerTransferService} 的静态访问、缓存与 Spring 回退。
 * Login-server player-transfer service locator providing static access to {@link PlayerTransferService}
 * local fallback. / local fallback.
 */
@Component
public final class LoginTransferServices implements DisposableBean {

    private static volatile ObjectProvider<PlayerTransferService> playerTransferServiceProvider;
    private static volatile PlayerTransferService resolvedPlayerTransferService;

    /**
     * 构造并注册 {@link PlayerTransferService} 的 Spring 提供者。
     * Construct and register the Spring provider for {@link PlayerTransferService}.
     *
     * @param playerTransferServiceProvider 玩家转移服务提供者 / player-transfer service provider
     */
    public LoginTransferServices(ObjectProvider<PlayerTransferService> playerTransferServiceProvider) {
        LoginTransferServices.playerTransferServiceProvider = playerTransferServiceProvider;
    }

    /**
     * 获取玩家转移服务：优先 Spring Bean，缓存解析结果，否则回退本地单例。
     * Obtain the player-transfer service: prefer Spring bean, cache the resolution, else fall back to a local singleton.
     *
     * @return 玩家转移服务 / player-transfer service
     */
    public static PlayerTransferService playerTransferService() {
        ObjectProvider<PlayerTransferService> provider = playerTransferServiceProvider;
        if (provider == null) {
            PlayerTransferService resolved = resolvedPlayerTransferService;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackPlayerTransferService());
        }
        return remember(provider.getIfAvailable(LoginTransferServices::fallbackPlayerTransferService));
    }

    /**
     * Spring 销毁时清空静态提供者引用。
     * Clear the static provider reference on Spring destroy.
     */
    @Override
    public void destroy() {
        playerTransferServiceProvider = null;
    }

    /**
     * 返回回退用的本地 {@link PlayerTransferService} 实例。
     * Return the local fallback {@link PlayerTransferService} instance.
     *
     * @return 回退玩家转移服务 / fallback player-transfer service
     */
    private static PlayerTransferService fallbackPlayerTransferService() {
        return Fallbacks.PLAYER_TRANSFER_SERVICE;
    }

    /**
     * 缓存并返回已解析的玩家转移服务。
     * Remember and return the resolved player-transfer service.
     *
     * resolved service
     * the same instance
     */
    private static PlayerTransferService remember(PlayerTransferService playerTransferService) {
        resolvedPlayerTransferService = playerTransferService;
        return playerTransferService;
    }

    /**
     * 测试用：重置静态提供者与缓存。
     * Test-only: reset static provider and cache.
     */
    static void resetForTests() {
        playerTransferServiceProvider = null;
        resolvedPlayerTransferService = null;
    }

    /**
     * 延迟初始化的回退单例持有者。
     * Lazy holder for the fallback singleton.
     */
    private static final class Fallbacks {

        private static final PlayerTransferService PLAYER_TRANSFER_SERVICE = new PlayerTransferService();
    }
}
