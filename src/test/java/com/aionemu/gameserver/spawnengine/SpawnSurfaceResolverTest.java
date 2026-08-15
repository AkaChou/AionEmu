package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class SpawnSurfaceResolverTest {

	@Test
	void resolvesRetailReferenceZByGeoThenCachesTheResult() {
		SpawnTemplate spawn = SpawnEngine.createSpawnTemplate(210010000, 700008, 483.675537f, 1544.752441f,
			114.441620f, (byte) 0);
		spawn.setResolveZ(true);
		AtomicInteger geoCalls = new AtomicInteger();

		float first = SpawnSurfaceResolver.resolve(spawn, () -> {
			geoCalls.incrementAndGet();
			return 108.88557f;
		}, () -> {
			throw new AssertionError("PATH must not run after successful GEO projection");
		}, () -> {
			throw new AssertionError("PNG must not run after successful GEO projection");
		});
		float second = SpawnSurfaceResolver.resolve(spawn, () -> {
			throw new AssertionError("Resolved spawn height must be cached");
		}, () -> null, () -> Float.NaN);

		assertEquals(108.88557f, first);
		assertEquals(first, second);
		assertEquals(first, spawn.getEffectiveZ());
		assertEquals(1, geoCalls.get());
	}

	@Test
	void fallsBackFromGeoToPathToPngAndFinallyReferenceZ() {
		SpawnTemplate path = spawn();
		assertEquals(109f, SpawnSurfaceResolver.resolve(path, () -> Float.NaN,
			() -> new float[] {1, 2, 109}, () -> {
				throw new AssertionError("PNG must not run after successful PATH projection");
			}));

		SpawnTemplate png = spawn();
		assertEquals(108f, SpawnSurfaceResolver.resolve(png, () -> Float.NaN, () -> null, () -> 108f));

		SpawnTemplate reference = spawn();
		assertEquals(reference.getZ(),
			SpawnSurfaceResolver.resolve(reference, () -> Float.NaN, () -> null, () -> Float.NaN));
	}

	@Test
	void preservesUnmarkedAndFlyingSpawnHeights() {
		SpawnTemplate unmarked = SpawnEngine.createSpawnTemplate(210010000, 700008, 1, 2, 3, (byte) 0);
		assertEquals(3f, SpawnSurfaceResolver.resolve(unmarked, () -> {
			throw new AssertionError("Unmarked spawns must not be projected");
		}, () -> null, () -> Float.NaN));

		SpawnTemplate flying = spawn();
		flying.setFly(1);
		assertEquals(flying.getZ(), SpawnSurfaceResolver.resolve(flying, () -> {
			throw new AssertionError("Flying spawns must not be projected");
		}, () -> null, () -> Float.NaN));
	}

	private static SpawnTemplate spawn() {
		SpawnTemplate spawn = SpawnEngine.createSpawnTemplate(210010000, 700008, 1, 2, 114.441620f, (byte) 0);
		spawn.setResolveZ(true);
		return spawn;
	}
}
