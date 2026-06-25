package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.ProtectorConquerorService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameProtectorConquerorGateway {

    private ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider;

    @Autowired(required = false)
    void setProtectorConquerorServiceProvider(ObjectProvider<ProtectorConquerorService> protectorConquerorServiceProvider) {
        this.protectorConquerorServiceProvider = protectorConquerorServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Protector/Conqueror initialization *** ");
        protectorConquerorService().initSystem();
    }

    private ProtectorConquerorService protectorConquerorService() {
        if (protectorConquerorServiceProvider == null) {
            return ProtectorConquerorService.getInstance();
        }
        return protectorConquerorServiceProvider.getIfAvailable(ProtectorConquerorService::getInstance);
    }
}
