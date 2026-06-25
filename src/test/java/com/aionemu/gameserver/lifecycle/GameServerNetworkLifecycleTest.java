package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.gameserver.GameServer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class GameServerNetworkLifecycleTest {

    @Test
    void stopDisconnectsPeersAndShutsDownStartedTransport() {
        List<String> events = new ArrayList<>();
        RecordingNetworkPeer loginServer = new RecordingNetworkPeer("login", events);
        RecordingNetworkPeer chatServer = new RecordingNetworkPeer("chat", events);
        GameServerNetworkLifecycle lifecycle = new GameServerNetworkLifecycle(
            new RecordingGameServerNetworkGateway(events, false, true, loginServer, chatServer)
        );

        lifecycle.start(new GameServer());
        events.clear();
        lifecycle.stop();

        assertEquals(
            List.of(
                "login:disconnect",
                "chat:disconnect",
                "transport:shutdown:netty"
            ),
            events
        );
    }

    @Test
    void startInitializesNettyTransportAndClearsLegacyPeerDispatcher() {
        List<String> events = new ArrayList<>();
        RecordingNetworkPeer loginServer = new RecordingNetworkPeer("login", events);
        RecordingNetworkPeer chatServer = new RecordingNetworkPeer("chat", events);
        GameServerNetworkLifecycle lifecycle = new GameServerNetworkLifecycle(
            new RecordingGameServerNetworkGateway(events, false, true, loginServer, chatServer)
        );

        lifecycle.start(new GameServer());

        assertNull(loginServer.nioServer);
        assertNull(chatServer.nioServer);
        assertEquals(
            List.of(
                "bannedMac:init",
                "login:setNioServer",
                "chat:setNioServer",
                "transport:connect:netty",
                "login:connect",
                "chat:connect"
            ),
            events
        );
    }

    @Test
    void startAttachesLifecycleToServerForLegacyStop() throws Exception {
        List<String> events = new ArrayList<>();
        GameServer server = new GameServer();
        GameServerNetworkLifecycle lifecycle = new GameServerNetworkLifecycle(
            new RecordingGameServerNetworkGateway(
                events,
                false,
                false,
                new RecordingNetworkPeer("login", events),
                new RecordingNetworkPeer("chat", events)
            )
        );

        lifecycle.start(server);

        Field networkLifecycle = GameServer.class.getDeclaredField("networkLifecycle");
        networkLifecycle.setAccessible(true);
        assertSame(lifecycle, networkLifecycle.get(server));
    }

    @Test
    void usesServerNetworkGatewayCollaborator() {
        assertEquals(GameServerNetworkGateway.class, fieldType("networkGateway"));
    }

    @Test
    void networkGatewayBridgesNetworkServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkGateway.class, "bannedMacManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkGateway.class, "loginServerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkGateway.class, "chatServerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkGateway.class, "runtimeBridgeProvider"));
    }

    private static Class<?> fieldType(String name) {
        return fieldType(GameServerNetworkLifecycle.class, name);
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameServerNetworkGateway extends GameServerNetworkGateway {

        private final List<String> events;
        private final boolean bootEmbedded;
        private final boolean chatServerEnabled;
        private final RecordingNetworkPeer loginServer;
        private final RecordingNetworkPeer chatServer;
        private final IncrementingClock clock = new IncrementingClock();

        private RecordingGameServerNetworkGateway(
            List<String> events,
            boolean bootEmbedded,
            boolean chatServerEnabled,
            RecordingNetworkPeer loginServer,
            RecordingNetworkPeer chatServer
        ) {
            this.events = events;
            this.bootEmbedded = bootEmbedded;
            this.chatServerEnabled = chatServerEnabled;
            this.loginServer = loginServer;
            this.chatServer = chatServer;
        }

        @Override
        public boolean isBootEmbedded() {
            return bootEmbedded;
        }

        @Override
        public boolean isChatServerEnabled() {
            return chatServerEnabled;
        }

        @Override
        public ServerTransport createNettyTransport() {
            return new RecordingTransport("netty", events);
        }

        @Override
        public void initializeBannedMacManager() {
            events.add("bannedMac:init");
        }

        @Override
        public GameServerNetworkLifecycle.NetworkPeer loginServer() {
            return loginServer;
        }

        @Override
        public GameServerNetworkLifecycle.NetworkPeer chatServer() {
            return chatServer;
        }

        @Override
        public long currentTimeMillis() {
            return clock.getAsLong();
        }
    }

    private static final class RecordingNetworkPeer implements GameServerNetworkLifecycle.NetworkPeer {

        private final String name;
        private final List<String> events;
        private NioServer nioServer;

        private RecordingNetworkPeer(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public void setNioServer(NioServer nioServer) {
            this.nioServer = nioServer;
            events.add(name + ":setNioServer");
        }

        @Override
        public void connect() {
            events.add(name + ":connect");
        }

        @Override
        public void connectAsync() {
            events.add(name + ":connectAsync");
        }

        @Override
        public void disconnect() {
            events.add(name + ":disconnect");
        }
    }

    private static final class RecordingTransport implements ServerTransport {

        private final String name;
        private final List<String> events;

        private RecordingTransport(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public void connect() {
            events.add("transport:connect:" + name);
        }

        @Override
        public void shutdown() {
            events.add("transport:shutdown:" + name);
        }

        @Override
        public int getActiveConnections() {
            return 0;
        }
    }

    private static final class IncrementingClock {

        private long value;

        private long getAsLong() {
            return value++;
        }
    }
}
