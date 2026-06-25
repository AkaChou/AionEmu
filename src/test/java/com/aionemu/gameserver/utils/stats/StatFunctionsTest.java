package com.aionemu.gameserver.utils.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.configs.main.CustomConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StatFunctionsTest {

	@AfterEach
	void resetMagicBoostCap() {
		CustomConfig.MAGICBOOST_CAP = 6500;
	}

	@Test
	void capsMagicBoostForDamageAtConfiguredLimit() {
		CustomConfig.MAGICBOOST_CAP = 6500;

		assertEquals(6500, StatFunctions.capMagicBoostForDamage(7200));
		assertEquals(6400, StatFunctions.capMagicBoostForDamage(6400));
		assertEquals(0, StatFunctions.capMagicBoostForDamage(-1));
	}
}
