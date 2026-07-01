package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.utils.ThreadPoolManager;

final class GameCoreServiceFallbacks {

    private GameCoreServiceFallbacks() {
    }

    static DataManager dataManager() {
        return DataManagerFallback.INSTANCE;
    }

    static ThreadPoolManager threadPoolManager() {
        return GameThreadPoolServices.threadPoolManager();
    }

    static HTMLCache htmlCache() {
        return HtmlCacheFallback.INSTANCE;
    }

    static XmlDataLoader xmlDataLoader() {
        return XmlDataLoaderFallback.INSTANCE;
    }

    private static final class DataManagerFallback {
        private static final DataManager INSTANCE = DataManager.getInstance();
    }

    private static final class HtmlCacheFallback {
        private static final HTMLCache INSTANCE = HTMLCache.getInstance();
    }

    private static final class XmlDataLoaderFallback {
        private static final XmlDataLoader INSTANCE = XmlDataLoader.getInstance();
    }
}
