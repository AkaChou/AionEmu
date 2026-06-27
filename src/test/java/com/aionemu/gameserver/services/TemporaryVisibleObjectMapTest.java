package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import org.junit.jupiter.api.Test;

class TemporaryVisibleObjectMapTest {

	@Test
	void anohaStoresSwordEffectsInJdkMap() throws Exception {
		assertJdkMap(new AnohaService(), "adventSwordEffect");
	}

	@Test
	void beritraStoresAdventObjectsInJdkMaps() throws Exception {
		BeritraService service = new BeritraService();

		assertJdkMap(service, "adventPortal");
		assertJdkMap(service, "adventEffect");
		assertJdkMap(service, "adventControl");
		assertJdkMap(service, "adventDirecting");
		assertJdkMap(service, "adventEreshPortal");
		assertJdkMap(service, "adventEreshEffect");
		assertJdkMap(service, "adventEreshControl");
		assertJdkMap(service, "adventEreshDirecting");
	}

	@Test
	void rvrStoresAdventObjectsInJdkMaps() throws Exception {
		RvrService service = new RvrService();

		assertJdkMap(service, "adventPortal");
		assertJdkMap(service, "adventEffect");
		assertJdkMap(service, "adventControl");
		assertJdkMap(service, "adventDirecting");
	}

	@Test
	void svsStoresAdvanceCorridorsInJdkMap() throws Exception {
		assertJdkMap(new SvsService(), "advanceCorridor");
	}

	@Test
	void zorshivDredgionStoresAdventObjectsInJdkMaps() throws Exception {
		ZorshivDredgionService service = new ZorshivDredgionService();

		assertJdkMap(service, "adventPortal");
		assertJdkMap(service, "adventEffect");
		assertJdkMap(service, "adventControl");
		assertJdkMap(service, "adventDirecting");
	}

	@SuppressWarnings("unchecked")
	private void assertJdkMap(Object service, String fieldName) throws Exception {
		Field field = service.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		Map<Integer, VisibleObject> objects = (Map<Integer, VisibleObject>) field.get(service);

		assertEquals(HashMap.class, objects.getClass());
	}
}
