package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.instance.EngulfedOphidanBridgeService;
import com.aionemu.gameserver.services.instance.GrandArenaTrainingCampService;
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
     *
     * Service instance
     */
    static KamarBattlefieldService kamarBattlefieldService() {
        return KamarBattlefieldServiceFallback.INSTANCE;
    }

    /**
     * 吞噬的奥菲丹桥服务回退实例。
     * Engulfed Ophidan Bridge service fallback instance.
     *
     * Service instance
     */
    static EngulfedOphidanBridgeService engulfedOphidanBridgeService() {
        return EngulfedOphidanBridgeServiceFallback.INSTANCE;
    }

    /**
     * 可疑的奥菲丹桥服务回退实例。
     * Suspicious Ophidan Bridge service fallback instance.
     *
     * Service instance
     */
    static SuspiciousOphidanBridgeService suspiciousOphidanBridgeService() {
        return SuspiciousOphidanBridgeServiceFallback.INSTANCE;
    }

    /**
     * 铁壁前线服务回退实例。
     * Iron Wall Warfront service fallback instance.
     *
     * Service instance
     */
    static IronWallWarfrontService ironWallWarfrontService() {
        return IronWallWarfrontServiceFallback.INSTANCE;
    }

    /**
     * 伊迪尔穹顶服务回退实例。
     * Idgel Dome service fallback instance.
     *
     * Service instance
     */
    static IdgelDomeService idgelDomeService() {
        return IdgelDomeServiceFallback.INSTANCE;
    }

    /**
     * 伊迪尔穹顶地标服务回退实例。
     * Idgel Dome Landmark service fallback instance.
     *
     * Service instance
     */
    static IdgelDomeLandmarkService idgelDomeLandmarkService() {
        return IdgelDomeLandmarkServiceFallback.INSTANCE;
    }

    /**
     * 大竞技场训练营服务回退实例。
     * Grand Arena Training Camp service fallback instance.
     *
     * Service instance
     */
    static GrandArenaTrainingCampService grandArenaTrainingCampService() {
        return GrandArenaTrainingCampServiceFallback.INSTANCE;
    }

    /**
     * IDRun 服务回退实例。
     * IDRun service fallback instance.
     *
     * Service instance
     */
    static IDRunService idRunService() {
        return IdRunServiceFallback.INSTANCE;
    }

    /**
     * {@link KamarBattlefieldService} 懒加载单例持有者。
     * Lazy singleton holder for {@link KamarBattlefieldService}.
     */
    private static final class KamarBattlefieldServiceFallback {
        private static final KamarBattlefieldService INSTANCE = KamarBattlefieldService.getInstance();
    }

    /**
     * {@link EngulfedOphidanBridgeService} 懒加载单例持有者。
     * Lazy singleton holder for {@link EngulfedOphidanBridgeService}.
     */
    private static final class EngulfedOphidanBridgeServiceFallback {
        private static final EngulfedOphidanBridgeService INSTANCE = EngulfedOphidanBridgeService.getInstance();
    }

    /**
     * {@link SuspiciousOphidanBridgeService} 懒加载单例持有者。
     * Lazy singleton holder for {@link SuspiciousOphidanBridgeService}.
     */
    private static final class SuspiciousOphidanBridgeServiceFallback {
        private static final SuspiciousOphidanBridgeService INSTANCE = SuspiciousOphidanBridgeService.getInstance();
    }

    /**
     * {@link IronWallWarfrontService} 懒加载单例持有者。
     * Lazy singleton holder for {@link IronWallWarfrontService}.
     */
    private static final class IronWallWarfrontServiceFallback {
        private static final IronWallWarfrontService INSTANCE = IronWallWarfrontService.getInstance();
    }

    /**
     * {@link IdgelDomeService} 懒加载单例持有者。
     * Lazy singleton holder for {@link IdgelDomeService}.
     */
    private static final class IdgelDomeServiceFallback {
        private static final IdgelDomeService INSTANCE = IdgelDomeService.getInstance();
    }

    /**
     * {@link IdgelDomeLandmarkService} 懒加载单例持有者。
     * Lazy singleton holder for {@link IdgelDomeLandmarkService}.
     */
    private static final class IdgelDomeLandmarkServiceFallback {
        private static final IdgelDomeLandmarkService INSTANCE = IdgelDomeLandmarkService.getInstance();
    }

    /**
     * {@link GrandArenaTrainingCampService} 懒加载单例持有者。
     * Lazy singleton holder for {@link GrandArenaTrainingCampService}.
     */
    private static final class GrandArenaTrainingCampServiceFallback {
        private static final GrandArenaTrainingCampService INSTANCE = GrandArenaTrainingCampService.getInstance();
    }

    /**
     * {@link IDRunService} 懒加载单例持有者。
     * Lazy singleton holder for {@link IDRunService}.
     */
    private static final class IdRunServiceFallback {
        private static final IDRunService INSTANCE = IDRunService.getInstance();
    }
}
