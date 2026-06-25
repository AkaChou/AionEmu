package com.aionemu.chatserver.configs;

import com.aionemu.chatserver.ChatProcessRuntimeBridge;
import com.aionemu.chatserver.ShutdownHook;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.network.netty.pipeline.LoginToClientPipeLineFactory;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
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
        return IdFactory.getInstance();
    }

    @Bean
    @Lazy
    public ClientPacketHandler clientPacketHandler(BroadcastService broadcastService, ChatService chatService) {
        return new ClientPacketHandler(broadcastService, chatService);
    }

    @Bean
    @Lazy
    public LoginToClientPipeLineFactory loginToClientPipeLineFactory(ClientPacketHandler clientPacketHandler) {
        return new LoginToClientPipeLineFactory(clientPacketHandler);
    }

    @Bean
    @Lazy
    public NettyServer nettyServer(
        ClientPacketHandler clientPacketHandler,
        LoginToClientPipeLineFactory loginToClientPipeLineFactory
    ) {
        return NettyServer.getInstance(clientPacketHandler, loginToClientPipeLineFactory);
    }

    @Bean
    @Lazy
    public GameServerService gameServerService() {
        return GameServerService.getInstance();
    }

    @Bean
    @Lazy
    public BroadcastService broadcastService() {
        return BroadcastService.getInstance();
    }

    @Bean
    @Lazy
    public ChatService chatService() {
        return ChatService.getInstance();
    }

    @Bean
    @Lazy
    public RestartService restartService() {
        return RestartService.getInstance();
    }

    @Bean
    @Lazy
    public ShutdownHook chatShutdownHook(ChatProcessRuntimeBridge processBridge) {
        return ShutdownHook.getInstance(processBridge);
    }
}
