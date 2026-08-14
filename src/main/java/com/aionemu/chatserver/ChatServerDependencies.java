package com.aionemu.chatserver;

import com.aionemu.chatserver.network.netty.NettyServer;
import com.aionemu.chatserver.service.BroadcastService;
import com.aionemu.chatserver.service.ChatService;
import com.aionemu.chatserver.service.GameServerService;
import com.aionemu.chatserver.service.RestartService;
import com.aionemu.chatserver.utils.IdFactory;

/**
 * 聊天服务器启动与运行时依赖契约。
 * Dependency contract for chat-server startup and runtime.
 */
interface ChatServerDependencies {

    /**
     * 提供启动桥接；默认新建空桥接实例。
     * Provide the startup bridge; defaults to a new empty bridge instance.
     *
     * @return 启动桥 / Startup bridge
     */
    default ChatServerStartupBridge startupBridge() {
        return new ChatServerStartupBridge();
    }

    /**
     * ID 工厂。
     * ID factory.
     *
     * @return {@link IdFactory}
     */
    IdFactory idFactory();

    /**
     * 游戏服对接服务。
     * Game-server integration service.
     *
     * @return {@link GameServerService}
     */
    GameServerService gameServerService();

    /**
     * 广播服务。
     * Broadcast service.
     *
     * @return {@link BroadcastService}
     */
    BroadcastService broadcastService();

    /**
     * 聊天核心服务。
     * Chat core service.
     *
     * @return {@link ChatService}
     */
    ChatService chatService();

    /**
     * Netty 网络服务器。
     * Netty network server.
     *
     * @return {@link NettyServer}
     */
    NettyServer nettyServer();

    /**
     * 重启调度服务。
     * Restart scheduling service.
     *
     * @return {@link RestartService}
     */
    RestartService restartService();
}
