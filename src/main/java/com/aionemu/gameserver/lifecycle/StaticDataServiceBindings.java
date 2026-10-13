package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 静态数据服务静态绑定：把 Spring ObjectProvider 喂入静态数据域服务自持的 fail-fast 单例通道。
 * Static-data static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Static-data services.
 * <p>本组件取代已退役的静态数据静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Static-data static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class StaticDataServiceBindings implements DisposableBean {

    /**
     * 构造并注册各静态数据服务的实例提供者。
     * Construct and register instance providers for Static-data services.
     * @param dataManagerProvider DataManager 提供者 / DataManager provider
     * @param htmlCacheProvider HTMLCache 提供者 / HTMLCache provider
     * @param xmlDataLoaderProvider XmlDataLoader 提供者 / XmlDataLoader provider
     */
    public StaticDataServiceBindings(ObjectProvider<DataManager> dataManagerProvider,
            ObjectProvider<HTMLCache> htmlCacheProvider,
            ObjectProvider<XmlDataLoader> xmlDataLoaderProvider) {
        DataManager.setInstanceProvider(dataManagerProvider);
        HTMLCache.setInstanceProvider(htmlCacheProvider);
        XmlDataLoader.setInstanceProvider(xmlDataLoaderProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        DataManager.setInstanceProvider(null);
        HTMLCache.setInstanceProvider(null);
        XmlDataLoader.setInstanceProvider(null);
    }
}
