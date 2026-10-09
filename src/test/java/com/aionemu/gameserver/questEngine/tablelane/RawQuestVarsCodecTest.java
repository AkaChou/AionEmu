package com.aionemu.gameserver.questEngine.tablelane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link RawQuestVarsCodec} 门测试：6/10 位槽算术 + 守卫位 fail-closed。
 * Gate tests for {@link RawQuestVarsCodec}: 6/10-bit slot arithmetic plus guard-bit fail-closed negatives.
 * <p>
 * 锚点取自 P0a 相机矩阵（原版脚本调用点，camera-params.tsv 带文件:行号证据）：
 * 任务 1842（10 位，fullValue 0x450 = 槽1:80 + 槽2:1）、任务 1112 族（6 位，0x145 = 槽1:5 + 槽2:5）。
 */
class RawQuestVarsCodecTest {

	private static final int QUEST_1842_FULL_VALUE = 0x450;
	private static final int QUEST_1112_FULL_VALUE = 0x145;

	@Test
	void decodesRetailEvidenceRows() {
		// 1842（10 位）：fun_763.cpp:958 / fun_760.cpp:643。
		// Quest 1842 (10-bit): retail evidence fun_763.cpp:958 / fun_760.cpp:643.
		assertEquals(80, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.TEN, QUEST_1842_FULL_VALUE, 1));
		assertEquals(1, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.TEN, QUEST_1842_FULL_VALUE, 2));
		assertEquals(0, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.TEN, QUEST_1842_FULL_VALUE, 3));
		// 1112 族（6 位）：0x145 = 槽1:5 + 槽2:5。
		// Quest family 1112 (6-bit): 0x145 = slot1:5 + slot2:5.
		assertEquals(5, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, QUEST_1112_FULL_VALUE, 1));
		assertEquals(5, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, QUEST_1112_FULL_VALUE, 2));
	}

	@Test
	void roundTripsSlotWrites() {
		int vars = RawQuestVarsCodec.withSlotValue(RawQuestVarsCodec.Width.SIX, 0, 1, 63);
		assertEquals(63, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, vars, 1));
		vars = RawQuestVarsCodec.withSlotValue(RawQuestVarsCodec.Width.SIX, vars, 5, 7);
		assertEquals(63, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, vars, 1));
		assertEquals(7, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, vars, 5));
		int tenBit = RawQuestVarsCodec.withSlotValue(RawQuestVarsCodec.Width.TEN, 0, 3, 1000);
		assertEquals(1000, RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.TEN, tenBit, 3));
	}

	@Test
	void rejectsOutOfRangeSlots() {
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, 0, 0));
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, 0, 6));
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.TEN, 0, 4));
	}

	@Test
	void rejectsGuardedAndNegativeVars() {
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.slotValue(RawQuestVarsCodec.Width.SIX, 0x40000000, 1));
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.withSlotValue(RawQuestVarsCodec.Width.SIX, -1, 1, 1));
		assertTrue(RawQuestVarsCodec.guardClear(0));
		assertTrue(RawQuestVarsCodec.guardClear(0x3fffffff));
		assertTrue(!RawQuestVarsCodec.guardClear(0x40000000));
	}

	@Test
	void rejectsValuesOverSlotMask() {
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.withSlotValue(RawQuestVarsCodec.Width.SIX, 0, 1, 64));
		assertThrows(IllegalArgumentException.class,
				() -> RawQuestVarsCodec.withSlotValue(RawQuestVarsCodec.Width.TEN, 0, 1, 1024));
	}
}
