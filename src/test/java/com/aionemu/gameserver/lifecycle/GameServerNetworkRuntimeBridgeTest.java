package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.NetworkController;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.network.PacketLoggerService;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.factories.AionPacketHandlerFactory;
import com.aionemu.gameserver.network.factories.LsPacketHandlerFactory;
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
        NetworkController networkController = instance(NetworkController.class);
        LoginServer loginServer = instance(LoginServer.class);
        ChatServer chatServer = instance(ChatServer.class);
        PacketLoggerService packetLoggerService = instance(PacketLoggerService.class);
        AionPacketHandlerFactory aionPacketHandlerFactory = instance(AionPacketHandlerFactory.class);
        PacketFloodFilter packetFloodFilter = instance(PacketFloodFilter.class);
        LsPacketHandlerFactory lsPacketHandlerFactory = instance(LsPacketHandlerFactory.class);
        GameServerNetworkServices gameServerNetworkServices = new GameServerNetworkServices(
            provider(LoginServer.class, loginServer),
            provider(ChatServer.class, chatServer),
            provider(BannedMacManager.class, bannedMacManager),
            provider(NetworkController.class, networkController),
            provider(PacketLoggerService.class, packetLoggerService),
            provider(AionPacketHandlerFactory.class, aionPacketHandlerFactory),
            provider(PacketFloodFilter.class, packetFloodFilter),
            provider(LsPacketHandlerFactory.class, lsPacketHandlerFactory)
        );

        try {
            LoginServer.setInstanceProvider(provider(LoginServer.class, loginServer));
            ChatServer.setInstanceProvider(provider(ChatServer.class, chatServer));
            NetworkController.setInstanceProvider(provider(NetworkController.class, networkController));
            PacketLoggerService.setInstanceProvider(provider(PacketLoggerService.class, packetLoggerService));

            assertSame(bannedMacManager, GameServerNetworkServices.bannedMacManager());
            assertSame(networkController, GameServerNetworkServices.networkController());
            assertSame(packetLoggerService, GameServerNetworkServices.packetLoggerService());
            assertSame(aionPacketHandlerFactory, GameServerNetworkServices.aionPacketHandlerFactory());
            assertSame(packetFloodFilter, GameServerNetworkServices.packetFloodFilter());
            assertSame(lsPacketHandlerFactory, GameServerNetworkServices.lsPacketHandlerFactory());
            assertSame(loginServer, LoginServer.getInstance());
            assertSame(chatServer, ChatServer.getInstance());
            assertSame(networkController, NetworkController.getInstance());
            assertSame(packetLoggerService, PacketLoggerService.getInstance());
            assertSame(aionPacketHandlerFactory, AionPacketHandlerFactory.getInstance());
            assertSame(packetFloodFilter, PacketFloodFilter.getInstance());
            assertSame(lsPacketHandlerFactory, LsPacketHandlerFactory.getInstance());
        } finally {
            gameServerNetworkServices.destroy();
            LoginServer.setInstanceProvider(null);
            ChatServer.setInstanceProvider(null);
            NetworkController.setInstanceProvider(null);
            PacketLoggerService.setInstanceProvider(null);
            AionPacketHandlerFactory.setInstanceProvider(null);
            PacketFloodFilter.setInstanceProvider(null);
            LsPacketHandlerFactory.setInstanceProvider(null);
        }
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameServerNetworkRuntimeBridge.java"));

        assertFalse(source.contains("BannedMacManager.getInstance()"));
        assertFalse(source.contains("LoginServer.getInstance()"));
        assertFalse(source.contains("ChatServer.getInstance()"));
        assertFalse(source.contains("NetworkController.getInstance()"));
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

    @Test
    void gameServerCodeUsesNetworkBridgeInsteadOfDirectNetworkControllerSingleton() throws IOException {
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            for (Path source : stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("network/NetworkController.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameServerNetworkFallbacks.java")))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameServerNetworkServices.java")))
                .toList()) {
                String content = Files.readString(source);

                assertFalse(content.contains("NetworkController.getInstance()"), source.toString());
            }
        }
    }

    @Test
    void aionConnectionUsesNetworkBridgeForPacketInfrastructure() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/network/aion/AionConnection.java"));

        assertFalse(source.contains("AionPacketHandlerFactory.getInstance()"));
        assertFalse(source.contains("PacketFloodFilter.getInstance()"));
        assertEquals(1, countOccurrences(source, "GameServerNetworkServices.aionPacketHandlerFactory()"));
        assertEquals(1, countOccurrences(source, "GameServerNetworkServices.packetFloodFilter()"));
    }

    @Test
    void loginServerConnectionUsesNetworkBridgeForPacketFactory() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/network/loginserver/LoginServerConnection.java"));

        assertFalse(source.contains("LsPacketHandlerFactory.getInstance()"));
        assertEquals(1, countOccurrences(source, "GameServerNetworkServices.lsPacketHandlerFactory()"));
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static int countOccurrences(String source, String value) {
        int count = 0;
        int index = source.indexOf(value);
        while (index >= 0) {
            count++;
            index = source.indexOf(value, index + value.length());
        }
        return count;
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
