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
	void staysEngagedInsideHomeChaseDistanceWhileCombatIsActive() {
		assertFalse(AttackManager.shouldGiveUpByHomeDistance(199, 200, 0, 0));
	}

	@Test
	void defaultMonstersStillGiveUpAfterNoCombatNearHomeLimit() {
		assertTrue(AttackManager.shouldGiveUpByHomeDistance(150, 200, 11, 11));
	}
}
