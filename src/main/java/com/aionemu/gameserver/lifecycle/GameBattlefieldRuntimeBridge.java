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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameBattlefieldRuntimeBridge {

    private ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider;
    private ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider;
    private ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider;
    private ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider;
    private ObjectProvider<IdgelDomeService> idgelDomeServiceProvider;
    private ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider;
    private ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider;
    private ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider;
    private ObjectProvider<IDRunService> idRunServiceProvider;

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

    public void printBattlefieldSection() {
        Util.printSection(" *** Battlefield *** ");
    }

    public boolean isAutoGroupEnabled() {
        return AutoGroupConfig.AUTO_GROUP_ENABLED;
    }

    public KamarBattlefieldService kamarBattlefieldService() {
        if (kamarBattlefieldServiceProvider == null) {
            return KamarBattlefieldService.getInstance();
        }
        return kamarBattlefieldServiceProvider.getIfAvailable(KamarBattlefieldService::getInstance);
    }

    public EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        if (engulfedOphidanBridgeServiceProvider == null) {
            return EngulfedOphidanBridgeService.getInstance();
        }
        return engulfedOphidanBridgeServiceProvider.getIfAvailable(EngulfedOphidanBridgeService::getInstance);
    }

    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        if (suspiciousOphidanBridgeServiceProvider == null) {
            return SuspiciousOphidanBridgeService.getInstance();
        }
        return suspiciousOphidanBridgeServiceProvider.getIfAvailable(SuspiciousOphidanBridgeService::getInstance);
    }

    public IronWallWarfrontService ironWallWarfrontService() {
        if (ironWallWarfrontServiceProvider == null) {
            return IronWallWarfrontService.getInstance();
        }
        return ironWallWarfrontServiceProvider.getIfAvailable(IronWallWarfrontService::getInstance);
    }

    public IdgelDomeService idgelDomeService() {
        if (idgelDomeServiceProvider == null) {
            return IdgelDomeService.getInstance();
        }
        return idgelDomeServiceProvider.getIfAvailable(IdgelDomeService::getInstance);
    }

    public IdgelDomeLandmarkService idgelDomeLandmarkService() {
        if (idgelDomeLandmarkServiceProvider == null) {
            return IdgelDomeLandmarkService.getInstance();
        }
        return idgelDomeLandmarkServiceProvider.getIfAvailable(IdgelDomeLandmarkService::getInstance);
    }

    public HallOfTenacityService hallOfTenacityService() {
        if (hallOfTenacityServiceProvider == null) {
            return HallOfTenacityService.getInstance();
        }
        return hallOfTenacityServiceProvider.getIfAvailable(HallOfTenacityService::getInstance);
    }

    public GrandArenaTrainingCampService grandArenaTrainingCampService() {
        if (grandArenaTrainingCampServiceProvider == null) {
            return GrandArenaTrainingCampService.getInstance();
        }
        return grandArenaTrainingCampServiceProvider.getIfAvailable(GrandArenaTrainingCampService::getInstance);
    }

    public IDRunService idRunService() {
        if (idRunServiceProvider == null) {
            return IDRunService.getInstance();
        }
        return idRunServiceProvider.getIfAvailable(IDRunService::getInstance);
    }
}
