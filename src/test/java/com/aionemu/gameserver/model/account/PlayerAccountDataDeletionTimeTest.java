package com.aionemu.gameserver.model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;

class PlayerAccountDataDeletionTimeTest {

	@Test
	void returnsUnixTimestampWithoutTimezoneOffset() {
		PlayerAccountData accountData = new PlayerAccountData(null, null, null, List.of(), null);
		accountData.setDeletionDate(new Timestamp(1_800_000_600_000L));

		assertEquals(1_800_000_600, accountData.getDeletionTimeInSeconds());
	}

	@Test
	void returnsZeroWhenDeletionIsNotScheduled() {
		PlayerAccountData accountData = new PlayerAccountData(null, null, null, List.of(), null);

		assertEquals(0, accountData.getDeletionTimeInSeconds());
	}
}
