package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.EventsWindowData;
import com.aionemu.gameserver.services.events.EventWindowService;
import com.aionemu.gameserver.services.events.FFAService;
import com.aionemu.gameserver.services.events.bg.DeathmatchBg;

class ServiceMapImplementationTest {

	@AfterEach
	void clearStaticData() {
		DataManager.EVENTS_WINDOW = null;
	}

	@Test
	void shieldServiceStoresRegisteredShieldsInJdkMaps() throws Exception {
		ShieldService service = new ShieldService();

		assertHashMap(service, "sphereShields");
		assertHashMap(service, "registeredShields");
	}

	@Test
	void eventWindowServiceStoresPendingEventsInJdkMap() throws Exception {
		DataManager.EVENTS_WINDOW = new EventsWindowData();

		assertHashMap(new EventWindowService(), "sendActiveEventsForPlayer");
	}

	@Test
	void eventServicesStorePreviousLocationsInJdkMaps() throws Exception {
		assertHashMap(new DeathmatchBg(), "previousLocations");
		assertHashMap(new FFAService(), "previousLocations");
	}

	private void assertHashMap(Object target, String fieldName) throws Exception {
		Field field = findField(target.getClass(), fieldName);
		field.setAccessible(true);

		assertEquals(HashMap.class, field.get(target).getClass());
	}

	private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				return current.getDeclaredField(fieldName);
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
