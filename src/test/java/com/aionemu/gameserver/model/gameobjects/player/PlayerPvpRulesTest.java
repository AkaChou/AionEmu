package com.aionemu.gameserver.model.gameobjects.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.world.MapRegion;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * 固化 {@link PlayerPvpRules} 的世界开关、禁区与同族区域语义。
 * Pins down {@link PlayerPvpRules} world-flag, disabled-zone and same-race zone semantics.
 */
class PlayerPvpRulesTest {

	private World originalWorld;

	@BeforeEach
	void setUp() throws Exception {
		originalWorld = resolvedWorld();
	}

	@AfterEach
	void tearDown() throws Exception {
		setResolvedWorld(originalWorld);
	}

	@Test
	void differentRaceWithWorldPvpAllowedRequiresNoDisableZone() throws Exception {
		setResolvedWorld(world(true));

		TestPlayer elyos = player(Race.ELYOS, 1, false, false, true);
		TestPlayer asmodian = player(Race.ASMODIANS, 2, false, false, true);
		assertTrue(PlayerPvpRules.canPvP(elyos, asmodian));

		asmodian = player(Race.ASMODIANS, 2, false, false, false);
		assertFalse(PlayerPvpRules.canPvP(elyos, asmodian));
	}

	@Test
	void differentRaceWithWorldPvpDisabledUsesPvpZoneFlag() throws Exception {
		setResolvedWorld(world(false));

		TestPlayer elyos = player(Race.ELYOS, 1, false, false, false);
		TestPlayer asmodian = player(Race.ASMODIANS, 2, false, false, false);
		assertTrue(PlayerPvpRules.canPvP(elyos, asmodian));

		asmodian = player(Race.ASMODIANS, 2, false, false, true);
		assertFalse(PlayerPvpRules.canPvP(elyos, asmodian));
	}

	@Test
	void sameRaceOutsideExclusionWorldRequiresPvpZoneAndDifferentTeam() throws Exception {
		setResolvedWorld(world(true));

		TestPlayer elyos = player(Race.ELYOS, 999999, true, false, true);
		TestPlayer other = player(Race.ELYOS, 999999, true, false, true);
		assertTrue(PlayerPvpRules.canPvP(elyos, other));

		TestPlayer sameTeam = player(Race.ELYOS, 999999, true, true, true);
		assertFalse(PlayerPvpRules.canPvP(sameTeam, other));

		other = player(Race.ELYOS, 999999, false, false, true);
		assertFalse(PlayerPvpRules.canPvP(elyos, other));
	}

	@Test
	void sameRaceInsideExclusionWorldNeverPvP() throws Exception {
		setResolvedWorld(world(true));

		TestPlayer elyos = player(Race.ELYOS, 210020000, true, false, true);
		TestPlayer other = player(Race.ELYOS, 210020000, true, false, true);
		assertFalse(PlayerPvpRules.canPvP(elyos, other));
	}

	private static TestWorld world(boolean pvpAllowed) throws Exception {
		TestWorld world = new ObjenesisStd().newInstance(TestWorld.class);
		TestWorldMap worldMap = new ObjenesisStd().newInstance(TestWorldMap.class);
		setField(TestWorldMap.class, worldMap, "pvpAllowed", pvpAllowed);
		setField(TestWorld.class, world, "worldMap", worldMap);
		return world;
	}

	private static TestPlayer player(Race race, int worldId, boolean insidePvpZoneType, boolean sameTeam,
			boolean zonePvpAllowed) throws Exception {
		TestPlayer player = new ObjenesisStd().newInstance(TestPlayer.class);
		setField(TestPlayer.class, player, "race", race);
		setField(TestPlayer.class, player, "worldId", worldId);
		setField(TestPlayer.class, player, "insidePvpZoneType", insidePvpZoneType);
		setField(TestPlayer.class, player, "sameTeam", sameTeam);

		TestZone zone = new ObjenesisStd().newInstance(TestZone.class);
		setField(TestZone.class, zone, "pvpAllowed", zonePvpAllowed);

		Map<Integer, TreeSet<ZoneInstance>> zoneMap = new HashMap<>();
		TreeSet<ZoneInstance> zoneSet = new TreeSet<>(Comparator.comparingInt(System::identityHashCode));
		zoneSet.add(zone);
		zoneMap.put(0, zoneSet);

		MapRegion region = new ObjenesisStd().newInstance(MapRegion.class);
		setField(MapRegion.class, region, "zoneMap", zoneMap);

		WorldPosition position = new ObjenesisStd().newInstance(WorldPosition.class);
		setField(WorldPosition.class, position, "mapRegion", region);
		setField(WorldPosition.class, position, "isSpawned", true);

		setField(TestPlayer.class, player, "position", position);
		return player;
	}

	private static World resolvedWorld() throws Exception {
		Field field = GameWorldBootstrapServices.class.getDeclaredField("resolvedWorld");
		field.setAccessible(true);
		return (World) field.get(null);
	}

	private static void setResolvedWorld(World world) throws Exception {
		Field field = GameWorldBootstrapServices.class.getDeclaredField("resolvedWorld");
		field.setAccessible(true);
		field.set(null, world);
	}

	private static void setField(Class<?> declaringClass, Object target, String name, Object value) throws Exception {
		Field field = declaringClass.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static final class TestWorld extends World {

		private WorldMap worldMap;

		private TestWorld() {
			super();
		}

		@Override
		public WorldMap getWorldMap(int id) {
			return worldMap;
		}
	}

	private static final class TestWorldMap extends WorldMap {

		private boolean pvpAllowed;

		private TestWorldMap() {
			super(null, null);
		}

		@Override
		public boolean isPvpAllowed() {
			return pvpAllowed;
		}
	}

	private static final class TestPlayer extends Player {

		private Race race;
		private int worldId;
		private boolean insidePvpZoneType;
		private boolean sameTeam;
		private WorldPosition position;

		private TestPlayer() {
			super(null, null, null, null);
		}

		@Override
		public Race getRace() {
			return race;
		}

		@Override
		public int getWorldId() {
			return worldId;
		}

		@Override
		public boolean isInsideZoneType(ZoneType zoneType) {
			return zoneType == ZoneType.PVP && insidePvpZoneType;
		}

		@Override
		public boolean isInSameTeam(Player player) {
			return sameTeam;
		}

		@Override
		public WorldPosition getPosition() {
			return position;
		}
	}

	private static final class TestZone extends ZoneInstance {

		private boolean pvpAllowed;

		private TestZone() {
			super(0, null);
		}

		@Override
		public boolean isPvpAllowed() {
			return pvpAllowed;
		}

		@Override
		public boolean isInsideCreature(Creature creature) {
			return true;
		}
	}
}
