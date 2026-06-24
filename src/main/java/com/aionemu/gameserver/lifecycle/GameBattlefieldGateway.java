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
import org.springframework.stereotype.Component;

@Component
public class GameBattlefieldGateway {

    public void start() {
        Util.printSection(" *** Battlefield *** ");
        runIfAutoGroupEnabled(() -> KamarBattlefieldService.getInstance().initKamarBattlefield());
        runIfAutoGroupEnabled(() -> EngulfedOphidanBridgeService.getInstance().initEngulfedOphidan());
        runIfAutoGroupEnabled(() -> SuspiciousOphidanBridgeService.getInstance().initSuspiciousOphidan());
        runIfAutoGroupEnabled(() -> IronWallWarfrontService.getInstance().initIronWallWarfront());
        runIfAutoGroupEnabled(() -> IdgelDomeService.getInstance().initIdgelDome());
        runIfAutoGroupEnabled(() -> IdgelDomeLandmarkService.getInstance().initLandmark());
        runIfAutoGroupEnabled(() -> HallOfTenacityService.getInstance().initHallOfTenacity());
        runIfAutoGroupEnabled(() -> GrandArenaTrainingCampService.getInstance().initGrandArenaTrainingCamp());
        runIfAutoGroupEnabled(() -> IDRunService.getInstance().initIDRun());
    }

    private void runIfAutoGroupEnabled(Runnable initializer) {
        if (AutoGroupConfig.AUTO_GROUP_ENABLED) {
            initializer.run();
        }
    }
}
