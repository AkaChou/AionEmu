package com.aionemu.gameserver.controllers.attack;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import org.junit.jupiter.api.Test;

class AttackUtilTest {

	@Test
	void usesRetailCriticalMultipliers() {
		assertEquals(1.7f, physicalCriticalMultiplier(WeaponType.GUN_1H, true));
		assertEquals(1.7f, physicalCriticalMultiplier(WeaponType.CANNON_2H, true));
		assertEquals(2f, physicalCriticalMultiplier(WeaponType.BOOK_2H, true));
		assertEquals(2f, physicalCriticalMultiplier(WeaponType.ORB_2H, true));
		assertEquals(2f, physicalCriticalMultiplier(WeaponType.HARP_2H, true));
		assertEquals(1.5f, physicalCriticalMultiplier(WeaponType.KEYBLADE_2H, true));
		assertEquals(1.5f, physicalCriticalMultiplier(null, false));
		assertEquals(1.5f, magicalCriticalMultiplier(true));
		assertEquals(1f, magicalCriticalMultiplier(false));
	}

	@Test
	void limitsCriticalReductionToNormalDamage() {
		assertEquals(1f, AttackUtil.calculateWeaponCriticalMultiplier(WeaponType.GUN_1H,
				StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 1000, 0, true));
	}

	@Test
	void usesRetailBlockAndParryDamage() {
		assertEquals(500, AttackUtil.calculateBlockedDamage(1000, true, 300, 200, 1000));
		assertEquals(600, AttackUtil.calculateBlockedDamage(1000, true, 300, 200, 400));
		assertEquals(900, AttackUtil.calculateBlockedDamage(1000, false, 1000, 0, 0));
		assertEquals(600, AttackUtil.calculateParriedDamage(1000, true));
		assertEquals(900, AttackUtil.calculateParriedDamage(1000, false));
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

	private static float physicalCriticalMultiplier(WeaponType weaponType, boolean playerAttacker) {
		return AttackUtil.calculateWeaponCriticalMultiplier(weaponType,
				StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 0, 0, playerAttacker);
	}

	private static float magicalCriticalMultiplier(boolean playerAttacker) {
		return AttackUtil.calculateWeaponCriticalMultiplier(null,
				StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 0, 0, playerAttacker);
	}
}
