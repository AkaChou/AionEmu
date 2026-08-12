package com.aionemu.gameserver.geoEngine.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.scene.Geometry;
import com.aionemu.gameserver.geoEngine.scene.Mesh;
import com.aionemu.gameserver.geoEngine.scene.VertexBuffer.Type;

class GeoMapWalkerCollisionTest {

	@Test
	void raisesWalkerRayAboveMinorGroundHeightMismatch() {
		GeoMap map = mapWithQuad(new float[] {
				0, -1, -0.12f,
				4, -1, -0.12f,
				4, 1, -0.08f,
				0, 1, -0.08f
		});

		assertFalse(map.canPass(0, 0.25f, 0, 4, 0.25f, -0.2f, 5, 1));
		assertTrue(map.canPassWalker(0, 0.25f, 0, 4, 0.25f, -0.2f, 5, 1));
	}

	@Test
	void raisedWalkerRayStillDetectsWalls() {
		GeoMap map = mapWithQuad(new float[] {
				2, -1, -1,
				2, 1, -1,
				2, 1, 2,
				2, -1, 2
		});

		assertFalse(map.canPassWalker(0, 0, 0, 4, 0, 0, 4, 1));
	}

	private static GeoMap mapWithQuad(float[] positions) {
		Mesh mesh = new Mesh();
		mesh.setBuffer(Type.Position, 3, positions);
		mesh.setBuffer(Type.Index, 3, new int[] {0, 1, 2, 0, 2, 3});
		mesh.setCollisionFlags((short) (CollisionIntention.PHYSICAL.getId() << 8));

		Geometry geometry = new Geometry("obstacle", mesh);
		geometry.updateModelBound();
		GeoMap map = new GeoMap("1", 256);
		map.attachChild(geometry);
		map.updateModelBound();
		return map;
	}
}
