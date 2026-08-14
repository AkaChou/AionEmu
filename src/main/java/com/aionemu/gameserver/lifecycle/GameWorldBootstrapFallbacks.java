package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.RoadService;
import com.aionemu.gameserver.services.teleport.HotspotTeleportService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.zone.ZoneService;

/**
 * 世界引导回退：在无 Spring 提供者时返回各组件 getInstance 单例。
 * World-bootstrap fallbacks: return each component's {@code getInstance} singleton when no Spring provider.
 */
final class GameWorldBootstrapFallbacks {

    /**
     * 工具类不可实例化。
     * Utility class is not instantiable.
     */
    private GameWorldBootstrapFallbacks() {
    }

    /**
     * 回退 IDFactory。
     * Fallback IDFactory.
     *
     * @return IDFactory 单例 / IDFactory singleton
     */
    static IDFactory idFactory() {
        return IdFactoryFallback.INSTANCE;
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
     * 回退 World。
     * Fallback World.
     *
     * @return World 单例 / World singleton
     */
    static World world() {
        return WorldFallback.INSTANCE;
    }

    /**
     * IDFactory 回退持有者。
     * IDFactory fallback holder.
     */
    private static final class IdFactoryFallback {
        /**
         * IDFactory 单例。
         * IDFactory singleton.
         */
        private static final IDFactory INSTANCE = IDFactory.getInstance();
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

    /**
     * World 回退持有者。
     * World fallback holder.
     */
    private static final class WorldFallback {
        /**
         * World 单例。
         * World singleton.
         */
        private static final World INSTANCE = World.getInstance();
    }
}
