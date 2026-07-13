package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.drop.DropRegistrationService;
import com.aionemu.gameserver.world.geo.GeoService;
import com.aionemu.gameserver.world.geo.path.PathService;

/**
 * 世界服务回退：在无 Spring 提供者时返回各组件 getInstance 单例。
 * World-service fallbacks: return each component's {@code getInstance} singleton when no Spring provider.
 */
final class GameWorldServiceFallbacks {

    /**
     * 工具类不可实例化。
     * Utility class is not instantiable.
     */
    private GameWorldServiceFallbacks() {
    }

    /**
     * 回退 GeoService。
     * Fallback GeoService.
     *
     * GeoService singleton
     */
    static GeoService geoService() {
        return GeoServiceFallback.INSTANCE;
    }

    /**
     * 回退 PathService。
     * Fallback PathService.
     *
     * PathService singleton
     */
    static PathService pathService() {
        return PathServiceFallback.INSTANCE;
    }

    /**
     * 回退 DropRegistrationService。
     * Fallback DropRegistrationService.
     *
     * DropRegistrationService singleton
     */
    static DropRegistrationService dropRegistrationService() {
        return DropRegistrationServiceFallback.INSTANCE;
    }

    /**
     * GeoService 回退持有者。
     * GeoService fallback holder.
     */
    private static final class GeoServiceFallback {
        /**
         * GeoService 单例。
         * GeoService singleton.
         */
        private static final GeoService INSTANCE = GeoService.getInstance();
    }

    /**
     * PathService 回退持有者。
     * PathService fallback holder.
     */
    private static final class PathServiceFallback {
        /**
         * PathService 单例。
         * PathService singleton.
         */
        private static final PathService INSTANCE = PathService.getInstance();
    }

    /**
     * DropRegistrationService 回退持有者。
     * DropRegistrationService fallback holder.
     */
    private static final class DropRegistrationServiceFallback {
        /**
         * DropRegistrationService 单例。
         * DropRegistrationService singleton.
         */
        private static final DropRegistrationService INSTANCE = DropRegistrationService.getInstance();
    }
}
