package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameServerNetworkRuntimeBridge {

    private ObjectProvider<BannedMacManager> bannedMacManagerProvider;
    private ObjectProvider<LoginServer> loginServerProvider;
    private ObjectProvider<ChatServer> chatServerProvider;

    @Autowired(required = false)
    void setBannedMacManagerProvider(ObjectProvider<BannedMacManager> bannedMacManagerProvider) {
        this.bannedMacManagerProvider = bannedMacManagerProvider;
    }

    @Autowired(required = false)
    void setLoginServerProvider(ObjectProvider<LoginServer> loginServerProvider) {
        this.loginServerProvider = loginServerProvider;
    }

    @Autowired(required = false)
    void setChatServerProvider(ObjectProvider<ChatServer> chatServerProvider) {
        this.chatServerProvider = chatServerProvider;
    }

    public BannedMacManager bannedMacManager() {
        if (bannedMacManagerProvider == null) {
            return GameServerNetworkFallbacks.bannedMacManager();
        }
        return bannedMacManagerProvider.getIfAvailable(GameServerNetworkFallbacks::bannedMacManager);
    }

    public LoginServer loginServer() {
        if (loginServerProvider == null) {
            return GameServerNetworkFallbacks.loginServer();
        }
        return loginServerProvider.getIfAvailable(GameServerNetworkFallbacks::loginServer);
    }

    public ChatServer chatServer() {
        if (chatServerProvider == null) {
            return GameServerNetworkFallbacks.chatServer();
        }
        return chatServerProvider.getIfAvailable(GameServerNetworkFallbacks::chatServer);
    }

    public GameConnectionFactoryImpl gameConnectionFactory() {
        return new GameConnectionFactoryImpl();
    }
}
