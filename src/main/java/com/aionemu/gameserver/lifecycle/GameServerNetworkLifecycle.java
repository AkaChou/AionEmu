package com.aionemu.gameserver.lifecycle;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 服务器网络生命周期：启动客户端传输、连接登录/聊天对等端，并在关停时有序断开。
 * Server-network lifecycle: starts the client transport, connects login/chat peers, and disconnects orderly on stop.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerNetworkLifecycle {

    /**
     * 服务器网络网关。
     * Server-network gateway.
     */
    private final GameServerNetworkGateway networkGateway;

    /**
     * 游戏客户端传输；未启动为 null。
     * Game-client transport; null before start.
     */
    private ServerTransport gameClientTransport;

    /**
     * 将本生命周期挂到 {@link GameServer} 后启动网络。
     * Attach this lifecycle to {@link GameServer}, then start networking.
     *
     * @param server 游戏服实例 / Game-server instance
     */
    public void start(GameServer server) {
        server.attachNetworkLifecycle(this);
        start();
    }

    /**
     * 启动客户端 Netty 传输，初始化 MAC 封禁，并连接登录/聊天对等端。
     * Start the client Netty transport, initialize banned-MAC, and connect login/chat peers.
     */
    public void start() {
        log.info(I18n.get("console.startup.network_config", NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, NetworkConfig.NIO_READ_WRITE_THREADS));

        gameClientTransport = networkGateway.createNettyTransport();

        networkGateway.initializeBannedMacManager();

        NetworkPeer loginServer = networkGateway.loginServer();
        NetworkPeer chatServer = networkGateway.chatServer();

        loginServer.prepareForConnect();
        chatServer.prepareForConnect();

        long transportStart = networkGateway.currentTimeMillis();
        gameClientTransport.connect();
        long transportTime = networkGateway.currentTimeMillis() - transportStart;
        log.info(I18n.get("console.startup.netty_started", transportTime));

        connectPeer("Login Server", loginServer);

        if (networkGateway.isChatServerEnabled()) {
            connectPeer("Chat Server", chatServer);
        } else {
            log.info(I18n.get("console.startup.chat_disabled"));
        }
    }

    /**
     * 停止网络：断开登录/聊天对等端并关闭客户端传输。
     * Stop networking: disconnect login/chat peers and shut down the client transport.
     */
    public void stop() {
        try {
            networkGateway.loginServer().disconnect();
        } catch (Exception e) {
            log.warn(I18n.get("shutdown.login_stop_failed"), e);
        }
        try {
            networkGateway.chatServer().disconnect();
        } catch (Exception e) {
            log.warn(I18n.get("shutdown.chat_stop_failed"), e);
        }
        try {
            if (gameClientTransport != null) {
                gameClientTransport.shutdown();
            }
        } catch (Exception e) {
            log.warn(I18n.get("shutdown.client_transport_stop_failed"), e);
        }
        gameClientTransport = null;
    }

    /**
     * 连接单个对等端：boot 嵌入式用异步，否则同步，并记录耗时。
     * Connect a single peer: async when boot-embedded, otherwise sync; log timing.
     *
     * @param peerName 对等端名称（用于日志） / Peer name for logging
     * @param peer 网络对等端 / Network peer
     */
    private void connectPeer(String peerName, NetworkPeer peer) {
        boolean bootEmbedded = networkGateway.isBootEmbedded();
        long start = networkGateway.currentTimeMillis();
        if (bootEmbedded) {
            peer.connectAsync();
        } else {
            peer.connect();
        }
        long time = networkGateway.currentTimeMillis() - start;
        log.info(I18n.get("console.startup.peer_timing", peerName, bootEmbedded ? I18n.get("console.startup.peer_scheduled") : I18n.get("console.startup.peer_connected"), time));
    }

    /**
     * 登录/聊天等网络对等端的统一抽象。
     * Unified abstraction for login/chat network peers.
     */
    interface NetworkPeer {

        /**
         * 连接前准备（配置/状态复位等）。
         * Prepare before connecting (config/state reset, etc.).
         */
        void prepareForConnect();

        /**
         * 同步连接。
         * Connect synchronously.
         */
        void connect();

        /**
         * 异步连接（嵌入式启动场景）。
         * Connect asynchronously (boot-embedded scenario).
         */
        void connectAsync();

        /**
         * 断开连接。
         * Disconnect.
         */
        void disconnect();
    }
}
