package com.aionemu.chatserver;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatCoreServices;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;

final class ChatServerLegacyDependencies implements ChatServerDependencies {

    @Override
    public IdFactory idFactory() {
        return ChatCoreServices.idFactory();
    }

    @Override
    public GameServerService gameServerService() {
        return ChatCoreServices.gameServerService();
    }

    @Override
    public BroadcastService broadcastService() {
        return ChatCoreServices.broadcastService();
    }

    @Override
    public ChatService chatService() {
        return ChatCoreServices.chatService();
    }

    @Override
    public NettyServer nettyServer() {
        return NettyServer.getInstance();
    }

    @Override
    public RestartService restartService() {
        return RestartService.getInstance();
    }
}
