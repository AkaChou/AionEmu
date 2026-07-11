package com.aionemu.gameserver.geoEngine.scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class NavGeometryTest {

	@Test
	void findsClosestPointAcrossTriangleRegions() {
		NavGeometry flat = new NavGeometry("flat", new float[] {0, 0, 0, 2, 0, 0, 0, 2, 0});

		assertArrayEquals(new float[] {0.5F, 0.5F, 0}, flat.getClosestPoint(0.5F, 0.5F, 3), 0.0001F);
		assertArrayEquals(new float[] {1, 0, 0}, flat.getClosestPoint(1, -1, 2), 0.0001F);
		assertArrayEquals(new float[] {0, 0, 0}, flat.getClosestPoint(-1, -1, 2), 0.0001F);

		NavGeometry sloped = new NavGeometry("sloped", new float[] {0, 0, 0, 2, 0, 0, 0, 2, 2});
		assertArrayEquals(new float[] {0.5F, 1.25F, 1.25F}, sloped.getClosestPoint(0.5F, 0.5F, 2), 0.0001F);
	}
}
