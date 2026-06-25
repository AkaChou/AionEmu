package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.configs.main.AutoGroupConfig;
import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import com.aionemu.gameserver.utils.Util;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameBattlefieldRuntimeBridge {

    public void printBattlefieldSection() {
        Util.printSection(" *** Battlefield *** ");
    }

    public boolean isAutoGroupEnabled() {
        return AutoGroupConfig.AUTO_GROUP_ENABLED;
    }

    public KamarBattlefieldService kamarBattlefieldService() {
        return KamarBattlefieldService.getInstance();
    }

    public EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return EngulfedOphidanBridgeService.getInstance();
    }

    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return SuspiciousOphidanBridgeService.getInstance();
    }

    public IronWallWarfrontService ironWallWarfrontService() {
        return IronWallWarfrontService.getInstance();
    }

    public IdgelDomeService idgelDomeService() {
        return IdgelDomeService.getInstance();
    }

    public IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return IdgelDomeLandmarkService.getInstance();
    }

    public HallOfTenacityService hallOfTenacityService() {
        return HallOfTenacityService.getInstance();
    }

    public GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return GrandArenaTrainingCampService.getInstance();
    }

    public IDRunService idRunService() {
        return IDRunService.getInstance();
    }
}
