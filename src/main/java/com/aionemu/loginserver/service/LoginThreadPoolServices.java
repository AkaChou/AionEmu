package com.aionemu.loginserver.service;

import com.aionemu.loginserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class LoginThreadPoolServices implements DisposableBean {

    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    public LoginThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        LoginThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    public static ThreadPoolManager threadPoolManager() {
        ObjectProvider<ThreadPoolManager> provider = threadPoolManagerProvider;
        if (provider == null) {
            return fallbackThreadPoolManager();
        }
        return provider.getIfAvailable(LoginThreadPoolServices::fallbackThreadPoolManager);
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
