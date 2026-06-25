package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.FFAConfig;
import com.aionemu.gameserver.configs.main.PvPModConfig;
import com.aionemu.gameserver.services.events.BGService;
import com.aionemu.gameserver.services.events.BanditService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.LadderService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameCustomEventsGateway {

    private ObjectProvider<FFAService> ffaServiceProvider;
    private ObjectProvider<LadderService> ladderServiceProvider;
    private ObjectProvider<BGService> bgServiceProvider;
    private ObjectProvider<BanditService> banditServiceProvider;

    @Autowired(required = false)
    void setFfaServiceProvider(ObjectProvider<FFAService> ffaServiceProvider) {
        this.ffaServiceProvider = ffaServiceProvider;
    }

    @Autowired(required = false)
    void setLadderServiceProvider(ObjectProvider<LadderService> ladderServiceProvider) {
        this.ladderServiceProvider = ladderServiceProvider;
    }

    @Autowired(required = false)
    void setBgServiceProvider(ObjectProvider<BGService> bgServiceProvider) {
        this.bgServiceProvider = bgServiceProvider;
    }

    @Autowired(required = false)
    void setBanditServiceProvider(ObjectProvider<BanditService> banditServiceProvider) {
        this.banditServiceProvider = banditServiceProvider;
    }

    public void start() {
        Util.printSection(" *** Custom Events *** ");
        if (FFAConfig.FFA_ENABLED) {
            ffaService();
        }
        if (PvPModConfig.BG_ENABLED) {
            ladderService();
            bgService();
        }
        banditService().onInit();
    }

    private FFAService ffaService() {
        if (ffaServiceProvider == null) {
            return FFAService.getInstance();
        }
        return ffaServiceProvider.getIfAvailable(FFAService::getInstance);
    }

    private LadderService ladderService() {
        if (ladderServiceProvider == null) {
            return LadderService.getInstance();
        }
        return ladderServiceProvider.getIfAvailable(LadderService::getInstance);
    }

    private BGService bgService() {
        if (bgServiceProvider == null) {
            return BGService.getInstance();
        }
        return bgServiceProvider.getIfAvailable(BGService::getInstance);
    }

    private BanditService banditService() {
        if (banditServiceProvider == null) {
            return BanditService.getInstance();
        }
        return banditServiceProvider.getIfAvailable(BanditService::getInstance);
    }
}
