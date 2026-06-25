package com.aionemu.chatserver;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;

interface ChatServerDependencies {

    default ChatServerStartupBridge startupBridge() {
        return new ChatServerStartupBridge();
    }

    IdFactory idFactory();

    GameServerService gameServerService();

    BroadcastService broadcastService();

    ChatService chatService();

    NettyServer nettyServer();

    RestartService restartService();
}
