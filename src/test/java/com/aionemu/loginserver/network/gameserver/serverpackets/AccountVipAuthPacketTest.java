package com.aionemu.loginserver.network.gameserver.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.network.gameserver.GsConnection;

class AccountVipAuthPacketTest {

	@Test
	void appendsIndependentVipStateAfterReturnFlag() {
		int accountId = 42;
		Account account = new Account();
		account.setId(accountId);
		account.setAccountTime(new AccountTime());
		GameServerInfo gameServer = new GameServerInfo((byte) 1, "127.0.0.1", "test");
		gameServer.addAccountToGameServer(account);
		GsConnection connection = new GsConnection(new NoopTransport());
		connection.setGameServerInfo(gameServer);

		ByteBuffer buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN);
		new SM_ACCOUNT_AUTH_RESPONSE(accountId, true, "test", (byte) 0, (byte) 7, 10, 20,
				(byte) 1, 4, 1035, 1_700_000_000L).write(connection, buffer);

		// tail: isReturn(1) + vipLevel(1) + vipExp(8) + expire(8) = 18
		buffer.position(buffer.limit() - 18);
		assertEquals(1, buffer.get());
		assertEquals(4, buffer.get());
		assertEquals(1035, buffer.getLong());
		assertEquals(1_700_000_000L, buffer.getLong());
	}

	private static final class NoopTransport implements ConnectionTransport {

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
