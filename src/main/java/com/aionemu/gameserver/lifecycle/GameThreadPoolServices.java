package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 线程池服务定位器：向 ThreadPoolManager 注入 Spring 提供者并缓存解析结果。
 * Thread-pool service locator: injects Spring provider into ThreadPoolManager and caches resolution.
 */
@Component
public final class GameThreadPoolServices implements DisposableBean {

    /**
     * ThreadPoolManager 提供者的静态缓存。
     * Static cache of the ThreadPoolManager provider.
     */
    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    /**
     * 已解析的 ThreadPoolManager 缓存。
     * Cache of the resolved ThreadPoolManager.
     */
    private static volatile ThreadPoolManager resolvedThreadPoolManager;

    /**
     * 构造并注册 ThreadPoolManager 实例提供者。
     * Construct and register the ThreadPoolManager instance provider.
     *
     * ThreadPoolManager provider
     */
    public GameThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        GameThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
        resolvedThreadPoolManager = null;
        ThreadPoolManager.setInstanceProvider(threadPoolManagerProvider);
    }

    /**
     * 解析 ThreadPoolManager：优先 Spring，否则回退并缓存。
     * Resolve ThreadPoolManager: prefer Spring, otherwise fallback, and remember the result.
     *
     * ThreadPoolManager instance
     */
    public static ThreadPoolManager threadPoolManager() {
        ThreadPoolManager resolved = resolvedThreadPoolManager;
        if (resolved != null) {
            return resolved;
        }
        ObjectProvider<ThreadPoolManager> provider = threadPoolManagerProvider;
        if (provider == null) {
            return rememberThreadPoolManager(fallbackThreadPoolManager());
        }
        return rememberThreadPoolManager(provider.getIfAvailable(GameThreadPoolServices::fallbackThreadPoolManager));
    }

    /**
     * 记住已解析的 ThreadPoolManager。
     * Remember the resolved ThreadPoolManager.
     *
     * @param threadPoolManager 已解析实例 / Resolved instance
     * The same instance
     */
    static ThreadPoolManager rememberThreadPoolManager(ThreadPoolManager threadPoolManager) {
        resolvedThreadPoolManager = threadPoolManager;
        return threadPoolManager;
    }

    /**
     * 销毁时清空静态提供者与单例注册。
     * Clear the static provider and singleton registration on destroy.
     */
    @Override
    public void destroy() {
        threadPoolManagerProvider = null;
        resolvedThreadPoolManager = null;
        ThreadPoolManager.setInstanceProvider(null);
    }

    /**
     * 回退 ThreadPoolManager（懒加载单例）。
     * Fallback ThreadPoolManager (lazy singleton).
     *
     * Fallback instance
     */
    private static ThreadPoolManager fallbackThreadPoolManager() {
        return Fallbacks.THREAD_POOL_MANAGER;
    }

    /**
     * 回退持有者：延迟创建默认 ThreadPoolManager。
     * Fallback holder: lazily creates the default ThreadPoolManager.
     */
    private static final class Fallbacks {

        /**
         * 默认 ThreadPoolManager 单例。
         * Default ThreadPoolManager singleton.
         */
        private static final ThreadPoolManager THREAD_POOL_MANAGER = new ThreadPoolManager();
    }
}
