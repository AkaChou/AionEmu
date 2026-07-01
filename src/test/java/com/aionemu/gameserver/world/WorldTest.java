package com.aionemu.gameserver.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.Npc;
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

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = World.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
