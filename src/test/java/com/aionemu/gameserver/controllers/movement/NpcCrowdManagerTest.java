package com.aionemu.gameserver.controllers.movement;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NpcCrowdManagerTest {

	@AfterEach
	void clearCrowd() {
		NpcCrowdManager.clear();
	}

	@Test
	void predictsHeadOnCollisionWithinRetailLookAhead() {
		assertEquals(0, NpcCrowdManager.predictedDistance(0, 0, 0, 1, 0, 0, 10, 0, 0, -1, 0, 0, 100, 100), 0.001f);
	}

	@Test
	void separatesParallelFormationMovement() {
		float distance = NpcCrowdManager.predictedDistance(0, 0, 0, 1, 0, 0, 0, 2, 0, 1, 0, 0, 100, 100);

		assertTrue(distance >= 2);
	}

	@Test
	void normalizesDisplacementsFromDifferentFrameDurations() {
		float distance = NpcCrowdManager.predictedDistance(1, 0, 0, 1, 0, 0,
				0, 0.5f, 0, 2.5f, 0, 0, 100, 250);

		assertEquals(Math.sqrt(1.25), distance, 0.001f);
	}

	@Test
	void discoversAgentsInAdjacentSpatialBucketsWithoutKnownList() {
		long now = 1000;
		NpcCrowdManager.Agent first = new NpcCrowdManager.Agent(10, 1, 1, 4.2f, 0, 0, 0.5f);
		NpcCrowdManager.Agent second = new NpcCrowdManager.Agent(20, 1, 1, 3.8f, 0, 0, 0.5f);
		NpcCrowdManager.choose(first, 3.2f, 0, 0, (x, y, z) -> true, now, 100);

		float[] step = NpcCrowdManager.choose(second, 4.8f, 0, 0, (x, y, z) -> true, now, 100);

		assertTrue(step == null || Math.abs(step[1]) > 0.1f || step[0] < 4.8f);
	}

	@Test
	void keepsInstancesIndependent() {
		long now = 1000;
		NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 2, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);

		float[] step = NpcCrowdManager.choose(new NpcCrowdManager.Agent(20, 1, 2, 0, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);

		assertArrayEquals(new float[] {1, 0, 0}, step);
	}

	@Test
	void scansLocalDirectionsWhenTheStraightMoveWouldCollide() {
		long now = 1000;
		NpcCrowdManager.choose(new NpcCrowdManager.Agent(20, 1, 1, 1, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);

		float[] step = NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);

		assertNotNull(step);
		assertEquals(1, Math.hypot(step[0], step[1]), 0.001f);
		assertTrue(Math.abs(step[1]) > 0.1f);
	}

	@Test
	void keepsDirectSlopeMovementStableWhenCrowded() {
		long now = 1000;
		NpcCrowdManager.choose(new NpcCrowdManager.Agent(20, 1, 1, 1, 0, -1, 0.5f), 1, 0, -1,
				(x, y, z) -> true, now, 100);

		float[] step = NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 1, 0, -1,
				(x, y, z) -> true, now, 100);

		assertNotNull(step);
		assertArrayEquals(new float[] {1, 0, -1}, step);
	}

	@Test
	void removesExpiredAgent() {
		long now = 1000;
		NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);
		assertEquals(1, NpcCrowdManager.agentCount(1, 1));

		NpcCrowdManager.cleanup(now + 6000);

		assertEquals(0, NpcCrowdManager.agentCount(1, 1));
	}

	@Test
	void keepsAvoidanceWithinCurrentMoveBudget() {
		float[] step = NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 0.2f, 0, 0,
				(x, y, z) -> Math.abs(y) > 0.01f, 1000, 100);

		assertEquals(0.2f, Math.hypot(step[0], step[1]), 0.001f);
	}

	@Test
	void doesNotSerializeGeoChecksForAllNpcsInAnInstance() throws Exception {
		CountDownLatch firstCheckingGeo = new CountDownLatch(1);
		CountDownLatch releaseFirst = new CountDownLatch(1);
		CompletableFuture<float[]> first = CompletableFuture.supplyAsync(() -> NpcCrowdManager.choose(
				new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 1, 0, 0, (x, y, z) -> {
					firstCheckingGeo.countDown();
					try {
						return releaseFirst.await(2, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return false;
					}
				}, 1000, 100));
		assertTrue(firstCheckingGeo.await(1, TimeUnit.SECONDS));
		try {
			CompletableFuture<float[]> second = CompletableFuture.supplyAsync(() -> NpcCrowdManager.choose(
					new NpcCrowdManager.Agent(20, 1, 1, 10, 0, 0, 0.5f), 11, 0, 0,
					(x, y, z) -> true, 1000, 100));
			assertNotNull(second.get(1, TimeUnit.SECONDS));
		} finally {
			releaseFirst.countDown();
			assertNotNull(first.get(1, TimeUnit.SECONDS));
		}
	}

	@Test
	void checksPassabilityOnlyForCollisionFreeCandidates() {
		long now = 1000;
		// 先占位一个邻居，迫使直达碰撞，只应探测偏转候选。
		NpcCrowdManager.choose(new NpcCrowdManager.Agent(20, 1, 1, 1, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);
		int[] checks = {0};
		float[] step = NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> {
					checks[0]++;
					return Math.abs(y) > 0.01f;
				}, now, 100);
		assertNotNull(step);
		assertTrue(checks[0] >= 1);
		assertTrue(checks[0] < 12, "passability should stop after first free candidate, checks=" + checks[0]);
		assertTrue(Math.abs(step[1]) > 0.1f);
	}

	@Test
	void fallsBackToPassableDirectStepWhenCrowded() {
		long now = 1000;
		NpcCrowdManager.choose(new NpcCrowdManager.Agent(20, 1, 1, 0, 0, 0, 10), 1, 0, 0,
				(x, y, z) -> true, now, 100);

		float[] step = NpcCrowdManager.choose(new NpcCrowdManager.Agent(10, 1, 1, 0, 0, 0, 0.5f), 1, 0, 0,
				(x, y, z) -> true, now, 100);

		assertArrayEquals(new float[] {1, 0, 0}, step);
	}

}
