package com.aionemu.chatserver;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatCoreServices;
import com.aionemu.chatserver.service.ChatNettyServers;
import com.aionemu.chatserver.service.ChatRestartServices;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;

/**
 * 基于遗留静态服务定位器的 {@link ChatServerDependencies} 实现。
 * {@link ChatServerDependencies} implementation backed by legacy static service locators.
 */
final class ChatServerLegacyDependencies implements ChatServerDependencies {

    /**
     * {@inheritDoc}
     */
    @Override
    public IdFactory idFactory() {
        return ChatCoreServices.idFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GameServerService gameServerService() {
        return ChatCoreServices.gameServerService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BroadcastService broadcastService() {
        return ChatCoreServices.broadcastService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatService chatService() {
        return ChatCoreServices.chatService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NettyServer nettyServer() {
        return ChatNettyServers.nettyServer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestartService restartService() {
        return ChatRestartServices.restartService();
    }
}
