package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 固化 {@link PlayerRepeatedAbnormalStatus} 的链语义：惰性分配、闭区间窗口、步数封顶与按状态独立。
 * Pins the {@link PlayerRepeatedAbnormalStatus} chain semantics: lazy allocation, inclusive window
 * boundary, step ceiling and per-state independence.
 */
class PlayerRepeatedAbnormalStatusTest {

	private final PlayerRepeatedAbnormalStatus status = new PlayerRepeatedAbnormalStatus();

	@Test
	void backingArraysStayUnallocatedUntilFirstWrite() {
		assertFalse(status.isTracked());
		assertEquals(0, status.currentStep(0, 1_000, 3_000));

		status.record(0, 3, 1_000);

		assertTrue(status.isTracked());
	}

	@Test
	void absorbsFirstHitThenStartsTheChain() {
		status.record(0, 1, 1_000);

		assertEquals(1, status.currentStep(0, 1_000, 3_000));
	}

	@Test
	void keepsStepAtWindowBoundaryAndResetsJustOutside() {
		status.record(0, 1, 1_000);

		// 窗口为闭区间：now == last + window 仍算链内 / inclusive boundary: now == last + window stays chained
		assertEquals(1, status.currentStep(0, 4_000, 3_000));
		assertEquals(0, status.currentStep(0, 4_001, 3_000));
	}

	@Test
	void neverHitStateReadsAsFreshChain() {
		status.record(0, 4, 1_000);

		assertEquals(0, status.currentStep(1, 1_000, 3_000));
	}

	@Test
	void capsStepAtRetailCeiling() {
		status.record(0, 9, 1_000);

		assertEquals(PlayerRepeatedAbnormalStatus.MAX_STEP, status.currentStep(0, 1_000, 3_000));
	}

	@Test
	void clampsStepIntoRetailRange() {
		status.record(0, 0, 1_000);

		assertEquals(1, status.currentStep(0, 1_000, 3_000));
	}

	@Test
	void keepsStatesIndependent() {
		status.record(0, 2, 1_000);
		status.record(2, 4, 1_000);

		assertEquals(2, status.currentStep(0, 1_000, 3_000));
		assertEquals(0, status.currentStep(1, 1_000, 3_000));
		assertEquals(4, status.currentStep(2, 1_000, 3_000));
	}

	@Test
	void clearReleasesTheChain() {
		status.record(0, 1, 1_000);

		status.clear();

		assertFalse(status.isTracked());
		assertEquals(0, status.currentStep(0, 1_000, 3_000));
	}

	@Test
	void negativeIndexIsIgnored() {
		status.record(-1, 3, 1_000);

		assertFalse(status.isTracked());
		assertEquals(0, status.currentStep(-1, 1_000, 3_000));
	}
}
