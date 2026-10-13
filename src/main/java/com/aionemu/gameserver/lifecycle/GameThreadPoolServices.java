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
     * @param threadPoolManagerProvider ThreadPoolManager 提供者 / ThreadPoolManager provider
     */
    public GameThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        GameThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
        resolvedThreadPoolManager = null;
        ThreadPoolManager.setInstanceProvider(threadPoolManagerProvider);
    }

    /**
     * 解析 ThreadPoolManager 并缓存。
     * Resolve ThreadPoolManager and remember the result.
     * @return ThreadPoolManager 实例 / ThreadPoolManager instance
     * @throws IllegalStateException 容器未启动即请求线程池（如离线构造服务）/ when the container is not up yet
     */
    public static ThreadPoolManager threadPoolManager() {
        ThreadPoolManager resolved = resolvedThreadPoolManager;
        if (resolved != null) {
            return resolved;
        }
        if (threadPoolManagerProvider == null) {
            throw new IllegalStateException(
                "ThreadPoolManager is only available inside the Spring container; "
                    + "offline construction must not touch the thread pool");
        }
        return rememberThreadPoolManager(threadPoolManagerProvider.getObject());
    }

    /**
     * 记住已解析的 ThreadPoolManager。
     * Remember the resolved ThreadPoolManager.
     * @param threadPoolManager 已解析实例 / Resolved instance
     * @return 同一实例 / The same instance
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

}
