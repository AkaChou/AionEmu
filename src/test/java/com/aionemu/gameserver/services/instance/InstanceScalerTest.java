package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InstanceScalerTest {

	@Test
	void scalesByPlayerCountWithoutGoingBelowFloor() {
		assertEquals(0.5f, InstanceScaler.calculateMultiplier(6, 0.5f, 1));
		assertEquals(2f / 3f, InstanceScaler.calculateMultiplier(6, 0.5f, 4));
		assertEquals(1f, InstanceScaler.calculateMultiplier(6, 0.5f, 8));
	}

	@Test
	void updatesWhenPlayerCountDecreases() {
		InstanceScaler.Scaling scaling = new InstanceScaler.Scaling();

		assertTrue(scaling.update(6, 6));
		assertTrue(scaling.update(6, 1));
		assertFalse(scaling.update(6, 1));
	}
}
