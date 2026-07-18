package com.aionemu.gameserver.skillengine.periodicaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.configs.main.SkillConfig;

class DpUsePeriodicActionTest {

	@AfterEach
	void resetConfig() {
		SkillConfig.CONSUME_DP = true;
	}

	@Test
	void skipsPeriodicDpDeductionWhenDisabled() {
		SkillConfig.CONSUME_DP = false;

		assertDoesNotThrow(() -> new DpUsePeriodicAction().act(null));
	}
}
