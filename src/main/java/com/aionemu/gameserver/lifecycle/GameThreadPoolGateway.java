package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameThreadPoolGateway {

    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    public void start() {
        threadPoolManager();
    }

    public void stop() {
        threadPoolManager().shutdown();
    }

    private ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return ThreadPoolManager.getInstance();
        }
        return threadPoolManagerProvider.getIfAvailable(ThreadPoolManager::getInstance);
    }
}
