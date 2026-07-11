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

/**
 * 战场服务门面：将 ObjectProvider 写入静态访问器并在销毁时清空。
 * Battlefield services facade: wires ObjectProviders into static accessors and clears them on destroy.
 */
@Component
public final class GameBattlefieldServices implements DisposableBean {

    /**
     * 卡玛战场服务提供者静态缓存。
     * Static cache of Kamar battlefield service provider.
     */
    private static volatile ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider;

    /**
     * 吞噬的奥菲丹桥服务提供者静态缓存。
     * Static cache of Engulfed Ophidan Bridge service provider.
     */
    private static volatile ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider;

    /**
     * 可疑的奥菲丹桥服务提供者静态缓存。
     * Static cache of Suspicious Ophidan Bridge service provider.
     */
    private static volatile ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider;

    /**
     * 铁壁前线服务提供者静态缓存。
     * Static cache of Iron Wall Warfront service provider.
     */
    private static volatile ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider;

    /**
     * 伊迪尔穹顶服务提供者静态缓存。
     * Static cache of Idgel Dome service provider.
     */
    private static volatile ObjectProvider<IdgelDomeService> idgelDomeServiceProvider;

    /**
     * 伊迪尔穹顶地标服务提供者静态缓存。
     * Static cache of Idgel Dome Landmark service provider.
     */
    private static volatile ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider;

    /**
     * 坚韧殿堂服务提供者静态缓存。
     * Static cache of Hall of Tenacity service provider.
     */
    private static volatile ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider;

    /**
     * 大竞技场训练营服务提供者静态缓存。
     * Static cache of Grand Arena Training Camp service provider.
     */
    private static volatile ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider;

    /**
     * IDRun 服务提供者静态缓存。
     * Static cache of IDRun service provider.
     */
    private static volatile ObjectProvider<IDRunService> idRunServiceProvider;

    /**
     * 构造并注册各战场服务的静态访问器。
     * Construct and register static accessors for battlefield services.
     *
     * @param kamarBattlefieldServiceProvider 卡玛战场服务提供者 / Kamar battlefield service provider
     * @param engulfedOphidanBridgeServiceProvider 吞噬的奥菲丹桥服务提供者 / Engulfed Ophidan Bridge service provider
     * @param suspiciousOphidanBridgeServiceProvider 可疑的奥菲丹桥服务提供者 / Suspicious Ophidan Bridge service provider
     * @param ironWallWarfrontServiceProvider 铁壁前线服务提供者 / Iron Wall Warfront service provider
     * @param idgelDomeServiceProvider 伊迪尔穹顶服务提供者 / Idgel Dome service provider
     * @param idgelDomeLandmarkServiceProvider 伊迪尔穹顶地标服务提供者 / Idgel Dome Landmark service provider
     * @param hallOfTenacityServiceProvider 坚韧殿堂服务提供者 / Hall of Tenacity service provider
     * @param grandArenaTrainingCampServiceProvider 大竞技场训练营服务提供者 / Grand Arena Training Camp service provider
     * @param idRunServiceProvider IDRun 服务提供者 / IDRun service provider
     */
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

    /**
     * 获取卡玛战场服务。
     * Obtain the Kamar battlefield service.
     *
     * Service instance
     */
    public static KamarBattlefieldService kamarBattlefieldService() {
        ObjectProvider<KamarBattlefieldService> provider = kamarBattlefieldServiceProvider;
        if (provider == null) {
            return KamarBattlefieldService.getInstance();
        }
        return provider.getIfAvailable(KamarBattlefieldService::getInstance);
    }

    /**
     * 获取吞噬的奥菲丹桥服务。
     * Obtain the Engulfed Ophidan Bridge service.
     *
     * Service instance
     */
    public static EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        ObjectProvider<EngulfedOphidanBridgeService> provider = engulfedOphidanBridgeServiceProvider;
        if (provider == null) {
            return EngulfedOphidanBridgeService.getInstance();
        }
        return provider.getIfAvailable(EngulfedOphidanBridgeService::getInstance);
    }

    /**
     * 获取可疑的奥菲丹桥服务。
     * Obtain the Suspicious Ophidan Bridge service.
     *
     * Service instance
     */
    public static SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        ObjectProvider<SuspiciousOphidanBridgeService> provider = suspiciousOphidanBridgeServiceProvider;
        if (provider == null) {
            return SuspiciousOphidanBridgeService.getInstance();
        }
        return provider.getIfAvailable(SuspiciousOphidanBridgeService::getInstance);
    }

    /**
     * 获取铁壁前线服务。
     * Obtain the Iron Wall Warfront service.
     *
     * Service instance
     */
    public static IronWallWarfrontService ironWallWarfrontService() {
        ObjectProvider<IronWallWarfrontService> provider = ironWallWarfrontServiceProvider;
        if (provider == null) {
            return IronWallWarfrontService.getInstance();
        }
        return provider.getIfAvailable(IronWallWarfrontService::getInstance);
    }

    /**
     * 获取伊迪尔穹顶服务。
     * Obtain the Idgel Dome service.
     *
     * Service instance
     */
    public static IdgelDomeService idgelDomeService() {
        ObjectProvider<IdgelDomeService> provider = idgelDomeServiceProvider;
        if (provider == null) {
            return IdgelDomeService.getInstance();
        }
        return provider.getIfAvailable(IdgelDomeService::getInstance);
    }

    /**
     * 获取伊迪尔穹顶地标服务。
     * Obtain the Idgel Dome Landmark service.
     *
     * Service instance
     */
    public static IdgelDomeLandmarkService idgelDomeLandmarkService() {
        ObjectProvider<IdgelDomeLandmarkService> provider = idgelDomeLandmarkServiceProvider;
        if (provider == null) {
            return IdgelDomeLandmarkService.getInstance();
        }
        return provider.getIfAvailable(IdgelDomeLandmarkService::getInstance);
    }

    /**
     * 获取坚韧殿堂服务。
     * Obtain the Hall of Tenacity service.
     *
     * Service instance
     */
    public static HallOfTenacityService hallOfTenacityService() {
        ObjectProvider<HallOfTenacityService> provider = hallOfTenacityServiceProvider;
        if (provider == null) {
            return HallOfTenacityService.getInstance();
        }
        return provider.getIfAvailable(HallOfTenacityService::getInstance);
    }

    /**
     * 获取大竞技场训练营服务。
     * Obtain the Grand Arena Training Camp service.
     *
     * Service instance
     */
    public static GrandArenaTrainingCampService grandArenaTrainingCampService() {
        ObjectProvider<GrandArenaTrainingCampService> provider = grandArenaTrainingCampServiceProvider;
        if (provider == null) {
            return GrandArenaTrainingCampService.getInstance();
        }
        return provider.getIfAvailable(GrandArenaTrainingCampService::getInstance);
    }

    /**
     * 获取 IDRun 服务。
     * Obtain the IDRun service.
     *
     * Service instance
     */
    public static IDRunService idRunService() {
        ObjectProvider<IDRunService> provider = idRunServiceProvider;
        if (provider == null) {
            return IDRunService.getInstance();
        }
        return provider.getIfAvailable(IDRunService::getInstance);
    }

    /**
     * 销毁时清空静态提供者与领域服务实例提供者。
     * Clear static providers and domain-service instance providers on destroy.
     */
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
