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

final class GameBattlefieldFallbacks {

    private GameBattlefieldFallbacks() {
    }

    static KamarBattlefieldService kamarBattlefieldService() {
        return KamarBattlefieldServiceFallback.INSTANCE;
    }

    static EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return EngulfedOphidanBridgeServiceFallback.INSTANCE;
    }

    static SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return SuspiciousOphidanBridgeServiceFallback.INSTANCE;
    }

    static IronWallWarfrontService ironWallWarfrontService() {
        return IronWallWarfrontServiceFallback.INSTANCE;
    }

    static IdgelDomeService idgelDomeService() {
        return IdgelDomeServiceFallback.INSTANCE;
    }

    static IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return IdgelDomeLandmarkServiceFallback.INSTANCE;
    }

    static HallOfTenacityService hallOfTenacityService() {
        return HallOfTenacityServiceFallback.INSTANCE;
    }

    static GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return GrandArenaTrainingCampServiceFallback.INSTANCE;
    }

    static IDRunService idRunService() {
        return IdRunServiceFallback.INSTANCE;
    }

    private static final class KamarBattlefieldServiceFallback {
        private static final KamarBattlefieldService INSTANCE = KamarBattlefieldService.getInstance();
    }

    private static final class EngulfedOphidanBridgeServiceFallback {
        private static final EngulfedOphidanBridgeService INSTANCE = EngulfedOphidanBridgeService.getInstance();
    }

    private static final class SuspiciousOphidanBridgeServiceFallback {
        private static final SuspiciousOphidanBridgeService INSTANCE = SuspiciousOphidanBridgeService.getInstance();
    }

    private static final class IronWallWarfrontServiceFallback {
        private static final IronWallWarfrontService INSTANCE = IronWallWarfrontService.getInstance();
    }

    private static final class IdgelDomeServiceFallback {
        private static final IdgelDomeService INSTANCE = IdgelDomeService.getInstance();
    }

    private static final class IdgelDomeLandmarkServiceFallback {
        private static final IdgelDomeLandmarkService INSTANCE = IdgelDomeLandmarkService.getInstance();
    }

    private static final class HallOfTenacityServiceFallback {
        private static final HallOfTenacityService INSTANCE = HallOfTenacityService.getInstance();
    }

    private static final class GrandArenaTrainingCampServiceFallback {
        private static final GrandArenaTrainingCampService INSTANCE = GrandArenaTrainingCampService.getInstance();
    }

    private static final class IdRunServiceFallback {
        private static final IDRunService INSTANCE = IDRunService.getInstance();
    }
}
