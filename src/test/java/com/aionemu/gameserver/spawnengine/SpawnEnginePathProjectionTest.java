package com.aionemu.gameserver.spawnengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.ai2.AI2;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.model.NpcType;
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

	@Test
	void fallsBackToGeoSurfaceWhenPathAndTerrainAreUnavailable() {
		ObjenesisStd objenesis = new ObjenesisStd();
		TestNpc npc = npc(objenesis, true);
		SpawnTemplate ground = SpawnEngine.createSpawnTemplate(301550000, 282006, 700, 800, 137.5f, (byte) 0);

		assertEquals(130.25f, SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null,
			ignored -> Float.NaN, ignored -> 130.25f));
		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null,
			ignored -> Float.NaN, ignored -> Float.NaN));
		assertEquals(104.63f, SpawnEngine.projectedSpawnZ(npc, ground, ignored -> null,
			ignored -> 104.63f, ignored -> {
				throw new AssertionError("Terrain height must win over the geo surface fallback");
			}));
	}

	@Test
	void keepsAuthoredZForPlacedObjects() throws ReflectiveOperationException {
		ObjenesisStd objenesis = new ObjenesisStd();
		TestNpc generator = npc(objenesis, true);
		NpcTemplate template = new NpcTemplate();
		Field npcType = NpcTemplate.class.getDeclaredField("npcType");
		npcType.setAccessible(true);
		npcType.set(template, NpcType.NON_ATTACKABLE);
		generator.template = template;
		SpawnTemplate ground = SpawnEngine.createSpawnTemplate(400010000, 260209, 1526.6611f, 1563.0392f, 2332.4578f,
			(byte) 0);

		assertEquals(ground.getZ(), SpawnEngine.projectedSpawnZ(generator, ground, ignored -> null,
			ignored -> Float.NaN, ignored -> {
				throw new AssertionError("Placed objects must keep the authored Z instead of the geo surface");
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
		private NpcTemplate template;

		private TestNpc() {
			super(0, new NpcController(), null, null);
		}

		@Override
		public AI2 getAi2() {
			return ai;
		}

		@Override
		public NpcTemplate getObjectTemplate() {
			return template;
		}
	}
}
