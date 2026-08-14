package com.aionemu.loginserver.service;

import com.aionemu.loginserver.controller.PremiumController;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服高级账号控制器的静态访问桥。
 * Static access bridge for the login premium controller.
 */
@Component
public final class LoginPremiumServices implements DisposableBean {

    private static volatile ObjectProvider<PremiumController> premiumControllerProvider;
    private static volatile PremiumController resolvedPremiumController;

    /**
     * 注入并缓存高级账号控制器 {@link ObjectProvider}。
     * Inject and cache the premium controller {@link ObjectProvider}.
     *
     * @param premiumControllerProvider 控制器提供者 / controller provider
     */
    public LoginPremiumServices(ObjectProvider<PremiumController> premiumControllerProvider) {
        LoginPremiumServices.premiumControllerProvider = premiumControllerProvider;
    }

    /**
     * 获取 {@link PremiumController}；必要时使用回退实例并缓存。
     * Resolve {@link PremiumController}; cache a fallback instance when needed.
     *
     * @return 高级账号控制器 / premium controller
     */
    public static PremiumController premiumController() {
        ObjectProvider<PremiumController> provider = premiumControllerProvider;
        if (provider == null) {
            PremiumController resolved = resolvedPremiumController;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackPremiumController());
        }
        return remember(provider.getIfAvailable(LoginPremiumServices::fallbackPremiumController));
    }

    /**
     * Spring 销毁时清理静态 provider。
     * Clear the static provider on Spring destroy.
     */
    @Override
    public void destroy() {
        premiumControllerProvider = null;
    }

    /**
     * 创建回退控制器实例。
     * Create the fallback controller instance.
     *
     * @return 回退控制器 / fallback controller
     */
    private static PremiumController fallbackPremiumController() {
        return Fallbacks.PREMIUM_CONTROLLER;
    }

    /**
     * 缓存已解析的控制器并返回。
     * Remember the resolved controller and return it.
     *
     * @param premiumController 控制器 / controller
     * @return 同一控制器 / same controller
     */
    private static PremiumController remember(PremiumController premiumController) {
        resolvedPremiumController = premiumController;
        return premiumController;
    }

    /**
     * 测试用：重置静态状态。
     * Test-only: reset static state.
     */
    static void resetForTests() {
        premiumControllerProvider = null;
        resolvedPremiumController = null;
    }

    /**
     * 无容器场景下的单例回退。
     * Singleton fallback when Spring is unavailable.
     */
    private static final class Fallbacks {

        private static final PremiumController PREMIUM_CONTROLLER = new PremiumController();
    }
}
