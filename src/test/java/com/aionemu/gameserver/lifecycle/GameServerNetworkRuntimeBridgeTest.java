package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameServerNetworkRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        BannedMacManager bannedMacManager = instance(BannedMacManager.class);
        LoginServer loginServer = instance(LoginServer.class);
        ChatServer chatServer = instance(ChatServer.class);
        GameServerNetworkRuntimeBridge runtimeBridge = new GameServerNetworkRuntimeBridge();

        runtimeBridge.setBannedMacManagerProvider(provider(BannedMacManager.class, bannedMacManager));
        runtimeBridge.setLoginServerProvider(provider(LoginServer.class, loginServer));
        runtimeBridge.setChatServerProvider(provider(ChatServer.class, chatServer));

        assertSame(bannedMacManager, runtimeBridge.bannedMacManager());
        assertSame(loginServer, runtimeBridge.loginServer());
        assertSame(chatServer, runtimeBridge.chatServer());
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
