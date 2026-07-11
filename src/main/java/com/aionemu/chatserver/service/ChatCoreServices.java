package com.aionemu.chatserver.service;

import com.aionemu.chatserver.utils.IdFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 聊天核心服务静态访问门面：在 Spring 可用时委托 {@link ObjectProvider}，否则回退本地实例。
 * Static facade for chat core services: delegates to {@link ObjectProvider} when Spring is available, otherwise falls back to local instances.
 */
@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public final class ChatCoreServices implements DisposableBean {

    private static volatile ObjectProvider<IdFactory> idFactoryProvider;
    private static volatile ObjectProvider<GameServerService> gameServerServiceProvider;
    private static volatile ObjectProvider<BroadcastService> broadcastServiceProvider;
    private static volatile ObjectProvider<ChatService> chatServiceProvider;

    /**
     * 注册各核心服务的 Spring {@link ObjectProvider}。
     * Register Spring {@link ObjectProvider}s for core services.
     *
     * @param idFactoryProvider ID 工厂提供者 / ID factory provider
     * @param gameServerServiceProvider 游戏服服务提供者 / Game server service provider
     * @param broadcastServiceProvider 广播服务提供者 / Broadcast service provider
     * @param chatServiceProvider 聊天服务提供者 / Chat service provider
     */
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

    /**
     * 获取 ID 工厂。
     * Obtain the ID factory.
     *
     * ID factory instance
     */
    public static IdFactory idFactory() {
        ObjectProvider<IdFactory> provider = idFactoryProvider;
        if (provider == null) {
            return fallbackIdFactory();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackIdFactory);
    }

    /**
     * 获取游戏服服务。
     * Obtain the game server service.
     *
     * @return 游戏服服务实例 / Game server service instance
     */
    public static GameServerService gameServerService() {
        ObjectProvider<GameServerService> provider = gameServerServiceProvider;
        if (provider == null) {
            return fallbackGameServerService();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackGameServerService);
    }

    /**
     * 获取广播服务。
     * Obtain the broadcast service.
     *
     * @return 广播服务实例 / Broadcast service instance
     */
    public static BroadcastService broadcastService() {
        ObjectProvider<BroadcastService> provider = broadcastServiceProvider;
        if (provider == null) {
            return fallbackBroadcastService();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackBroadcastService);
    }

    /**
     * 获取聊天服务。
     * Obtain the chat service.
     *
     * @return 聊天服务实例 / Chat service instance
     */
    public static ChatService chatService() {
        ObjectProvider<ChatService> provider = chatServiceProvider;
        if (provider == null) {
            return fallbackChatService();
        }
        return provider.getIfAvailable(ChatCoreServices::fallbackChatService);
    }

    /**
     * Bean 销毁时清空静态提供者引用。
     * Clear static provider references when the bean is destroyed.
     */
    @Override
    public void destroy() {
        idFactoryProvider = null;
        gameServerServiceProvider = null;
        broadcastServiceProvider = null;
        chatServiceProvider = null;
    }

    /**
     * 回退到本地 ID 工厂。
     * Fall back to the local ID factory.
     *
     * Fallback instance
     */
    private static IdFactory fallbackIdFactory() {
        return Fallbacks.ID_FACTORY;
    }

    /**
     * 回退到本地游戏服服务。
     * Fall back to the local game server service.
     *
     * Fallback instance
     */
    private static GameServerService fallbackGameServerService() {
        return Fallbacks.GAME_SERVER_SERVICE;
    }

    /**
     * 回退到本地广播服务。
     * Fall back to the local broadcast service.
     *
     * Fallback instance
     */
    private static BroadcastService fallbackBroadcastService() {
        return Fallbacks.BROADCAST_SERVICE;
    }

    /**
     * 回退到本地聊天服务。
     * Fall back to the local chat service.
     *
     * Fallback instance
     */
    private static ChatService fallbackChatService() {
        return Fallbacks.CHAT_SERVICE;
    }

    /**
     * 非 Spring 环境下的懒加载回退实例容器。
     * Holder for lazy fallback instances outside Spring.
     */
    private static final class Fallbacks {

        private static final IdFactory ID_FACTORY = new IdFactory();
        private static final GameServerService GAME_SERVER_SERVICE = new GameServerService();
        private static final BroadcastService BROADCAST_SERVICE = new BroadcastService();
        private static final ChatService CHAT_SERVICE = new ChatService(BROADCAST_SERVICE);
    }
}
