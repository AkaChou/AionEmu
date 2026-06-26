package com.aionemu.chatserver.configs;

import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.chatserver.service.ChatRestartRequest;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatServerSpringConfiguration {

    @Bean
    @Lazy
    public IdFactory idFactory() {
        return new IdFactory();
    }

    @Bean
    @Lazy
    public ClientPacketHandler clientPacketHandler(BroadcastService broadcastService, ChatService chatService) {
        return new ClientPacketHandler(broadcastService, chatService);
    }

    @Bean
    @Lazy
    public NettyServer nettyServer(ClientPacketHandler clientPacketHandler) {
        return new NettyServer(clientPacketHandler);
    }

    @Bean
    @Lazy
    public GameServerService gameServerService() {
        return new GameServerService();
    }

    @Bean
    @Lazy
    public BroadcastService broadcastService() {
        return new BroadcastService();
    }

    @Bean
    @Lazy
    public ChatService chatService(BroadcastService broadcastService) {
        return new ChatService(broadcastService);
    }

    @Bean
    @Lazy
    public RestartService restartService(ObjectProvider<ChatProcessRuntimeBridge> processBridgeProvider) {
        ChatRestartRequest.setProcessBridgeProvider(processBridgeProvider);
        return new RestartService();
    }

    @Bean
    @Lazy
    public ShutdownHook chatShutdownHook(ChatProcessRuntimeBridge processBridge, RestartService restartService, GameServerService gameServerService) {
        return new ShutdownHook(processBridge, restartService, gameServerService);
    }
}
