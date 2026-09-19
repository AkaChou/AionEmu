package com.aionemu.gameserver.services.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import jakarta.xml.bind.JAXBContext;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.configs.main.InstanceConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.InstanceCooltimeData;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAlliance;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceGroup;
import com.aionemu.gameserver.model.team2.league.League;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.testutil.ConfigSnapshot;

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
		ConfigSnapshot snapshot = ConfigSnapshot.of(InstanceConfig.class,
			"DESTROY_DELAY_SECONDS", "SOLO_DESTROY_DELAY_SECONDS");
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 90;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = 30;
			assertEquals(90_000L, InstanceService.getDestroyDelayMillis(false));
			assertEquals(30_000L, InstanceService.getDestroyDelayMillis(true));
		} finally {
			snapshot.restore();
		}
	}

	@Test
	void protectsPlayerTransitionsWhenDestroyDelayIsZero() {
		ConfigSnapshot snapshot = ConfigSnapshot.of(InstanceConfig.class, "DESTROY_DELAY_SECONDS");
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 0;
			assertEquals(1000L, InstanceService.getScheduledDestroyDelayMillis(false));
		} finally {
			snapshot.restore();
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
			assertTrue(InstanceService.isPlayerSoloInstance(regular, 1001));
			assertFalse(InstanceService.isPlayerSoloInstance(regular, 1002));

			TestWorldMapInstance nonPersonal = instanceWithPlayerCount(0);
			nonPersonal.mapId = 300200000;
			nonPersonal.soloPlayerObjectId = 1001;
			assertTrue(InstanceService.isPlayerSoloInstance(nonPersonal, 1001));
			assertFalse(InstanceService.isPlayerSoloInstance(nonPersonal, 1002));

			TestWorldMapInstance soloEnteredGroup = instanceWithPlayerCount(0);
			soloEnteredGroup.mapId = 300030000;
			soloEnteredGroup.soloPlayerObjectId = 1001;
			assertTrue(InstanceService.isPlayerSoloInstance(soloEnteredGroup, 1001));
			assertFalse(InstanceService.isPlayerSoloInstance(soloEnteredGroup, 1002));
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

		TestWorldMapInstance allianceInstance = instanceWithPlayerCount(0);
		allianceInstance.soloPlayerObjectId = 1001;
		allianceInstance.registredAlliance = objenesis.newInstance(PlayerAlliance.class);
		assertFalse(InstanceService.isPlayerSoloInstance(allianceInstance, 1001));

		TestWorldMapInstance leagueInstance = instanceWithPlayerCount(0);
		leagueInstance.personal = true;
		leagueInstance.ownerId = 1001;
		leagueInstance.registredLeague = objenesis.newInstance(League.class);
		assertFalse(InstanceService.isPlayerSoloInstance(leagueInstance, 1001));
	}

	@Test
	void identifiesPlayerInstancesForSoloAndTeams() {
		Player player = playerWithId(1001);
		Player otherPlayer = playerWithId(1002);

		TestWorldMapInstance soloPersonal = instanceWithPlayerCount(0);
		soloPersonal.personal = true;
		soloPersonal.ownerId = 1001;
		assertTrue(InstanceService.isPlayerInstance(soloPersonal, player));
		assertFalse(InstanceService.isPlayerInstance(soloPersonal, otherPlayer));

		TestWorldMapInstance soloEnteredGroup = instanceWithPlayerCount(0);
		soloEnteredGroup.soloPlayerObjectId = 1001;
		assertTrue(InstanceService.isPlayerInstance(soloEnteredGroup, player));
		assertFalse(InstanceService.isPlayerInstance(soloEnteredGroup, otherPlayer));

		PlayerGroup myGroup = objenesis.newInstance(PlayerGroup.class);
		setField(AionObject.class, myGroup, "objectId", 2001);
		PlayerGroup otherGroup = objenesis.newInstance(PlayerGroup.class);
		setField(AionObject.class, otherGroup, "objectId", 2002);

		TestWorldMapInstance groupInstance = instanceWithPlayerCount(0);
		groupInstance.registeredGroup = myGroup;

		player.setPlayerGroup2(myGroup);
		assertTrue(InstanceService.isPlayerInstance(groupInstance, player));
		assertFalse(InstanceService.isPlayerInstance(groupInstance, otherPlayer));
		otherPlayer.setPlayerGroup2(otherGroup);
		assertFalse(InstanceService.isPlayerInstance(groupInstance, otherPlayer));

		PlayerAlliance myAlliance = objenesis.newInstance(PlayerAlliance.class);
		setField(AionObject.class, myAlliance, "objectId", 3001);
		PlayerAlliance otherAlliance = objenesis.newInstance(PlayerAlliance.class);
		setField(AionObject.class, otherAlliance, "objectId", 3002);

		PlayerAllianceGroup myAllianceGroup = objenesis.newInstance(PlayerAllianceGroup.class);
		setField(PlayerAllianceGroup.class, myAllianceGroup, "alliance", myAlliance);
		player.setPlayerAllianceGroup2(myAllianceGroup);

		TestWorldMapInstance allianceInstance = instanceWithPlayerCount(0);
		allianceInstance.registredAlliance = myAlliance;

		assertTrue(InstanceService.isPlayerInstance(allianceInstance, player));
		PlayerAllianceGroup otherAllianceGroup = objenesis.newInstance(PlayerAllianceGroup.class);
		setField(PlayerAllianceGroup.class, otherAllianceGroup, "alliance", otherAlliance);
		otherPlayer.setPlayerAllianceGroup2(otherAllianceGroup);
		assertFalse(InstanceService.isPlayerInstance(allianceInstance, otherPlayer));

		League myLeague = objenesis.newInstance(League.class);
		setField(AionObject.class, myLeague, "objectId", 4001);
		myAlliance.setLeague(myLeague);

		TestWorldMapInstance leagueInstance = instanceWithPlayerCount(0);
		leagueInstance.registredLeague = myLeague;

		assertTrue(InstanceService.isPlayerInstance(leagueInstance, player));
		League otherLeague = objenesis.newInstance(League.class);
		setField(AionObject.class, otherLeague, "objectId", 4002);
		otherAlliance.setLeague(otherLeague);
		assertFalse(InstanceService.isPlayerInstance(leagueInstance, otherPlayer));

		assertFalse(InstanceService.isPlayerInstance(soloPersonal, null));
		assertFalse(InstanceService.isPlayerInstance(soloPersonal, playerWithId(0)));
	}

	@Test
	void usesSoloDestroyDelayForSoloEnteredGroupInstance() throws Exception {
		ConfigSnapshot snapshot = ConfigSnapshot.of(InstanceConfig.class,
				"DESTROY_DELAY_SECONDS", "SOLO_DESTROY_DELAY_SECONDS");
		InstanceCooltimeData previous = DataManager.INSTANCE_COOLTIME_DATA;
		try {
			InstanceConfig.DESTROY_DELAY_SECONDS = 90;
			InstanceConfig.SOLO_DESTROY_DELAY_SECONDS = 30;
			DataManager.INSTANCE_COOLTIME_DATA = loadInstanceCooltimes();
			TestWorldMapInstance soloEnteredGroup = instanceWithPlayerCount(0);
			soloEnteredGroup.mapId = 300030000;
			soloEnteredGroup.soloPlayerObjectId = 1001;
			assertEquals(30_000L, InstanceService.getScheduledDestroyDelayMillis(soloEnteredGroup));

			TestWorldMapInstance groupInstance = instanceWithPlayerCount(0);
			groupInstance.mapId = 300030000;
			groupInstance.registeredGroup = objenesis.newInstance(PlayerGroup.class);
			assertEquals(90_000L,
					InstanceService.getScheduledDestroyDelayMillis(groupInstance));
		} finally {
			DataManager.INSTANCE_COOLTIME_DATA = previous;
			snapshot.restore();
		}
	}

	private Player playerWithId(int objectId) {
		Player player = objenesis.newInstance(Player.class);
		setField(AionObject.class, player, "objectId", objectId);
		return player;
	}

	private static void setField(Class<?> owner, Object target, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
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
		private PlayerAlliance registredAlliance;
		private League registredLeague;
		private boolean personal;
		private int ownerId;
		private Integer soloPlayerObjectId;

		private TestWorldMapInstance() {
			super(null, 0);
		}

		@Override
		public PlayerAlliance getRegistredAlliance() {
			return registredAlliance;
		}

		@Override
		public League getRegistredLeague() {
			return registredLeague;
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
