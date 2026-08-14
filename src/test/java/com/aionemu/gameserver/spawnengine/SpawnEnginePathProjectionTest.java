package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;

class SpawnEnginePathProjectionTest {

	@Test
	void projectsOnlyMovableGroundNpcSpawnHeight() {
		ObjenesisStd objenesis = new ObjenesisStd();
		TestNpc npc = npc(objenesis, true);
		SpawnTemplate ground = SpawnEngine.createSpawnTemplate(310010000, 211044, 253, 240, 208.80693f, (byte) 55);

		assertEquals(208.98f,
			SpawnEngine.projectedSpawnZ(npc, ground, ignored -> new float[] {253.25f, 240.25f, 208.98f}, ignored -> {
				throw new AssertionError("Terrain fallback must not run after successful PATH projection");
			}));
		assertEquals(104.63f,
			SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null, ignored -> 104.63f));
		assertEquals(ground.getZ(),
			SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null, ignored -> Float.NaN));

		TestNpc interactionObject = npc(objenesis, false);
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(interactionObject, ground, ignored -> {
			throw new AssertionError("Immobile interaction objects must not be PATH-projected");
		}, ignored -> {
			throw new AssertionError("Immobile interaction objects must not use terrain height");
		}));

		ground.setFly(1);
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(npc, ground, ignored -> {
			throw new AssertionError("Flying spawns must not be projected");
		}, ignored -> {
			throw new AssertionError("Flying spawns must not use terrain height");
		}));

		ground.setFly(0);
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(objenesis.newInstance(Gatherable.class), ground,
				ignored -> {
					throw new AssertionError("Non-NPC spawns must not be projected");
				}, ignored -> {
					throw new AssertionError("Non-NPC spawns must not use terrain height");
				}));
	}

	private static TestNpc npc(ObjenesisStd objenesis, boolean moveSupported) {
		TestNpc npc = objenesis.newInstance(TestNpc.class);
		npc.ai = new NpcAI2() {
			@Override
			public boolean isMoveSupported() {
				return moveSupported;
			}
		};
		return npc;
	}

	private static final class TestNpc extends Npc {
		private NpcAI2 ai;

		private TestNpc() {
			super(0, new NpcController(), null, (NpcTemplate) null);
		}

		@Override
		public AI2 getAi2() {
			return ai;
		}
	}
}
