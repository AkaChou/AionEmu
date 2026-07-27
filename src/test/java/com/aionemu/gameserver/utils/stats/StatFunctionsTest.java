package com.aionemu.gameserver.utils.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.configs.main.SkillConfig;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatSetFunction;
import com.aionemu.gameserver.model.stats.container.NpcGameStats;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class StatFunctionsTest {
	private final ObjenesisStd objenesis = new ObjenesisStd();

	@AfterEach
	void resetConfig() {
		RateConfig.DAMAGE_MULTIPLIER = 1f;
		SkillConfig.MAGICBOOST_CAP = 3400;
	}

	@Test
	void appliesRetailMagicBoostAndKnowledgeFormula() {
		SkillConfig.MAGICBOOST_CAP = 4000;
		assertEquals(4000, StatFunctions.capMagicBoostForDamage(7200));
		assertEquals(3200, StatFunctions.capMagicBoostForDamage(3200));
		assertEquals(0, StatFunctions.capMagicBoostForDamage(-1));
		assertEquals(2.4f, StatFunctions.calculateMagicalSkillDamageFactor(1000, 140), 0.001f);
		assertEquals(800, StatFunctions.scaleMagicBoostDifference(1200, 1.5f));
		assertEquals(2000, StatFunctions.scaleMagicBoostDifference(5000, 2f));
		assertEquals(125, StatFunctions.applyOneTimeSkillAttack(100, 5, 1.2f));
	}

	@Test
	void appliesMagicalDefenseBeforeElementalResistance() {
		assertEquals(810f, StatFunctions.applyMagicalDefenseModifiers(1000, 1000, 130, 1300), 0.001f);
	}

	@Test
	void appliesRetailElementalDefenseLowerCap() {
		assertEquals(-1000, StatFunctions.applyElementalDefenseLowerCap(-2000, true, 50));
		assertEquals(-1010, StatFunctions.applyElementalDefenseLowerCap(-2000, true, 51));
		assertEquals(-1350, StatFunctions.applyElementalDefenseLowerCap(-2000, true, 85));
		assertEquals(-1300, StatFunctions.applyElementalDefenseLowerCap(-2000, false, 85));
		assertEquals(-500, StatFunctions.applyElementalDefenseLowerCap(-500, true, 85));
	}

	@Test
	void appliesRetailPhysicalDefenseObjectCoefficient() {
		assertEquals(400f, StatFunctions.applyPhysicalDefenseModifiers(1000, 2000, 2f), 0.001f);
	}

	@Test
	void preservesNpcAttackModifiersAroundRetailDamageRange() {
		assertEquals(2299f, StatFunctions.scaleNpcAttackDamage(2299, 2299, 3043, 2671), 0.001f);
		assertEquals(4598f, StatFunctions.scaleNpcAttackDamage(2299, 2299, 3043, 5342), 0.001f);
		assertEquals(0, StatFunctions.rollNpcAttackDamage(0, 0));
		int reversedRoll = StatFunctions.rollNpcAttackDamage(5466, 3345);
		assertTrue(reversedRoll >= 3345 && reversedRoll <= 5466);
	}

	@Test
	void appliesRetailLimitAttributeAsAdditionalDamage() {
		assertEquals(2200f, StatFunctions.applyLimitAttributeBonus(2000, 1000, 250, 50), 0.001f);
		assertEquals(2000f, StatFunctions.applyLimitAttributeBonus(2000, 1000, 50, 50), 0.001f);
		assertEquals(0f, StatFunctions.applyLimitAttributeBonus(0, 1000, 250, 50), 0.001f);
	}

	@Test
	void appliesConfiguredDamageMultiplier() {
		RateConfig.DAMAGE_MULTIPLIER = 1.5f;

		assertEquals(150, StatFunctions.applyDamageMultiplier(100));
	}

	@Test
	void appliesCriticalModifierBeforeResistanceAndDefenderStatRatio() {
		assertEquals(100, StatFunctions.calculateEffectiveMagicalCritical(500, 100, 25, 50, false, 1.25f));
		assertEquals(500, StatFunctions.calculateEffectiveMagicalCritical(500, 100, 150, 100, true, 1.5f));
	}

	@Test
	void appliesRestingAndAttackerStatRatioToMagicalResistanceDifference() {
		assertEquals(200, StatFunctions.calculateMagicalResistDifference(1200, 800, 100, false, 1.5f));
		assertEquals(100, StatFunctions.calculateMagicalResistDifference(1200, 800, 100, true, 1.5f));
	}

	@Test
	void calculatesAvoidanceFromDirectDifferenceAndAttackerStatRatio() {
		assertEquals(200, StatFunctions.calculateAvoidanceRate(1200, 800, 100, 1.5f, 300));
		assertEquals(300, StatFunctions.calculateAvoidanceRate(2000, 800, 0, 1f, 300));
		assertEquals(0, StatFunctions.calculateAvoidanceRate(500, 800, 0, 1f, 300));
	}

	@Test
	void scalesNpcAvoidanceBeforeSubtractingAccuracy() {
		assertEquals(1200, StatFunctions.applyNpcAvoidanceLevelBonus(1000, 4, 0.1f));
		assertEquals(1400, StatFunctions.applyNpcAvoidanceLevelBonus(1000, 4, 0.2f));
	}

	@Test
	void appliesRetailMagicalResistanceLevelGrace() {
		assertEquals(200, StatFunctions.calculateLevelResistModifier(70, 76, false, false));
		assertEquals(0, StatFunctions.calculateLevelResistModifier(70, 76, true, false));
		assertEquals(100, StatFunctions.calculateLevelResistModifier(70, 77, true, false));
		assertEquals(0, StatFunctions.calculateLevelResistModifier(70, 80, true, true));
		assertEquals(-200, StatFunctions.calculateLevelResistModifier(76, 70, false, false));
	}

	@Test
	void appliesRetailMovementCombatStatTable() {
		assertEquals(800, StatFunctions.applyMovementStatModifier(0, StatEnum.PHYSICAL_DEFENSE, 1000));
		assertEquals(800, StatFunctions.applyMovementStatModifier(0, StatEnum.MAGICAL_DEFEND, 1000));
		assertEquals(500, StatFunctions.applyMovementStatModifier(0, StatEnum.FIRE_RESISTANCE, 1000));
		assertEquals(1000, StatFunctions.applyMovementStatModifier(1, StatEnum.PHYSICAL_DEFENSE, 1000));
		assertEquals(1300, StatFunctions.applyMovementStatModifier(1, StatEnum.EVASION, 1000));
		assertEquals(1500, StatFunctions.applyMovementStatModifier(4, StatEnum.BLOCK, 1000));
		assertEquals(1000, StatFunctions.applyMovementStatModifier(4, StatEnum.EVASION, 1000));
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

	@Test
	void appliesRetailPvpDamageRatioLimitsAndSkillModifier() {
		assertEquals(630f, StatFunctions.applyPvpDamageModifiers(1000, 0, 600, 0), 0.001f);
		assertEquals(315f, StatFunctions.applyPvpDamageModifiers(1000, 75, 0, 0), 0.001f);
		assertEquals(42f, StatFunctions.applyPvpDamageModifiers(1000, 0, -12000, 12000), 0.001f);
	}

	@Test
	void appliesRetailPveDamageRatioLimits() {
		assertEquals(12000f, StatFunctions.applyPveDamageModifiers(1000, 12000, -12000), 0.001f);
		assertEquals(100f, StatFunctions.applyPveDamageModifiers(1000, -12000, 12000), 0.001f);
	}

	@Test
	void appliesRetailNpcLevelPenaltyWithArchDaevaGraceLevel() {
		assertEquals(0.1f, StatFunctions.getNpcDamageFactor(11, false), 0.001f);
		assertEquals(0f, StatFunctions.getNpcDamageFactor(12, false), 0.001f);
		assertEquals(0.1f, StatFunctions.getNpcDamageFactor(12, true), 0.001f);
		assertEquals(0f, StatFunctions.getNpcDamageFactor(13, true), 0.001f);
	}

	@Test
	void absolutePvpStatOverridesGeneralAndTypedBonuses() {
		Npc npc = objenesis.newInstance(Npc.class);
		NpcGameStats stats = new NpcGameStats(npc);
		npc.setGameStats(stats);
		StatOwner normalStats = new StatOwner() { };
		stats.addEffectOnly(normalStats, List.of(
				new StatAddFunction(StatEnum.PVP_ATTACK_RATIO, 200, true),
				new StatAddFunction(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 50, true),
				new StatAddFunction(StatEnum.PVP_ATTACK_RATIO_MAGICAL, 80, true)));
		assertEquals(250, StatFunctions.getPvpRatio(npc, StatEnum.PVP_ATTACK_RATIO,
				StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
		assertEquals(280, StatFunctions.getPvpRatio(npc, StatEnum.PVP_ATTACK_RATIO,
				StatEnum.PVP_ATTACK_RATIO_MAGICAL));

		stats.addEffectOnly(new StatOwner() { }, List.of(
				new StatSetFunction(StatEnum.PVP_ATTACK_RATIO_PHYSICAL, 600, false)));
		assertEquals(600, StatFunctions.getPvpRatio(npc, StatEnum.PVP_ATTACK_RATIO,
				StatEnum.PVP_ATTACK_RATIO_PHYSICAL));
	}
}
