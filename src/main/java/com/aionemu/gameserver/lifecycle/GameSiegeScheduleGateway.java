package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameSiegeScheduleGateway {

    private ObjectProvider<SiegeService> siegeServiceProvider;
    private ObjectProvider<BaseService> baseServiceProvider;

    @Autowired(required = false)
    void setSiegeServiceProvider(ObjectProvider<SiegeService> siegeServiceProvider) {
        this.siegeServiceProvider = siegeServiceProvider;
    }

    @Autowired(required = false)
    void setBaseServiceProvider(ObjectProvider<BaseService> baseServiceProvider) {
        this.baseServiceProvider = baseServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Sieges *** ");
        siegeService().initSieges();
        baseService().initBases();
    }

    private SiegeService siegeService() {
        if (siegeServiceProvider == null) {
            return SiegeService.getInstance();
        }
        return siegeServiceProvider.getIfAvailable(SiegeService::getInstance);
    }

    private BaseService baseService() {
        if (baseServiceProvider == null) {
            return BaseService.getInstance();
        }
        return baseServiceProvider.getIfAvailable(BaseService::getInstance);
    }
}
