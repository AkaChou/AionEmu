package com.aionemu.gameserver.network.aion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

class PlayerInfoTimeTest {

	@Test
	void writesLastOnlineAsUnixSeconds() {
		assertEquals(1_800_000_600, PlayerInfo.getLastOnlineTimeInSeconds(new Timestamp(1_800_000_600_999L)));
	}

	@Test
	void writesZeroWhenLastOnlineIsUnknown() {
		assertEquals(0, PlayerInfo.getLastOnlineTimeInSeconds(null));
	}
}
