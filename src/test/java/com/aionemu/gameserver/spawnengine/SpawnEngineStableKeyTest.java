package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.objenesis.ObjenesisStd;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class SpawnEngineStableKeyTest {

	@Test
	void assignsEntityKeyBeforeDeterministicStaticFallback() {
		SpawnTemplate entity = SpawnEngine.createSpawnTemplate(300540000, 231130, 1, 2, 3, (byte) 4);
		entity.setEntityId(77);
		SpawnEngine.assignStableKey(entity, 5, 6);

		SpawnTemplate fallback = SpawnEngine.createSpawnTemplate(300540000, 231130, 1, 2, 3, (byte) 4);
		SpawnEngine.assignStableKey(fallback, 5, 6);

		assertEquals("entity:77", entity.getStableKey());
		assertEquals("static:5:6", fallback.getStableKey());
	}

	@Test
	void projectsOnlyGroundNpcSpawnHeight() {
		ObjenesisStd objenesis = new ObjenesisStd();
		Npc npc = objenesis.newInstance(Npc.class);
		SpawnTemplate ground = SpawnEngine.createSpawnTemplate(300040000, 214894, 237.596f, 420.791f, 105, (byte) 24);

		assertEquals(103.8f, SpawnEngine.projectedSpawnZ(npc, ground, ignored -> new float[] {237.75f, 420.75f, 103.8f}));
		assertEquals(105, SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null));

		ground.setFly(1);
		assertEquals(105, SpawnEngine.projectedSpawnZ(npc, ground, ignored -> {
			throw new AssertionError("Flying spawns must not be projected");
		}));
		assertEquals(105, SpawnEngine.projectedSpawnZ(objenesis.newInstance(Gatherable.class), ground, ignored -> {
			throw new AssertionError("Non-NPC spawns must not be projected");
		}));
	}

	@Test
	void resumesInitialSpawnDelayFromInstanceCreationTime() {
		assertEquals(5_000, SpawnEngine.initialSpawnDelayMillis(10, 100_000, 105_000));
		assertEquals(0, SpawnEngine.initialSpawnDelayMillis(10, 100_000, 111_000));
		assertEquals(0, SpawnEngine.initialSpawnDelayMillis(0, 100_000, 100_000));
	}
}
