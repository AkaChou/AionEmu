package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameCoreServicesRuntimeBridge {

    public DataManager dataManager() {
        return DataManager.getInstance();
    }

    public ThreadPoolManager threadPoolManager() {
        return ThreadPoolManager.getInstance();
    }

    public HTMLCache htmlCache() {
        return HTMLCache.getInstance();
    }
}
