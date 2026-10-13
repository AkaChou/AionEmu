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
 * 战场生命周期：在启动序列中打印分区，并在自动组队开启时初始化各战场实例服务。
 * Battlefield lifecycle: prints the section during startup and initializes battlefield instance
 * services when auto-group is enabled.
 */
@Component
public class GameBattlefieldLifecycle extends GameStartupStage {

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
     * @param kamarBattlefieldServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setKamarBattlefieldServiceProvider(ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider) {
        this.kamarBattlefieldServiceProvider = kamarBattlefieldServiceProvider;
    }

    /**
     * 可选注入吞噬的奥菲丹桥服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Engulfed Ophidan Bridge service.
     * @param engulfedOphidanBridgeServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setEngulfedOphidanBridgeServiceProvider(ObjectProvider<EngulfedOphidanBridgeService> engulfedOphidanBridgeServiceProvider) {
        this.engulfedOphidanBridgeServiceProvider = engulfedOphidanBridgeServiceProvider;
    }

    /**
     * 可选注入可疑的奥菲丹桥服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Suspicious Ophidan Bridge service.
     * @param suspiciousOphidanBridgeServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setSuspiciousOphidanBridgeServiceProvider(ObjectProvider<SuspiciousOphidanBridgeService> suspiciousOphidanBridgeServiceProvider) {
        this.suspiciousOphidanBridgeServiceProvider = suspiciousOphidanBridgeServiceProvider;
    }

    /**
     * 可选注入铁壁前线服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Iron Wall Warfront service.
     * @param ironWallWarfrontServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIronWallWarfrontServiceProvider(ObjectProvider<IronWallWarfrontService> ironWallWarfrontServiceProvider) {
        this.ironWallWarfrontServiceProvider = ironWallWarfrontServiceProvider;
    }

    /**
     * 可选注入伊迪尔穹顶服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Idgel Dome service.
     * @param idgelDomeServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIdgelDomeServiceProvider(ObjectProvider<IdgelDomeService> idgelDomeServiceProvider) {
        this.idgelDomeServiceProvider = idgelDomeServiceProvider;
    }

    /**
     * 可选注入伊迪尔穹顶地标服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Idgel Dome Landmark service.
     * @param idgelDomeLandmarkServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIdgelDomeLandmarkServiceProvider(ObjectProvider<IdgelDomeLandmarkService> idgelDomeLandmarkServiceProvider) {
        this.idgelDomeLandmarkServiceProvider = idgelDomeLandmarkServiceProvider;
    }

    /**
     * 可选注入坚韧殿堂服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Hall of Tenacity service.
     * @param hallOfTenacityServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setHallOfTenacityServiceProvider(ObjectProvider<HallOfTenacityService> hallOfTenacityServiceProvider) {
        this.hallOfTenacityServiceProvider = hallOfTenacityServiceProvider;
    }

    /**
     * 可选注入大竞技场训练营服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of Grand Arena Training Camp service.
     * @param grandArenaTrainingCampServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setGrandArenaTrainingCampServiceProvider(ObjectProvider<GrandArenaTrainingCampService> grandArenaTrainingCampServiceProvider) {
        this.grandArenaTrainingCampServiceProvider = grandArenaTrainingCampServiceProvider;
    }

    /**
     * 可选注入 IDRun 服务 {@link ObjectProvider}。
     * Optionally inject the {@link ObjectProvider} of IDRun service.
     * @param idRunServiceProvider 服务提供者 / Service provider
     */
    @Autowired(required = false)
    void setIdRunServiceProvider(ObjectProvider<IDRunService> idRunServiceProvider) {
        this.idRunServiceProvider = idRunServiceProvider;
    }


    /**
     * 启动本阶段：打印分区并在自动组队开启时初始化各战场服务。
     * Start this stage: print section and init each battlefield service when auto-group is on.
     */
    @Override
    protected void runStage() {
        printBattlefieldSection();
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

    /**
     * 自动组队是否已启用。
     * Whether auto-group is enabled.
     * @return 已启用为 {@code true} / {@code true} if enabled
     */
    protected boolean isAutoGroupEnabled() {
        return AutoGroupConfig.AUTO_GROUP_ENABLED;
    }

    /**
     * 仅在自动组队开启时执行初始化。
     * Run the initializer only when auto-group is enabled.
     * @param initializer 初始化逻辑 / Initializer
     */
    protected void runIfAutoGroupEnabled(Runnable initializer) {
        if (isAutoGroupEnabled()) {
            initializer.run();
        }
    }

    /**
     * 解析卡玛战场服务：优先 Spring，否则经运行时桥回退。
     * Resolve Kamar battlefield service: prefer Spring, otherwise fall back via runtime bridge.
     * @return 服务实例 / Service instance
     */
    private KamarBattlefieldService kamarBattlefieldService() {
        return kamarBattlefieldServiceProvider.getObject();
    }

    /**
     * 解析吞噬的奥菲丹桥服务。
     * Resolve Engulfed Ophidan Bridge service.
     * @return 服务实例 / Service instance
     */
    private EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return engulfedOphidanBridgeServiceProvider.getObject();
    }

    /**
     * 解析可疑的奥菲丹桥服务。
     * Resolve Suspicious Ophidan Bridge service.
     * @return 服务实例 / Service instance
     */
    private SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return suspiciousOphidanBridgeServiceProvider.getObject();
    }

    /**
     * 解析铁壁前线服务。
     * Resolve Iron Wall Warfront service.
     * @return 服务实例 / Service instance
     */
    private IronWallWarfrontService ironWallWarfrontService() {
        return ironWallWarfrontServiceProvider.getObject();
    }

    /**
     * 解析伊迪尔穹顶服务。
     * Resolve Idgel Dome service.
     * @return 服务实例 / Service instance
     */
    private IdgelDomeService idgelDomeService() {
        return idgelDomeServiceProvider.getObject();
    }

    /**
     * 解析伊迪尔穹顶地标服务。
     * Resolve Idgel Dome Landmark service.
     * @return 服务实例 / Service instance
     */
    private IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return idgelDomeLandmarkServiceProvider.getObject();
    }

    /**
     * 解析坚韧殿堂服务。
     * Resolve Hall of Tenacity service.
     * @return 服务实例 / Service instance
     */
    private HallOfTenacityService hallOfTenacityService() {
        return hallOfTenacityServiceProvider.getObject();
    }

    /**
     * 解析大竞技场训练营服务。
     * Resolve Grand Arena Training Camp service.
     * @return 服务实例 / Service instance
     */
    private GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return grandArenaTrainingCampServiceProvider.getObject();
    }

    /**
     * 解析 IDRun 服务。
     * Resolve IDRun service.
     * @return 服务实例 / Service instance
     */
    private IDRunService idRunService() {
        return idRunServiceProvider.getObject();
    }

    /**
     * 打印战场分区标题。
     * Print the battlefield section header.
     */
    private void printBattlefieldSection() {
        Util.printSection(I18n.get("console.section.battlefield"));
    }
}
