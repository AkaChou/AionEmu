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

/**
 * 战场服务回退工厂：Spring Bean 不可用时提供懒加载单例。
 * Battlefield service fallbacks: lazy singleton holders when Spring beans are unavailable.
 */
final class GameBattlefieldFallbacks {

    /**
     * 工具类禁止实例化。
     * Utility class; not instantiable.
     */
    private GameBattlefieldFallbacks() {
    }

    /**
     * 卡玛战场服务回退实例。
     * Kamar battlefield service fallback instance.
     * @return 服务实例 / Service instance
     */
    static KamarBattlefieldService kamarBattlefieldService() {
        return KamarBattlefieldService.getInstance();
    }

    /**
     * 吞噬的奥菲丹桥服务回退实例。
     * Engulfed Ophidan Bridge service fallback instance.
     * @return 服务实例 / Service instance
     */
    static EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return EngulfedOphidanBridgeService.getInstance();
    }

    /**
     * 可疑的奥菲丹桥服务回退实例。
     * Suspicious Ophidan Bridge service fallback instance.
     * @return 服务实例 / Service instance
     */
    static SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return SuspiciousOphidanBridgeService.getInstance();
    }

    /**
     * 铁壁前线服务回退实例。
     * Iron Wall Warfront service fallback instance.
     * @return 服务实例 / Service instance
     */
    static IronWallWarfrontService ironWallWarfrontService() {
        return IronWallWarfrontService.getInstance();
    }

    /**
     * 伊迪尔穹顶服务回退实例。
     * Idgel Dome service fallback instance.
     * @return 服务实例 / Service instance
     */
    static IdgelDomeService idgelDomeService() {
        return IdgelDomeService.getInstance();
    }

    /**
     * 伊迪尔穹顶地标服务回退实例。
     * Idgel Dome Landmark service fallback instance.
     * @return 服务实例 / Service instance
     */
    static IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return IdgelDomeLandmarkService.getInstance();
    }

    /**
     * 坚韧殿堂服务回退实例。
     * Hall of Tenacity service fallback instance.
     * @return 服务实例 / Service instance
     */
    static HallOfTenacityService hallOfTenacityService() {
        return HallOfTenacityService.getInstance();
    }

    /**
     * 大竞技场训练营服务回退实例。
     * Grand Arena Training Camp service fallback instance.
     * @return 服务实例 / Service instance
     */
    static GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return GrandArenaTrainingCampService.getInstance();
    }

    /**
     * IDRun 服务回退实例。
     * IDRun service fallback instance.
     * @return 服务实例 / Service instance
     */
    static IDRunService idRunService() {
        return IDRunService.getInstance();
    }
}
