package com.aionemu.gameserver.model.stats.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.stats.container.CombatMode;
import com.aionemu.gameserver.model.stats.container.RatioType;
import org.junit.jupiter.api.Test;

class StatCapUtilTest {

	@Test
	void clampsAggregatedPvpAndPveRatioBonuses() {
		assertEquals(1000, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.ATTACK, 1200));
		assertEquals(-1000, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.DEFENSE, -1200));
		assertEquals(5000, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.ATTACK, 7000));
		assertEquals(-5000, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.DEFENSE, -7000));
	}
}
