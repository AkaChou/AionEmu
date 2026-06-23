package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.springframework.stereotype.Component;

@Component
public class GameRatioLimitGateway {

    public boolean isRatioLimitationEnabled() {
        return GSConfig.ENABLE_RATIO_LIMITATION;
    }

    public void registerRatioLimitStartupHook() {
        GameServer.registerRatioLimitStartupHook();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
