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
            return ThreadPoolManager.getInstance();
        }
        return provider.getIfAvailable(ThreadPoolManager::getInstance);
    }

    @Override
    public void destroy() {
        threadPoolManagerProvider = null;
    }
}
