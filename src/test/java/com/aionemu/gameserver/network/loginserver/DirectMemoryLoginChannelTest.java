package com.aionemu.gameserver.network.loginserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.testutil.ServiceContextUnwind;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.gameserver.network.loginserver.DirectMemoryLoginChannel.DirectEndpoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

/**
 * 验证直连端序、上下文及双端并发关闭时的一次性清理。
 * Verifies direct framing, contexts and exactly-once cleanup during concurrent endpoint closure.
 */
class DirectMemoryLoginChannelTest {

	@AfterEach
	void unwindContext() {
		ServiceContextUnwind.unwind();
	}

	@Test
	void deliversLittleEndianFramesToPeer() {
		SenderEndpoint source = new SenderEndpoint(new PortPacket());
		CollectorEndpoint target = new CollectorEndpoint();

		String previous = ServiceContext.current();
		DirectMemoryLoginChannel.pumpWrites(source, target);
		assertEquals(previous, ServiceContext.current());

		assertEquals(1, target.frames.size());
		assertEquals("login", source.lastWriteContext);
		ByteBuffer received = target.frames.getFirst();
		assertEquals(ByteOrder.LITTLE_ENDIAN, received.order());
		assertEquals(0x05, Byte.toUnsignedInt(received.get()));
		assertEquals(9014, Short.toUnsignedInt(received.getShort()));
		assertEquals(0x01020304, received.getInt());
	}

	@Test
	void cleansUpEachEndpointInItsOwnServiceContext() {
		SenderEndpoint source = new SenderEndpoint();
		CollectorEndpoint target = new CollectorEndpoint();

		DirectMemoryLoginChannel.cleanupEndpoint(source);
		DirectMemoryLoginChannel.cleanupEndpoint(target);
		DirectMemoryLoginChannel.cleanupEndpoint(source);
		DirectMemoryLoginChannel.cleanupEndpoint(target);

		assertEquals("login", source.lastCleanupContext);
		assertEquals("game", target.lastCleanupContext);
		assertEquals(1, source.cleanupCount.get());
		assertEquals(1, target.cleanupCount.get());
	}

	@Test
	void bothEndsCanCloseConcurrentlyWithoutTakingEachOthersGuard() throws Exception {
		SenderEndpoint source = new SenderEndpoint();
		CollectorEndpoint target = new CollectorEndpoint();
		ConnectionTransport first = transport(source, target);
		ConnectionTransport second = transport(target, source);
		CountDownLatch ready = new CountDownLatch(2);
		var executor = Executors.newFixedThreadPool(2, Thread.ofPlatform().daemon(true).factory());
		try {
			var a = executor.submit(() -> closeUnderGuard(first, source, ready));
			var b = executor.submit(() -> closeUnderGuard(second, target, ready));
			a.get(5, TimeUnit.SECONDS);
			b.get(5, TimeUnit.SECONDS);
			first.close(true);
			second.close(true);
			assertEquals(1, source.cleanupCount.get());
			assertEquals(1, target.cleanupCount.get());
			assertEquals("login", source.lastCleanupContext);
			assertEquals("game", target.lastCleanupContext);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void failingOwnerStillCleansPeerAndIsNotRetried() throws Exception {
		SenderEndpoint source = new SenderEndpoint();
		source.failCleanup = true;
		CollectorEndpoint target = new CollectorEndpoint();
		ConnectionTransport transport = transport(source, target);

		assertThrows(IllegalStateException.class, () -> transport.close(true));
		transport.close(true);
		assertEquals(1, source.cleanupCount.get());
		assertEquals(1, target.cleanupCount.get());
	}

	private static ConnectionTransport transport(DirectEndpoint owner, DirectEndpoint peer) throws Exception {
		Class<?> type = Class.forName(DirectMemoryLoginChannel.class.getName() + "$DirectTransport");
		var constructor = type.getDeclaredConstructor(String.class);
		constructor.setAccessible(true);
		ConnectionTransport transport = (ConnectionTransport) constructor.newInstance("127.0.0.1");
		var connect = type.getDeclaredMethod("connectTo", DirectEndpoint.class, DirectEndpoint.class);
		connect.setAccessible(true);
		connect.invoke(transport, owner, peer);
		return transport;
	}

	private static void closeUnderGuard(ConnectionTransport transport, DirectEndpoint endpoint, CountDownLatch ready) {
		synchronized (endpoint.syncGuard()) {
			ready.countDown();
			try {
				if (!ready.await(3, TimeUnit.SECONDS)) {
					throw new AssertionError("both endpoint guards must be held");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new AssertionError(e);
			}
			transport.close(true);
		}
	}

	@Test
	void deliversMultipleFramesInWriteOrder() {
		SenderEndpoint source = new SenderEndpoint(new PortPacket(), new PortPacket());
		CollectorEndpoint target = new CollectorEndpoint();

		DirectMemoryLoginChannel.pumpWrites(source, target);

		assertEquals(2, target.frames.size());
		for (ByteBuffer received : target.frames) {
			assertEquals(9014, Short.toUnsignedInt(received.getShort(1)));
		}
		assertEquals(0, source.buffer.position());
		assertEquals(source.buffer.capacity(), source.buffer.limit());
	}

	/**
	 * 测试用登录服封包：写入端口与整型字段以覆盖多字节端序。
	 * Test login packet: writes port and int fields to cover multi-byte endianness.
	 */
	private static final class PortPacket extends LsServerPacket {

		private PortPacket() {
			super(0x05);
		}

		@Override
		protected void writeImpl(LoginServerConnection con) {
			writeH(9014);
			writeD(0x01020304);
		}
	}

	/**
	 * 复用真实 {@link LsServerPacket#write} 帧编码的写出端。
	 * Write side reusing the real {@link LsServerPacket#write} framing.
	 */
	private static final class SenderEndpoint implements DirectEndpoint {

		private final Deque<LsServerPacket> pending = new ArrayDeque<>();
		private final ByteBuffer buffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN).flip();
		private String lastWriteContext;
		private String lastCleanupContext;
		private final AtomicBoolean closed = new AtomicBoolean();
		private final AtomicInteger cleanupCount = new AtomicInteger();
		private boolean failCleanup;

		private SenderEndpoint(LsServerPacket... packets) {
			for (LsServerPacket packet : packets) {
				pending.addLast(packet);
			}
		}

		@Override
		public boolean writeDirect(ByteBuffer target) {
			lastWriteContext = ServiceContext.current();
			LsServerPacket packet = pending.pollFirst();
			if (packet == null) {
				return false;
			}
			packet.write(null, target);
			return true;
		}

		@Override
		public boolean processDirect(ByteBuffer buffer) {
			return true;
		}

		@Override
		public boolean isWriteEnabled() {
			return true;
		}

		@Override
		public Object syncGuard() {
			return this;
		}

		@Override
		public ByteBuffer writeBuffer() {
			return buffer;
		}

		@Override
		public String serviceContext() {
			return "login";
		}

		@Override
		public boolean claimClose() {
			return closed.compareAndSet(false, true);
		}

		@Override
		public void cleanupDirect() {
			lastCleanupContext = ServiceContext.current();
			cleanupCount.incrementAndGet();
			if (failCleanup) {
				throw new IllegalStateException("owner cleanup failed");
			}
		}
	}

	/**
	 * 记录对端收到的载荷视图。
	 * Records payload views received by the peer.
	 */
	private static final class CollectorEndpoint implements DirectEndpoint {

		private final List<ByteBuffer> frames = new ArrayList<>();
		private String lastDeliveryContext;
		private String lastCleanupContext;
		private final AtomicBoolean closed = new AtomicBoolean();
		private final AtomicInteger cleanupCount = new AtomicInteger();

		@Override
		public boolean writeDirect(ByteBuffer buffer) {
			return false;
		}

		@Override
		public boolean processDirect(ByteBuffer buffer) {
			lastDeliveryContext = ServiceContext.current();
			ByteBuffer copy = ByteBuffer.allocate(buffer.remaining()).order(buffer.order());
			copy.put(buffer);
			copy.flip();
			frames.add(copy);
			return true;
		}

		@Override
		public boolean isWriteEnabled() {
			return true;
		}

		@Override
		public Object syncGuard() {
			return this;
		}

		@Override
		public ByteBuffer writeBuffer() {
			return ByteBuffer.allocate(16);
		}

		@Override
		public String serviceContext() {
			return "game";
		}

		@Override
		public boolean claimClose() {
			return closed.compareAndSet(false, true);
		}

		@Override
		public void cleanupDirect() {
			lastCleanupContext = ServiceContext.current();
			cleanupCount.incrementAndGet();
		}
	}
}
