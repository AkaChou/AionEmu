package com.aionemu.loginserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Proxy;

import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import com.aionemu.loginserver.persistence.dao.SvstatDao;
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
		PingPongThread pingPongThread = new PingPongThread(connection(statsDao()));

		assertDoesNotThrow(pingPongThread::closeMe);
		assertFalse(pingPongThread.uptime);
	}

	@Test
	void closeMeSurvivesUnavailableStatsDataSource() {
		// 关闭流程中数据源可能已不可用（login 服的关闭早于 game 服侧的断开清理）：
		// 统计失败必须被吞掉，否则会让 GsConnection.onDisconnect() 的剩余清理中断。
		SvStatsConfig.SVSTATS_ENABLE = true;
		GsConnection connection = connection((SvstatDao) Proxy.newProxyInstance(
			SvstatDao.class.getClassLoader(), new Class<?>[] { SvstatDao.class },
			(proxy, method, args) -> {
				throw new IllegalStateException("data source closed");
			}));
		connection.setGameServerInfo(new GameServerInfo((byte) 1, "127.0.0.1", "password"));

		assertDoesNotThrow(new PingPongThread(connection)::closeMe);
	}

	private static GsConnection connection(SvstatDao svstatsDao) {
		return new GsConnection(new StubTransport(), null, svstatsDao, null);
	}

	private static SvstatDao statsDao() {
		return (SvstatDao) Proxy.newProxyInstance(SvstatDao.class.getClassLoader(),
			new Class<?>[] { SvstatDao.class }, (proxy, method, args) -> null);
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
