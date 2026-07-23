package com.aionemu.gameserver.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

class RetailPatternRuntimeStateTest {

	@Test
	void removesPersistedDynamicSpawnWhenNpcLeavesItsLifecycle() {
		TestWorldMapInstance instance = new ObjenesisStd().newInstance(TestWorldMapInstance.class);
		String key = "retail.pattern.ai.static:1:2.spawn.on_wake_up.0.0.1.0.";
		instance.getRuntimeState().put(key + "npc", 231130);
		instance.getRuntimeState().put(key + "x", 1);
		SpawnTemplate spawn = new ObjenesisStd().newInstance(SpawnTemplate.class);
		spawn.setRuntimeLifecycleKey(key);

		TestNpc npc = new ObjenesisStd().newInstance(TestNpc.class);
		npc.spawn = spawn;
		npc.position = new TestWorldPosition(instance);
		RetailPatternAI2.onDynamicSpawnRemoved(npc);

		assertTrue(instance.getRuntimeState().snapshot(key).isEmpty());
	}

	private static final class TestNpc extends Npc {
		private SpawnTemplate spawn;
		private WorldPosition position;

		private TestNpc() {
			super(0, null, null, null);
		}

		@Override
		public SpawnTemplate getSpawn() {
			return spawn;
		}

		@Override
		public WorldPosition getPosition() {
			return position;
		}
	}

	private static final class TestWorldPosition extends WorldPosition {
		private final WorldMapInstance instance;

		private TestWorldPosition(WorldMapInstance instance) {
			super(300540000);
			this.instance = instance;
		}

		@Override
		public WorldMapInstance getWorldMapInstanceOrNull() {
			return instance;
		}
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public Integer getMapId() {
			return 300540000;
		}

		@Override
		public void doOnAllPlayers(Visitor<Player> visitor) {
		}

		@Override
		public MapRegion getRegion(float x, float y, float z) {
			return null;
		}

		@Override
		protected MapRegion createMapRegion(int regionId) {
			return null;
		}

		@Override
		protected void initMapRegions() {
		}

		@Override
		public boolean isPersonal() {
			return false;
		}

		@Override
		public int getOwnerId() {
			return 0;
		}
	}
}
