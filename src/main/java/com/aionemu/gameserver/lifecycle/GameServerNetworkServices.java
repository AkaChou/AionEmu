package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameServerNetworkServices implements DisposableBean {

    private static volatile ObjectProvider<BannedMacManager> bannedMacManagerProvider;
    private static volatile ObjectProvider<NetworkController> networkControllerProvider;
    private static volatile ObjectProvider<PacketLoggerService> packetLoggerServiceProvider;

    public GameServerNetworkServices(ObjectProvider<LoginServer> loginServerProvider, ObjectProvider<ChatServer> chatServerProvider,
            ObjectProvider<BannedMacManager> bannedMacManagerProvider, ObjectProvider<NetworkController> networkControllerProvider,
            ObjectProvider<PacketLoggerService> packetLoggerServiceProvider) {
        GameServerNetworkServices.bannedMacManagerProvider = bannedMacManagerProvider;
        GameServerNetworkServices.networkControllerProvider = networkControllerProvider;
        GameServerNetworkServices.packetLoggerServiceProvider = packetLoggerServiceProvider;
        LoginServer.setInstanceProvider(loginServerProvider);
        ChatServer.setInstanceProvider(chatServerProvider);
        NetworkController.setInstanceProvider(networkControllerProvider);
        PacketLoggerService.setInstanceProvider(packetLoggerServiceProvider);
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

    @Override
    public void destroy() {
        bannedMacManagerProvider = null;
        networkControllerProvider = null;
        packetLoggerServiceProvider = null;
        LoginServer.setInstanceProvider(null);
        ChatServer.setInstanceProvider(null);
        NetworkController.setInstanceProvider(null);
        PacketLoggerService.setInstanceProvider(null);
    }
}
