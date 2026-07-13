package com.aionemu.gameserver.model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AccountVipTest {

	@Test
	void vipStateIsIndependentFromMembership() {
		Account account = new Account(42);
		account.setMembership((byte) 7);
		account.setVipLevel((byte) 4);
		account.setVipExp(1035);

		assertEquals(7, account.getMembership());
		assertEquals(4, account.getVipLevel());
		assertEquals(1035, account.getVipExp());
	}
}
