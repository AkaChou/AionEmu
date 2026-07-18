package com.aionemu.gameserver.utils.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StatFunctionsTest {

	@AfterEach
	void resetMagicBoostCap() {
		SkillConfig.MAGICBOOST_CAP = 6500;
		RateConfig.DAMAGE_MULTIPLIER = 1f;
	}

	@Test
	void capsMagicBoostForDamageAtConfiguredLimit() {
		SkillConfig.MAGICBOOST_CAP = 6500;

		assertEquals(6500, StatFunctions.capMagicBoostForDamage(7200));
		assertEquals(6400, StatFunctions.capMagicBoostForDamage(6400));
		assertEquals(0, StatFunctions.capMagicBoostForDamage(-1));
	}

	@Test
	void appliesConfiguredDamageMultiplier() {
		RateConfig.DAMAGE_MULTIPLIER = 1.5f;

		assertEquals(150, StatFunctions.applyDamageMultiplier(100));
	}

	@Test
	void calculatesEffectiveMagicalCriticalWithoutDuplicatingBaseValue() {
		assertEquals(350, StatFunctions.calculateEffectiveMagicalCritical(500, 100, 50, 100));
		assertEquals(175, StatFunctions.calculateEffectiveMagicalCritical(500, 100, 50, 50));
	}

	@Test
	void appliesAccuracyModifierToAvoidanceDifference() {
		assertEquals(100f, StatFunctions.calculateAvoidanceDifference(1000, 800, 100));
	}

	@Test
	void mapsTargetTypesToTheirPveAttackRatioStats() {
		assertEquals(StatEnum.PVE_ATTACK_RATIO_TYPE_A, StatFunctions.getPveAttackRatioStat(Race.TYPE_A));
		assertEquals(StatEnum.PVE_ATTACK_RATIO_TYPE_B, StatFunctions.getPveAttackRatioStat(Race.TYPE_B));
		assertEquals(StatEnum.PVE_ATTACK_RATIO_TYPE_C, StatFunctions.getPveAttackRatioStat(Race.TYPE_C));
		assertEquals(StatEnum.PVE_ATTACK_RATIO_TYPE_D, StatFunctions.getPveAttackRatioStat(Race.TYPE_D));
		assertEquals(StatEnum.PVE_ATTACK_RATIO_TYPE_E, StatFunctions.getPveAttackRatioStat(Race.TYPE_E));
		assertNull(StatFunctions.getPveAttackRatioStat(Race.BEAST));
	}
}
