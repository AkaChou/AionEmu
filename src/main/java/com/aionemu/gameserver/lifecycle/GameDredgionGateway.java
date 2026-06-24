package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameDredgionGateway {

    public void start() {
        Util.printSection(" *** Dredgion *** ");
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            DredgionService2.getInstance().initDredgion();
        }
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            AsyunatarService.getInstance().initAsyunatar();
        }
    }
}
