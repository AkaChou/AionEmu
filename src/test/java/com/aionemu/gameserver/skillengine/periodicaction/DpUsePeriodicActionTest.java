package com.aionemu.gameserver.skillengine.periodicaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.testutil.ConfigSnapshot;
import com.aionemu.gameserver.configs.main.SkillConfig;

class DpUsePeriodicActionTest {

	private final ConfigSnapshot skillConfigSnapshot = ConfigSnapshot.of(SkillConfig.class, "CONSUME_DP");

	@AfterEach
	void resetConfig() {
		skillConfigSnapshot.restore();
	}

	@Test
	void skipsPeriodicDpDeductionWhenDisabled() {
		SkillConfig.CONSUME_DP = false;

		assertDoesNotThrow(() -> new DpUsePeriodicAction().act(null));
	}
}
