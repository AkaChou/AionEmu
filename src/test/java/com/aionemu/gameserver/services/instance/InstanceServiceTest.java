package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;

class InstanceServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void emptyInstanceCanResetEvenWhenPlayerRegistrationRemains() {
		TestWorldMapInstance instance = instanceWithPlayerCount(0);
		instance.register(1001);

		assertTrue(instance.isRegistered(1001));
		assertTrue(InstanceService.isEmptyForResetAfterLeave(instance));
	}

	@Test
	void instanceWithPlayersInsideCannotResetAfterOnePlayerLeaves() {
		TestWorldMapInstance instance = instanceWithPlayerCount(1);

		assertFalse(InstanceService.isEmptyForResetAfterLeave(instance));
	}

	@Test
	void usesSeparateRegularAndSoloDestroyDelays() {
		int regularDelay = InstanceConfig.DESTROY_DELAY_SECONDS;
		int soloDelay = InstanceConfig.SOLO_DESTROY_DELAY_SECONDS;
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 90;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = 30;
			assertEquals(90_000L, InstanceService.getDestroyDelayMillis(false));
			assertEquals(30_000L, InstanceService.getDestroyDelayMillis(true));
		} finally {
			InstanceConfig.DESTROY_DELAY_SECONDS = regularDelay;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = soloDelay;
		}
	}

	@Test
	void protectsPlayerTransitionsWhenDestroyDelayIsZero() {
		int regularDelay = InstanceConfig.DESTROY_DELAY_SECONDS;
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 0;
			assertEquals(1000L, InstanceService.getScheduledDestroyDelayMillis(false));
		} finally {
			InstanceConfig.DESTROY_DELAY_SECONDS = regularDelay;
		}
	}

	private TestWorldMapInstance instanceWithPlayerCount(int playerCount) {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		instance.playerCount = playerCount;
		return instance;
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private int playerCount;
		private Integer registeredObjectId;

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public void register(int objectId) {
			registeredObjectId = objectId;
		}

		@Override
		public boolean isRegistered(int objectId) {
			return registeredObjectId != null && registeredObjectId == objectId;
		}

		@Override
		public int playersCount() {
			return playerCount;
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
