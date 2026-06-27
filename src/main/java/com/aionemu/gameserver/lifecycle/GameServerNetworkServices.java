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

@Component
public final class GameServerNetworkServices implements DisposableBean {

    private static volatile ObjectProvider<BannedMacManager> bannedMacManagerProvider;
    private static volatile ObjectProvider<NetworkController> networkControllerProvider;
    private static volatile ObjectProvider<PacketLoggerService> packetLoggerServiceProvider;
    private static volatile ObjectProvider<AionPacketHandlerFactory> aionPacketHandlerFactoryProvider;
    private static volatile ObjectProvider<PacketFloodFilter> packetFloodFilterProvider;
    private static volatile ObjectProvider<LsPacketHandlerFactory> lsPacketHandlerFactoryProvider;

    public GameServerNetworkServices(ObjectProvider<LoginServer> loginServerProvider, ObjectProvider<ChatServer> chatServerProvider,
            ObjectProvider<BannedMacManager> bannedMacManagerProvider, ObjectProvider<NetworkController> networkControllerProvider,
            ObjectProvider<PacketLoggerService> packetLoggerServiceProvider,
            ObjectProvider<AionPacketHandlerFactory> aionPacketHandlerFactoryProvider,
            ObjectProvider<PacketFloodFilter> packetFloodFilterProvider,
            ObjectProvider<LsPacketHandlerFactory> lsPacketHandlerFactoryProvider) {
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

    public static BannedMacManager bannedMacManager() {
        ObjectProvider<BannedMacManager> provider = bannedMacManagerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.bannedMacManager();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::bannedMacManager);
    }

    public static NetworkController networkController() {
        ObjectProvider<NetworkController> provider = networkControllerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.networkController();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::networkController);
    }

    public static PacketLoggerService packetLoggerService() {
        ObjectProvider<PacketLoggerService> provider = packetLoggerServiceProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.packetLoggerService();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::packetLoggerService);
    }

    public static AionPacketHandlerFactory aionPacketHandlerFactory() {
        ObjectProvider<AionPacketHandlerFactory> provider = aionPacketHandlerFactoryProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.aionPacketHandlerFactory();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::aionPacketHandlerFactory);
    }

    public static PacketFloodFilter packetFloodFilter() {
        ObjectProvider<PacketFloodFilter> provider = packetFloodFilterProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.packetFloodFilter();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::packetFloodFilter);
    }

    public static LsPacketHandlerFactory lsPacketHandlerFactory() {
        ObjectProvider<LsPacketHandlerFactory> provider = lsPacketHandlerFactoryProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.lsPacketHandlerFactory();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::lsPacketHandlerFactory);
    }

    @Override
    public void destroy() {
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
