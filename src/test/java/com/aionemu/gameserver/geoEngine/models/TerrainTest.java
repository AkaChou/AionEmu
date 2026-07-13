package com.aionemu.gameserver.geoEngine.models;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerrainTest {

	@Test
	void pathHeightPreservesMissingTerrainSentinel() {
		Terrain terrain = new Terrain();
		terrain.setHeightmap(new short[] {0, -1, 0, 0}, 2, 2);

		assertTrue(Float.isNaN(terrain.getPathHeight(0.5f, 0.5f)));
	}
}
