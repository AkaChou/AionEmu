package com.aionemu.gameserver.skillengine.periodicaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MpUsePeriodicActionTest {

	@Test
	void supportsRatioAndFixedCosts() {
		MpUsePeriodicAction action = new MpUsePeriodicAction();
		action.value = 4;

		assertEquals(40, action.requiredMp(1000));
		action.ratio = false;
		assertEquals(4, action.requiredMp(1000));
	}
}
