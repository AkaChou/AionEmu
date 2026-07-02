package com.aionemu.gameserver.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.commons.utils.collections.IntObjectHashMap;
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

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = World.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
