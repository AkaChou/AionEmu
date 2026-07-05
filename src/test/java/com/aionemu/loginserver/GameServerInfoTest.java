package com.aionemu.loginserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.loginserver.model.Account;
import org.junit.jupiter.api.Test;

class GameServerInfoTest {

	@Test
	void accountRegistryKeepsExistingAddGetRemoveSemantics() {
		GameServerInfo serverInfo = new GameServerInfo((byte) 1, "127.0.0.1", "password");
		Account account = new Account();
		account.setId(42);

		serverInfo.addAccountToGameServer(account);

		assertTrue(serverInfo.isAccountOnGameServer(42));
		assertSame(account, serverInfo.getAccountFromGameServer(42));
		assertEquals(1, serverInfo.getCurrentPlayers());
		assertSame(account, serverInfo.removeAccountFromGameServer(42));
		assertFalse(serverInfo.isAccountOnGameServer(42));
		assertNull(serverInfo.getAccountFromGameServer(42));
		assertEquals(0, serverInfo.getCurrentPlayers());
	}
}
