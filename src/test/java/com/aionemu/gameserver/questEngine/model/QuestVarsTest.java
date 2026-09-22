package com.aionemu.gameserver.questEngine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证任务子变量的 6 位边界、索引校验与打包契约。
 * Verifies six-bit quest sub-variable bounds, index validation and packing contract.
 */
class QuestVarsTest {

	@Test
	void acceptsOnlySixVariableSlots() {
		assertTrue(QuestVars.isValidVarId(0));
		assertTrue(QuestVars.isValidVarId(5));
		assertFalse(QuestVars.isValidVarId(-1));
		assertFalse(QuestVars.isValidVarId(6));
		assertFalse(QuestVars.isValidVarId(20));
	}

	@Test
	void acceptsOnlySixBitVariableValues() {
		assertTrue(QuestVars.isValidVarValue(0));
		assertTrue(QuestVars.isValidVarValue(63));
		assertFalse(QuestVars.isValidVarValue(-1));
		assertFalse(QuestVars.isValidVarValue(64));
	}

	@Test
	void rejectsOutOfRangeIndexBeforeArrayAccess() {
		QuestVars vars = new QuestVars();

		assertThrows(IllegalArgumentException.class, () -> vars.getVarById(20));
		assertThrows(IllegalArgumentException.class, () -> vars.setVarById(20, 1));
	}

	@Test
	void rejectsOutOfRangeValueForSingleVariableUpdate() {
		QuestVars vars = new QuestVars();

		assertThrows(IllegalArgumentException.class, () -> vars.setVarById(0, -1));
		assertThrows(IllegalArgumentException.class, () -> vars.setVarById(0, 64));
	}

	@Test
	void packsAndUnpacksEverySixBitSection() {
		QuestVars vars = new QuestVars();
		vars.setVarById(0, 1);
		vars.setVarById(1, 2);
		vars.setVarById(2, 3);
		vars.setVarById(3, 4);
		vars.setVarById(4, 5);
		vars.setVarById(5, 6);

		QuestVars unpacked = new QuestVars(vars.getQuestVars());
		assertEquals(1, unpacked.getVarById(0));
		assertEquals(2, unpacked.getVarById(1));
		assertEquals(3, unpacked.getVarById(2));
		assertEquals(4, unpacked.getVarById(3));
		assertEquals(5, unpacked.getVarById(4));
		assertEquals(6, unpacked.getVarById(5));
	}
}
