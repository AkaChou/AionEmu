package com.aionemu.gameserver.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
import com.aionemu.gameserver.world.container.PlayerContainer;
import com.aionemu.gameserver.world.exceptions.DuplicateAionObjectException;
import java.util.ArrayList;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class WorldTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void getNpcsReturnsSnapshotSafeForRemovalDuringIteration() throws ReflectiveOperationException {
		World world = objenesis.newInstance(World.class);
		Map<Integer, Npc> allNpcs = new LinkedHashMap<Integer, Npc>();
		allNpcs.put(1, objenesis.newInstance(Npc.class));
		allNpcs.put(2, objenesis.newInstance(Npc.class));
		allNpcs.put(3, objenesis.newInstance(Npc.class));
		setField(world, "allNpcs", allNpcs);

		Collection<Npc> snapshot = world.getNpcs();

		assertDoesNotThrow(() -> {
			for (Npc npc : snapshot) {
				allNpcs.values().remove(npc);
			}
		});
		assertTrue(allNpcs.isEmpty());
	}

	@Test
	void getLocalSiegeNpcsReturnsSnapshotSafeForRemovalDuringIteration() throws ReflectiveOperationException {
		World world = objenesis.newInstance(World.class);
		Collection<SiegeNpc> localNpcs = new ArrayList<SiegeNpc>();
		localNpcs.add(objenesis.newInstance(SiegeNpc.class));
		localNpcs.add(objenesis.newInstance(SiegeNpc.class));
		localNpcs.add(objenesis.newInstance(SiegeNpc.class));
		IntObjectHashMap<Collection<SiegeNpc>> localSiegeNpcs = new IntObjectHashMap<Collection<SiegeNpc>>();
		localSiegeNpcs.put(1, localNpcs);
		setField(world, "localSiegeNpcs", localSiegeNpcs);

		Collection<SiegeNpc> snapshot = world.getLocalSiegeNpcs(1);

		assertDoesNotThrow(() -> {
			for (SiegeNpc npc : snapshot) {
				localNpcs.remove(npc);
			}
		});
		assertTrue(localNpcs.isEmpty());
	}

	@Test
	void storeObjectDuplicatePlayerNameDoesNotLeaveObjectIndexedGlobally() throws ReflectiveOperationException {
		World world = objenesis.newInstance(World.class);
		setField(world, "allPlayers", new PlayerContainer());
		setField(world, "allObjects", new LinkedHashMap<Integer, VisibleObject>());
		setField(world, "allNpcs", new LinkedHashMap<Integer, Npc>());
		Player existing = player(1, "same-name");
		Player duplicateName = player(2, "same-name");
		world.storeObject(existing);

		assertThrows(DuplicateAionObjectException.class, () -> world.storeObject(duplicateName));

		assertSame(existing, world.findVisibleObject(1));
		assertSame(existing, world.findPlayer("same-name"));
		assertNull(world.findVisibleObject(2));
	}

	@Test
	void storeObjectDuplicateObjectIdDoesNotReplaceExistingGlobalObject() throws ReflectiveOperationException {
		World world = objenesis.newInstance(World.class);
		setField(world, "allPlayers", new PlayerContainer());
		setField(world, "allObjects", new LinkedHashMap<Integer, VisibleObject>());
		setField(world, "allNpcs", new LinkedHashMap<Integer, Npc>());
		Player existing = player(1, "existing");
		Player duplicateId = player(1, "duplicate");
		world.storeObject(existing);

		assertThrows(DuplicateAionObjectException.class, () -> world.storeObject(duplicateId));

		assertSame(existing, world.findVisibleObject(1));
		assertSame(existing, world.findPlayer("existing"));
		assertNull(world.findPlayer("duplicate"));
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = World.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private Player player(int objectId, String name) throws ReflectiveOperationException {
		Player player = objenesis.newInstance(Player.class);
		Field objectIdField = AionObject.class.getDeclaredField("objectId");
		objectIdField.setAccessible(true);
		objectIdField.set(player, objectId);
		PlayerCommonData commonData = new PlayerCommonData(objectId);
		commonData.setName(name);
		Field commonDataField = Player.class.getDeclaredField("playerCommonData");
		commonDataField.setAccessible(true);
		commonDataField.set(player, commonData);
		Field positionField = VisibleObject.class.getDeclaredField("position");
		positionField.setAccessible(true);
		positionField.set(player, new WorldPosition(1));
		return player;
	}
}
