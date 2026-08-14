package com.aionemu.loginserver.service;

import com.aionemu.loginserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 登录服线程池服务定位器，提供 {@link ThreadPoolManager} 的静态访问、缓存与 Spring 回退。
 * Login-server thread-pool service locator providing static access to {@link ThreadPoolManager}
 * with caching and a local fallback.
 */
@Component
public final class LoginThreadPoolServices implements DisposableBean {

    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private static volatile ThreadPoolManager resolvedThreadPoolManager;

    /**
     * 构造并注册 {@link ThreadPoolManager} 的 Spring 提供者。
     * Construct and register the Spring provider for {@link ThreadPoolManager}.
     *
     * @param threadPoolManagerProvider 线程池管理器提供者 / thread-pool manager provider
     */
    public LoginThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        LoginThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    /**
     * 获取线程池管理器：优先 Spring Bean，缓存解析结果，否则回退本地单例。
     * Obtain the thread-pool manager: prefer Spring bean, cache the resolution, else fall back to a local singleton.
     *
     * @return 线程池管理器 / thread-pool manager
     */
    public static ThreadPoolManager threadPoolManager() {
        ObjectProvider<ThreadPoolManager> provider = threadPoolManagerProvider;
        if (provider == null) {
            ThreadPoolManager resolved = resolvedThreadPoolManager;
            if (resolved != null) {
                return resolved;
            }
            return remember(fallbackThreadPoolManager());
        }
        return remember(provider.getIfAvailable(LoginThreadPoolServices::fallbackThreadPoolManager));
    }

    /**
     * Spring 销毁时清空静态提供者引用。
     * Clear the static provider reference on Spring destroy.
     */
    @Override
    public void destroy() {
        threadPoolManagerProvider = null;
    }

    /**
     * 返回回退用的本地 {@link ThreadPoolManager} 实例。
     * Return the local fallback {@link ThreadPoolManager} instance.
     *
     * @return 回退线程池管理器 / fallback thread-pool manager
     */
    private static ThreadPoolManager fallbackThreadPoolManager() {
        return Fallbacks.THREAD_POOL_MANAGER;
    }

    /**
     * 缓存并返回已解析的线程池管理器。
     * Remember and return the resolved thread-pool manager.
     *
     * @param threadPoolManager 已解析的管理器 / resolved manager
     * @return 同一实例 / the same instance
     */
    private static ThreadPoolManager remember(ThreadPoolManager threadPoolManager) {
        resolvedThreadPoolManager = threadPoolManager;
        return threadPoolManager;
    }

    /**
     * 测试用：重置静态提供者与缓存。
     * Test-only: reset static provider and cache.
     */
    static void resetForTests() {
        threadPoolManagerProvider = null;
        resolvedThreadPoolManager = null;
    }

    /**
     * 延迟初始化的回退单例持有者。
     * Lazy holder for the fallback singleton.
     */
    private static final class Fallbacks {

        private static final ThreadPoolManager THREAD_POOL_MANAGER = new ThreadPoolManager();
    }
}
