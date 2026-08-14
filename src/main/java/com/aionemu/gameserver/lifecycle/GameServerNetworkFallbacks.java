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
 * 服务器网络组件回退工厂：在 Spring 未提供 bean 时返回各网络单例。
 * Fallback factory for server-network components: returns classic singletons when Spring beans are absent.
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
     * 返回登录服连接回退实例。
     * Return the login-server fallback.
     *
     * @return 登录服连接 / Login server
     */
    static LoginServer loginServer() {
        return LoginServerFallback.INSTANCE;
    }

    /**
     * 返回聊天服连接回退实例。
     * Return the chat-server fallback.
     *
     * @return 聊天服连接 / Chat server
     */
    static ChatServer chatServer() {
        return ChatServerFallback.INSTANCE;
    }

    /**
     * 返回数据包日志服务回退实例。
     * Return the packet-logger service fallback.
     *
     * @return 数据包日志服务 / Packet logger service
     */
    static PacketLoggerService packetLoggerService() {
        return PacketLoggerServiceFallback.INSTANCE;
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
     * 返回 Aion 数据包处理器工厂回退实例。
     * Return the Aion packet-handler factory fallback.
     *
     * @return Aion 数据包处理器工厂 / Aion packet-handler factory
     */
    static AionPacketHandlerFactory aionPacketHandlerFactory() {
        return AionPacketHandlerFactoryFallback.INSTANCE;
    }

    /**
     * 返回数据包洪水过滤器回退实例。
     * Return the packet flood-filter fallback.
     *
     * @return 数据包洪水过滤器 / Packet flood filter
     */
    static PacketFloodFilter packetFloodFilter() {
        return PacketFloodFilterFallback.INSTANCE;
    }

    /**
     * 返回登录服数据包处理器工厂回退实例。
     * Return the login-server packet-handler factory fallback.
     *
     * @return 登录服数据包处理器工厂 / LS packet-handler factory
     */
    static LsPacketHandlerFactory lsPacketHandlerFactory() {
        return LsPacketHandlerFactoryFallback.INSTANCE;
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
     * 懒加载 {@link LoginServer} 回退单例。
     * Lazy holder for the {@link LoginServer} fallback singleton.
     */
    private static final class LoginServerFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final LoginServer INSTANCE = LoginServer.getInstance();
    }

    /**
     * 懒加载 {@link ChatServer} 回退单例。
     * Lazy holder for the {@link ChatServer} fallback singleton.
     */
    private static final class ChatServerFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final ChatServer INSTANCE = ChatServer.getInstance();
    }

    /**
     * 懒加载 {@link PacketLoggerService} 回退单例。
     * Lazy holder for the {@link PacketLoggerService} fallback singleton.
     */
    private static final class PacketLoggerServiceFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final PacketLoggerService INSTANCE = PacketLoggerService.getInstance();
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

    /**
     * 懒加载 {@link AionPacketHandlerFactory} 回退单例。
     * Lazy holder for the {@link AionPacketHandlerFactory} fallback singleton.
     */
    private static final class AionPacketHandlerFactoryFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final AionPacketHandlerFactory INSTANCE = AionPacketHandlerFactory.getInstance();
    }

    /**
     * 懒加载 {@link PacketFloodFilter} 回退单例。
     * Lazy holder for the {@link PacketFloodFilter} fallback singleton.
     */
    private static final class PacketFloodFilterFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final PacketFloodFilter INSTANCE = PacketFloodFilter.getInstance();
    }

    /**
     * 懒加载 {@link LsPacketHandlerFactory} 回退单例。
     * Lazy holder for the {@link LsPacketHandlerFactory} fallback singleton.
     */
    private static final class LsPacketHandlerFactoryFallback {
        /**
         * 回退实例。
         * Fallback instance.
         */
        private static final LsPacketHandlerFactory INSTANCE = LsPacketHandlerFactory.getInstance();
    }
}
