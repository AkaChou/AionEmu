package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameCoreServicesRuntimeBridge {

    private ObjectProvider<DataManager> dataManagerProvider;
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private ObjectProvider<HTMLCache> htmlCacheProvider;

    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }

    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    @Autowired(required = false)
    void setHtmlCacheProvider(ObjectProvider<HTMLCache> htmlCacheProvider) {
        this.htmlCacheProvider = htmlCacheProvider;
    }

    public DataManager dataManager() {
        if (dataManagerProvider == null) {
            return DataManager.getInstance();
        }
        return dataManagerProvider.getIfAvailable(DataManager::getInstance);
    }

    public ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return ThreadPoolManager.getInstance();
        }
        return threadPoolManagerProvider.getIfAvailable(ThreadPoolManager::getInstance);
    }

    public HTMLCache htmlCache() {
        if (htmlCacheProvider == null) {
            return HTMLCache.getInstance();
        }
        return htmlCacheProvider.getIfAvailable(HTMLCache::getInstance);
    }
}
