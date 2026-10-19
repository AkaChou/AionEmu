package com.aionemu.gameserver.services.player.CreativityPanel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CreativityEssenceServiceTest {

	@Test
	void keepsRetailEstimaTierValues() {
		assertEquals(8, CreativityEssenceService.estimaCpForEnchantLevel(6));
		assertEquals(10, CreativityEssenceService.estimaCpForEnchantLevel(7));
		assertEquals(12, CreativityEssenceService.estimaCpForEnchantLevel(8));
		assertEquals(14, CreativityEssenceService.estimaCpForEnchantLevel(9));
		assertEquals(17, CreativityEssenceService.estimaCpForEnchantLevel(10));
	}

	@Test
	void grantsLevelPlusOneBelowRetailTier() {
		assertEquals(1, CreativityEssenceService.estimaCpForEnchantLevel(0));
		assertEquals(5, CreativityEssenceService.estimaCpForEnchantLevel(4));
		assertEquals(6, CreativityEssenceService.estimaCpForEnchantLevel(5));
	}

	@Test
	void keepsGrowingPastRetailCeilingInsteadOfDroppingToLevelPlusOne() {
		int levelTen = CreativityEssenceService.estimaCpForEnchantLevel(10);

		for (int level = 11; level <= 50; level++) {
			int cp = CreativityEssenceService.estimaCpForEnchantLevel(level);
			assertTrue(cp > levelTen, "level " + level + " should exceed the +10 tier");
			assertTrue(cp > CreativityEssenceService.estimaCpForEnchantLevel(level - 1),
				"level " + level + " should exceed level " + (level - 1));
		}
	}

	@Test
	void extendsTheTenToNineTierSlope() {
		assertEquals(20, CreativityEssenceService.estimaCpForEnchantLevel(11));
		assertEquals(23, CreativityEssenceService.estimaCpForEnchantLevel(12));
		assertEquals(137, CreativityEssenceService.estimaCpForEnchantLevel(50));
	}

	@Test
	void neverGrantsNegativePointsForNegativeLevels() {
		assertEquals(1, CreativityEssenceService.estimaCpForEnchantLevel(-3));
	}
}
