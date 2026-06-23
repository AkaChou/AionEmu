package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.network.NettyServer;
import com.aionemu.commons.network.NettyServerCfg;
import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerCfg;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.commons.utils.AionRuntimeMode;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.BannedMacManager;
import com.aionemu.gameserver.network.aion.GameConnectionFactoryImpl;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameServerNetworkLifecycle {

    private final BooleanSupplier nettyTransportEnabled;
    private final BooleanSupplier bootEmbedded;
    private final BooleanSupplier chatServerEnabled;
    private final Supplier<ServerTransport> nettyTransportSupplier;
    private final Supplier<NioServer> nioServerSupplier;
    private final Runnable bannedMacInitializer;
    private final Supplier<NetworkPeer> loginServerSupplier;
    private final Supplier<NetworkPeer> chatServerSupplier;
    private final LongSupplier currentTimeMillis;

    private NioServer nioServer;
    private ServerTransport gameClientTransport;

    public GameServerNetworkLifecycle() {
        this(
            () -> Boolean.getBoolean("aion.transport.netty"),
            AionRuntimeMode::isBootEmbedded,
            () -> GSConfig.ENABLE_CHAT_SERVER,
            GameServerNetworkLifecycle::createNettyTransport,
            GameServerNetworkLifecycle::createNioServer,
            BannedMacManager::getInstance,
            () -> new LoginServerPeer(LoginServer.getInstance()),
            () -> new ChatServerPeer(ChatServer.getInstance()),
            System::currentTimeMillis
        );
    }

    GameServerNetworkLifecycle(
        BooleanSupplier nettyTransportEnabled,
        BooleanSupplier bootEmbedded,
        BooleanSupplier chatServerEnabled,
        Supplier<ServerTransport> nettyTransportSupplier,
        Supplier<NioServer> nioServerSupplier,
        Runnable bannedMacInitializer,
        Supplier<NetworkPeer> loginServerSupplier,
        Supplier<NetworkPeer> chatServerSupplier,
        LongSupplier currentTimeMillis
    ) {
        this.nettyTransportEnabled = nettyTransportEnabled;
        this.bootEmbedded = bootEmbedded;
        this.chatServerEnabled = chatServerEnabled;
        this.nettyTransportSupplier = nettyTransportSupplier;
        this.nioServerSupplier = nioServerSupplier;
        this.bannedMacInitializer = bannedMacInitializer;
        this.loginServerSupplier = loginServerSupplier;
        this.chatServerSupplier = chatServerSupplier;
        this.currentTimeMillis = currentTimeMillis;
    }

    public void start(GameServer server) {
        server.attachNetworkLifecycle(this);
        start();
    }

    public void start() {
        log.info("Network Config - Bind: {}, Port: {}, Threads: {}", NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, NetworkConfig.NIO_READ_WRITE_THREADS);

        boolean netty = nettyTransportEnabled.getAsBoolean();
        if (netty) {
            nioServer = null;
            gameClientTransport = nettyTransportSupplier.get();
        } else {
            nioServer = nioServerSupplier.get();
            gameClientTransport = nioServer;
        }

        bannedMacInitializer.run();

        NetworkPeer loginServer = loginServerSupplier.get();
        NetworkPeer chatServer = chatServerSupplier.get();

        loginServer.setNioServer(nioServer);
        chatServer.setNioServer(nioServer);

        long transportStart = currentTimeMillis.getAsLong();
        gameClientTransport.connect();
        long transportTime = currentTimeMillis.getAsLong() - transportStart;
        log.info("{} server transport started in {} ms", netty ? "Netty" : "NIO", transportTime);

        System.out.println("");

        connectPeer("Login Server", loginServer);

        if (chatServerEnabled.getAsBoolean()) {
            connectPeer("Chat Server", chatServer);
        } else {
            log.info("Chat Server is disabled by configuration");
        }
    }

    public void stop() {
        try {
            loginServerSupplier.get().disconnect();
        } catch (Exception e) {
            log.warn("Failed to disconnect from Login Server cleanly.", e);
        }
        try {
            chatServerSupplier.get().disconnect();
        } catch (Exception e) {
            log.warn("Failed to disconnect from Chat Server cleanly.", e);
        }
        try {
            if (gameClientTransport != null) {
                gameClientTransport.shutdown();
            }
        } catch (Exception e) {
            log.warn("Failed to stop game client transport cleanly.", e);
        }
        try {
            if (nioServer != null && nioServer != gameClientTransport) {
                nioServer.shutdown();
            }
        } catch (Exception e) {
            log.warn("Failed to stop game connector dispatcher cleanly.", e);
        }
        gameClientTransport = null;
        nioServer = null;
    }

    private void connectPeer(String peerName, NetworkPeer peer) {
        long start = currentTimeMillis.getAsLong();
        if (bootEmbedded.getAsBoolean()) {
            peer.connectAsync();
        } else {
            peer.connect();
        }
        long time = currentTimeMillis.getAsLong() - start;
        log.info("{} {} in {} ms", peerName, bootEmbedded.getAsBoolean() ? "connection scheduled" : "connected", time);
    }

    private static ServerTransport createNettyTransport() {
        return new NettyServer(new NettyServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl()));
    }

    private static NioServer createNioServer() {
        return new NioServer(NetworkConfig.NIO_READ_WRITE_THREADS, new ServerCfg(NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, "Game Connections", new GameConnectionFactoryImpl()));
    }

    interface NetworkPeer {

        void setNioServer(NioServer nioServer);

        void connect();

        void connectAsync();

        void disconnect();
    }

    @RequiredArgsConstructor
    private static final class LoginServerPeer implements NetworkPeer {

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
    private static final class ChatServerPeer implements NetworkPeer {

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
