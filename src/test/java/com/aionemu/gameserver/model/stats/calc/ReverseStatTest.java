package com.aionemu.gameserver.model.stats.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.stats.container.StatEnum;

class ReverseStatTest {

	@Test
	void keepsNegativeModifiersAndClampsReductionsAboveOneHundredPercent() {
		ReverseStat stat = new ReverseStat(StatEnum.BOOST_CASTING_TIME, 0, null);

		assertEquals(1.2f, stat.calculatePercent(-20));
		assertEquals(0, stat.calculatePercent(150));
	}
}
