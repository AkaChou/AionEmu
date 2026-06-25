package com.aionemu.boot.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.boot.config.AionServicesProperties;
import org.junit.jupiter.api.Test;

class AionTransportBoundaryTest {

    @Test
    void preparesAndStopsNettyTransportByDefault() {
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(new AionServicesProperties(), netty);

        boundary.prepare();
        boundary.destroy();

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
    void unsupportedTransportModeIsRejected() {
        AionServicesProperties properties = new AionServicesProperties();
        properties.getTransport().setMode("LEGACY_NIO");
        RecordingNettyTransport netty = new RecordingNettyTransport();
        AionTransportBoundary boundary = new AionTransportBoundary(properties, netty);

        assertThrows(IllegalArgumentException.class, boundary::prepare);

        assertEquals(0, netty.starts);
        assertEquals(0, netty.stops);
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
