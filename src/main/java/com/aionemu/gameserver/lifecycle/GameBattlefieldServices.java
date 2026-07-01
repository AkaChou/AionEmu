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

    private static volatile ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider;
    private static volatile ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider;
    private static volatile ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider;
    private static volatile ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider;
    private static volatile ObjectProvider<IdgelDomeService> idgelDomeServiceProvider;
    private static volatile ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider;
    private static volatile ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider;
    private static volatile ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider;
    private static volatile ObjectProvider<IDRunService> idRunServiceProvider;

    public GameBattlefieldServices(ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider,
            ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider,
            ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider,
            ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider,
            ObjectProvider<IdgelDomeService> idgelDomeServiceProvider,
            ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider,
            ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider,
            ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider,
            ObjectProvider<IDRunService> idRunServiceProvider) {
        GameBattlefieldServices.kamarBattlefieldServiceProvider = kamarBattlefieldServiceProvider;
        GameBattlefieldServices.engulfedOphidanBridgeServiceProvider = engulfedOphidanBridgeServiceProvider;
        GameBattlefieldServices.suspiciousOphidanBridgeServiceProvider = suspiciousOphidanBridgeServiceProvider;
        GameBattlefieldServices.ironWallWarfrontServiceProvider = ironWallWarfrontServiceProvider;
        GameBattlefieldServices.idgelDomeServiceProvider = idgelDomeServiceProvider;
        GameBattlefieldServices.idgelDomeLandmarkServiceProvider = idgelDomeLandmarkServiceProvider;
        GameBattlefieldServices.hallOfTenacityServiceProvider = hallOfTenacityServiceProvider;
        GameBattlefieldServices.grandArenaTrainingCampServiceProvider = grandArenaTrainingCampServiceProvider;
        GameBattlefieldServices.idRunServiceProvider = idRunServiceProvider;
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

    public static KamarBattlefieldService kamarBattlefieldService() {
        ObjectProvider<KamarBattlefieldService> provider = kamarBattlefieldServiceProvider;
        if (provider == null) {
            return KamarBattlefieldService.getInstance();
        }
        return provider.getIfAvailable(KamarBattlefieldService::getInstance);
    }

    public static EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        ObjectProvider<EngulfedOphidanBridgeService> provider = engulfedOphidanBridgeServiceProvider;
        if (provider == null) {
            return EngulfedOphidanBridgeService.getInstance();
        }
        return provider.getIfAvailable(EngulfedOphidanBridgeService::getInstance);
    }

    public static SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        ObjectProvider<SuspiciousOphidanBridgeService> provider = suspiciousOphidanBridgeServiceProvider;
        if (provider == null) {
            return SuspiciousOphidanBridgeService.getInstance();
        }
        return provider.getIfAvailable(SuspiciousOphidanBridgeService::getInstance);
    }

    public static IronWallWarfrontService ironWallWarfrontService() {
        ObjectProvider<IronWallWarfrontService> provider = ironWallWarfrontServiceProvider;
        if (provider == null) {
            return IronWallWarfrontService.getInstance();
        }
        return provider.getIfAvailable(IronWallWarfrontService::getInstance);
    }

    public static IdgelDomeService idgelDomeService() {
        ObjectProvider<IdgelDomeService> provider = idgelDomeServiceProvider;
        if (provider == null) {
            return IdgelDomeService.getInstance();
        }
        return provider.getIfAvailable(IdgelDomeService::getInstance);
    }

    public static IdgelDomeLandmarkService idgelDomeLandmarkService() {
        ObjectProvider<IdgelDomeLandmarkService> provider = idgelDomeLandmarkServiceProvider;
        if (provider == null) {
            return IdgelDomeLandmarkService.getInstance();
        }
        return provider.getIfAvailable(IdgelDomeLandmarkService::getInstance);
    }

    public static HallOfTenacityService hallOfTenacityService() {
        ObjectProvider<HallOfTenacityService> provider = hallOfTenacityServiceProvider;
        if (provider == null) {
            return HallOfTenacityService.getInstance();
        }
        return provider.getIfAvailable(HallOfTenacityService::getInstance);
    }

    public static GrandArenaTrainingCampService grandArenaTrainingCampService() {
        ObjectProvider<GrandArenaTrainingCampService> provider = grandArenaTrainingCampServiceProvider;
        if (provider == null) {
            return GrandArenaTrainingCampService.getInstance();
        }
        return provider.getIfAvailable(GrandArenaTrainingCampService::getInstance);
    }

    public static IDRunService idRunService() {
        ObjectProvider<IDRunService> provider = idRunServiceProvider;
        if (provider == null) {
            return IDRunService.getInstance();
        }
        return provider.getIfAvailable(IDRunService::getInstance);
    }

    @Override
    public void destroy() {
        kamarBattlefieldServiceProvider = null;
        KamarBattlefieldService.setInstanceProvider(null);
        engulfedOphidanBridgeServiceProvider = null;
        EngulfedOphidanBridgeService.setInstanceProvider(null);
        suspiciousOphidanBridgeServiceProvider = null;
        SuspiciousOphidanBridgeService.setInstanceProvider(null);
        ironWallWarfrontServiceProvider = null;
        IronWallWarfrontService.setInstanceProvider(null);
        idgelDomeServiceProvider = null;
        IdgelDomeService.setInstanceProvider(null);
        idgelDomeLandmarkServiceProvider = null;
        IdgelDomeLandmarkService.setInstanceProvider(null);
        hallOfTenacityServiceProvider = null;
        HallOfTenacityService.setInstanceProvider(null);
        grandArenaTrainingCampServiceProvider = null;
        GrandArenaTrainingCampService.setInstanceProvider(null);
        idRunServiceProvider = null;
        IDRunService.setInstanceProvider(null);
    }
}
