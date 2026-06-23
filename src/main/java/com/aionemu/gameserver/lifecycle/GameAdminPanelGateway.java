package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ServerCommandProcessor;
import com.aionemu.gameserver.configs.main.GSConfig;
import org.springframework.stereotype.Component;

@Component
public class GameAdminPanelGateway {

    public boolean isAdminPanelEnabled() {
        return GSConfig.SERVER_YAADMINPANEL_SWITCH_ON;
    }

    public void startAdminPanel() {
        new ServerCommandProcessor().startAdminPanel();
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
