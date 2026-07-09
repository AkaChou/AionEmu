package com.aionemu.gameserver.model.stats.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import org.junit.jupiter.api.Test;

class Stat2ExactDamageTest {

	@Test
	void keepsExactFloatCurrentUntilIntegerRead() {
		Stat2 stat = new AdditionStat(StatEnum.PHYSICAL_ATTACK, 10.5f, null, 1.25f);

		stat.addToBonus(2.4f);
		stat.setFixedBonusRate(0.5f);

		assertEquals(19.5f, stat.getExactCurrent(), 0.0001f);
		assertEquals(19, stat.getCurrent());
	}
}
