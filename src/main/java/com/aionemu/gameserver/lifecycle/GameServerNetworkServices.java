package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameServerNetworkServices implements DisposableBean {

    public GameServerNetworkServices(ObjectProvider<LoginServer> loginServerProvider, ObjectProvider<ChatServer> chatServerProvider) {
        LoginServer.setInstanceProvider(loginServerProvider);
        ChatServer.setInstanceProvider(chatServerProvider);
    }

    @Override
    public void destroy() {
        LoginServer.setInstanceProvider(null);
        ChatServer.setInstanceProvider(null);
    }
}
