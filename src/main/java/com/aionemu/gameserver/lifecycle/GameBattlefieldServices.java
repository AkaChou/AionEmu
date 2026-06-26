package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
import com.aionemu.gameserver.services.instance.HallOfTenacityService;
import com.aionemu.gameserver.services.instance.IDRunService;
import com.aionemu.gameserver.services.instance.IdgelDomeLandmarkService;
import com.aionemu.gameserver.services.instance.IdgelDomeService;
import com.aionemu.gameserver.services.instance.IronWallWarfrontService;
import com.aionemu.gameserver.services.instance.KamarBattlefieldService;
import com.aionemu.gameserver.services.instance.SuspiciousOphidanBridgeService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameBattlefieldServices implements DisposableBean {

    public GameBattlefieldServices(ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider,
            ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider,
            ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider,
            ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider,
            ObjectProvider<IdgelDomeService> idgelDomeServiceProvider,
            ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider,
            ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider,
            ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider,
            ObjectProvider<IDRunService> idRunServiceProvider) {
        KamarBattlefieldService.setInstanceProvider(kamarBattlefieldServiceProvider);
        EngulfedOphidanBridgeService.setInstanceProvider(engulfedOphidanBridgeServiceProvider);
        SuspiciousOphidanBridgeService.setInstanceProvider(suspiciousOphidanBridgeServiceProvider);
        IronWallWarfrontService.setInstanceProvider(ironWallWarfrontServiceProvider);
        IdgelDomeService.setInstanceProvider(idgelDomeServiceProvider);
        IdgelDomeLandmarkService.setInstanceProvider(idgelDomeLandmarkServiceProvider);
        HallOfTenacityService.setInstanceProvider(hallOfTenacityServiceProvider);
        GrandArenaTrainingCampService.setInstanceProvider(grandArenaTrainingCampServiceProvider);
        IDRunService.setInstanceProvider(idRunServiceProvider);
    }

    @Override
    public void destroy() {
        KamarBattlefieldService.setInstanceProvider(null);
        EngulfedOphidanBridgeService.setInstanceProvider(null);
        SuspiciousOphidanBridgeService.setInstanceProvider(null);
        IronWallWarfrontService.setInstanceProvider(null);
        IdgelDomeService.setInstanceProvider(null);
        IdgelDomeLandmarkService.setInstanceProvider(null);
        HallOfTenacityService.setInstanceProvider(null);
        GrandArenaTrainingCampService.setInstanceProvider(null);
        IDRunService.setInstanceProvider(null);
    }
}
