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
     * @param dataManagerProvider DataManager 提供者 / DataManager provider
     * @param htmlCacheProvider HTMLCache 提供者 / HTMLCache provider
     * @param xmlDataLoaderProvider XmlDataLoader 提供者 / XmlDataLoader provider
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
     * 解析 DataManager：由 Spring 提供，容器未启动即抛异常。
     * Resolve DataManager from Spring, failing fast when the container is not up.
     * @return DataManager 实例 / DataManager instance
     */
    public static DataManager dataManager() {
        ObjectProvider<DataManager> provider = dataManagerProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "DataManager is only available inside the Spring container; "
                    + "offline construction must not touch static data services");
        }
        return provider.getObject();
    }

    /**
     * 解析 HTMLCache：由 Spring 提供，容器未启动即抛异常。
     * Resolve HTMLCache from Spring, failing fast when the container is not up.
     * @return HTMLCache 实例 / HTMLCache instance
     */
    public static HTMLCache htmlCache() {
        ObjectProvider<HTMLCache> provider = htmlCacheProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "HTMLCache is only available inside the Spring container; "
                    + "offline construction must not touch static data services");
        }
        return provider.getObject();
    }

    /**
     * 解析 XmlDataLoader：由 Spring 提供，容器未启动即抛异常。
     * Resolve XmlDataLoader from Spring, failing fast when the container is not up.
     * @return XmlDataLoader 实例 / XmlDataLoader instance
     */
    public static XmlDataLoader xmlDataLoader() {
        ObjectProvider<XmlDataLoader> provider = xmlDataLoaderProvider;
        if (provider == null) {
            throw new IllegalStateException(
                "XmlDataLoader is only available inside the Spring container; "
                    + "offline construction must not touch static data services");
        }
        return provider.getObject();
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
