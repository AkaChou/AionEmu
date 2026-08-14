package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.BannedMacManager;
import com.aionemu.loginserver.utils.BruteForceProtector;
import com.aionemu.loginserver.utils.FloodProtector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服防护相关组件的静态访问桥（MAC/IP 封禁、暴力破解与洪水防护）。
 * Static access bridge for login protection components (MAC/IP ban, brute-force & flood).
 */
@Component
public final class LoginProtectionServices implements DisposableBean {

    private static volatile ObjectProvider<BannedMacManager> bannedMacManagerProvider;
    private static volatile ObjectProvider<LoginBannedIpService> bannedIpServiceProvider;
    private static volatile ObjectProvider<BruteForceProtector> bruteForceProtectorProvider;
    private static volatile ObjectProvider<FloodProtector> floodProtectorProvider;
    private static volatile BannedMacManager resolvedBannedMacManager;
    private static volatile LoginBannedIpService resolvedBannedIpService;
    private static volatile BruteForceProtector resolvedBruteForceProtector;
    private static volatile FloodProtector resolvedFloodProtector;

    /**
     * 注入并缓存各防护组件的 {@link ObjectProvider}。
     * Inject and cache providers for all protection components.
     *
     * @param bannedMacManagerProvider MAC 封禁管理器提供者 / banned MAC manager provider
     * @param bannedIpServiceProvider IP 封禁服务提供者 / banned IP service provider
     * @param bruteForceProtectorProvider 暴力破解防护提供者 / brute-force protector provider
     * @param floodProtectorProvider 洪水防护提供者 / flood protector provider
     */
    public LoginProtectionServices(
        ObjectProvider<BannedMacManager> bannedMacManagerProvider,
        ObjectProvider<LoginBannedIpService> bannedIpServiceProvider,
        ObjectProvider<BruteForceProtector> bruteForceProtectorProvider,
        ObjectProvider<FloodProtector> floodProtectorProvider
    ) {
        LoginProtectionServices.bannedMacManagerProvider = bannedMacManagerProvider;
        LoginProtectionServices.bannedIpServiceProvider = bannedIpServiceProvider;
        LoginProtectionServices.bruteForceProtectorProvider = bruteForceProtectorProvider;
        LoginProtectionServices.floodProtectorProvider = floodProtectorProvider;
    }

    /**
     * 获取 MAC 封禁管理器。
     * Resolve the banned MAC manager.
     *
     * @return MAC 封禁管理器 / banned MAC manager
     */
    public static BannedMacManager bannedMacManager() {
        ObjectProvider<BannedMacManager> provider = bannedMacManagerProvider;
        if (provider == null) {
            BannedMacManager resolved = resolvedBannedMacManager;
            if (resolved != null) {
                return resolved;
            }
            return rememberBannedMacManager(fallbackBannedMacManager());
        }
        return rememberBannedMacManager(provider.getIfAvailable(LoginProtectionServices::fallbackBannedMacManager));
    }

    /**
     * 获取 IP 封禁服务。
     * Resolve the banned IP service.
     *
     * @return IP 封禁服务 / banned IP service
     */
    public static LoginBannedIpService bannedIpService() {
        ObjectProvider<LoginBannedIpService> provider = bannedIpServiceProvider;
        if (provider == null) {
            LoginBannedIpService resolved = resolvedBannedIpService;
            if (resolved != null) {
                return resolved;
            }
            return new LoginBannedIpService();
        }
        return rememberBannedIpService(provider.getIfAvailable(LoginBannedIpService::new));
    }

    /**
     * 获取暴力破解防护器。
     * Resolve the brute-force protector.
     *
     * @return 暴力破解防护 / brute-force protector
     */
    public static BruteForceProtector bruteForceProtector() {
        ObjectProvider<BruteForceProtector> provider = bruteForceProtectorProvider;
        if (provider == null) {
            BruteForceProtector resolved = resolvedBruteForceProtector;
            if (resolved != null) {
                return resolved;
            }
            return rememberBruteForceProtector(fallbackBruteForceProtector());
        }
        return rememberBruteForceProtector(provider.getIfAvailable(LoginProtectionServices::fallbackBruteForceProtector));
    }

    /**
     * 获取洪水防护器。
     * Resolve the flood protector.
     *
     * @return 洪水防护 / flood protector
     */
    public static FloodProtector floodProtector() {
        ObjectProvider<FloodProtector> provider = floodProtectorProvider;
        if (provider == null) {
            FloodProtector resolved = resolvedFloodProtector;
            if (resolved != null) {
                return resolved;
            }
            return rememberFloodProtector(fallbackFloodProtector());
        }
        return rememberFloodProtector(provider.getIfAvailable(LoginProtectionServices::fallbackFloodProtector));
    }

    /**
     * Spring 销毁时清理静态 provider。
     * Clear static providers on Spring destroy.
     */
    @Override
    public void destroy() {
        bannedMacManagerProvider = null;
        bannedIpServiceProvider = null;
        bruteForceProtectorProvider = null;
        floodProtectorProvider = null;
    }

    /**
     * 回退 MAC 封禁管理器。
     * Fallback banned MAC manager.
     *
     * @return 回退实例 / fallback instance
     */
    private static BannedMacManager fallbackBannedMacManager() {
        return Fallbacks.BANNED_MAC_MANAGER;
    }

    /**
     * 回退暴力破解防护。
     * Fallback brute-force protector.
     *
     * @return 回退实例 / fallback instance
     */
    private static BruteForceProtector fallbackBruteForceProtector() {
        return Fallbacks.BRUTE_FORCE_PROTECTOR;
    }

    /**
     * 回退洪水防护。
     * Fallback flood protector.
     *
     * @return 回退实例 / fallback instance
     */
    private static FloodProtector fallbackFloodProtector() {
        return Fallbacks.FLOOD_PROTECTOR;
    }

    /**
     * 缓存 MAC 封禁管理器。
     * Remember the banned MAC manager.
     *
     * @param bannedMacManager 管理器 / manager
     * @return 同一实例 / same instance
     */
    private static BannedMacManager rememberBannedMacManager(BannedMacManager bannedMacManager) {
        resolvedBannedMacManager = bannedMacManager;
        return bannedMacManager;
    }

    /**
     * 缓存 IP 封禁服务。
     * Remember the banned IP service.
     *
     * @param bannedIpService 服务 / service
     * @return 同一实例 / same instance
     */
    private static LoginBannedIpService rememberBannedIpService(LoginBannedIpService bannedIpService) {
        resolvedBannedIpService = bannedIpService;
        return bannedIpService;
    }

    /**
     * 缓存暴力破解防护。
     * Remember the brute-force protector.
     *
     * @param bruteForceProtector 防护器 / protector
     * @return 同一实例 / same instance
     */
    private static BruteForceProtector rememberBruteForceProtector(BruteForceProtector bruteForceProtector) {
        resolvedBruteForceProtector = bruteForceProtector;
        return bruteForceProtector;
    }

    /**
     * 缓存洪水防护。
     * Remember the flood protector.
     *
     * @param floodProtector 防护器 / protector
     * @return 同一实例 / same instance
     */
    private static FloodProtector rememberFloodProtector(FloodProtector floodProtector) {
        resolvedFloodProtector = floodProtector;
        return floodProtector;
    }

    /**
     * 测试用：重置静态状态。
     * Test-only: reset static state.
     */
    static void resetForTests() {
        bannedMacManagerProvider = null;
        bannedIpServiceProvider = null;
        bruteForceProtectorProvider = null;
        floodProtectorProvider = null;
        resolvedBannedMacManager = null;
        resolvedBannedIpService = null;
        resolvedBruteForceProtector = null;
        resolvedFloodProtector = null;
    }

    /**
     * 无容器场景下的单例回退。
     * Singleton fallbacks when Spring is unavailable.
     */
    private static final class Fallbacks {

        private static final BannedMacManager BANNED_MAC_MANAGER = new BannedMacManager();
        private static final BruteForceProtector BRUTE_FORCE_PROTECTOR = new BruteForceProtector();
        private static final FloodProtector FLOOD_PROTECTOR = new FloodProtector();
    }
}
