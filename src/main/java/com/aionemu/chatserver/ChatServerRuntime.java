package com.aionemu.chatserver;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
public class ChatServerRuntime implements ChatServerDependencies {

    private final ObjectProvider<IdFactory> idFactory;
    private final ObjectProvider<GameServerService> gameServerService;
    private final ObjectProvider<BroadcastService> broadcastService;
    private final ObjectProvider<ChatService> chatService;
    private final ObjectProvider<NettyServer> nettyServer;
    private final ObjectProvider<RestartService> restartService;

    public ChatServerRuntime(
        ObjectProvider<IdFactory> idFactory,
        ObjectProvider<GameServerService> gameServerService,
        ObjectProvider<BroadcastService> broadcastService,
        ObjectProvider<ChatService> chatService,
        ObjectProvider<NettyServer> nettyServer,
        ObjectProvider<RestartService> restartService
    ) {
        this.idFactory = idFactory;
        this.gameServerService = gameServerService;
        this.broadcastService = broadcastService;
        this.chatService = chatService;
        this.nettyServer = nettyServer;
        this.restartService = restartService;
    }

    public void start(String[] args) {
        ChatServer.start(args, this);
    }

    @Override
    public IdFactory idFactory() {
        return idFactory.getObject();
    }

    @Override
    public GameServerService gameServerService() {
        return gameServerService.getObject();
    }

    @Override
    public BroadcastService broadcastService() {
        return broadcastService.getObject();
    }

    @Override
    public ChatService chatService() {
        return chatService.getObject();
    }

    @Override
    public NettyServer nettyServer() {
        return nettyServer.getObject();
    }

    @Override
    public RestartService restartService() {
        return restartService.getObject();
    }
}
