package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * 核心服务回退工厂：Spring Bean 不可用时提供懒加载单例；已退役双源兜底的组件直接 fail-fast。
 * Core service fallbacks: lazy singleton holders when Spring beans are unavailable;
 * components whose dual-source fallback is retired fail fast instead.
 */
final class GameCoreServiceFallbacks {

    /**
     * 工具类禁止实例化。
     * Utility class; not instantiable.
     */
    private GameCoreServiceFallbacks() {
    }

    /**
     * 返回 DataManager：双源兜底已退役，交由 {@link DataManager#getInstance()} fail-fast。
     * Returns DataManager: the dual-source fallback is retired; delegates to DataManager.getInstance() and fails fast.
     *
     * @return DataManager 实例 / DataManager instance
     */
    static DataManager dataManager() {
        return DataManager.getInstance();
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
     * @return HTML 缓存 / HTML cache
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
