package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.gametime.GameTimeManager;

public class GameRuntimeServiceBridge {

    public void loadInstances() {
        InstanceService.load();
    }

    public void startGameTimeClock() {
        GameTimeManager.startClock();
    }
}
