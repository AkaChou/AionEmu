package com.aionemu.loginserver.network.gameserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.configs.Config;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.loginserver.utils.ThreadPoolManager;
import com.aionemu.testutil.ConfigSnapshot;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 验证心跳保留 5 秒延迟且在长任务池运行，延迟或排队期间断线均不再启动循环。
 * Verifies the 5-second heartbeat delay and long-running pool handoff, including disconnects before delayed/queued start.
 */
class GsConnectionHeartbeatTest {

	private final ConfigSnapshot pingConfig = ConfigSnapshot.of(Config.class, "ENABLE_PINGPONG");
	private final ConfigSnapshot statsConfig = ConfigSnapshot.of(SvStatsConfig.class, "SVSTATS_ENABLE");

	@AfterEach
	void restoreConfig() {
		pingConfig.restore();
		statsConfig.restore();
	}

	@Test
	void heartbeatRetainsDelayAndHandsLoopToLongRunningPool() {
		RecordingPools pools = new RecordingPools();
		connection(pools);

		assertEquals(5000, pools.delay);
		assertNotNull(pools.delayed);
		assertNull(pools.longRunning);
		pools.delayed.run();
		assertNotNull(pools.longRunning);
	}

	@Test
	void disconnectedBeforeDelayDoesNotSubmitHeartbeat() {
		RecordingPools pools = new RecordingPools();
		GsConnection connection = connection(pools);
		connection.onDisconnect();

		assertDoesNotThrow(pools.delayed::run);
		assertNull(pools.longRunning);
		assertDoesNotThrow(connection::initialized);
	}

	@Test
	void disconnectedAfterHandoffDoesNotDereferenceClearedServerInfo() {
		RecordingPools pools = new RecordingPools();
		GsConnection connection = connection(pools);
		pools.delayed.run();
		connection.onDisconnect();

		assertDoesNotThrow(pools.longRunning::run);
		assertNull(connection.getGameServerInfo());
	}

	@Test
	void disconnectBeforeInitializationPreventsLaterHeartbeatCreation() {
		Config.ENABLE_PINGPONG = true;
		SvStatsConfig.SVSTATS_ENABLE = false;
		RecordingPools pools = new RecordingPools();
		GsConnection connection = new GsConnection(new StubTransport(), null, null, pools);
		connection.onDisconnect();
		try (ServiceContext.Scope ignored = ServiceContext.use("heartbeat-test")) {
			connection.initialized();
			connection.setState(GsConnection.State.AUTHED);
		}

		assertNull(pools.delayed);
		assertNull(pools.longRunning);
	}

	@Test
	void repeatedAuthenticationDoesNotScheduleAnotherHeartbeat() {
		RecordingPools pools = new RecordingPools();
		GsConnection connection = connection(pools);
		Runnable delayed = pools.delayed;
		connection.setState(GsConnection.State.AUTHED);

		assertEquals(delayed, pools.delayed);
	}

	@Test
	void repeatedAuthenticationDoesNotSubmitAnotherHeartbeatLoop() {
		RecordingPools pools = new RecordingPools();
		GsConnection connection = connection(pools);
		pools.delayed.run();
		connection.setState(GsConnection.State.CONNECTED);
		connection.setState(GsConnection.State.AUTHED);
		pools.delayed.run();
		pools.delayed.run();

		assertEquals(1, pools.longRunningSubmissions);
	}

	private GsConnection connection(RecordingPools pools) {
		Config.ENABLE_PINGPONG = true;
		SvStatsConfig.SVSTATS_ENABLE = false;
		GsConnection connection = new GsConnection(new StubTransport(), null, null, pools);
		connection.initialized();
		connection.setGameServerInfo(new GameServerInfo((byte) 1, "127.0.0.1", "password"));
		connection.setState(GsConnection.State.AUTHED);
		return connection;
	}

	/** 不创建实际线程的延迟/长任务记录器。 / Delayed/long-task recorder without real worker threads. */
	private static final class RecordingPools extends ThreadPoolManager {
		private long delay;
		private Runnable delayed;
		private Runnable longRunning;
		private int longRunningSubmissions;

		@Override
		public ScheduledFuture<?> schedule(Runnable task, long delay) {
			this.delay = delay;
			delayed = task;
			return null;
		}

		@Override
		public void executeHeartbeat(Runnable task) {
			longRunningSubmissions++;
			longRunning = task;
		}
	}

	/** 不建立网络连接的传输替身。 / Transport stub without network connections. */
	private static final class StubTransport implements ConnectionTransport {
		@Override
		public String getIP() {
			return "127.0.0.1";
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
}
