package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.instance.DynamicInstance;
import com.aionemu.gameserver.model.instance.InstanceRuntimeState;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;

class RetailMatchmakingInstanceIdentityTest {

	@Test
	void usesPersistentUidWhenRuntimeIdsCollideAcrossMaps() {
		TestInstance first = instance(101);
		TestInstance second = instance(202);

		assertEquals(first.getInstanceId(), second.getInstanceId());
		assertNotEquals(RetailMatchmakingService.instanceUid(first), RetailMatchmakingService.instanceUid(second));
	}

	private static TestInstance instance(long uid) {
		TestInstance instance = new ObjenesisStd().newInstance(TestInstance.class);
		instance.setDynamicInstance(new DynamicInstance(uid, 0, 0, 0, 0, DynamicInstance.OWNER_MATCH, 0,
				(byte) 0, DynamicInstance.ACTIVE, (byte) 0, 0, 0, 0, 0, 1, "{}", 0),
				new InstanceRuntimeState());
		return instance;
	}

	private static final class TestInstance extends WorldMapInstance {
		private TestInstance() {
			super(null, 0);
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
