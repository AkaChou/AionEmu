package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameThreadPoolServices implements DisposableBean {

    public GameThreadPoolServices(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        ThreadPoolManager.setInstanceProvider(threadPoolManagerProvider);
    }

    @Override
    public void destroy() {
        ThreadPoolManager.setInstanceProvider(null);
    }
}
