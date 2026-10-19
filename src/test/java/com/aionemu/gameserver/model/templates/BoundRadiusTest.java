package com.aionemu.gameserver.model.templates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 边界半径两套约定的回归闸门。
 * Regression gate for the two bound-radius conventions.
 */
class BoundRadiusTest {

	@Test
	void radiusConventionKeepsGeometricMeanCollision() {
		BoundRadius bound = new BoundRadius(0.5f, 0.5f, 2f);

		assertFalse(bound.isFullLength());
		assertEquals(0.5f, bound.getCollision(), 1e-6);
		assertEquals(0.5f, bound.getHalfFront(), 1e-6);
		assertEquals(0.5f, bound.getHalfSide(), 1e-6);
		assertEquals(0.5f, bound.getHalfMaxHorizontal(), 1e-6);
	}

	@Test
	void fullLengthConventionUsesHalfOfLargestHorizontalAxis() {
		BoundRadius bound = new BoundRadius(0.5f, 0.7f, 2f, true);

		assertTrue(bound.isFullLength());
		assertEquals(0.35f, bound.getCollision(), 1e-6);
		assertEquals(0.25f, bound.getHalfFront(), 1e-6);
		assertEquals(0.35f, bound.getHalfSide(), 1e-6);
		assertEquals(0.35f, bound.getHalfMaxHorizontal(), 1e-6);
	}
}
