package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttackManagerLeashTest {

	@Test
	void numericRetailChaseTimeRefreshesAfterEachAttack() {
		assertFalse(AttackManager.shouldStopTimedChase(1_000, 0, 5, 5_999));
		assertTrue(AttackManager.shouldStopTimedChase(1_000, 0, 5, 6_000));
		assertFalse(AttackManager.shouldStopTimedChase(1_000, 5_000, 5, 9_999));
		assertTrue(AttackManager.shouldStopTimedChase(1_000, 5_000, 5, 10_000));
		assertFalse(AttackManager.shouldStopTimedChase(1_000, 5_000, 0, 20_000));
	}

	@Test
	void spawnPointChaseChecksEveryTwoSeconds() {
		assertFalse(AttackManager.shouldCheckSpawnPointChase(0, 1_000));
		assertFalse(AttackManager.shouldCheckSpawnPointChase(1_000, 2_999));
		assertTrue(AttackManager.shouldCheckSpawnPointChase(1_000, 3_000));
	}

	@Test
	void spawnPointChaseStopsOnRetailThirtyOnePercentBand() {
		assertFalse(AttackManager.shouldStopSpawnPointChase(69));
		assertTrue(AttackManager.shouldStopSpawnPointChase(70));
		assertTrue(AttackManager.shouldStopSpawnPointChase(100));
	}
}
