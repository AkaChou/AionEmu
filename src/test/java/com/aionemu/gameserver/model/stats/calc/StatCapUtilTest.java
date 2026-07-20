package com.aionemu.gameserver.model.stats.calc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.stats.container.CombatMode;
import com.aionemu.gameserver.model.stats.container.RatioType;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import org.junit.jupiter.api.Test;

class StatCapUtilTest {

	@Test
	void clampsAggregatedPvpAndPveRatioBonuses() {
		assertEquals(9999, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.ATTACK, 12000));
		assertEquals(-9999, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVP, RatioType.DEFENSE, -12000));
		assertEquals(10000, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.ATTACK, 12000));
		assertEquals(-10000, StatCapUtil.limitValueForPvpOrPveStat(CombatMode.PVE, RatioType.DEFENSE, -12000));
	}

	@Test
	void keepsPvpStatsUncappedUntilTheCombinedDamageRatioIsCalculated() {
		assertEquals(Integer.MIN_VALUE, StatCapUtil.getLowerCap(StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(Integer.MAX_VALUE, StatCapUtil.getUpperCap(StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
	}
}
