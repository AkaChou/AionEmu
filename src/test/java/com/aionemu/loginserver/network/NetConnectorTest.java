package com.aionemu.loginserver.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.commons.network.ServerTransport;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NetConnectorTest {

	private NetConnector connector() {
		return new NetConnector(null, null, null, null, null, null, null);
	}

	@AfterEach
	void resetTransportFactory() {
		connector().resetTransportFactory();
	}

	@Test
	void shutdownIfInitializedDoesNotCreateTransport() {
		AtomicInteger createdTransports = new AtomicInteger();
		NetConnector netConnector = connector();
		netConnector.useTransportFactory(() -> {
			createdTransports.incrementAndGet();
			return new RecordingTransport();
		});

		assertFalse(netConnector.shutdownIfInitialized());
		assertEquals(0, createdTransports.get());
	}

	@Test
	void shutdownClearsTransportForNextLifecycle() {
		AtomicInteger shutdowns = new AtomicInteger();
		NetConnector netConnector = connector();
		netConnector.useTransportFactory(() -> new RecordingTransport(shutdowns));

		ServerTransport firstTransport = netConnector.currentTransport();
		assertTrue(netConnector.shutdownIfInitialized());
		ServerTransport secondTransport = netConnector.currentTransport();

		assertEquals(1, shutdowns.get());
		assertNotSame(firstTransport, secondTransport);
	}

	private record RecordingTransport(AtomicInteger shutdowns) implements ServerTransport {
		private RecordingTransport() {
			this(new AtomicInteger());
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
