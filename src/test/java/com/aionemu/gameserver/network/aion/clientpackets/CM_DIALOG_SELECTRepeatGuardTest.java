package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.player.DialogSelectRepeat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 CM_DIALOG_SELECT 的重发跟踪只对「同一目标、同一上一页、同一动作、同一任务」的连续重发计数，
 * 并在达到阈值时判定为需要打断的死循环。
 * Verifies that CM_DIALOG_SELECT repeat tracking only counts consecutive resends of the same target,
 * previous page, action, and quest id, and reports a breakable loop once the threshold is reached.
 */
class CM_DIALOG_SELECTRepeatGuardTest {
	private static final int TARGET_OBJECT = 124836;
	private static final int LAST_PAGE = 1012;
	private static final int ACTION = 1013;
	private static final int QUEST = 14045;

	@Test
	void countsIdenticalResendsUpToTheLoopThreshold() {
		DialogSelectRepeat state = null;
		long now = 10_000;
		for (int repeats = 1; repeats <= CM_DIALOG_SELECT.MAX_IDENTICAL_DIALOG_SELECTS; repeats++) {
			state = CM_DIALOG_SELECT.nextDialogSelectRepeat(state, TARGET_OBJECT, LAST_PAGE, ACTION, QUEST, now);
			assertEquals(repeats, state.count());
			assertEquals(repeats >= CM_DIALOG_SELECT.MAX_IDENTICAL_DIALOG_SELECTS,
				state.count() >= CM_DIALOG_SELECT.MAX_IDENTICAL_DIALOG_SELECTS);
			now += 1_000;
		}
	}

	@Test
	void restartsWheneverTheSelectionSignatureChanges() {
		DialogSelectRepeat previous = CM_DIALOG_SELECT.nextDialogSelectRepeat(null, TARGET_OBJECT, LAST_PAGE,
			ACTION, QUEST, 1_000);
		assertEquals(1, previous.count());

		assertEquals(1, CM_DIALOG_SELECT.nextDialogSelectRepeat(previous, TARGET_OBJECT, LAST_PAGE, ACTION,
			QUEST + 1, 1_100).count());
		assertEquals(1, CM_DIALOG_SELECT.nextDialogSelectRepeat(previous, TARGET_OBJECT, LAST_PAGE, ACTION + 1,
			QUEST, 1_100).count());
		assertEquals(1, CM_DIALOG_SELECT.nextDialogSelectRepeat(previous, TARGET_OBJECT, LAST_PAGE + 1, ACTION,
			QUEST, 1_100).count());
		assertEquals(1, CM_DIALOG_SELECT.nextDialogSelectRepeat(previous, TARGET_OBJECT + 1, LAST_PAGE, ACTION,
			QUEST, 1_100).count());
	}

	@Test
	void restartsAfterTheRepeatWindowExpires() {
		DialogSelectRepeat previous = CM_DIALOG_SELECT.nextDialogSelectRepeat(null, TARGET_OBJECT, LAST_PAGE,
			ACTION, QUEST, 1_000);

		DialogSelectRepeat late = CM_DIALOG_SELECT.nextDialogSelectRepeat(previous, TARGET_OBJECT, LAST_PAGE,
			ACTION, QUEST, 1_000 + CM_DIALOG_SELECT.DIALOG_SELECT_REPEAT_WINDOW_MILLIS + 1);

		assertEquals(1, late.count());
	}

	@Test
	void rapidClicksNeverCountAsLoopResends() {
		DialogSelectRepeat state = null;
		long now = 20_000;
		for (int index = 0; index < 10; index++) {
			state = CM_DIALOG_SELECT.nextDialogSelectRepeat(state, TARGET_OBJECT, LAST_PAGE, ACTION, QUEST, now);
			assertEquals(1, state.count());
			now += CM_DIALOG_SELECT.MIN_DIALOG_SELECT_RESEND_GAP_MILLIS - 100;
		}
	}

	@Test
	void alternatingSelectionsNeverReachTheThreshold() {
		DialogSelectRepeat state = null;
		long now = 5_000;
		for (int index = 0; index < 10; index++) {
			state = CM_DIALOG_SELECT.nextDialogSelectRepeat(state, TARGET_OBJECT, LAST_PAGE, ACTION + index % 2,
				QUEST, now);
			assertTrue(state.count() < CM_DIALOG_SELECT.MAX_IDENTICAL_DIALOG_SELECTS);
			now += 500;
		}
	}
}
