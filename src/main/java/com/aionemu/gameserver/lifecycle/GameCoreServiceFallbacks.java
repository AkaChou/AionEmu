package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 核心服务回退工厂：Spring Bean 不可用时提供懒加载单例。
 * Core service fallbacks: lazy singleton holders when Spring beans are unavailable.
 */
final class GameCoreServiceFallbacks {

    /**
     * 工具类禁止实例化。
     * Utility class; not instantiable.
     */
    private GameCoreServiceFallbacks() {
    }

    /**
     * 数据管理器回退实例。
     * DataManager fallback instance.
     *
     * @return 数据管理器 / Data manager
     */
    static DataManager dataManager() {
        return DataManagerFallback.INSTANCE;
    }

    /**
     * 线程池管理器（经 {@link GameThreadPoolServices} 解析）。
     * Thread-pool manager (resolved via {@link GameThreadPoolServices}).
     *
     * @return 线程池管理器 / Thread-pool manager
     */
    static ThreadPoolManager threadPoolManager() {
        return GameThreadPoolServices.threadPoolManager();
    }

    /**
     * HTML 缓存回退实例。
     * HTML cache fallback instance.
     *
     * HTML cache
     */
    static HTMLCache htmlCache() {
        return HtmlCacheFallback.INSTANCE;
    }

    /**
     * XML 数据加载器回退实例。
     * XML data loader fallback instance.
     *
     * @return XML 数据加载器 / XML data loader
     */
    static XmlDataLoader xmlDataLoader() {
        return XmlDataLoaderFallback.INSTANCE;
    }

    /**
     * {@link DataManager} 懒加载单例持有者。
     * Lazy singleton holder for {@link DataManager}.
     */
    private static final class DataManagerFallback {
        private static final DataManager INSTANCE = DataManager.getInstance();
    }

    /**
     * {@link HTMLCache} 懒加载单例持有者。
     * Lazy singleton holder for {@link HTMLCache}.
     */
    private static final class HtmlCacheFallback {
        private static final HTMLCache INSTANCE = HTMLCache.getInstance();
    }

    /**
     * {@link XmlDataLoader} 懒加载单例持有者。
     * Lazy singleton holder for {@link XmlDataLoader}.
     */
    private static final class XmlDataLoaderFallback {
        private static final XmlDataLoader INSTANCE = XmlDataLoader.getInstance();
    }
}
