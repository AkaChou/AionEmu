package com.aionemu.chatserver.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.chatserver.utils.IdFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class ChatCoreServicesTest {

    @Test
    void usesSpringProvidersBeforeLocalFallbacks() {
        IdFactory idFactory = new IdFactory();
        GameServerService gameServerService = new GameServerService();
        BroadcastService broadcastService = new BroadcastService();
        ChatService chatService = new ChatService(broadcastService);
        ChatCoreServices services = new ChatCoreServices(
            provider(IdFactory.class, idFactory),
            provider(GameServerService.class, gameServerService),
            provider(BroadcastService.class, broadcastService),
            provider(ChatService.class, chatService)
        );

        try {
            assertSame(idFactory, ChatCoreServices.idFactory());
            assertSame(gameServerService, ChatCoreServices.gameServerService());
            assertSame(broadcastService, ChatCoreServices.broadcastService());
            assertSame(chatService, ChatCoreServices.chatService());
        } finally {
            services.destroy();
        }
    }

    @Test
    void packetAndLegacyPathsUseCoreServicesBridgeInsteadOfDirectSingletons() throws IOException {
        assertBridged("src/main/java/com/aionemu/chatserver/ChatServerLegacyDependencies.java");
        assertBridged("src/main/java/com/aionemu/chatserver/model/channel/Channel.java");
        assertBridged("src/main/java/com/aionemu/chatserver/network/aion/ClientPacketHandler.java");
        assertBridged("src/main/java/com/aionemu/chatserver/network/gameserver/GsConnection.java");
        assertBridged("src/main/java/com/aionemu/chatserver/network/gameserver/clientpackets/CM_CS_AUTH.java");
        assertBridged("src/main/java/com/aionemu/chatserver/network/gameserver/clientpackets/CM_PLAYER_AUTH.java");
        assertBridged("src/main/java/com/aionemu/chatserver/network/gameserver/clientpackets/CM_PLAYER_LOGOUT.java");
        assertBridged("src/main/java/com/aionemu/chatserver/network/gameserver/clientpackets/CM_PLAYER_GAG.java");
    }

    @Test
    void defaultChatServiceConstructorUsesCoreBridgeForBroadcastDependency() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/chatserver/service/ChatService.java"));

        assertFalse(source.contains("this(BroadcastService.getInstance())"));
        assertTrue(source.contains("this(ChatCoreServices.broadcastService())"));
    }

    private static void assertBridged(String path) throws IOException {
        String source = Files.readString(Path.of(path));

        assertFalse(source.contains("IdFactory.getInstance()"));
        assertFalse(source.contains("GameServerService.getInstance()"));
        assertFalse(source.contains("BroadcastService.getInstance()"));
        assertFalse(source.contains("ChatService.getInstance()"));
        assertTrue(source.contains("ChatCoreServices."));
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
