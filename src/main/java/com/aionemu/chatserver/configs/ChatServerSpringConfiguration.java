package com.aionemu.chatserver.configs;

import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.chatserver.service.ChatRestartRequest;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatNettyServers;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 聊天服务器 Spring Bean 装配（仅在 {@code aion.services.chat.enabled=true} 时生效）。
 * Spring bean wiring for the chat server (active only when {@code aion.services.chat.enabled=true}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatServerSpringConfiguration {

    /**
     * ID 工厂 Bean。
     * ID factory bean.
     *
     * @return {@link IdFactory}
     */
    @Bean
    @Lazy
    public IdFactory idFactory() {
        return new IdFactory();
    }

    /**
     * 客户端封包处理器 Bean。
     * Client packet handler bean.
     *
     * Broadcast service
     * Chat service
     * @return {@link ClientPacketHandler}
     */
    @Bean
    @Lazy
    public ClientPacketHandler clientPacketHandler(BroadcastService broadcastService, ChatService chatService) {
        return new ClientPacketHandler(broadcastService, chatService);
    }

    /**
     * Netty 服务器 Bean，并注册到 {@link ChatNettyServers}。
     * Netty server bean, also registered with {@link ChatNettyServers}.
     *
     * @param clientPacketHandler 客户端封包处理器 / Client packet handler
     * @return {@link NettyServer}
     */
    @Bean
    @Lazy
    public NettyServer nettyServer(ClientPacketHandler clientPacketHandler) {
        return ChatNettyServers.register(new NettyServer(clientPacketHandler));
    }

    /**
     * 游戏服对接服务 Bean。
     * Game-server integration service bean.
     *
     * @return {@link GameServerService}
     */
    @Bean
    @Lazy
    public GameServerService gameServerService() {
        return new GameServerService();
    }

    /**
     * 广播服务 Bean。
     * Broadcast service bean.
     *
     * @return {@link BroadcastService}
     */
    @Bean
    @Lazy
    public BroadcastService broadcastService() {
        return new BroadcastService();
    }

    /**
     * 聊天服务 Bean。
     * Chat service bean.
     *
     * Broadcast service
     * @return {@link ChatService}
     */
    @Bean
    @Lazy
    public ChatService chatService(BroadcastService broadcastService) {
        return new ChatService(broadcastService);
    }

    /**
     * 重启服务 Bean，并绑定进程桥提供者到 {@link ChatRestartRequest}。
     * Restart service bean; also binds process-bridge provider on {@link ChatRestartRequest}.
     *
     * @param processBridgeProvider 进程桥提供者 / Process-bridge provider
     * @return {@link RestartService}
     */
    @Bean
    @Lazy
    public RestartService restartService(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        ChatRestartRequest.setProcessBridgeProvider(processBridgeProvider);
        return new RestartService();
    }

    /**
     * 聊天服关停钩子 Bean。
     * Chat-server shutdown-hook bean.
     *
     * @param processBridge 进程运行时桥 / Process runtime bridge
     * @param restartServiceProvider 重启服务提供者 / Restart-service provider
     * @param gameServerServiceProvider 游戏服服务提供者 / Game-server-service provider
     * @return {@link ShutdownHook}
     */
    @Bean
    @Lazy
    public ShutdownHook chatShutdownHook(
        ChatProcessRuntimeBridge processBridge,
        ObjectProvider<RestartService> restartServiceProvider,
        ObjectProvider<GameServerService> gameServerServiceProvider
    ) {
        return new ShutdownHook(processBridge, restartServiceProvider, gameServerServiceProvider);
    }
}
