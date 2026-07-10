package com.aionemu.gameserver.ai2.manager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AttackManagerLeashTest {

	@Test
	void givesUpImmediatelyAfterMovingBeyondHomeChaseDistance() {
		assertTrue(AttackManager.shouldGiveUpByHomeDistance(201, 200, 0, 0));
	}

	@Test
	void bossGivesUpBeyondBossHomeChaseDistance() {
		assertTrue(AttackManager.shouldGiveUpByHomeDistance(151, 150, 0, 0));
	}

	@Test
	void staysEngagedInsideHomeChaseDistanceWhileCombatIsActive() {
		assertFalse(AttackManager.shouldGiveUpByHomeDistance(199, 200, 0, 0));
	}

	@Test
	void defaultMonstersStillGiveUpAfterNoCombatNearHomeLimit() {
		assertTrue(AttackManager.shouldGiveUpByHomeDistance(50, 200, 21, 21));
	}

	@Test
	void givesUpPastHalfHomeDistanceAfterTenSecondsWithoutBeingHit() {
		assertTrue(AttackManager.shouldGiveUpByHomeDistance(76, 150, 0, 11));
	}
}
