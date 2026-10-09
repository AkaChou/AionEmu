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

	/**
	 * 打包值是 32 位整数（SM_QUEST_LIST/SM_QUEST_ACTION 用 writeD 下发、player_quests 存 INT 列），
	 * 因此槽 0..4 各占 6 bit，第 5 槽只剩 bit30..31 → 只能表示 0..3。
	 * 原版生产 XML 也只在 var5 上声明 1 bit 标志（全库 6 个任务，max=1），本用例锁定该可表示区间。
	 * The packed value is a 32-bit int (writeD on the wire and an INT column in player_quests), so slots 0..4
	 * own bits 0..29 and slot 5 keeps only bits 30..31. Production XML only uses var5 as a 1-bit flag.
	 */
	@Test
	void packsAndUnpacksEverySixBitSectionTheWireFormatCanCarry() {
		QuestVars vars = new QuestVars();
		vars.setVarById(0, 63);
		vars.setVarById(1, 2);
		vars.setVarById(2, 3);
		vars.setVarById(3, 4);
		vars.setVarById(4, 5);
		vars.setVarById(5, 3);

		QuestVars unpacked = new QuestVars(vars.getQuestVars());
		assertEquals(63, unpacked.getVarById(0));
		assertEquals(2, unpacked.getVarById(1));
		assertEquals(3, unpacked.getVarById(2));
		assertEquals(4, unpacked.getVarById(3));
		assertEquals(5, unpacked.getVarById(4));
		assertEquals(3, unpacked.getVarById(5));
	}

	/**
	 * 第 5 槽的高位在 32 位打包里被截断：写入 4（需要 bit32）回读为 0，与原版 32 位包一致。
	 * Slot 5 bits beyond bit31 are truncated by the 32-bit packing, matching the retail 32-bit wire value.
	 */
	@Test
	void fifthSlotValueBeyondTwoBitsIsDroppedByTheWireFormat() {
		QuestVars vars = new QuestVars();
		// 4 = 0b100 需要 bit32；32 位打包把它整段丢弃，回读为 0。
		// 4 needs bit32, which the 32-bit packed value cannot carry, so it reads back as zero.
		vars.setVarById(5, 4);
		assertEquals(0, new QuestVars(vars.getQuestVars()).getVarById(5));
		vars.setVarById(5, 1);
		assertEquals(1, new QuestVars(vars.getQuestVars()).getVarById(5));
	}

	/** 无参构造 + 部分赋值不得因为未赋值的槽位抛 NPE。 / Partial population must not throw. */
	@Test
	void unsetSlotsReadAsZero() {
		QuestVars vars = new QuestVars();
		vars.setVarById(2, 7);
		assertEquals(0, vars.getVarById(0));
		QuestVars unpacked = new QuestVars(vars.getQuestVars());
		assertEquals(7, unpacked.getVarById(2));
		assertEquals(0, unpacked.getVarById(5));
	}
}
