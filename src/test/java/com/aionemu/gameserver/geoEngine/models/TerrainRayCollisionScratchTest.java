package com.aionemu.gameserver.geoEngine.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.collision.CollisionResult;
import com.aionemu.gameserver.geoEngine.collision.CollisionResults;
import com.aionemu.gameserver.geoEngine.math.Ray;
import com.aionemu.gameserver.geoEngine.math.Vector3f;

/**
 * 守护 {@link Terrain#collide} 的线程本地碰撞缓冲：结果必须与逐次分配的实现完全一致，且交点对象在
 * {@code results != null} 时不得被复用破坏。
 * Guards the per-thread collision scratch of {@link Terrain#collide}: results must match the
 * allocate-per-probe implementation, and hit points must not be corrupted by reuse when
 * {@code results != null}.
 */
class TerrainRayCollisionScratchTest {

	/** 平坦地形（16×16 网格，世界 Z 恒为 0）上：射线在 (3.0, 0.6, 0) 与地面相交，距离 2.6。 */
	private static final float EXPECTED_DISTANCE = 2.6f;

	@Test
	void reusedVectorsKeepSingleThreadResultStable() {
		Terrain terrain = flatTerrain();
		CollisionResults results = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, 1);

		assertTrue(terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, results));
		assertEquals(1, results.size());
		assertEquals(EXPECTED_DISTANCE, results.getClosestCollision().getDistance(), 1e-4f);

		// 交替走 null 与 results 两条路径，结果必须逐次一致（scratch 不得把上次的值带进下一次判定）。
		// Alternate the null and results paths; every call must produce the same outcome (no scratch carry-over).
		for (int i = 0; i < 1000; i++) {
			assertTrue(terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, null));
			CollisionResults perCall = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, 1);
			assertTrue(terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, perCall));
			assertEquals(EXPECTED_DISTANCE, perCall.getClosestCollision().getDistance(), 1e-4f);
		}
	}

	@Test
	void hitPointCollectedIntoResultsSurvivesLaterProbes() {
		Terrain terrain = flatTerrain();
		CollisionResults results = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, 1);
		assertTrue(terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, results));

		CollisionResult collected = results.getClosestCollision();
		float x = collected.getContactPoint().x;
		float y = collected.getContactPoint().y;
		float z = collected.getContactPoint().z;

		// results 路径的交点会被 CollisionResult 长期持有，后续探测不得改写它。
		// The collected hit point is retained by a CollisionResult and must not be overwritten by later probes.
		for (int i = 0; i < 1000; i++) {
			assertTrue(terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, null));
		}
		assertEquals(x, collected.getContactPoint().x, 0f);
		assertEquals(y, collected.getContactPoint().y, 0f);
		assertEquals(z, collected.getContactPoint().z, 0f);
	}

	@Test
	void missAndOutOfRangeRaysStayUnchanged() {
		Terrain terrain = flatTerrain();

		// 平行于地表、高于地面的射线不命中。 / A ray parallel to and above the surface must not hit.
		assertFalse(terrain.collide(horizontalRayAboveGround(5.0f), 3.0f, 0.6f, null));
		CollisionResults results = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, 1);
		assertFalse(terrain.collide(horizontalRayAboveGround(5.0f), 3.0f, 0.6f, results));
		assertEquals(0, results.size());

		// 命中距离超过 limit 时仍算未命中（且不写入结果集）。 / A hit beyond the ray limit stays a miss (and is not collected).
		CollisionResults limited = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, 1);
		assertFalse(terrain.collide(descendingRay(2.0f), 3.0f, 0.6f, limited));
		assertEquals(0, limited.size());
	}

	@Test
	void concurrentProbesDoNotLeakBetweenThreads() throws Exception {
		Terrain terrain = flatTerrain();
		int threads = 8;
		int iterations = 500;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			List<Future<String>> futures = new ArrayList<>();
			for (int thread = 0; thread < threads; thread++) {
				futures.add(pool.submit(() -> {
					for (int i = 0; i < iterations; i++) {
						if (!terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, null)) {
							return "null-path miss at iteration " + i;
						}
						CollisionResults results = new CollisionResults(CollisionIntention.DEFAULT_COLLISIONS.getId(), false, 1);
						if (!terrain.collide(descendingRay(5.0f), 3.0f, 0.6f, results)) {
							return "results-path miss at iteration " + i;
						}
						CollisionResult closest = results.getClosestCollision();
						if (closest == null) {
							return "empty results at iteration " + i;
						}
						if (Math.abs(closest.getDistance() - EXPECTED_DISTANCE) > 1e-3f) {
							return "distance " + closest.getDistance() + " at iteration " + i;
						}
						if (terrain.collide(horizontalRayAboveGround(5.0f), 3.0f, 0.6f, null)) {
							return "unexpected hit at iteration " + i;
						}
					}
					return null;
				}));
			}
			for (Future<String> future : futures) {
				assertNull(future.get(60, TimeUnit.SECONDS), "并发探测结果与单线程基线不一致");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	private static Terrain flatTerrain() {
		Terrain terrain = new Terrain();
		terrain.setHeightmap(new short[16 * 16], 16, 16);
		return terrain;
	}

	/** 从 (0.6,0.6,-1) 抬升到 (3.0,0.6,0) 的斜射线。 / Descending ray crossing the surface at (3.0, 0.6). */
	private static Ray descendingRay(float limit) {
		Ray ray = new Ray(new Vector3f(0.6f, 0.6f, -1.0f), new Vector3f(2.4f, 0.0f, 1.0f).normalizeLocal());
		ray.setLimit(limit);
		return ray;
	}

	/** 高于地面、平行于 X 轴的射线。 / Ray above the ground, parallel to the X axis. */
	private static Ray horizontalRayAboveGround(float limit) {
		Ray ray = new Ray(new Vector3f(0.6f, 0.6f, 1.0f), new Vector3f(1.0f, 0.0f, 0.0f));
		ray.setLimit(limit);
		return ray;
	}
}
