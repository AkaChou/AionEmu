package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 核心服务运行时桥接：解析 DataManager、线程池与 HTML 缓存，无 Spring 时回退。
 * Core-services runtime bridge: resolves DataManager, thread pool and HTML cache; falls back when Spring is absent.
 */
@Component
public class GameCoreServicesRuntimeBridge {

    /**
     * 数据管理器提供者。
     * DataManager provider.
     */
    private ObjectProvider<DataManager> dataManagerProvider;

    /**
     * 线程池管理器提供者。
     * Thread-pool manager provider.
     */
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    /**
     * HTML 缓存提供者。
     * HTML cache provider.
     */
    private ObjectProvider<HTMLCache> htmlCacheProvider;

    /**
     * 可选注入数据管理器 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of DataManager.
     *
     * @param dataManagerProvider 数据管理器提供者 / DataManager provider
     */
    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }

    /**
     * 可选注入线程池管理器 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of ThreadPoolManager.
     *
     * @param threadPoolManagerProvider 线程池管理器提供者 / Thread-pool manager provider
     */
    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    /**
     * 可选注入 HTML 缓存 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of HTMLCache.
     *
     * @param htmlCacheProvider HTML 缓存提供者 / HTML cache provider
     */
    @Autowired(required = false)
    void setHtmlCacheProvider(ObjectProvider<HTMLCache> htmlCacheProvider) {
        this.htmlCacheProvider = htmlCacheProvider;
    }

    /**
     * 解析数据管理器：优先 Spring，否则回退。
     * Resolve DataManager: prefer Spring, otherwise fall back.
     *
     * @return 数据管理器 / Data manager
     */
    public DataManager dataManager() {
        if (dataManagerProvider == null) {
            return GameCoreServiceFallbacks.dataManager();
        }
        return dataManagerProvider.getIfAvailable(GameCoreServiceFallbacks::dataManager);
    }

    /**
     * 解析线程池管理器：优先 Spring，否则回退。
     * Resolve ThreadPoolManager: prefer Spring, otherwise fall back.
     *
     * @return 线程池管理器 / Thread-pool manager
     */
    public ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return GameCoreServiceFallbacks.threadPoolManager();
        }
        return threadPoolManagerProvider.getIfAvailable(GameCoreServiceFallbacks::threadPoolManager);
    }

    /**
     * 解析 HTML 缓存：优先 Spring，否则回退。
     * Resolve HTMLCache: prefer Spring, otherwise fall back.
     *
     * @return HTML 缓存 / HTML cache
     */
    public HTMLCache htmlCache() {
        if (htmlCacheProvider == null) {
            return GameCoreServiceFallbacks.htmlCache();
        }
        return htmlCacheProvider.getIfAvailable(GameCoreServiceFallbacks::htmlCache);
    }
}
