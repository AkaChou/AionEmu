package com.aionemu.loginserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.loginserver.configs.SvStatsConfig;
import com.aionemu.loginserver.network.gameserver.GsConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PingPongThreadTest {

	@AfterEach
	void resetStatsConfig() {
		SvStatsConfig.SVSTATS_ENABLE = false;
	}

	@Test
	void closeMeAllowsUnauthenticatedConnection() {
		SvStatsConfig.SVSTATS_ENABLE = true;
		PingPongThread pingPongThread = new PingPongThread(new GsConnection(new StubTransport()));

		assertDoesNotThrow(pingPongThread::closeMe);
		assertFalse(pingPongThread.uptime);
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
