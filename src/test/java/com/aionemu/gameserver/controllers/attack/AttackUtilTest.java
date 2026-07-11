package com.aionemu.gameserver.controllers.attack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import org.junit.jupiter.api.Test;

class AttackUtilTest {

	@Test
	void calculatesWeaponCriticalMultiplierWithoutRoundingFortitude() {
		assertEquals(1.5f, AttackUtil.calculateWeaponCriticalMultiplier(null,
				StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 0, 0));
		assertEquals(2.0f, AttackUtil.calculateWeaponCriticalMultiplier(WeaponType.GUN_1H,
				StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 300, 0));
	}

	@Test
	void addsTenPercentDamageForAdditionalPhysicalHits() {
		assertArrayEquals(new int[] { 100, 10, 10 }, AttackUtil.splitPhysicalDamageValues(3, 100));
	}

	@Test
	void usesRetailRandomDamageRanges() {
		assertEquals(50, AttackUtil.getRandomDamagePercent(1, 6));
		assertEquals(100, AttackUtil.getRandomDamagePercent(1, 7));
		assertEquals(150, AttackUtil.getRandomDamagePercent(1, 13));
		assertEquals(60, AttackUtil.getRandomDamagePercent(2, 13));
		assertEquals(200, AttackUtil.getRandomDamagePercent(2, 14));
		assertEquals(90, AttackUtil.getRandomDamagePercent(3, 6));
		assertEquals(110, AttackUtil.getRandomDamagePercent(3, 13));
		assertEquals(200, AttackUtil.getRandomDamagePercent(6, 14));
		assertEquals(100, AttackUtil.getRandomDamagePercent(10, 19));
	}
}
