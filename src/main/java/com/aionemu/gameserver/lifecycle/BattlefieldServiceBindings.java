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
 * 战场/副本服务静态绑定：把 Spring ObjectProvider 喂入战场/副本域服务自持的 fail-fast 单例通道。
 * Battlefield/instance static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Battlefield/instance services.
 * <p>本组件取代已退役的战场/副本静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Battlefield/instance static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class BattlefieldServiceBindings implements DisposableBean {

    /**
     * 构造并注册各战场/副本服务的实例提供者。
     * Construct and register instance providers for Battlefield/instance services.
     * @param kamarBattlefieldServiceProvider KamarBattlefieldService 提供者 / KamarBattlefieldService provider
     * @param engulfedOphidanBridgeServiceProvider EngulfedOphidanBridgeService 提供者 / EngulfedOphidanBridgeService provider
     * @param suspiciousOphidanBridgeServiceProvider SuspiciousOphidanBridgeService 提供者 / SuspiciousOphidanBridgeService provider
     * @param ironWallWarfrontServiceProvider IronWallWarfrontService 提供者 / IronWallWarfrontService provider
     * @param idgelDomeServiceProvider IdgelDomeService 提供者 / IdgelDomeService provider
     * @param idgelDomeLandmarkServiceProvider IdgelDomeLandmarkService 提供者 / IdgelDomeLandmarkService provider
     * @param hallOfTenacityServiceProvider HallOfTenacityService 提供者 / HallOfTenacityService provider
     * @param grandArenaTrainingCampServiceProvider GrandArenaTrainingCampService 提供者 / GrandArenaTrainingCampService provider
     * @param idRunServiceProvider IDRunService 提供者 / IDRunService provider
     */
    public BattlefieldServiceBindings(ObjectProvider<KamarBattlefieldService> kamarBattlefieldServiceProvider,
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

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
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
