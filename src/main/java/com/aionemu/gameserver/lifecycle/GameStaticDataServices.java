package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameStaticDataServices implements DisposableBean {

    private static volatile ObjectProvider<DataManager> dataManagerProvider;
    private static volatile ObjectProvider<HTMLCache> htmlCacheProvider;

    public GameStaticDataServices(ObjectProvider<DataManager> dataManagerProvider,
            ObjectProvider<HTMLCache> htmlCacheProvider) {
        GameStaticDataServices.dataManagerProvider = dataManagerProvider;
        GameStaticDataServices.htmlCacheProvider = htmlCacheProvider;
        DataManager.setInstanceProvider(dataManagerProvider);
        HTMLCache.setInstanceProvider(htmlCacheProvider);
    }

    public static DataManager dataManager() {
        ObjectProvider<DataManager> provider = dataManagerProvider;
        if (provider == null) {
            return GameCoreServiceFallbacks.dataManager();
        }
        return provider.getIfAvailable(GameCoreServiceFallbacks::dataManager);
    }

    public static HTMLCache htmlCache() {
        ObjectProvider<HTMLCache> provider = htmlCacheProvider;
        if (provider == null) {
            return GameCoreServiceFallbacks.htmlCache();
        }
        return provider.getIfAvailable(GameCoreServiceFallbacks::htmlCache);
    }

    @Override
    public void destroy() {
        dataManagerProvider = null;
        htmlCacheProvider = null;
        DataManager.setInstanceProvider(null);
        HTMLCache.setInstanceProvider(null);
    }
}
