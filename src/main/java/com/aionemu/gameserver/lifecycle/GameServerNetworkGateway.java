package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 服务器网络网关：创建客户端 Netty 传输、初始化 MAC 封禁，并包装登录/聊天对等端为 {@link GameServerNetworkLifecycle.NetworkPeer}。
 * Server-network gateway: creates the client Netty transport, initializes banned-MAC, and wraps login/chat peers as {@link GameServerNetworkLifecycle.NetworkPeer}.
 */
@Component
public class GameServerNetworkGateway {

    /**
     * MAC 封禁管理器提供者（可选）。
     * Optional banned-MAC manager provider.
     */
    private ObjectProvider<BannedMacManager> bannedMacManagerProvider;

    /**
     * 登录服连接提供者（可选）。
     * Optional login-server provider.
     */
    private ObjectProvider<LoginServer> loginServerProvider;

    /**
     * 聊天服连接提供者（可选）。
     * Optional chat-server provider.
     */
    private ObjectProvider<ChatServer> chatServerProvider;

    /**
     * 网络运行时桥接提供者（可选）。
     * Optional network runtime-bridge provider.
     */
    private ObjectProvider<GameServerNetworkRuntimeBridge> runtimeBridgeProvider;

    /**
     * 注入 MAC 封禁管理器提供者。
     * Inject the banned-MAC manager provider.
     *
     * @param bannedMacManagerProvider MAC 封禁管理器提供者 / Banned-MAC manager provider
     */
    @Autowired(required = false)
    void setBannedMacManagerProvider(ObjectProvider<BannedMacManager> bannedMacManagerProvider) {
        this.bannedMacManagerProvider = bannedMacManagerProvider;
    }

    /**
     * 注入登录服连接提供者。
     * Inject the login-server provider.
     *
     * @param loginServerProvider 登录服连接提供者 / Login-server provider
     */
    @Autowired(required = false)
    void setLoginServerProvider(ObjectProvider<LoginServer> loginServerProvider) {
        this.loginServerProvider = loginServerProvider;
    }

    /**
     * 注入聊天服连接提供者。
     * Inject the chat-server provider.
     *
     * @param chatServerProvider 聊天服连接提供者 / Chat-server provider
     */
    @Autowired(required = false)
    void setChatServerProvider(ObjectProvider<ChatServer> chatServerProvider) {
        this.chatServerProvider = chatServerProvider;
    }

    /**
     * 注入网络运行时桥接提供者。
     * Inject the network runtime-bridge provider.
     *
     * @param runtimeBridgeProvider 运行时桥接提供者 / Runtime bridge provider
     */
    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameServerNetworkRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    /**
     * 当前是否为 boot 嵌入式运行模式。
     * Whether the process is running in boot-embedded mode.
     *
     * true when boot-embedded
     */
    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    /**
     * 是否启用聊天服务器连接。
     * Whether chat-server connectivity is enabled.
     *
     * true when chat server is enabled
     */
    public boolean isChatServerEnabled() {
        return GSConfig.ENABLE_CHAT_SERVER;
    }

    /**
     * 创建绑定游戏端口的 Netty 客户端传输。
     * Create the Netty client transport bound to the game port.
     *
     * @return 服务器传输 / Server transport
     */
    public ServerTransport createNettyTransport() {
        return new NettyServer(new NettyServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", runtimeBridge().gameConnectionFactory()));
    }

    /**
     * 初始化 MAC 封禁管理器（触发解析/加载）。
     * Initialize the banned-MAC manager (trigger resolve/load).
     */
    public void initializeBannedMacManager() {
        bannedMacManager();
    }

    /**
     * 包装登录服为网络对等端。
     * Wrap the login server as a network peer.
     *
     * @return 登录服对等端 / Login-server peer
     */
    public GameServerNetworkLifecycle.NetworkPeer loginServer() {
        return new LoginServerPeer(loginServerInstance());
    }

    /**
     * 包装聊天服为网络对等端。
     * Wrap the chat server as a network peer.
     *
     * @return 聊天服对等端 / Chat-server peer
     */
    public GameServerNetworkLifecycle.NetworkPeer chatServer() {
        return new ChatServerPeer(chatServerInstance());
    }

    /**
     * 当前时间毫秒数。
     * Current time in milliseconds.
     *
     * @return 当前毫秒时间戳 / Current epoch millis
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 解析 MAC 封禁管理器。
     * Resolve the banned-MAC manager.
     *
     * @return MAC 封禁管理器 / Banned MAC manager
     */
    private BannedMacManager bannedMacManager() {
        if (bannedMacManagerProvider == null) {
            return runtimeBridge().bannedMacManager();
        }
        return bannedMacManagerProvider.getIfAvailable(() -> runtimeBridge().bannedMacManager());
    }

    /**
     * 解析登录服连接实例。
     * Resolve the login-server instance.
     *
     * Login server
     */
    private LoginServer loginServerInstance() {
        if (loginServerProvider == null) {
            return runtimeBridge().loginServer();
        }
        return loginServerProvider.getIfAvailable(() -> runtimeBridge().loginServer());
    }

    /**
     * 解析聊天服连接实例。
     * Resolve the chat-server instance.
     *
     * Chat server
     */
    private ChatServer chatServerInstance() {
        if (chatServerProvider == null) {
            return runtimeBridge().chatServer();
        }
        return chatServerProvider.getIfAvailable(() -> runtimeBridge().chatServer());
    }

    /**
     * 解析网络运行时桥接。
     * Resolve the network runtime bridge.
     *
     * @return 运行时桥接 / Runtime bridge
     */
    private GameServerNetworkRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameServerNetworkRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameServerNetworkRuntimeBridge::new);
    }

    /**
     * 登录服网络对等端适配器。
     * Adapter peer for the login server.
     */
    @RequiredArgsConstructor
    private static final class LoginServerPeer implements GameServerNetworkLifecycle.NetworkPeer {

        /**
         * 底层登录服连接。
         * Underlying login-server connection.
         */
        private final LoginServer loginServer;

        /**
         * 连接前准备。
         * Prepare before connecting.
         */
        @Override
        public void prepareForConnect() {
            loginServer.prepareForConnect();
        }

        /**
         * 同步连接。
         * Connect synchronously.
         */
        @Override
        public void connect() {
            loginServer.connect();
        }

        /**
         * 异步连接。
         * Connect asynchronously.
         */
        @Override
        public void connectAsync() {
            loginServer.connectAsync();
        }

        /**
         * 断开连接。
         * Disconnect.
         */
        @Override
        public void disconnect() {
            loginServer.gameServerDisconnected();
        }
    }

    /**
     * 聊天服网络对等端适配器。
     * Adapter peer for the chat server.
     */
    @RequiredArgsConstructor
    private static final class ChatServerPeer implements GameServerNetworkLifecycle.NetworkPeer {

        /**
         * 底层聊天服连接。
         * Underlying chat-server connection.
         */
        private final ChatServer chatServer;

        /**
         * 连接前准备。
         * Prepare before connecting.
         */
        @Override
        public void prepareForConnect() {
            chatServer.prepareForConnect();
        }

        /**
         * 同步连接。
         * Connect synchronously.
         */
        @Override
        public void connect() {
            chatServer.connect();
        }

        /**
         * 异步连接。
         * Connect asynchronously.
         */
        @Override
        public void connectAsync() {
            chatServer.connectAsync();
        }

        /**
         * 断开连接。
         * Disconnect.
         */
        @Override
        public void disconnect() {
            chatServer.gameServerDisconnected();
        }
    }
}
