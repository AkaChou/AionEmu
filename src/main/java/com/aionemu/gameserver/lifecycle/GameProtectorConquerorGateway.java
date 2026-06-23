package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameProtectorConquerorGateway {

    public void start() {
        Util.printSection(" *** Protector/Conqueror initialization *** ");
        ProtectorConquerorService.getInstance().initSystem();
    }
}
