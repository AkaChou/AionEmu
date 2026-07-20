package com.aionemu.gameserver.utils.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
	}

	@Test
	void appliesMagicalDefenseBeforeElementalResistance() {
		assertEquals(810f, StatFunctions.applyMagicalDefenseModifiers(1000, 1000, 130, 1300), 0.001f);
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
