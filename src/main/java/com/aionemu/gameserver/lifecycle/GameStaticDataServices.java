package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameStaticDataServices implements DisposableBean {

    private static volatile ObjectProvider<DataManager> dataManagerProvider;
    private static volatile ObjectProvider<HTMLCache> htmlCacheProvider;
    private static volatile ObjectProvider<XmlDataLoader> xmlDataLoaderProvider;

    public GameStaticDataServices(ObjectProvider<DataManager> dataManagerProvider,
            ObjectProvider<HTMLCache> htmlCacheProvider,
            ObjectProvider<XmlDataLoader> xmlDataLoaderProvider) {
        GameStaticDataServices.dataManagerProvider = dataManagerProvider;
        GameStaticDataServices.htmlCacheProvider = htmlCacheProvider;
        GameStaticDataServices.xmlDataLoaderProvider = xmlDataLoaderProvider;
        DataManager.setInstanceProvider(dataManagerProvider);
        HTMLCache.setInstanceProvider(htmlCacheProvider);
        XmlDataLoader.setInstanceProvider(xmlDataLoaderProvider);
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

    public static XmlDataLoader xmlDataLoader() {
        ObjectProvider<XmlDataLoader> provider = xmlDataLoaderProvider;
        if (provider == null) {
            return GameCoreServiceFallbacks.xmlDataLoader();
        }
        return provider.getIfAvailable(GameCoreServiceFallbacks::xmlDataLoader);
    }

    @Override
    public void destroy() {
        dataManagerProvider = null;
        htmlCacheProvider = null;
        xmlDataLoaderProvider = null;
        DataManager.setInstanceProvider(null);
        HTMLCache.setInstanceProvider(null);
        XmlDataLoader.setInstanceProvider(null);
    }
}
