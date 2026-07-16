package com.aionemu.gameserver.geoEngine.bounding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

class BoundingBoxRayIntersectionTest {

	private final BoundingBox box = new BoundingBox(new Vector3f(0, 0, 0), 1, 1, 1);

	@Test
	void detectsForwardAndInsideRayIntersections() {
		assertTrue(box.intersects(ray(-2, 0, 0, 1, 0, 0)));
		assertTrue(box.intersects(ray(0, 0, 0, 0, 1, 0)));
		assertTrue(box.intersects(ray(-2, -2, 0, 1, 1, 0)));
	}

	@Test
	void rejectsRaysPointingAwayOrPassingOutsideTheBox() {
		assertFalse(box.intersects(ray(-2, 0, 0, -1, 0, 0)));
		assertFalse(box.intersects(ray(-2, 2, 0, 1, 0, 0)));
		assertFalse(box.intersects(ray(-2, -2, 2, 1, 1, 0)));
	}

	private static Ray ray(float originX, float originY, float originZ, float directionX, float directionY,
			float directionZ) {
		return new Ray(new Vector3f(originX, originY, originZ), new Vector3f(directionX, directionY, directionZ));
	}
}
