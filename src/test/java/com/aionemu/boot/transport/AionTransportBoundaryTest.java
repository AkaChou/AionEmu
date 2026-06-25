package com.aionemu.boot.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.boot.config.AionServicesProperties;
import com.aionemu.boot.config.AionServicesProperties.TransportMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AionTransportBoundaryTest {

    @AfterEach
    void clearTransportFlag() {
        System.clearProperty("aion.transport.netty");
    }

    @Test
    void preparesAndStopsNettyTransportByDefault() {
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(new AionServicesProperties(), netty);

        boundary.prepare();
        boundary.destroy();

        assertEquals("true", System.getProperty("aion.transport.netty"));
        assertEquals(1, netty.starts);
        assertEquals(1, netty.stops);
    }

    @Test
    void stopsActiveTransportOnlyOnceWhenDestroyedRepeatedly() {
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(new AionServicesProperties(), netty);

        boundary.prepare();
        boundary.destroy();
        boundary.destroy();

        assertEquals(1, netty.stops);
    }

    @Test
    void legacyNioModeIsAcceptedAsNettyAlias() {
        AionServicesProperties properties = new AionServicesProperties();
        properties.getTransport().setMode("LEGACY_NIO");
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(properties, netty);

        boundary.prepare();
        boundary.destroy();

        assertEquals("true", System.getProperty("aion.transport.netty"));
        assertEquals(1, netty.starts);
        assertEquals(1, netty.stops);
    }

    private static final class RecordingNettyTransport extends NettyTransportLifecycle {
        private int starts;
        private int stops;

        @Override
        public synchronized void start() {
            starts++;
        }

        @Override
        public synchronized void stop() {
            stops++;
        }
    }
}
