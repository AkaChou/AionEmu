package com.aionemu.loginserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.testutil.ConfigSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PingPongThreadTest {

	private final ConfigSnapshot svStatsSnapshot = ConfigSnapshot.of(SvStatsConfig.class, "SVSTATS_ENABLE");

	@AfterEach
	void resetStatsConfig() {
		svStatsSnapshot.restore();
	}

	@Test
	void closeMeAllowsUnauthenticatedConnection() {
		SvStatsConfig.SVSTATS_ENABLE = true;
		PingPongThread pingPongThread = new PingPongThread(new GsConnection(new StubTransport()));

		assertDoesNotThrow(pingPongThread::closeMe);
		assertFalse(pingPongThread.uptime);
	}

	@Test
	void svStatsUpdatesAreSkippedWhenTheDaoRegistryIsGone() {
		// 同一进程内 login 服先关闭会清空 DAO 注册表，而 game 服的断开清理可能在其之后执行：
		// 这里必须跳过而不是抛 DAONotFoundException（旧行为会让 GsConnection.onDisconnect() 的剩余清理中断）。
		SvStatsConfig.SVSTATS_ENABLE = true;
		assertFalse(DAOManager.isInitialized(), "测试 JVM 不应注册 DAO，否则本用例失去意义");

		assertDoesNotThrow(() -> PingPongThread.updateSvStatsOffline(1));
		assertDoesNotThrow(() -> PingPongThread.updateSvStatsOnline(1, 0, 0));
	}

	@Test
	void svStatsUpdatesAreSkippedWhenTheFeatureIsDisabled() {
		// 直接关闭开关，保证用例与执行顺序无关。
		// Disable the switch directly so the case does not depend on execution order.
		SvStatsConfig.SVSTATS_ENABLE = false;

		assertDoesNotThrow(() -> PingPongThread.updateSvStatsOffline(1));
		assertDoesNotThrow(() -> PingPongThread.updateSvStatsOnline(1, 0, 0));
	}

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
