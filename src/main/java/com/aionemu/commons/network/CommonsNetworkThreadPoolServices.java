package com.aionemu.commons.network;

import com.aionemu.commons.network.util.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 网络线程池服务入口，支持 Spring 注入与静态回退。
 * Network thread-pool service entry with Spring injection and static fallback.
 */
@Component
public final class CommonsNetworkThreadPoolServices implements DisposableBean {

    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private static volatile ThreadPoolManager resolvedThreadPoolManager;

    /**
     * 注册 Spring 提供的线程池管理器。
     * Register Spring-provided thread pool manager.
     *
     * @param threadPoolManagerProvider 线程池管理器提供者 / Thread pool manager provider
     */
    public CommonsNetworkThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        CommonsNetworkThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    /**
     * 获取可用的线程池管理器（优先 Spring，否则回退）。
     * Get available thread pool manager (Spring preferred, then fallback).
     *
     * @return 线程池管理器 / Thread pool manager
     */
    public static ThreadPoolManager threadPoolManager() {
        ObjectProvider<ThreadPoolManager> provider = threadPoolManagerProvider;
        if (provider == null) {
            ThreadPoolManager resolved = resolvedThreadPoolManager;
            if (resolved != null) {
                return resolved;
            }
            return rememberThreadPoolManager(fallbackThreadPoolManager());
        }
        return rememberThreadPoolManager(provider.getIfAvailable(CommonsNetworkThreadPoolServices::fallbackThreadPoolManager));
    }

    /**
     * 缓存已解析的线程池管理器。
     * Remember resolved thread pool manager.
     *
     * @param threadPoolManager 线程池管理器 / Thread pool manager
     * Same instance
     */
    static ThreadPoolManager rememberThreadPoolManager(ThreadPoolManager threadPoolManager) {
        resolvedThreadPoolManager = threadPoolManager;
        return threadPoolManager;
    }

    /**
     * 销毁时清理静态提供者引用。
     * Clear static provider reference on destroy.
     */
    @Override
    public void destroy() {
        threadPoolManagerProvider = null;
    }

    /**
     * 回退线程池管理器。
     * Fallback thread pool manager.
     *
     * Fallback instance
     */
    private static ThreadPoolManager fallbackThreadPoolManager() {
        return Fallbacks.THREAD_POOL_MANAGER;
    }

    /**
     * 懒加载回退单例持有者。
     * Lazy fallback singleton holder.
     */
    private static final class Fallbacks {

        private static final ThreadPoolManager THREAD_POOL_MANAGER = new ThreadPoolManager();
    }
}
