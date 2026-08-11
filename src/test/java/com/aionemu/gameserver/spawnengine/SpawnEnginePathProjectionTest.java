package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class SpawnEnginePathProjectionTest {

	@Test
	void projectsOnlyGroundNpcSpawnHeight() {
		ObjenesisStd objenesis = new ObjenesisStd();
		Npc npc = objenesis.newInstance(Npc.class);
		SpawnTemplate ground = SpawnEngine.createSpawnTemplate(310010000, 211044, 253, 240, 208.80693f, (byte) 55);

		assertEquals(208.98f,
				SpawnEngine.projectedSpawnZ(npc, ground, ignored -> new float[] {253.25f, 240.25f, 208.98f}));
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null));

		ground.setFly(1);
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(npc, ground, ignored -> {
			throw new AssertionError("Flying spawns must not be projected");
		}));

		ground.setFly(0);
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(objenesis.newInstance(Gatherable.class), ground,
				ignored -> {
					throw new AssertionError("Non-NPC spawns must not be projected");
				}));
	}
}
