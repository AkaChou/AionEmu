package com.aionemu.chatserver.service;

import com.aionemu.chatserver.utils.IdFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public final class ChatCoreServices implements DisposableBean {

    private static volatile ObjectProvider<IdFactory> idFactoryProvider;
    private static volatile ObjectProvider<GameServerService> gameServerServiceProvider;
    private static volatile ObjectProvider<BroadcastService> broadcastServiceProvider;
    private static volatile ObjectProvider<ChatService> chatServiceProvider;

    public ChatCoreServices(
        ObjectProvider<IdFactory> idFactoryProvider,
        ObjectProvider<GameServerService> gameServerServiceProvider,
        ObjectProvider<BroadcastService> broadcastServiceProvider,
        ObjectProvider<ChatService> chatServiceProvider
    ) {
        ChatCoreServices.idFactoryProvider = idFactoryProvider;
        ChatCoreServices.gameServerServiceProvider = gameServerServiceProvider;
        ChatCoreServices.broadcastServiceProvider = broadcastServiceProvider;
        ChatCoreServices.chatServiceProvider = chatServiceProvider;
    }

    public static IdFactory idFactory() {
        ObjectProvider<IdFactory> provider = idFactoryProvider;
        if (provider == null) {
            return fallbackIdFactory();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackIdFactory);
    }

    public static GameServerService gameServerService() {
        ObjectProvider<GameServerService> provider = gameServerServiceProvider;
        if (provider == null) {
            return fallbackGameServerService();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackGameServerService);
    }

    public static BroadcastService broadcastService() {
        ObjectProvider<BroadcastService> provider = broadcastServiceProvider;
        if (provider == null) {
            return fallbackBroadcastService();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackBroadcastService);
    }

    public static ChatService chatService() {
        ObjectProvider<ChatService> provider = chatServiceProvider;
        if (provider == null) {
            return fallbackChatService();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackChatService);
    }

    @Override
    public void destroy() {
        idFactoryProvider = null;
        gameServerServiceProvider = null;
        broadcastServiceProvider = null;
        chatServiceProvider = null;
    }

    private static IdFactory fallbackIdFactory() {
        return Fallbacks.ID_FACTORY;
    }

    private static GameServerService fallbackGameServerService() {
        return Fallbacks.GAME_SERVER_SERVICE;
    }

    private static BroadcastService fallbackBroadcastService() {
        return Fallbacks.BROADCAST_SERVICE;
    }

    private static ChatService fallbackChatService() {
        return Fallbacks.CHAT_SERVICE;
    }

    private static final class Fallbacks {

        private static final IdFactory ID_FACTORY = new IdFactory();
        private static final GameServerService GAME_SERVER_SERVICE = new GameServerService();
        private static final BroadcastService BROADCAST_SERVICE = new BroadcastService();
        private static final ChatService CHAT_SERVICE = new ChatService(BROADCAST_SERVICE);
    }
}
