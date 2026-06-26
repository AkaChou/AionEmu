package com.aionemu.commons.network;

import com.aionemu.commons.network.util.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class CommonsNetworkThreadPoolServices implements DisposableBean {

    private static volatile ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    public CommonsNetworkThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        CommonsNetworkThreadPoolServices.threadPoolManagerProvider = threadPoolManagerProvider;
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
