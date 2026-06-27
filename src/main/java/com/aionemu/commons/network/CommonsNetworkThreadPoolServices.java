package com.aionemu.commons.network;

import com.aionemu.commons.network.util.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class CommonsNetworkThreadPoolServices implements DisposableBean {

    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private static volatile ThreadPoolManager resolvedThreadPoolManager;

    public CommonsNetworkThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        CommonsNetworkThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
    }

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

    static ThreadPoolManager rememberThreadPoolManager(ThreadPoolManager threadPoolManager) {
        resolvedThreadPoolManager = threadPoolManager;
        return threadPoolManager;
    }

    @Override
    public void destroy() {
        threadPoolManagerProvider = null;
    }

    private static ThreadPoolManager fallbackThreadPoolManager() {
        return Fallbacks.THREAD_POOL_MANAGER;
    }

    private static final class Fallbacks {

        private static final ThreadPoolManager THREAD_POOL_MANAGER = new ThreadPoolManager();
    }
}
