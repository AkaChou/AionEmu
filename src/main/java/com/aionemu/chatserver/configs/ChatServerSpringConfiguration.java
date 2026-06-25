package com.aionemu.chatserver.configs;

import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.network.netty.pipeline.LoginToClientPipeLineFactory;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class ChatServerSpringConfiguration {

    @Bean
    public IdFactory idFactory() {
        return IdFactory.getInstance();
    }

    @Bean
    public ClientPacketHandler clientPacketHandler(BroadcastService broadcastService, ChatService chatService) {
        return new ClientPacketHandler(broadcastService, chatService);
    }

    @Bean
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
    public GameServerService gameServerService() {
        return GameServerService.getInstance();
    }

    @Bean
    public BroadcastService broadcastService() {
        return BroadcastService.getInstance();
    }

    @Bean
    public ChatService chatService() {
        return ChatService.getInstance();
    }

    @Bean
    @Lazy
    public RestartService restartService() {
        return RestartService.getInstance();
    }
}
