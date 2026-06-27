package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameServerNetworkServices implements DisposableBean {

    private static volatile ObjectProvider<BannedMacManager> bannedMacManagerProvider;

    public GameServerNetworkServices(ObjectProvider<LoginServer> loginServerProvider, ObjectProvider<ChatServer> chatServerProvider,
            ObjectProvider<BannedMacManager> bannedMacManagerProvider) {
        GameServerNetworkServices.bannedMacManagerProvider = bannedMacManagerProvider;
        LoginServer.setInstanceProvider(loginServerProvider);
        ChatServer.setInstanceProvider(chatServerProvider);
    }

    public static BannedMacManager bannedMacManager() {
        ObjectProvider<BannedMacManager> provider = bannedMacManagerProvider;
        if (provider == null) {
            return GameServerNetworkFallbacks.bannedMacManager();
        }
        return provider.getIfAvailable(GameServerNetworkFallbacks::bannedMacManager);
    }

    @Override
    public void destroy() {
        bannedMacManagerProvider = null;
        LoginServer.setInstanceProvider(null);
        ChatServer.setInstanceProvider(null);
    }
}
