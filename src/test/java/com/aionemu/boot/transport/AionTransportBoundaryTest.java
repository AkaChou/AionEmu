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
        RecordingLegacyTransport legacy = new RecordingLegacyTransport();
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(new AionServicesProperties(), legacy, netty);

        boundary.prepare();
        boundary.destroy();

        assertEquals("true", System.getProperty("aion.transport.netty"));
        assertEquals(0, legacy.starts);
        assertEquals(0, legacy.stops);
        assertEquals(1, netty.starts);
        assertEquals(1, netty.stops);
    }

    @Test
    void stopsActiveTransportOnlyOnceWhenDestroyedRepeatedly() {
        RecordingLegacyTransport legacy = new RecordingLegacyTransport();
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(new AionServicesProperties(), legacy, netty);

        boundary.prepare();
        boundary.destroy();
        boundary.destroy();

        assertEquals(0, legacy.stops);
        assertEquals(1, netty.stops);
    }

    @Test
    void preparesAndStopsLegacyTransportWhenConfigured() {
        AionServicesProperties properties = new AionServicesProperties();
        properties.getTransport().setMode(TransportMode.LEGACY_NIO);
        RecordingLegacyTransport legacy = new RecordingLegacyTransport();
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(properties, legacy, netty);

        boundary.prepare();
        boundary.destroy();

        assertEquals("false", System.getProperty("aion.transport.netty"));
        assertEquals(1, legacy.starts);
        assertEquals(1, legacy.stops);
        assertEquals(0, netty.starts);
        assertEquals(0, netty.stops);
    }

    private static final class RecordingLegacyTransport extends LegacyNioTransportLifecycle {
        private int starts;
        private int stops;

        @Override
        public void start() {
            starts++;
        }

        @Override
        public void stop() {
            stops++;
        }
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
