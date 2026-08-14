package com.aionemu.chatserver.network.netty;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.network.aion.ClientPacketHandler;
import com.aionemu.chatserver.network.gameserver.GsConnectionFactoryImpl;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerTransport;

/**
 * 聊天服 Netty 网络入口：同时启动客户端接入与游戏服连接监听。
 * Chat-server Netty network entry: starts both client acceptor and game-server listener.
 *
 * @author ATracer
 */
@Slf4j
public class NettyServer {

    private Netty4ChatClientServer netty4ChatClientServer;
    private ServerTransport gameServerTransport;
    private static NettyServer instance;

    /**
     * 获取单例（已废弃，迁移至 Boot 后请使用注入）。
     * Return the singleton (deprecated; prefer injection after Boot migration).
     *
     * @return 单例实例 / Singleton instance
     * @deprecated boot-migration
     */
    @Deprecated(since = "boot-migration")
    public static synchronized NettyServer getInstance() {
        if (instance == null) {
            instance = new NettyServer(new ClientPacketHandler());
        }
        return instance;
    }

    /**
     * 使用指定客户端包处理器获取单例（已废弃）。
     * Return the singleton with a custom client packet handler (deprecated).
     *
     * @param clientPacketHandler 客户端包处理器 / Client packet handler
     * @return 单例实例 / Singleton instance
     * @deprecated boot-migration
     */
    @Deprecated(since = "boot-migration")
    public static synchronized NettyServer getInstance(ClientPacketHandler clientPacketHandler) {
        if (instance == null) {
            instance = new NettyServer(clientPacketHandler);
        }
        return instance;
    }

    /**
     * 若单例已初始化则关闭全部网络资源并清空单例。
     * Shut down all network resources and clear the singleton when initialized.
     */
    public static void shutdownIfInitialized() {
        NettyServer server;
        synchronized (NettyServer.class) {
            server = instance;
            instance = null;
        }
        if (server != null) {
            server.shutdownAll();
        }
    }

    /**
     * 判断单例是否已创建。
     * Whether the singleton has been created.
     *
     * @return 已初始化则为 true / {@code true} if initialized
     */
    static synchronized boolean isInitialized() {
        return instance != null;
    }

    /**
     * 使用默认客户端包处理器构造并初始化。
     * Construct and initialize with the default client packet handler.
     */
    public NettyServer() {
        initialize(new ClientPacketHandler());
    }

    /**
     * 使用指定客户端包处理器构造并初始化。
     * Construct and initialize with the given client packet handler.
     *
     * @param clientPacketHandler 客户端包处理器 / Client packet handler
     */
    public NettyServer(ClientPacketHandler clientPacketHandler) {
        initialize(clientPacketHandler);
    }

    /**
     * 使用默认客户端包处理器初始化监听端口。
     * Initialize listening ports with the default client packet handler.
     */
    public void initialize() {
        initialize(new ClientPacketHandler());
    }

    /**
     * 启动聊天客户端服务端与游戏服传输层。
     * Start the chat-client server and the game-server transport.
     *
     * @param clientPacketHandler 客户端包处理器 / Client packet handler
     */
    private void initialize(ClientPacketHandler clientPacketHandler) {
        netty4ChatClientServer = new Netty4ChatClientServer(Config.CHAT_ADDRESS, clientPacketHandler);
        netty4ChatClientServer.connect();
        String gameHost = Config.GAME_ADDRESS.getAddress().getHostAddress();
        int gamePort = Config.GAME_ADDRESS.getPort();
        gameServerTransport = new com.aionemu.commons.network.NettyServer(
            new NettyServerCfg(gameHost, gamePort, "Gs Connections", new GsConnectionFactoryImpl())
        );
        gameServerTransport.connect();
    }

    /**
     * 关闭聊天客户端服务端与游戏服传输层。
     * Shut down the chat-client server and the game-server transport.
     */
    public void shutdownAll() {
        if (netty4ChatClientServer != null) {
            netty4ChatClientServer.shutdown();
        }
        if (gameServerTransport != null) {
            gameServerTransport.shutdown();
        }
    }
}
