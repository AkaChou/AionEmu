package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.network.packet.BaseClientPacket;

/**
 * 连接亲和条带化数据包处理器并发与保序单元测试。
 * Unit tests verifying concurrency, mutual exclusion, and FIFO ordering for sharded PacketProcessor.
 */
class ShardedPacketProcessorTest {

	@Test
	void singleConnectionPacketsExecuteStrictlyInOrder() throws Exception {
		int minThreads = 4;
		int maxThreads = 4;
		PacketProcessor<MockConnection> processor = new PacketProcessor<>(minThreads, maxThreads, 50, 10);

		int packetCount = 200;
		CountDownLatch latch = new CountDownLatch(packetCount);
		MockConnection connection = new MockConnection("127.0.0.1");

		List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());
		AtomicInteger concurrencyViolation = new AtomicInteger(0);
		AtomicInteger activeCount = new AtomicInteger(0);

		for (int i = 0; i < packetCount; i++) {
			int seq = i;
			processor.executePacket(new MockClientPacket(connection, () -> {
				int current = activeCount.incrementAndGet();
				if (current > 1) {
					concurrencyViolation.incrementAndGet();
				}
				executionOrder.add(seq);
				activeCount.decrementAndGet();
				latch.countDown();
			}));
		}

		assertTrue(latch.await(5, TimeUnit.SECONDS), "All packets should execute within timeout");
		assertEquals(0, concurrencyViolation.get(), "Packets on same connection must never execute concurrently");
		assertEquals(packetCount, executionOrder.size());

		for (int i = 0; i < packetCount; i++) {
			assertEquals(i, executionOrder.get(i), "Packets must be executed in strict FIFO order");
		}
	}

	@Test
	void multipleConnectionsExecuteConcurrentlyWithoutDeadlock() throws Exception {
		int minThreads = 4;
		int maxThreads = 8;
		PacketProcessor<MockConnection> processor = new PacketProcessor<>(minThreads, maxThreads, 100, 20);

		int connectionCount = 16;
		int packetsPerConnection = 50;
		int totalPackets = connectionCount * packetsPerConnection;
		CountDownLatch latch = new CountDownLatch(totalPackets);

		AtomicInteger executedTotal = new AtomicInteger(0);
		List<MockConnection> connections = new ArrayList<>();
		for (int i = 0; i < connectionCount; i++) {
			connections.add(new MockConnection("10.0.0." + i));
		}

		for (int p = 0; p < packetsPerConnection; p++) {
			for (int c = 0; c < connectionCount; c++) {
				processor.executePacket(new MockClientPacket(connections.get(c), () -> {
					executedTotal.incrementAndGet();
					latch.countDown();
				}));
			}
		}

		assertTrue(latch.await(5, TimeUnit.SECONDS), "All packets across multiple connections should complete without deadlock");
		assertEquals(totalPackets, executedTotal.get());
	}

	private static final class MockConnection extends AConnection {
		private MockConnection(String ip) {
			super(new MockTransport(ip), 64, 64);
		}

		@Override
		protected void initialized() {
		}

		@Override
		protected long getDisconnectionDelay() {
			return 0;
		}

		@Override
		protected boolean processData(ByteBuffer data) {
			return true;
		}

		@Override
		protected boolean writeData(ByteBuffer data) {
			return true;
		}

		@Override
		protected void onDisconnect() {
		}

		@Override
		protected void onServerClose() {
		}
	}

	private record MockTransport(String ip) implements ConnectionTransport {

		@Override
			public String getIP() {
				return ip;
			}

			@Override
			public void enableWriteInterest() {
			}

			@Override
			public void close(boolean forced) {
			}

			@Override
			public boolean onlyClose() {
				return true;
			}
		}

	private static final class MockClientPacket extends BaseClientPacket<MockConnection> {
		private final Runnable action;

		private MockClientPacket(MockConnection connection, Runnable action) {
			super(0);
			setConnection(connection);
			this.action = action;
		}

		@Override
		protected void readImpl() {
		}

		@Override
		public void run() {
			runImpl();
		}

		@Override
		protected void runImpl() {
			if (action != null) {
				action.run();
			}
		}
	}
}
