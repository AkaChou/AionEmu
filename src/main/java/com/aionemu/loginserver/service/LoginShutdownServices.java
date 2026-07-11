package com.aionemu.loginserver.service;

import com.aionemu.loginserver.Shutdown;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服 {@link Shutdown} 的静态访问桥。
 * Static access bridge for login {@link Shutdown}.
 */
@Component
public final class LoginShutdownServices implements DisposableBean {

    private static volatile ObjectProvider<Shutdown> shutdownProvider;
    private static volatile Shutdown resolvedShutdown;

    /**
     * 注入并设置关机协调器 provider。
     * Inject and set the shutdown coordinator provider.
     *
     * @param shutdownProvider 关机协调器提供者 / shutdown provider
     */
    public LoginShutdownServices(ObjectProvider<Shutdown> shutdownProvider) {
        setShutdownProvider(shutdownProvider);
    }

    /**
     * 设置关机协调器 {@link ObjectProvider}。
     * Set the shutdown {@link ObjectProvider}.
     *
     * @param shutdownProvider 关机协调器提供者 / shutdown provider
     */
    public static void setShutdownProvider(ObjectProvider<Shutdown> shutdownProvider) {
        LoginShutdownServices.shutdownProvider = shutdownProvider;
    }

    /**
     * 获取 {@link Shutdown}；必要时使用回退实例并缓存。
     * Resolve {@link Shutdown}; cache a fallback instance when needed.
     *
     * @return 关机协调器 / shutdown coordinator
     */
    public static Shutdown shutdown() {
        ObjectProvider<Shutdown> provider = shutdownProvider;
        if (provider == null) {
            Shutdown resolved = resolvedShutdown;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackShutdown());
        }
        return remember(provider.getIfAvailable(LoginShutdownServices::fallbackShutdown));
    }

    /**
     * Spring 销毁时清理静态 provider。
     * Clear the static provider on Spring destroy.
     */
    @Override
    public void destroy() {
        shutdownProvider = null;
    }

    /**
     * 回退关机实例。
     * Fallback shutdown instance.
     *
     * fallback instance
     */
    private static Shutdown fallbackShutdown() {
        return Fallbacks.SHUTDOWN;
    }

    /**
     * 缓存已解析的关机实例。
     * Remember the resolved shutdown instance.
     *
     * @param shutdown 关机协调器 / shutdown coordinator
     * same instance
     */
    private static Shutdown remember(Shutdown shutdown) {
        resolvedShutdown = shutdown;
        return shutdown;
    }

    /**
     * 测试用：重置静态状态。
     * Test-only: reset static state.
     */
    public static void resetForTests() {
        shutdownProvider = null;
        resolvedShutdown = null;
    }

    /**
     * 无容器场景下的单例回退。
     * Singleton fallback when Spring is unavailable.
     */
    private static final class Fallbacks {

        private static final Shutdown SHUTDOWN = new Shutdown();
    }
}
