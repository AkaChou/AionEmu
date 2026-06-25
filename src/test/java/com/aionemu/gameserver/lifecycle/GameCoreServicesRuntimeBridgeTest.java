package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameCoreServicesRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        DataManager dataManager = instance(DataManager.class);
        ThreadPoolManager threadPoolManager = instance(ThreadPoolManager.class);
        HTMLCache htmlCache = instance(HTMLCache.class);
        GameCoreServicesRuntimeBridge runtimeBridge = new GameCoreServicesRuntimeBridge();

        runtimeBridge.setDataManagerProvider(provider(DataManager.class, dataManager));
        runtimeBridge.setThreadPoolManagerProvider(provider(ThreadPoolManager.class, threadPoolManager));
        runtimeBridge.setHtmlCacheProvider(provider(HTMLCache.class, htmlCache));

        assertSame(dataManager, runtimeBridge.dataManager());
        assertSame(threadPoolManager, runtimeBridge.threadPoolManager());
        assertSame(htmlCache, runtimeBridge.htmlCache());
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
