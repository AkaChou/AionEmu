package com.aionemu.gameserver.controllers.attack;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AggroListTest {

	@Test
	void getListReturnsSnapshotSafeForRemovalDuringIteration() throws ReflectiveOperationException {
		AggroList aggroList = new AggroList(null);
		Map<Integer, AggroInfo> entries = new LinkedHashMap<Integer, AggroInfo>();
		entries.put(1, new AggroInfo(null));
		entries.put(2, new AggroInfo(null));
		entries.put(3, new AggroInfo(null));
		setAggroList(aggroList, entries);

		Collection<AggroInfo> snapshot = aggroList.getList();

		assertDoesNotThrow(() -> {
			for (AggroInfo aggroInfo : snapshot) {
				entries.values().remove(aggroInfo);
			}
		});
	}

	private static void setAggroList(AggroList target, Map<Integer, AggroInfo> value) throws ReflectiveOperationException {
		Field field = AggroList.class.getDeclaredField("aggroList");
		field.setAccessible(true);
		field.set(target, value);
	}
}
