package com.aionemu.gameserver.model.geometry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RectangleAreaTest {

	@Test
	void detectsThreeDimensionalRectangleIntersections() {
		RectangleArea area = rectangle(0, 0, 10, 10, 0, 10);

		assertTrue(area.intersectsRectangle(rectangle(5, 5, 15, 15, 5, 15)));
		assertTrue(area.intersectsRectangle(rectangle(10, 10, 20, 20, 10, 20)));
		assertFalse(area.intersectsRectangle(rectangle(11, 0, 20, 10, 0, 10)));
		assertFalse(area.intersectsRectangle(rectangle(0, 0, 10, 10, 11, 20)));
	}

	private static RectangleArea rectangle(float minX, float minY, float maxX, float maxY, float minZ, float maxZ) {
		return new RectangleArea(null, 0, minX, minY, maxX, maxY, minZ, maxZ);
	}
}
