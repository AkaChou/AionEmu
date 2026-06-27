package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.PacketLoggerService;
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
        BannedMacManager bannedMacManager = instance(BannedMacManager.class);
        LoginServer loginServer = instance(LoginServer.class);
        ChatServer chatServer = instance(ChatServer.class);
        PacketLoggerService packetLoggerService = instance(PacketLoggerService.class);
        GameServerNetworkServices gameServerNetworkServices = new GameServerNetworkServices(
            provider(LoginServer.class, loginServer),
            provider(ChatServer.class, chatServer),
            provider(BannedMacManager.class, bannedMacManager),
            provider(PacketLoggerService.class, packetLoggerService)
        );

        try {
            LoginServer.setInstanceProvider(provider(LoginServer.class, loginServer));
            ChatServer.setInstanceProvider(provider(ChatServer.class, chatServer));
            PacketLoggerService.setInstanceProvider(provider(PacketLoggerService.class, packetLoggerService));

            assertSame(bannedMacManager, GameServerNetworkServices.bannedMacManager());
            assertSame(packetLoggerService, GameServerNetworkServices.packetLoggerService());
            assertSame(loginServer, LoginServer.getInstance());
            assertSame(chatServer, ChatServer.getInstance());
            assertSame(packetLoggerService, PacketLoggerService.getInstance());
        } finally {
            gameServerNetworkServices.destroy();
            LoginServer.setInstanceProvider(null);
            ChatServer.setInstanceProvider(null);
            PacketLoggerService.setInstanceProvider(null);
        }
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameServerNetworkRuntimeBridge.java"));

        assertFalse(source.contains("BannedMacManager.getInstance()"));
        assertFalse(source.contains("LoginServer.getInstance()"));
        assertFalse(source.contains("ChatServer.getInstance()"));
        assertFalse(source.contains("PacketLoggerService.getInstance()"));
    }

    @Test
    void gameServerCodeUsesNetworkBridgeInsteadOfDirectBannedMacSingleton() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            for (Path source : stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("network/BannedMacManager.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameServerNetworkFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameServerNetworkServices.java")))
                .toList()) {
                String content = Files.readString(source);

                assertFalse(content.contains("BannedMacManager.getInstance()"), source.toString());
            }
        }
    }

    @Test
    void gameServerCodeUsesNetworkBridgeInsteadOfDirectPacketLoggerSingleton() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            for (Path source : stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("network/PacketLoggerService.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameServerNetworkFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameServerNetworkServices.java")))
                .toList()) {
                String content = Files.readString(source);

                assertFalse(content.contains("PacketLoggerService.getInstance()"), source.toString());
            }
        }
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
