package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameStaticDataServices implements DisposableBean {

    public GameStaticDataServices(ObjectProvider<DataManager> dataManagerProvider) {
        DataManager.setInstanceProvider(dataManagerProvider);
    }

    @Override
    public void destroy() {
        DataManager.setInstanceProvider(null);
    }
}
