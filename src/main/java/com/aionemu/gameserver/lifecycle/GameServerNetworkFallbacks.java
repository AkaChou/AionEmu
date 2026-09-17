package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.LoginServer;

/**
 * 服务器网络组件回退工厂：在 Spring 未提供 bean 时返回各网络单例；已退役双源兜底的组件直接 fail-fast。
 * Fallback factory for server-network components: returns classic singletons when Spring beans are absent;
 * components whose dual-source fallback is retired fail fast instead.
 */
final class GameServerNetworkFallbacks {

    /**
     * 禁止实例化。
     * Prevent instantiation.
     */
    private GameServerNetworkFallbacks() {
    }

    /**
     * 返回 MAC 封禁管理器回退实例。
     * Return the banned-MAC manager fallback.
     *
     * @return MAC 封禁管理器 / Banned MAC manager
     */
    static BannedMacManager bannedMacManager() {
        return BannedMacManagerFallback.INSTANCE;
    }

    /**
     * 返回 LoginServer：双源兜底已退役，交由 {@link LoginServer#getInstance()} fail-fast。
     * Returns LoginServer: the dual-source fallback is retired; delegates to LoginServer.getInstance() and fails fast.
     *
     * @return LoginServer 实例 / LoginServer instance
     */
    static LoginServer loginServer() {
        return LoginServer.getInstance();
    }

    /**
     * 返回 ChatServer：双源兜底已退役，交由 {@link ChatServer#getInstance()} fail-fast。
     * Returns ChatServer: the dual-source fallback is retired; delegates to ChatServer.getInstance() and fails fast.
     *
     * @return ChatServer 实例 / ChatServer instance
     */
    static ChatServer chatServer() {
        return ChatServer.getInstance();
    }

    /**
     * 返回数据包日志服务回退实例。
     * Return the packet-logger service fallback.
     *
     * @return 数据包日志服务 / Packet logger service
     */
    static PacketLoggerService packetLoggerService() {
        return PacketLoggerService.getInstance();
    }

    /**
     * 返回网络控制器回退实例。
     * Return the network-controller fallback.
     *
     * @return 网络控制器 / Network controller
     */
    static NetworkController networkController() {
        return NetworkControllerFallback.INSTANCE;
    }

    /**
     * 返回 AionPacketHandlerFactory：双源兜底已退役，交由 {@link AionPacketHandlerFactory#getInstance()} fail-fast。
     * Returns AionPacketHandlerFactory: the dual-source fallback is retired;
     * delegates to AionPacketHandlerFactory.getInstance() and fails fast.
     *
     * @return AionPacketHandlerFactory 实例 / AionPacketHandlerFactory instance
     */
    static AionPacketHandlerFactory aionPacketHandlerFactory() {
        return AionPacketHandlerFactory.getInstance();
    }

    /**
     * 返回 PacketFloodFilter：双源兜底已退役，交由 {@link PacketFloodFilter#getInstance()} fail-fast。
     * Returns PacketFloodFilter: the dual-source fallback is retired;
     * delegates to PacketFloodFilter.getInstance() and fails fast.
     *
     * @return PacketFloodFilter 实例 / PacketFloodFilter instance
     */
    static PacketFloodFilter packetFloodFilter() {
        return PacketFloodFilter.getInstance();
    }

    /**
     * 返回 LsPacketHandlerFactory：双源兜底已退役，交由 {@link LsPacketHandlerFactory#getInstance()} fail-fast。
     * Returns LsPacketHandlerFactory: the dual-source fallback is retired;
     * delegates to LsPacketHandlerFactory.getInstance() and fails fast.
     *
     * @return LsPacketHandlerFactory 实例 / LsPacketHandlerFactory instance
     */
    static LsPacketHandlerFactory lsPacketHandlerFactory() {
        return LsPacketHandlerFactory.getInstance();
    }

    /**
     * 懒加载 {@link BannedMacManager} 回退单例。
     * Lazy holder for the {@link BannedMacManager} fallback singleton.
     */
    private static final class BannedMacManagerFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final BannedMacManager INSTANCE = BannedMacManager.getInstance();
    }

    /**
     * 懒加载 {@link NetworkController} 回退单例。
     * Lazy holder for the {@link NetworkController} fallback singleton.
     */
    private static final class NetworkControllerFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final NetworkController INSTANCE = NetworkController.getInstance();
    }
}
