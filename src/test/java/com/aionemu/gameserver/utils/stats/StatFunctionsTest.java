package com.aionemu.gameserver.utils.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StatFunctionsTest {

	@AfterEach
	void resetMagicBoostCap() {
		CustomConfig.MAGICBOOST_CAP = 6500;
		RateConfig.DAMAGE_MULTIPLIER = 1f;
	}

	@Test
	void capsMagicBoostForDamageAtConfiguredLimit() {
		CustomConfig.MAGICBOOST_CAP = 6500;

		assertEquals(6500, StatFunctions.capMagicBoostForDamage(7200));
		assertEquals(6400, StatFunctions.capMagicBoostForDamage(6400));
		assertEquals(0, StatFunctions.capMagicBoostForDamage(-1));
	}

	@Test
	void appliesConfiguredDamageMultiplier() {
		RateConfig.DAMAGE_MULTIPLIER = 1.5f;

		assertEquals(150, StatFunctions.applyDamageMultiplier(100));
	}
}
