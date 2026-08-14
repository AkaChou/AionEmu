package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
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
import org.springframework.stereotype.Component;

/**
 * 战场运行时桥接：解析战场服务与自动组队配置，无 Spring 时回退到 Fallbacks。
 * Battlefield runtime bridge: resolves battlefield services and auto-group config; falls back when Spring is absent.
 */
@Component
public class GameBattlefieldRuntimeBridge {

    /**
     * 卡玛战场服务提供者。
     * Kamar battlefield service provider.
     */
    private ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider;

    /**
     * 吞噬的奥菲丹桥服务提供者。
     * Engulfed Ophidan Bridge service provider.
     */
    private ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider;

    /**
     * 可疑的奥菲丹桥服务提供者。
     * Suspicious Ophidan Bridge service provider.
     */
    private ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider;

    /**
     * 铁壁前线服务提供者。
     * Iron Wall Warfront service provider.
     */
    private ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider;

    /**
     * 伊迪尔穹顶服务提供者。
     * Idgel Dome service provider.
     */
    private ObjectProvider<IdgelDomeService> idgelDomeServiceProvider;

    /**
     * 伊迪尔穹顶地标服务提供者。
     * Idgel Dome Landmark service provider.
     */
    private ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider;

    /**
     * 坚韧殿堂服务提供者。
     * Hall of Tenacity service provider.
     */
    private ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider;

    /**
     * 大竞技场训练营服务提供者。
     * Grand Arena Training Camp service provider.
     */
    private ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider;

    /**
     * IDRun 服务提供者。
     * IDRun service provider.
     */
    private ObjectProvider<IDRunService> idRunServiceProvider;

    /**
     * 可选注入卡玛战场服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Kamar battlefield service.
     *
     * @param kamarBattlefieldServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setKamarBattlefieldServiceProvider(ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider) {
        this.kamarBattlefieldServiceProvider = kamarBattlefieldServiceProvider;
    }

    /**
     * 可选注入吞噬的奥菲丹桥服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Engulfed Ophidan Bridge service.
     *
     * @param engulfedOphidanBridgeServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setEngulfedOphidanBridgeServiceProvider(ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider) {
        this.engulfedOphidanBridgeServiceProvider = engulfedOphidanBridgeServiceProvider;
    }

    /**
     * 可选注入可疑的奥菲丹桥服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Suspicious Ophidan Bridge service.
     *
     * @param suspiciousOphidanBridgeServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setSuspiciousOphidanBridgeServiceProvider(ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider) {
        this.suspiciousOphidanBridgeServiceProvider = suspiciousOphidanBridgeServiceProvider;
    }

    /**
     * 可选注入铁壁前线服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Iron Wall Warfront service.
     *
     * @param ironWallWarfrontServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIronWallWarfrontServiceProvider(ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider) {
        this.ironWallWarfrontServiceProvider = ironWallWarfrontServiceProvider;
    }

    /**
     * 可选注入伊迪尔穹顶服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Idgel Dome service.
     *
     * @param idgelDomeServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIdgelDomeServiceProvider(ObjectProvider<IdgelDomeService> idgelDomeServiceProvider) {
        this.idgelDomeServiceProvider = idgelDomeServiceProvider;
    }

    /**
     * 可选注入伊迪尔穹顶地标服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Idgel Dome Landmark service.
     *
     * @param idgelDomeLandmarkServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIdgelDomeLandmarkServiceProvider(ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider) {
        this.idgelDomeLandmarkServiceProvider = idgelDomeLandmarkServiceProvider;
    }

    /**
     * 可选注入坚韧殿堂服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Hall of Tenacity service.
     *
     * @param hallOfTenacityServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setHallOfTenacityServiceProvider(ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider) {
        this.hallOfTenacityServiceProvider = hallOfTenacityServiceProvider;
    }

    /**
     * 可选注入大竞技场训练营服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Grand Arena Training Camp service.
     *
     * @param grandArenaTrainingCampServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setGrandArenaTrainingCampServiceProvider(ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider) {
        this.grandArenaTrainingCampServiceProvider = grandArenaTrainingCampServiceProvider;
    }

    /**
     * 可选注入 IDRun 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of IDRun service.
     *
     * @param idRunServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIdRunServiceProvider(ObjectProvider<IDRunService> idRunServiceProvider) {
        this.idRunServiceProvider = idRunServiceProvider;
    }

    /**
     * 打印战场控制台分区标题。
     * Print the battlefield console section header.
     */
    public void printBattlefieldSection() {
        Util.printSection(I18n.get("console.section.battlefield"));
    }

    /**
     * 自动组队是否启用。
     * Whether auto-group is enabled.
     *
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    public boolean isAutoGroupEnabled() {
        return AutoGroupConfig.AUTO_GROUP_ENABLED;
    }

    /**
     * 解析卡玛战场服务：优先 Spring，否则回退。
     * Resolve Kamar battlefield service: prefer Spring, otherwise fall back.
     *
     * @return 服务实例 / Service instance
     */
    public KamarBattlefieldService kamarBattlefieldService() {
        if (kamarBattlefieldServiceProvider == null) {
            return GameBattlefieldFallbacks.kamarBattlefieldService();
        }
        return kamarBattlefieldServiceProvider.getIfAvailable(GameBattlefieldFallbacks::kamarBattlefieldService);
    }

    /**
     * 解析吞噬的奥菲丹桥服务。
     * Resolve Engulfed Ophidan Bridge service.
     *
     * @return 服务实例 / Service instance
     */
    public EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        if (engulfedOphidanBridgeServiceProvider == null) {
            return GameBattlefieldFallbacks.engulfedOphidanBridgeService();
        }
        return engulfedOphidanBridgeServiceProvider.getIfAvailable(GameBattlefieldFallbacks::engulfedOphidanBridgeService);
    }

    /**
     * 解析可疑的奥菲丹桥服务。
     * Resolve Suspicious Ophidan Bridge service.
     *
     * @return 服务实例 / Service instance
     */
    public SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        if (suspiciousOphidanBridgeServiceProvider == null) {
            return GameBattlefieldFallbacks.suspiciousOphidanBridgeService();
        }
        return suspiciousOphidanBridgeServiceProvider.getIfAvailable(GameBattlefieldFallbacks::suspiciousOphidanBridgeService);
    }

    /**
     * 解析铁壁前线服务。
     * Resolve Iron Wall Warfront service.
     *
     * @return 服务实例 / Service instance
     */
    public IronWallWarfrontService ironWallWarfrontService() {
        if (ironWallWarfrontServiceProvider == null) {
            return GameBattlefieldFallbacks.ironWallWarfrontService();
        }
        return ironWallWarfrontServiceProvider.getIfAvailable(GameBattlefieldFallbacks::ironWallWarfrontService);
    }

    /**
     * 解析伊迪尔穹顶服务。
     * Resolve Idgel Dome service.
     *
     * @return 服务实例 / Service instance
     */
    public IdgelDomeService idgelDomeService() {
        if (idgelDomeServiceProvider == null) {
            return GameBattlefieldFallbacks.idgelDomeService();
        }
        return idgelDomeServiceProvider.getIfAvailable(GameBattlefieldFallbacks::idgelDomeService);
    }

    /**
     * 解析伊迪尔穹顶地标服务。
     * Resolve Idgel Dome Landmark service.
     *
     * @return 服务实例 / Service instance
     */
    public IdgelDomeLandmarkService idgelDomeLandmarkService() {
        if (idgelDomeLandmarkServiceProvider == null) {
            return GameBattlefieldFallbacks.idgelDomeLandmarkService();
        }
        return idgelDomeLandmarkServiceProvider.getIfAvailable(GameBattlefieldFallbacks::idgelDomeLandmarkService);
    }

    /**
     * 解析坚韧殿堂服务。
     * Resolve Hall of Tenacity service.
     *
     * @return 服务实例 / Service instance
     */
    public HallOfTenacityService hallOfTenacityService() {
        if (hallOfTenacityServiceProvider == null) {
            return GameBattlefieldFallbacks.hallOfTenacityService();
        }
        return hallOfTenacityServiceProvider.getIfAvailable(GameBattlefieldFallbacks::hallOfTenacityService);
    }

    /**
     * 解析大竞技场训练营服务。
     * Resolve Grand Arena Training Camp service.
     *
     * @return 服务实例 / Service instance
     */
    public GrandArenaTrainingCampService grandArenaTrainingCampService() {
        if (grandArenaTrainingCampServiceProvider == null) {
            return GameBattlefieldFallbacks.grandArenaTrainingCampService();
        }
        return grandArenaTrainingCampServiceProvider.getIfAvailable(GameBattlefieldFallbacks::grandArenaTrainingCampService);
    }

    /**
     * 解析 IDRun 服务。
     * Resolve IDRun service.
     *
     * @return 服务实例 / Service instance
     */
    public IDRunService idRunService() {
        if (idRunServiceProvider == null) {
            return GameBattlefieldFallbacks.idRunService();
        }
        return idRunServiceProvider.getIfAvailable(GameBattlefieldFallbacks::idRunService);
    }
}
