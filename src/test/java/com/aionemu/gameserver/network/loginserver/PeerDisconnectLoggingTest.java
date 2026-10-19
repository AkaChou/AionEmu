package com.aionemu.gameserver.network.loginserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyClient;
import com.aionemu.gameserver.network.chatserver.ChatServer;
import com.aionemu.gameserver.network.chatserver.ChatServerConnection;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.testutil.LogCapture;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * 验证主动关闭无误报警、重复关闭幂等、迟到资源仍清理，意外断线仍告警并重连。
 * Verifies quiet intentional closes, idempotency, late-resource cleanup, and warnings/reconnect for unexpected loss.
 */
class PeerDisconnectLoggingTest {

	@Test
	void intentionalDisconnectIsIdempotentAndDoesNotWarn() throws Exception {
		for (boolean login : List.of(true, false)) {
			try (Peer peer = new Peer(login); LogCapture logs = new LogCapture(peer.type());
				LogCapture lateTransportLogs = new LogCapture(RecordingTransport.class)) {
				peer.publish();
				peer.disconnect.run();
				peer.disconnect.run();
				assertEquals(1, peer.transport.closes.get());
				assertEquals(1, peer.client.closes.get());
				assertEquals(1, logs.events().stream().filter(event -> event.getLevel() == Level.INFO).count());
				assertEquals(0, logs.events().stream().filter(event -> event.getLevel() == Level.WARN).count());
				assertEquals(0, lateTransportLogs.events().stream().filter(event -> event.getLevel() == Level.WARN)
					.count());
			}
		}
	}

	@Test
	void lateConnectionIsCleanedWithoutRepeatingShutdownSummary() throws Exception {
		for (boolean login : List.of(true, false)) {
			try (Peer peer = new Peer(login); LogCapture logs = new LogCapture(peer.type());
				LogCapture transportLogs = new LogCapture(RecordingTransport.class)) {
				peer.disconnect.run();
				peer.publish();
				peer.disconnect.run();
				assertEquals(1, peer.transport.closes.get());
				assertEquals(1, peer.client.closes.get());
				assertFalse(logs.events().stream().anyMatch(event -> event.getLevel() == Level.WARN
					|| event.getLevel() == Level.ERROR));
				assertNull(peer.field(peer.connectionField()));
				assertNull(peer.field("nettyClient"));
			}
		}
	}

	@Test
	void concurrentDisconnectClaimsEachResourceOnce() throws Exception {
		for (boolean login : List.of(true, false)) {
			try (Peer peer = new Peer(login); LogCapture logs = new LogCapture(peer.type());
				LogCapture transportLogs = new LogCapture(RecordingTransport.class);
				var executor = Executors.newFixedThreadPool(2)) {
				peer.publish();
				CountDownLatch start = new CountDownLatch(1);
				var first = executor.submit(() -> {
					await(start);
					peer.disconnect.run();
				});
				var second = executor.submit(() -> {
					await(start);
					peer.disconnect.run();
				});
				start.countDown();
				first.get(5, TimeUnit.SECONDS);
				second.get(5, TimeUnit.SECONDS);
				assertEquals(1, peer.transport.closes.get());
				assertEquals(1, peer.client.closes.get());
				assertFalse(logs.events().stream().anyMatch(event -> event.getLevel() == Level.WARN
					|| event.getLevel() == Level.ERROR));
			}
		}
	}

	@Test
	void inFlightConnectReleasesResourcesCreatedAfterInitialShutdown() throws Exception {
		for (boolean login : List.of(true, false)) {
			try (Peer peer = new Peer(login); LogCapture logs = new LogCapture(peer.type());
				LogCapture transportLogs = new LogCapture(RecordingTransport.class);
				var executor = Executors.newSingleThreadExecutor()) {
				peer.client = new RecordingClient();
				peer.client.connectStarted = new CountDownLatch(1);
				peer.client.finishConnect = new CountDownLatch(1);
				peer.client.onConnected = () -> {
					try {
						peer.publishConnection();
					} catch (Exception e) {
						throw new AssertionError(e);
					}
				};
				var method = peer.type().getDeclaredMethod("connectWithNetty", NettyClient.class);
				method.setAccessible(true);
				var connecting = executor.submit(() -> {
					try {
						return method.invoke(peer.facade, peer.client);
					} catch (InvocationTargetException e) {
						throw new AssertionError(e.getCause());
					}
				});
				try {
					assertTrue(peer.client.connectStarted.await(5, TimeUnit.SECONDS));
					peer.disconnect.run();
				} finally {
					peer.client.finishConnect.countDown();
				}
				connecting.get(5, TimeUnit.SECONDS);
				assertFalse(peer.client.resourcesOpen);
				assertEquals(1, peer.client.resourceReleases.get());
				assertEquals(1, peer.transport.closes.get());
				assertNull(peer.field(peer.connectionField()));
				assertNull(peer.field("nettyClient"));
				assertEquals(0, logs.events().stream().filter(event -> event.getLevel() == Level.WARN).count());
				assertEquals(0, transportLogs.events().stream().filter(event -> event.getLevel() == Level.WARN)
					.count());
			}
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new AssertionError("timed out awaiting test coordination");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	@Test
	void unexpectedDisconnectWarnsAndSchedulesReconnectAfterPrepare() throws Exception {
		for (boolean login : List.of(true, false)) {
			try (Peer peer = new Peer(login); LogCapture logs = new LogCapture(peer.type())) {
				peer.disconnect.run();
				peer.prepare.run();
				peer.down.run();
				assertEquals(1, logs.events().stream().filter(event -> event.getLevel() == Level.WARN).count());
				assertEquals(5000, peer.scheduler.delay);
				assertEquals(1, peer.scheduler.scheduled);
			}
		}
	}

	/** 一个门面周期及其离线传输资源。 / One facade cycle and its offline transport resources. */
	private static final class Peer implements AutoCloseable {
		private final boolean login;
		private AConnection connection;
		private final Object facade;
		private final Runnable disconnect;
		private final Runnable down;
		private final Runnable prepare;
		private final CapturingScheduler scheduler = new CapturingScheduler();
		private RecordingTransport transport;
		private RecordingClient client;

		private Peer(boolean login) throws Exception {
			this.login = login;
			if (login) {
				LoginServer server = new LoginServer(null, null, null);
				facade = server;
				disconnect = server::gameServerDisconnected;
				down = server::loginServerDown;
				prepare = server::prepareForConnect;
			} else {
				ChatServer server = new ChatServer();
				facade = server;
				disconnect = server::gameServerDisconnected;
				down = server::chatServerDown;
				prepare = server::prepareForConnect;
			}
			ThreadPoolManager pools = new ObjenesisStd().newInstance(ThreadPoolManager.class);
			set(ThreadPoolManager.class, pools, "scheduledPool", scheduler);
			DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
			beans.registerSingleton("pools", pools);
			ThreadPoolManager.setInstanceProvider(beans.getBeanProvider(ThreadPoolManager.class));
		}

		private Class<?> type() {
			return facade.getClass();
		}

		private String connectionField() {
			return login ? "loginServer" : "chatServer";
		}

		private Object field(String name) throws Exception {
			Field field = type().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(facade);
		}

		private void publish() throws Exception {
			client = new RecordingClient();
			publishConnection();
			set(type(), facade, "nettyClient", client);
		}

		private void publishConnection() throws Exception {
			Class<? extends AConnection> type = login ? LoginServerConnection.class : ChatServerConnection.class;
			connection = new ObjenesisStd().newInstance(type);
			transport = new RecordingTransport(connection, down);
			set(AConnection.class, connection, "guard", new Object());
			set(AConnection.class, connection, "transport", transport);
			set(type(), facade, connectionField(), this.connection);
			if (login) {
				set(type(), facade, "dispatcher", new SocketLoginMessageDispatcher((LoginServerConnection) this.connection));
			}
		}

		private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
			Field field = type.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		}

		@Override
		public void close() {
			disconnect.run();
			scheduler.shutdownNow();
			ThreadPoolManager.setInstanceProvider(null);
		}
	}

	/** 关闭时同步模拟断线回调。 / Simulates a synchronous disconnect callback on close. */
	private static final class RecordingTransport implements ConnectionTransport {
		private final AConnection connection;
		private final Runnable callback;
		private final AtomicInteger closes = new AtomicInteger();

		private RecordingTransport(AConnection connection, Runnable callback) {
			this.connection = connection;
			this.callback = callback;
		}

		@Override
		public String getIP() {
			return "127.0.0.1";
		}

		@Override
		public void enableWriteInterest() {
		}

		@Override
		public void close(boolean forced) {
			closes.incrementAndGet();
			try {
				Field field = AConnection.class.getDeclaredField("closed");
				field.setAccessible(true);
				field.setBoolean(connection, true);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
			callback.run();
		}

		@Override
		public boolean onlyClose() {
			return true;
		}
	}

	/** 不创建连接的 Netty 客户端替身。 / Netty-client stub that never connects. */
	private static final class RecordingClient extends NettyClient {
		private final AtomicInteger closes = new AtomicInteger();
		private final AtomicInteger resourceReleases = new AtomicInteger();
		private CountDownLatch connectStarted;
		private CountDownLatch finishConnect;
		private Runnable onConnected;
		private volatile boolean resourcesOpen;

		private RecordingClient() {
			super(new InetSocketAddress("127.0.0.1", 1), "offline-test", transport -> null);
		}

		@Override
		public synchronized void shutdown() {
			closes.incrementAndGet();
			if (resourcesOpen) {
				resourcesOpen = false;
				resourceReleases.incrementAndGet();
			}
		}

		@Override
		public void connect() {
			connectStarted.countDown();
			await(finishConnect);
			resourcesOpen = true;
			if (onConnected != null) {
				onConnected.run();
			}
		}
	}

	/** 只记录调度，不启动任务。 / Records scheduling without running tasks. */
	private static final class CapturingScheduler extends ScheduledThreadPoolExecutor {
		private long delay;
		private int scheduled;

		private CapturingScheduler() {
			super(1);
		}

		@Override
		public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
			this.delay = unit.toMillis(delay);
			scheduled++;
			return new UnstartedFuture();
		}
	}

	/** 可取消但不执行的调度句柄。 / Cancellable scheduled handle that is never executed. */
	private static final class UnstartedFuture extends FutureTask<Void> implements ScheduledFuture<Void> {
		private UnstartedFuture() {
			super(() -> null);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return 0;
		}

		@Override
		public int compareTo(Delayed other) {
			return 0;
		}
	}
}
