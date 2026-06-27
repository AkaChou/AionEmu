package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameThreadPoolServices implements DisposableBean {

    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private static volatile ThreadPoolManager resolvedThreadPoolManager;

    public GameThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        GameThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
        ThreadPoolManager.setInstanceProvider(threadPoolManagerProvider);
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
        return rememberThreadPoolManager(provider.getIfAvailable(GameThreadPoolServices::fallbackThreadPoolManager));
    }

    static ThreadPoolManager rememberThreadPoolManager(ThreadPoolManager threadPoolManager) {
        resolvedThreadPoolManager = threadPoolManager;
        return threadPoolManager;
    }

    @Override
    public void destroy() {
        threadPoolManagerProvider = null;
        ThreadPoolManager.setInstanceProvider(null);
    }

    private static ThreadPoolManager fallbackThreadPoolManager() {
        return Fallbacks.THREAD_POOL_MANAGER;
    }

    private static final class Fallbacks {

        private static final ThreadPoolManager THREAD_POOL_MANAGER = new ThreadPoolManager();
    }
}
