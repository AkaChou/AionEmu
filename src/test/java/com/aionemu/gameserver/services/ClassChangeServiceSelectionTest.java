package com.aionemu.gameserver.services;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassChangeServiceSelectionTest {
	@Test
	void selectionsAreBoundToTheExpectedRaceAndQuest() {
		assertEquals(1006, ClassChangeService.questIdForRace(Race.ELYOS));
		assertEquals(2008, ClassChangeService.questIdForRace(Race.ASMODIANS));
		assertEquals(PlayerClass.GLADIATOR, ClassChangeService.classForSelection(Race.ELYOS, 2376));
		assertEquals(PlayerClass.AETHERTECH, ClassChangeService.classForSelection(Race.ELYOS, 3740));
		assertEquals(PlayerClass.GUNSLINGER, ClassChangeService.classForSelection(Race.ASMODIANS, 3591));
		assertEquals(PlayerClass.SONGWEAVER, ClassChangeService.classForSelection(Race.ASMODIANS, 3911));
		assertNull(ClassChangeService.classForSelection(Race.ELYOS, 9999));
		assertNull(ClassChangeService.classForSelection(Race.ASMODIANS, 2376));
	}

	@Test
	void validationRejectsCrossBranchAndRepeatedClassChanges() {
		assertTrue(ClassChangeService.isValidClassSwitch(PlayerClass.WARRIOR, PlayerClass.GLADIATOR));
		assertTrue(ClassChangeService.isValidClassSwitch(PlayerClass.TECHNIST, PlayerClass.AETHERTECH));
		assertFalse(ClassChangeService.isValidClassSwitch(PlayerClass.WARRIOR, PlayerClass.SONGWEAVER));
		assertFalse(ClassChangeService.isValidClassSwitch(PlayerClass.SCOUT, PlayerClass.CLERIC));
		assertFalse(ClassChangeService.isValidClassSwitch(PlayerClass.GLADIATOR, PlayerClass.TEMPLAR));
	}
}
