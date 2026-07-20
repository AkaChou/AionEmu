package com.aionemu.gameserver.network.aion.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SML2AuthLoginCheckTest {

	@Test
	void selectsClientDataByCountryCode() {
		byte[] china = SM_L2AUTH_LOGIN_CHECK.dataForCountry(5);
		byte[] europe = SM_L2AUTH_LOGIN_CHECK.dataForCountry(2);

		assertNotSame(china, europe);
		assertSame(china, SM_L2AUTH_LOGIN_CHECK.dataForCountry(5));
		assertSame(europe, SM_L2AUTH_LOGIN_CHECK.dataForCountry(1));
		assertEquals(1818, china.length);
		assertEquals(1812, europe.length);
		assertEquals(0, Byte.toUnsignedInt(china[7]));
		assertEquals(1, Byte.toUnsignedInt(europe[7]));
	}
}
