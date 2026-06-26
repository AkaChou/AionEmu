package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void networkSingletonAccessorsUseSpringProvidersBeforeLegacyFallbacks() {
        LoginServer loginServer = instance(LoginServer.class);
        ChatServer chatServer = instance(ChatServer.class);

        try {
            LoginServer.setInstanceProvider(provider(LoginServer.class, loginServer));
            ChatServer.setInstanceProvider(provider(ChatServer.class, chatServer));

            assertSame(loginServer, LoginServer.getInstance());
            assertSame(chatServer, ChatServer.getInstance());
        } finally {
            LoginServer.setInstanceProvider(null);
            ChatServer.setInstanceProvider(null);
        }
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameServerNetworkRuntimeBridge.java"));

        assertFalse(source.contains("BannedMacManager.getInstance()"));
        assertFalse(source.contains("LoginServer.getInstance()"));
        assertFalse(source.contains("ChatServer.getInstance()"));
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
