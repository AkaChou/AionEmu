package com.aionemu.gameserver.lifecycle;

import com.aionemu.commons.network.ServerTransport;
import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameServerNetworkLifecycle {

    private final GameServerNetworkGateway networkGateway;
    private ServerTransport gameClientTransport;

    public void start(GameServer server) {
        server.attachNetworkLifecycle(this);
        start();
    }

    public void start() {
        log.info("Network Config - Bind: {}, Port: {}, Threads: {}", NetworkConfig.GAME_BIND_ADDRESS, NetworkConfig.GAME_PORT, NetworkConfig.NIO_READ_WRITE_THREADS);

        gameClientTransport = networkGateway.createNettyTransport();

        networkGateway.initializeBannedMacManager();

        NetworkPeer loginServer = networkGateway.loginServer();
        NetworkPeer chatServer = networkGateway.chatServer();

        loginServer.prepareForConnect();
        chatServer.prepareForConnect();

        long transportStart = networkGateway.currentTimeMillis();
        gameClientTransport.connect();
        long transportTime = networkGateway.currentTimeMillis() - transportStart;
        log.info("Netty server transport started in {} ms", transportTime);

        System.out.println("");

        connectPeer("Login Server", loginServer);

        if (networkGateway.isChatServerEnabled()) {
            connectPeer("Chat Server", chatServer);
        } else {
            log.info("Chat Server is disabled by configuration");
        }
    }

    public void stop() {
        try {
            networkGateway.loginServer().disconnect();
        } catch (Exception e) {
            log.warn("Failed to disconnect from Login Server cleanly.", e);
        }
        try {
            networkGateway.chatServer().disconnect();
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
        gameClientTransport = null;
    }

    private void connectPeer(String peerName, NetworkPeer peer) {
        boolean bootEmbedded = networkGateway.isBootEmbedded();
        long start = networkGateway.currentTimeMillis();
        if (bootEmbedded) {
            peer.connectAsync();
        } else {
            peer.connect();
        }
        long time = networkGateway.currentTimeMillis() - start;
        log.info("{} {} in {} ms", peerName, bootEmbedded ? "connection scheduled" : "connected", time);
    }

    interface NetworkPeer {

        void prepareForConnect();

        void connect();

        void connectAsync();

        void disconnect();
    }
}
