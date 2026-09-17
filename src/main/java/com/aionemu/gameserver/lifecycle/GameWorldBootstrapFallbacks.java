package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;

/**
 * 世界引导回退：在无 Spring 提供者时返回各组件 getInstance 单例；已退役双源兜底的组件直接 fail-fast。
 * World-bootstrap fallbacks: return each component's getInstance singleton when no Spring provider;
 * components whose dual-source fallback is retired fail fast instead.
 */
final class GameWorldBootstrapFallbacks {

    /**
     * 工具类不可实例化。
     * Utility class is not instantiable.
     */
    private GameWorldBootstrapFallbacks() {
    }

    /**
     * 返回 IDFactory：双源兜底已退役，交由 {@link IDFactory#getInstance()} fail-fast。
     * Returns IDFactory: the dual-source fallback is retired; delegates to IDFactory.getInstance() and fails fast.
     *
     * @return IDFactory 实例 / IDFactory instance
     */
    static IDFactory idFactory() {
        return IDFactory.getInstance();
    }

    /**
     * 回退 ZoneService。
     * Fallback ZoneService.
     *
     * @return ZoneService 单例 / ZoneService singleton
     */
    static ZoneService zoneService() {
        return ZoneServiceFallback.INSTANCE;
    }

    /**
     * 回退 HotspotTeleportService。
     * Fallback HotspotTeleportService.
     *
     * @return HotspotTeleportService 单例 / HotspotTeleportService singleton
     */
    static HotspotTeleportService hotspotTeleportService() {
        return HotspotTeleportServiceFallback.INSTANCE;
    }

    /**
     * 回退 RoadService。
     * Fallback RoadService.
     *
     * @return RoadService 单例 / RoadService singleton
     */
    static RoadService roadService() {
        return RoadServiceFallback.INSTANCE;
    }

    /**
     * 返回 World：双源兜底已退役，交由 {@link World#getInstance()} fail-fast。
     * Returns World: the dual-source fallback is retired; delegates to World.getInstance() and fails fast.
     *
     * @return World 实例 / World instance
     */
    static World world() {
        return World.getInstance();
    }

    /**
     * ZoneService 回退持有者。
     * ZoneService fallback holder.
     */
    private static final class ZoneServiceFallback {
        /**
         * ZoneService 单例。
         * ZoneService singleton.
         */
        private static final ZoneService INSTANCE = ZoneService.getInstance();
    }

    /**
     * HotspotTeleportService 回退持有者。
     * HotspotTeleportService fallback holder.
     */
    private static final class HotspotTeleportServiceFallback {
        /**
         * HotspotTeleportService 单例。
         * HotspotTeleportService singleton.
         */
        private static final HotspotTeleportService INSTANCE = HotspotTeleportService.getInstance();
    }

    /**
     * RoadService 回退持有者。
     * RoadService fallback holder.
     */
    private static final class RoadServiceFallback {
        /**
         * RoadService 单例。
         * RoadService singleton.
         */
        private static final RoadService INSTANCE = RoadService.getInstance();
    }
}
