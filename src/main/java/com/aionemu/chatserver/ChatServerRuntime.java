package com.aionemu.chatserver;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring 托管的聊天服运行时，实现 {@link ChatServerDependencies} 并驱动启动序列。
 * Spring-managed chat-server runtime implementing {@link ChatServerDependencies} and driving startup.
 */
@Component
@Lazy
@ConditionalOnProperty(prefix = "aion.services.chat", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class ChatServerRuntime implements ChatServerDependencies {

    private final ObjectProvider<IdFactory> idFactory;
    private final ObjectProvider<GameServerService> gameServerService;
    private final ObjectProvider<BroadcastService> broadcastService;
    private final ObjectProvider<ChatService> chatService;
    private final ObjectProvider<NettyServer> nettyServer;
    private final ObjectProvider<RestartService> restartService;
    private final ObjectProvider<ChatServerStartupBridge> startupBridge;

    /**
     * 使用本运行时依赖启动聊天服。
     * Start the chat server with this runtime as dependencies.
     *
     * @param args 启动参数 / Startup arguments
     */
    public void start(String[] args) {
        ChatServerStartupSequence.start(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatServerStartupBridge startupBridge() {
        return startupBridge.getIfAvailable(ChatServerStartupBridge::new);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IdFactory idFactory() {
        return idFactory.getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GameServerService gameServerService() {
        return gameServerService.getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BroadcastService broadcastService() {
        return broadcastService.getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatService chatService() {
        return chatService.getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NettyServer nettyServer() {
        return nettyServer.getObject();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestartService restartService() {
        return restartService.getObject();
    }
}
