package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameStaticDataServices implements DisposableBean {

    public GameStaticDataServices(ObjectProvider<DataManager> dataManagerProvider,
            ObjectProvider<HTMLCache> htmlCacheProvider) {
        DataManager.setInstanceProvider(dataManagerProvider);
        HTMLCache.setInstanceProvider(htmlCacheProvider);
    }

    @Override
    public void destroy() {
        DataManager.setInstanceProvider(null);
        HTMLCache.setInstanceProvider(null);
    }
}
