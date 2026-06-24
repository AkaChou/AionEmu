package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class GameServerNetworkGateway {

    public boolean isNettyTransportEnabled() {
        return Boolean.getBoolean("aion.transport.netty");
    }

    public boolean isBootEmbedded() {
        return AionRuntimeMode.isBootEmbedded();
    }

    public boolean isChatServerEnabled() {
        return GSConfig.ENABLE_CHAT_SERVER;
    }

    public ServerTransport createNettyTransport() {
        return new NettyServer(new NettyServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl()));
    }

    public NioServer createNioServer() {
        return new NioServer(NetworkConfig.NIO_READ_WRITE_THREADS, new ServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl()));
    }

    public void initializeBannedMacManager() {
        BannedMacManager.getInstance();
    }

    public GameServerNetworkLifecycle.NetworkPeer loginServer() {
        return new LoginServerPeer(LoginServer.getInstance());
    }

    public GameServerNetworkLifecycle.NetworkPeer chatServer() {
        return new ChatServerPeer(ChatServer.getInstance());
    }

    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    @RequiredArgsConstructor
    private static final class LoginServerPeer implements GameServerNetworkLifecycle.NetworkPeer {

        private final LoginServer loginServer;

        @Override
        public void setNioServer(NioServer nioServer) {
            loginServer.setNioServer(nioServer);
        }

        @Override
        public void connect() {
            loginServer.connect();
        }

        @Override
        public void connectAsync() {
            loginServer.connectAsync();
        }

        @Override
        public void disconnect() {
            loginServer.gameServerDisconnected();
        }
    }

    @RequiredArgsConstructor
    private static final class ChatServerPeer implements GameServerNetworkLifecycle.NetworkPeer {

        private final ChatServer chatServer;

        @Override
        public void setNioServer(NioServer nioServer) {
            chatServer.setNioServer(nioServer);
        }

        @Override
        public void connect() {
            chatServer.connect();
        }

        @Override
        public void connectAsync() {
            chatServer.connectAsync();
        }

        @Override
        public void disconnect() {
            chatServer.gameServerDisconnected();
        }
    }
}
