package com.aionemu.loginserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.network.ServerTransport;
import com.aionemu.loginserver.service.LoginNetworkServices;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NetConnectorTest {

    @AfterEach
    void resetTransportFactory() {
        NetConnector.resetTransportFactory();
    }

    @Test
    void shutdownIfInitializedDoesNotCreateTransport() {
        AtomicInteger createdTransports = new AtomicInteger();
        NetConnector.useTransportFactory(() -> {
            createdTransports.incrementAndGet();
            return new RecordingTransport();
        });

        assertFalse(NetConnector.shutdownIfInitialized());
        assertEquals(0, createdTransports.get());
    }

    @Test
    void shutdownClearsTransportForNextLifecycle() {
        AtomicInteger shutdowns = new AtomicInteger();
        NetConnector.useTransportFactory(() -> new RecordingTransport(shutdowns));

        ServerTransport firstTransport = NetConnector.currentTransport();
        assertTrue(NetConnector.shutdownIfInitialized());
        ServerTransport secondTransport = NetConnector.currentTransport();

        assertEquals(1, shutdowns.get());
        assertNotSame(firstTransport, secondTransport);
    }

    @Test
    void loginNetworkFallbackUsesCurrentTransportAfterShutdown() {
        AtomicInteger shutdowns = new AtomicInteger();
        NetConnector.useTransportFactory(() -> new RecordingTransport(shutdowns));
        new LoginNetworkServices(null);

        try {
            ServerTransport firstTransport = LoginNetworkServices.serverTransport();
            assertTrue(NetConnector.shutdownIfInitialized());
            ServerTransport secondTransport = LoginNetworkServices.serverTransport();

            assertEquals(1, shutdowns.get());
            assertNotSame(firstTransport, secondTransport);
        } finally {
            new LoginNetworkServices(null).destroy();
        }
    }

    private static final class RecordingTransport implements ServerTransport {
        private final AtomicInteger shutdowns;

        private RecordingTransport() {
            this(new AtomicInteger());
        }

        private RecordingTransport(AtomicInteger shutdowns) {
            this.shutdowns = shutdowns;
        }

        @Override
        public void connect() {
        }

        @Override
        public void shutdown() {
            shutdowns.incrementAndGet();
        }

        @Override
        public int getActiveConnections() {
            return 0;
        }
    }
}
