package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 服务器网络服务静态门面：绑定登录/聊天/封禁/控制器与包处理相关 instance provider，
 * 并在销毁时清空；解析时优先 Spring，否则回退 {@link GameServerNetworkFallbacks}。
 * Static facade for server-network services: binds login/chat/ban/controller and packet-handler
 * instance providers, clears them on destroy; resolves via Spring then {@link GameServerNetworkFallbacks}.
 */
@Component
public final class GameServerNetworkServices implements DisposableBean {

    /**
     * 登录服连接提供者。
     * Login-server provider.
     */
    private static volatile ObjectProvider<LoginServer> loginServerProvider;

    /**
     * 聊天服连接提供者。
     * Chat-server provider.
     */
    private static volatile ObjectProvider<ChatServer> chatServerProvider;

    /**
     * MAC 封禁管理器提供者。
     * Banned-MAC manager provider.
     */
    private static volatile ObjectProvider<BannedMacManager> bannedMacManagerProvider;

    /**
     * 网络控制器提供者。
     * Network-controller provider.
     */
    private static volatile ObjectProvider<NetworkController> networkControllerProvider;

    /**
     * 数据包日志服务提供者。
     * Packet-logger service provider.
     */
    private static volatile ObjectProvider<PacketLoggerService> packetLoggerServiceProvider;

    /**
     * Aion 数据包处理器工厂提供者。
     * Aion packet-handler factory provider.
     */
    private static volatile ObjectProvider<AionPacketHandlerFactory> aionPacketHandlerFactoryProvider;

    /**
     * 数据包洪水过滤器提供者。
     * Packet flood-filter provider.
     */
    private static volatile ObjectProvider<PacketFloodFilter> packetFloodFilterProvider;

    /**
     * 登录服数据包处理器工厂提供者。
     * Login-server packet-handler factory provider.
     */
    private static volatile ObjectProvider<LsPacketHandlerFactory> lsPacketHandlerFactoryProvider;

    /**
     * 构造并绑定各网络服务的 instance provider。
     * Construct and bind instance providers for network services.
     *
     * Login server
     * Chat server
     * Banned MAC
     * @param networkControllerProvider 网络控制器 / Network controller
     * Packet logger
     * @param aionPacketHandlerFactoryProvider Aion 包处理工厂 / Aion packet-handler factory
     * Packet flood filter
     * @param lsPacketHandlerFactoryProvider 登录服包处理工厂 / LS packet-handler factory
     */
    public GameServerNetworkServices(ObjectProvider<LoginServer> loginServerProvider, ObjectProvider<ChatServer> chatServerProvider,
            ObjectProvider<BannedMacManager> bannedMacManagerProvider, ObjectProvider<NetworkController> networkControllerProvider,
            ObjectProvider<PacketLoggerService> packetLoggerServiceProvider,
            ObjectProvider<AionPacketHandlerFactory> aionPacketHandlerFactoryProvider,
            ObjectProvider<PacketFloodFilter> packetFloodFilterProvider,
            ObjectProvider<LsPacketHandlerFactory> lsPacketHandlerFactoryProvider) {
        GameServerNetworkServices.loginServerProvider = loginServerProvider;
        GameServerNetworkServices.chatServerProvider = chatServerProvider;
        GameServerNetworkServices.bannedMacManagerProvider = bannedMacManagerProvider;
        GameServerNetworkServices.networkControllerProvider = networkControllerProvider;
        GameServerNetworkServices.packetLoggerServiceProvider = packetLoggerServiceProvider;
        GameServerNetworkServices.aionPacketHandlerFactoryProvider = aionPacketHandlerFactoryProvider;
        GameServerNetworkServices.packetFloodFilterProvider = packetFloodFilterProvider;
        GameServerNetworkServices.lsPacketHandlerFactoryProvider = lsPacketHandlerFactoryProvider;
        LoginServer.setInstanceProvider(loginServerProvider);
        ChatServer.setInstanceProvider(chatServerProvider);
        NetworkController.setInstanceProvider(networkControllerProvider);
        PacketLoggerService.setInstanceProvider(packetLoggerServiceProvider);
        AionPacketHandlerFactory.setInstanceProvider(aionPacketHandlerFactoryProvider);
        PacketFloodFilter.setInstanceProvider(packetFloodFilterProvider);
        LsPacketHandlerFactory.setInstanceProvider(lsPacketHandlerFactoryProvider);
    }

    /**
     * 解析 MAC 封禁管理器。
     * Resolve the banned-MAC manager.
     *
     * @return MAC 封禁管理器 / Banned MAC manager
     */
    public static BannedMacManager bannedMacManager() {
        ObjectProvider<BannedMacManager> provider = bannedMacManagerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.bannedMacManager();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::bannedMacManager);
    }

    /**
     * 解析网络控制器。
     * Resolve the network controller.
     *
     * @return 网络控制器 / Network controller
     */
    public static NetworkController networkController() {
        ObjectProvider<NetworkController> provider = networkControllerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.networkController();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::networkController);
    }

    /**
     * 解析数据包日志服务。
     * Resolve the packet-logger service.
     *
     * @return 数据包日志服务 / Packet logger service
     */
    public static PacketLoggerService packetLoggerService() {
        ObjectProvider<PacketLoggerService> provider = packetLoggerServiceProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.packetLoggerService();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::packetLoggerService);
    }

    /**
     * 解析登录服连接。
     * Resolve the login-server connection.
     *
     * Login server
     */
    public static LoginServer loginServer() {
        ObjectProvider<LoginServer> provider = loginServerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.loginServer();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::loginServer);
    }

    /**
     * 解析聊天服连接。
     * Resolve the chat-server connection.
     *
     * Chat server
     */
    public static ChatServer chatServer() {
        ObjectProvider<ChatServer> provider = chatServerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.chatServer();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::chatServer);
    }

    /**
     * 解析 Aion 数据包处理器工厂。
     * Resolve the Aion packet-handler factory.
     *
     * @return Aion 数据包处理器工厂 / Aion packet-handler factory
     */
    public static AionPacketHandlerFactory aionPacketHandlerFactory() {
        ObjectProvider<AionPacketHandlerFactory> provider = aionPacketHandlerFactoryProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.aionPacketHandlerFactory();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::aionPacketHandlerFactory);
    }

    /**
     * 解析数据包洪水过滤器。
     * Resolve the packet flood filter.
     *
     * @return 数据包洪水过滤器 / Packet flood filter
     */
    public static PacketFloodFilter packetFloodFilter() {
        ObjectProvider<PacketFloodFilter> provider = packetFloodFilterProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.packetFloodFilter();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::packetFloodFilter);
    }

    /**
     * 解析登录服数据包处理器工厂。
     * Resolve the login-server packet-handler factory.
     *
     * @return 登录服数据包处理器工厂 / LS packet-handler factory
     */
    public static LsPacketHandlerFactory lsPacketHandlerFactory() {
        ObjectProvider<LsPacketHandlerFactory> provider = lsPacketHandlerFactoryProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.lsPacketHandlerFactory();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::lsPacketHandlerFactory);
    }

    /**
     * Spring 销毁时清空静态提供者与各服务 instance provider。
     * Clear static providers and each service instance provider on Spring destroy.
     */
    @Override
    public void destroy() {
        loginServerProvider = null;
        chatServerProvider = null;
        bannedMacManagerProvider = null;
        networkControllerProvider = null;
        packetLoggerServiceProvider = null;
        aionPacketHandlerFactoryProvider = null;
        packetFloodFilterProvider = null;
        lsPacketHandlerFactoryProvider = null;
        LoginServer.setInstanceProvider(null);
        ChatServer.setInstanceProvider(null);
        NetworkController.setInstanceProvider(null);
        PacketLoggerService.setInstanceProvider(null);
        AionPacketHandlerFactory.setInstanceProvider(null);
        PacketFloodFilter.setInstanceProvider(null);
        LsPacketHandlerFactory.setInstanceProvider(null);
    }
}
