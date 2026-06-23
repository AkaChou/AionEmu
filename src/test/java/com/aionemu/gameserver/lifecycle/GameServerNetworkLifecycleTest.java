package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.commons.network.NioServer;
import com.aionemu.commons.network.ServerTransport;
import com.aionemu.gameserver.GameServer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameServerNetworkLifecycleTest {

    @Test
    void stopDisconnectsPeersAndShutsDownStartedTransport() {
        List<String> events = new ArrayList<>();
        RecordingNioServer nioServer = new RecordingNioServer(events);
        RecordingNetworkPeer loginServer = new RecordingNetworkPeer("login", events);
        RecordingNetworkPeer chatServer = new RecordingNetworkPeer("chat", events);
        GameServerNetworkLifecycle lifecycle = new GameServerNetworkLifecycle(
            () -> true,
            () -> false,
            () -> true,
            () -> new RecordingTransport("netty", events),
            () -> nioServer,
            () -> events.add("bannedMac:init"),
            () -> loginServer,
            () -> chatServer,
            new IncrementingClock()
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
    void startInitializesNioTransportAndConnectsPeersInLegacyOrder() {
        List<String> events = new ArrayList<>();
        RecordingNioServer nioServer = new RecordingNioServer(events);
        RecordingNetworkPeer loginServer = new RecordingNetworkPeer("login", events);
        RecordingNetworkPeer chatServer = new RecordingNetworkPeer("chat", events);
        GameServerNetworkLifecycle lifecycle = new GameServerNetworkLifecycle(
            () -> false,
            () -> false,
            () -> true,
            () -> new RecordingTransport("netty", events),
            () -> nioServer,
            () -> events.add("bannedMac:init"),
            () -> loginServer,
            () -> chatServer,
            new IncrementingClock()
        );

        lifecycle.start(new GameServer());

        assertSame(nioServer, loginServer.nioServer);
        assertSame(nioServer, chatServer.nioServer);
        assertEquals(
            List.of(
                "bannedMac:init",
                "login:setNioServer",
                "chat:setNioServer",
                "transport:connect:nio",
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
            () -> true,
            () -> false,
            () -> false,
            () -> new RecordingTransport("netty", events),
            () -> new RecordingNioServer(events),
            () -> events.add("bannedMac:init"),
            () -> new RecordingNetworkPeer("login", events),
            () -> new RecordingNetworkPeer("chat", events),
            new IncrementingClock()
        );

        lifecycle.start(server);

        Field networkLifecycle = GameServer.class.getDeclaredField("networkLifecycle");
        networkLifecycle.setAccessible(true);
        assertSame(lifecycle, networkLifecycle.get(server));
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

    private static final class RecordingNioServer extends NioServer {

        private final List<String> events;

        private RecordingNioServer(List<String> events) {
            super(1);
            this.events = events;
        }

        @Override
        public void connect() {
            events.add("transport:connect:nio");
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

    private static final class IncrementingClock implements java.util.function.LongSupplier {

        private long value;

        @Override
        public long getAsLong() {
            return value++;
        }
    }
}
