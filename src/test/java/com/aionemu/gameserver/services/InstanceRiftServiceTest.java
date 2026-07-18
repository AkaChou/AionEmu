package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class InstanceRiftServiceTest {

	@Test
	void restoresOnlyTheRemainingPartOfAnOpenWindow() {
		Calendar now = Calendar.getInstance();
		now.clear();
		now.set(2026, Calendar.JULY, 18, 15, 0, 0);

		long remaining = InstanceRiftService.getRemainingOpenMillis(
			"0 0 9 ? * SAT,SUN *", TimeUnit.HOURS.toMillis(24), now.getTimeInMillis());

		assertEquals(TimeUnit.HOURS.toMillis(18), remaining);
	}
}
