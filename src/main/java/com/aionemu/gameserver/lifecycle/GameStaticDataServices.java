package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 静态数据服务定位器：向 DataManager / HTMLCache / XmlDataLoader 注入 Spring 提供者。
 * HTMLCache / XmlDataLoader.
 */
@Component
public final class GameStaticDataServices implements DisposableBean {

    /**
     * DataManager 提供者的静态缓存。
     * Static cache of the DataManager provider.
     */
    private static volatile ObjectProvider<DataManager> dataManagerProvider;

    /**
     * HTMLCache 提供者的静态缓存。
     * Static cache of the HTMLCache provider.
     */
    private static volatile ObjectProvider<HTMLCache> htmlCacheProvider;

    /**
     * XmlDataLoader 提供者的静态缓存。
     * Static cache of the XmlDataLoader provider.
     */
    private static volatile ObjectProvider<XmlDataLoader> xmlDataLoaderProvider;

    /**
     * 构造并注册各静态数据组件的实例提供者。
     * Construct and register instance providers for static-data components.
     *
     * DataManager provider
     * HTMLCache provider
     * XmlDataLoader provider
     */
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

    /**
     * 解析 DataManager：优先 Spring，否则回退。
     * Resolve DataManager: prefer Spring, otherwise fallback.
     *
     * DataManager instance
     */
    public static DataManager dataManager() {
        ObjectProvider<DataManager> provider = dataManagerProvider;
        if (provider == null) {
            return GameCoreServiceFallbacks.dataManager();
        }
        return provider.getIfAvailable(GameCoreServiceFallbacks::dataManager);
    }

    /**
     * 解析 HTMLCache：优先 Spring，否则回退。
     * Resolve HTMLCache: prefer Spring, otherwise fallback.
     *
     * HTMLCache instance
     */
    public static HTMLCache htmlCache() {
        ObjectProvider<HTMLCache> provider = htmlCacheProvider;
        if (provider == null) {
            return GameCoreServiceFallbacks.htmlCache();
        }
        return provider.getIfAvailable(GameCoreServiceFallbacks::htmlCache);
    }

    /**
     * 解析 XmlDataLoader：优先 Spring，否则回退。
     * Resolve XmlDataLoader: prefer Spring, otherwise fallback.
     *
     * XmlDataLoader instance
     */
    public static XmlDataLoader xmlDataLoader() {
        ObjectProvider<XmlDataLoader> provider = xmlDataLoaderProvider;
        if (provider == null) {
            return GameCoreServiceFallbacks.xmlDataLoader();
        }
        return provider.getIfAvailable(GameCoreServiceFallbacks::xmlDataLoader);
    }

    /**
     * 销毁时清空静态提供者与单例注册。
     * Clear static providers and singleton registrations on destroy.
     */
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
