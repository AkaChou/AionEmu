package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameStaticDataGateway {

    private ObjectProvider<DataManager> dataManagerProvider;

    @Autowired(required = false)
    void setDataManagerProvider(ObjectProvider<DataManager> dataManagerProvider) {
        this.dataManagerProvider = dataManagerProvider;
    }

    public void load() {
        dataManager();
    }

    private DataManager dataManager() {
        if (dataManagerProvider == null) {
            return DataManager.getInstance();
        }
        return dataManagerProvider.getIfAvailable(DataManager::getInstance);
    }
}
