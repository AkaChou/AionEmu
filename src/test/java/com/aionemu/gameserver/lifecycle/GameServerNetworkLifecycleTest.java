package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
        GameServerNetworkLifecycle lifecycle = new RecordingGameServerNetworkLifecycle(events, false, true, loginServer, chatServer);

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
    void startInitializesNettyTransportAndPreparesPeers() {
        List<String> events = new ArrayList<>();
        RecordingNetworkPeer loginServer = new RecordingNetworkPeer("login", events);
        RecordingNetworkPeer chatServer = new RecordingNetworkPeer("chat", events);
        GameServerNetworkLifecycle lifecycle = new RecordingGameServerNetworkLifecycle(events, false, true, loginServer, chatServer);

        lifecycle.start(new GameServer());

        assertEquals(
            List.of(
                "bannedMac:init",
                "login:prepareForConnect",
                "chat:prepareForConnect",
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
        GameServerNetworkLifecycle lifecycle = new RecordingGameServerNetworkLifecycle(
            events,
            false,
            false,
            new RecordingNetworkPeer("login", events),
            new RecordingNetworkPeer("chat", events)
        );

        lifecycle.start(server);

        Field networkLifecycle = GameServer.class.getDeclaredField("networkLifecycle");
        networkLifecycle.setAccessible(true);
        assertSame(lifecycle, networkLifecycle.get(server));
    }

    @Test
    void serverNetworkLifecycleBridgesNetworkServicesThroughSpringProviders() {
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkLifecycle.class, "bannedMacManagerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkLifecycle.class, "loginServerProvider"));
        assertEquals(ObjectProvider.class, fieldType(GameServerNetworkLifecycle.class, "chatServerProvider"));
    }

    private static Class<?> fieldType(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name).getType();
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Missing field: " + name, e);
        }
    }

    private static final class RecordingGameServerNetworkLifecycle extends GameServerNetworkLifecycle {

        private final List<String> events;
        private final boolean bootEmbedded;
        private final boolean chatServerEnabled;
        private final RecordingNetworkPeer loginServer;
        private final RecordingNetworkPeer chatServer;

        private RecordingGameServerNetworkLifecycle(
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
        protected boolean isBootEmbedded() {
            return bootEmbedded;
        }

        @Override
        protected boolean isChatServerEnabled() {
            return chatServerEnabled;
        }

        @Override
        protected ServerTransport createNettyTransport() {
            return new RecordingTransport("netty", events);
        }

        @Override
        protected void initializeBannedMacManager() {
            events.add("bannedMac:init");
        }

        @Override
        protected NetworkPeer loginServer() {
            return loginServer;
        }

        @Override
        protected NetworkPeer chatServer() {
            return chatServer;
        }
    }

	private record RecordingNetworkPeer(String name,
										List<String> events) implements GameServerNetworkLifecycle.NetworkPeer {

		@Override
		public void prepareForConnect() {
			events.add(name + ":prepareForConnect");
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

    private record RecordingTransport(String name, List<String> events) implements ServerTransport {

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
}
