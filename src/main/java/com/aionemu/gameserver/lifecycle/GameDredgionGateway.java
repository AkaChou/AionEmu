package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameDredgionGateway {

    private ObjectProvider<DredgionService2> dredgionServiceProvider;
    private ObjectProvider<AsyunatarService> asyunatarServiceProvider;

    @Autowired(required = false)
    void setDredgionServiceProvider(ObjectProvider<DredgionService2> dredgionServiceProvider) {
        this.dredgionServiceProvider = dredgionServiceProvider;
    }

    @Autowired(required = false)
    void setAsyunatarServiceProvider(ObjectProvider<AsyunatarService> asyunatarServiceProvider) {
        this.asyunatarServiceProvider = asyunatarServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Dredgion *** ");
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            dredgionService().initDredgion();
        }
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            asyunatarService().initAsyunatar();
        }
    }

    private DredgionService2 dredgionService() {
        if (dredgionServiceProvider == null) {
            return DredgionService2.getInstance();
        }
        return dredgionServiceProvider.getIfAvailable(DredgionService2::getInstance);
    }

    private AsyunatarService asyunatarService() {
        if (asyunatarServiceProvider == null) {
            return AsyunatarService.getInstance();
        }
        return asyunatarServiceProvider.getIfAvailable(AsyunatarService::getInstance);
    }
}
