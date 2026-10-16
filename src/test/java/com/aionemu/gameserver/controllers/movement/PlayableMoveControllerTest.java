package com.aionemu.gameserver.controllers.movement;

import com.aionemu.gameserver.controllers.movement.PlayableMoveController.RetailMoveStateTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 真端 MoveState 双状态滞后语义回归（666ms 生效延迟 / 1332ms 保留窗口 / 位或合并）。
 * Regression for the retail dual-state MoveState hysteresis (666 ms effect delay, 1332 ms retain
 * window, bitwise merge).
 */
class PlayableMoveControllerTest {

	private static final long T0 = 1_000_000L;

	@Test
	void delaysNewDirectionUntilEffectDelayElapses() {
		RetailMoveStateTracker tracker = new RetailMoveStateTracker();
		tracker.record(1, T0);
		assertEquals(0, tracker.current(T0 + 600));
		assertEquals(1, tracker.current(T0 + 667));
	}

	@Test
	void keepsPreviousDirectionAliveDuringRetainWindow() {
		RetailMoveStateTracker tracker = new RetailMoveStateTracker();
		tracker.record(1, T0);
		tracker.record(2, T0 + 700);
		// 新方向尚未生效、旧方向仍在保留窗口：读到旧方向。
		// New direction not yet effective, previous one still retained: read stays on the old one.
		assertEquals(1, tracker.current(T0 + 800));
		// 新方向生效且旧方向未过期：位或组合（前进|后退=3）。
		// New direction effective while the old one is retained: bitwise combine (forward|back=3).
		assertEquals(3, tracker.current(T0 + 1367));
		// 旧方向保留期过后只剩新方向。
		// Once the retain window lapses only the new direction remains.
		assertEquals(2, tracker.current(T0 + 2100));
	}

	@Test
	void hardStopKeepsDirectionForRetainWindow() {
		RetailMoveStateTracker tracker = new RetailMoveStateTracker();
		tracker.record(1, T0);
		tracker.record(0, T0 + 800);
		// 急停后旧方向仍参与判定（真端写入 0 但保留态存活）。
		// After a hard stop the previous direction still counts (retail writes 0 but keeps the retained state).
		assertEquals(1, tracker.current(T0 + 900));
		assertEquals(0, tracker.current(T0 + 2200));
	}

	@Test
	void rewritingSameStateDoesNotRefreshTimestamps() {
		RetailMoveStateTracker tracker = new RetailMoveStateTracker();
		tracker.record(1, T0);
		tracker.record(1, T0 + 500);
		// 时间戳未被刷新：666ms 从首次写入起算。
		// Timestamps were not refreshed: the 666 ms clock started at the first write.
		assertEquals(1, tracker.current(T0 + 667));
	}
}
