package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.InstanceCooltimeData;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
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

	@Test
	void identifiesOnlyOwnedSoloInstances() throws Exception {
		InstanceCooltimeData previous = DataManager.INSTANCE_COOLTIME_DATA;
		try {
			DataManager.INSTANCE_COOLTIME_DATA = loadInstanceCooltimes();

			TestWorldMapInstance solo = instanceWithPlayerCount(0);
			solo.mapId = 300200000;
			solo.personal = true;
			solo.ownerId = 1001;
			assertTrue(InstanceService.isPlayerSoloInstance(solo, 1001));
			assertFalse(InstanceService.isPlayerSoloInstance(solo, 1002));

			TestWorldMapInstance regular = instanceWithPlayerCount(0);
			regular.mapId = 300030000;
			regular.personal = true;
			regular.ownerId = 1001;
			assertFalse(InstanceService.isPlayerSoloInstance(regular, 1001));

			TestWorldMapInstance nonPersonal = instanceWithPlayerCount(0);
			nonPersonal.mapId = 300200000;
			nonPersonal.soloPlayerObjectId = 1001;
			assertTrue(InstanceService.isPlayerSoloInstance(nonPersonal, 1001));
			assertFalse(InstanceService.isPlayerSoloInstance(nonPersonal, 1002));
		} finally {
			DataManager.INSTANCE_COOLTIME_DATA = previous;
		}
	}

	@Test
	void doesNotResetGroupRegisteredInstances() {
		TestWorldMapInstance instance = instanceWithPlayerCount(0);
		instance.personal = true;
		instance.ownerId = 1001;
		instance.registeredGroup = objenesis.newInstance(PlayerGroup.class);

		assertFalse(InstanceService.isPlayerSoloInstance(instance, 1001));
	}

	private TestWorldMapInstance instanceWithPlayerCount(int playerCount) {
		TestWorldMapInstance instance = objenesis.newInstance(TestWorldMapInstance.class);
		instance.playerCount = playerCount;
		return instance;
	}

	private static InstanceCooltimeData loadInstanceCooltimes() throws Exception {
		return (InstanceCooltimeData) JAXBContext.newInstance(InstanceCooltimeData.class).createUnmarshaller()
				.unmarshal(new File("src/main/resources/aion/data/static_data/instance_cooltimes/instance_cooltimes.xml"));
	}

	private static final class TestWorldMapInstance extends WorldMapInstance {
		private int mapId;
		private int playerCount;
		private Integer registeredObjectId;
		private PlayerGroup registeredGroup;
		private boolean personal;
		private int ownerId;
		private Integer soloPlayerObjectId;

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
			return personal;
		}

		@Override
		public int getOwnerId() {
			return ownerId;
		}

		@Override
		public Integer getMapId() {
			return mapId;
		}

		@Override
		public PlayerGroup getRegisteredGroup() {
			return registeredGroup;
		}

		@Override
		public Integer getSoloPlayerObj() {
			return soloPlayerObjectId;
		}
	}
}
