package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameServerNetworkRuntimeBridge {

    public BannedMacManager bannedMacManager() {
        return BannedMacManager.getInstance();
    }

    public LoginServer loginServer() {
        return LoginServer.getInstance();
    }

    public ChatServer chatServer() {
        return ChatServer.getInstance();
    }

    public GameConnectionFactoryImpl gameConnectionFactory() {
        return new GameConnectionFactoryImpl();
    }
}
