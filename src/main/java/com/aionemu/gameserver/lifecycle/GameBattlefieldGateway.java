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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameBattlefieldGateway {

    private ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider;
    private ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider;
    private ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider;
    private ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider;
    private ObjectProvider<IdgelDomeService> idgelDomeServiceProvider;
    private ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider;
    private ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider;
    private ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider;
    private ObjectProvider<IDRunService> idRunServiceProvider;
    private ObjectProvider<GameBattlefieldRuntimeBridge> runtimeBridgeProvider;

    @Autowired(required = false)
    void setKamarBattlefieldServiceProvider(ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider) {
        this.kamarBattlefieldServiceProvider = kamarBattlefieldServiceProvider;
    }

    @Autowired(required = false)
    void setEngulfedOphidanBridgeServiceProvider(ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider) {
        this.engulfedOphidanBridgeServiceProvider = engulfedOphidanBridgeServiceProvider;
    }

    @Autowired(required = false)
    void setSuspiciousOphidanBridgeServiceProvider(ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider) {
        this.suspiciousOphidanBridgeServiceProvider = suspiciousOphidanBridgeServiceProvider;
    }

    @Autowired(required = false)
    void setIronWallWarfrontServiceProvider(ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider) {
        this.ironWallWarfrontServiceProvider = ironWallWarfrontServiceProvider;
    }

    @Autowired(required = false)
    void setIdgelDomeServiceProvider(ObjectProvider<IdgelDomeService> idgelDomeServiceProvider) {
        this.idgelDomeServiceProvider = idgelDomeServiceProvider;
    }

    @Autowired(required = false)
    void setIdgelDomeLandmarkServiceProvider(ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider) {
        this.idgelDomeLandmarkServiceProvider = idgelDomeLandmarkServiceProvider;
    }

    @Autowired(required = false)
    void setHallOfTenacityServiceProvider(ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider) {
        this.hallOfTenacityServiceProvider = hallOfTenacityServiceProvider;
    }

    @Autowired(required = false)
    void setGrandArenaTrainingCampServiceProvider(ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider) {
        this.grandArenaTrainingCampServiceProvider = grandArenaTrainingCampServiceProvider;
    }

    @Autowired(required = false)
    void setIdRunServiceProvider(ObjectProvider<IDRunService> idRunServiceProvider) {
        this.idRunServiceProvider = idRunServiceProvider;
    }

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameBattlefieldRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void start() {
        runtimeBridge().printBattlefieldSection();
        runIfAutoGroupEnabled(() -> kamarBattlefieldService().initKamarBattlefield());
        runIfAutoGroupEnabled(() -> engulfedOphidanBridgeService().initEngulfedOphidan());
        runIfAutoGroupEnabled(() -> suspiciousOphidanBridgeService().initSuspiciousOphidan());
        runIfAutoGroupEnabled(() -> ironWallWarfrontService().initIronWallWarfront());
        runIfAutoGroupEnabled(() -> idgelDomeService().initIdgelDome());
        runIfAutoGroupEnabled(() -> idgelDomeLandmarkService().initLandmark());
        runIfAutoGroupEnabled(() -> hallOfTenacityService().initHallOfTenacity());
        runIfAutoGroupEnabled(() -> grandArenaTrainingCampService().initGrandArenaTrainingCamp());
        runIfAutoGroupEnabled(() -> idRunService().initIDRun());
    }

    private KamarBattlefieldService kamarBattlefieldService() {
        if (kamarBattlefieldServiceProvider == null) {
            return runtimeBridge().kamarBattlefieldService();
        }
        return kamarBattlefieldServiceProvider.getIfAvailable(() -> runtimeBridge().kamarBattlefieldService());
    }

    private EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        if (engulfedOphidanBridgeServiceProvider == null) {
            return runtimeBridge().engulfedOphidanBridgeService();
        }
        return engulfedOphidanBridgeServiceProvider.getIfAvailable(() -> runtimeBridge().engulfedOphidanBridgeService());
    }

    private SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        if (suspiciousOphidanBridgeServiceProvider == null) {
            return runtimeBridge().suspiciousOphidanBridgeService();
        }
        return suspiciousOphidanBridgeServiceProvider.getIfAvailable(() -> runtimeBridge().suspiciousOphidanBridgeService());
    }

    private IronWallWarfrontService ironWallWarfrontService() {
        if (ironWallWarfrontServiceProvider == null) {
            return runtimeBridge().ironWallWarfrontService();
        }
        return ironWallWarfrontServiceProvider.getIfAvailable(() -> runtimeBridge().ironWallWarfrontService());
    }

    private IdgelDomeService idgelDomeService() {
        if (idgelDomeServiceProvider == null) {
            return runtimeBridge().idgelDomeService();
        }
        return idgelDomeServiceProvider.getIfAvailable(() -> runtimeBridge().idgelDomeService());
    }

    private IdgelDomeLandmarkService idgelDomeLandmarkService() {
        if (idgelDomeLandmarkServiceProvider == null) {
            return runtimeBridge().idgelDomeLandmarkService();
        }
        return idgelDomeLandmarkServiceProvider.getIfAvailable(() -> runtimeBridge().idgelDomeLandmarkService());
    }

    private HallOfTenacityService hallOfTenacityService() {
        if (hallOfTenacityServiceProvider == null) {
            return runtimeBridge().hallOfTenacityService();
        }
        return hallOfTenacityServiceProvider.getIfAvailable(() -> runtimeBridge().hallOfTenacityService());
    }

    private GrandArenaTrainingCampService grandArenaTrainingCampService() {
        if (grandArenaTrainingCampServiceProvider == null) {
            return runtimeBridge().grandArenaTrainingCampService();
        }
        return grandArenaTrainingCampServiceProvider.getIfAvailable(() -> runtimeBridge().grandArenaTrainingCampService());
    }

    private IDRunService idRunService() {
        if (idRunServiceProvider == null) {
            return runtimeBridge().idRunService();
        }
        return idRunServiceProvider.getIfAvailable(() -> runtimeBridge().idRunService());
    }

    private void runIfAutoGroupEnabled(Runnable initializer) {
        if (runtimeBridge().isAutoGroupEnabled()) {
            initializer.run();
        }
    }

    private GameBattlefieldRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameBattlefieldRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameBattlefieldRuntimeBridge::new);
    }
}
