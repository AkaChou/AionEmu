package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import org.junit.jupiter.api.Test;

class TemporaryVisibleObjectMapTest {

	@Test
	void anohaStoresSwordEffectsInJdkList() throws Exception {
		assertJdkList(new AnohaService(), "adventSwordEffect");
	}

	@Test
	void beritraStoresAdventObjectsInJdkLists() throws Exception {
		BeritraService service = new BeritraService();

		assertJdkList(service, "adventPortal");
		assertJdkList(service, "adventEffect");
		assertJdkList(service, "adventControl");
		assertJdkList(service, "adventDirecting");
		assertJdkList(service, "adventEreshPortal");
		assertJdkList(service, "adventEreshEffect");
		assertJdkList(service, "adventEreshControl");
		assertJdkList(service, "adventEreshDirecting");
	}

	@Test
	void rvrStoresAdventObjectsInJdkLists() throws Exception {
		RvrService service = new RvrService();

		assertJdkList(service, "adventPortal");
		assertJdkList(service, "adventEffect");
		assertJdkList(service, "adventControl");
		assertJdkList(service, "adventDirecting");
	}

	@Test
	void svsStoresAdvanceCorridorsInJdkList() throws Exception {
		assertJdkList(new SvsService(), "advanceCorridor");
	}

	@Test
	void zorshivDredgionStoresAdventObjectsInJdkLists() throws Exception {
		ZorshivDredgionService service = new ZorshivDredgionService();

		assertJdkList(service, "adventPortal");
		assertJdkList(service, "adventEffect");
		assertJdkList(service, "adventControl");
		assertJdkList(service, "adventDirecting");
	}

	@Test
	void eventServicesDeleteTrackedObjectsWhenCleared() throws Exception {
		assertObjectsCleared(new AnohaService(), 1, "adventSwordEffect");
		BeritraService beritraService = new BeritraService();
		assertObjectsCleared(beritraService, 1,
				"adventPortal", "adventEffect", "adventControl", "adventDirecting");
		assertObjectsCleared(beritraService, 35,
				"adventEreshPortal", "adventEreshEffect", "adventEreshControl", "adventEreshDirecting");
		assertObjectsCleared(new RvrService(), 5, "adventPortal", "adventEffect", "adventControl", "adventDirecting");
		assertObjectsCleared(new SvsService(), 5, "advanceCorridor");
		assertObjectsCleared(new ZorshivDredgionService(), 3,
				"adventPortal", "adventEffect", "adventControl", "adventDirecting");
	}

	private void assertJdkList(Object service, String fieldName) throws Exception {
		Field field = service.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		assertTrue(List.class.isAssignableFrom(field.get(service).getClass()));
	}

	@SuppressWarnings("unchecked")
	private void assertObjectsCleared(Object service, int id, String... fieldNames) throws Exception {
		List<TestVisibleObject> objects = new ArrayList<>();
		for (String fieldName : fieldNames) {
			TestVisibleObject object = new TestVisibleObject();
			objects.add(object);
			Field field = service.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			((List<VisibleObject>) field.get(service)).add(object);
		}

		service.getClass().getMethod("clearAdventObjects", int.class).invoke(service, id);

		for (TestVisibleObject object : objects) {
			assertTrue(object.deleted);
		}
		for (String fieldName : fieldNames) {
			Field field = service.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			assertTrue(((List<?>) field.get(service)).isEmpty());
		}
	}

	private static final class TestVisibleObject extends VisibleObject {
		private boolean deleted;

		private TestVisibleObject() {
			super(1, new TestVisibleObjectController(), null, null, null);
			controller().setOwner(this);
		}

		@SuppressWarnings("unchecked")
		private VisibleObjectController<VisibleObject> controller() {
			return (VisibleObjectController<VisibleObject>) getController();
		}

		@Override
		public boolean isSpawned() {
			return true;
		}

		@Override
		public String getName() {
			return "temporary-test-object";
		}
	}

	private static final class TestVisibleObjectController extends VisibleObjectController<VisibleObject> {
		@Override
		public void onDelete() {
			((TestVisibleObject) getOwner()).deleted = true;
		}
	}
}
