package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.dataholders.DataManager;
import org.springframework.stereotype.Component;

@Component
public class GameStaticDataGateway {

    public void load() {
        DataManager.getInstance();
    }
}
