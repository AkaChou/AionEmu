package com.aionemu.gameserver.model.gameobjects;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import org.junit.jupiter.api.Test;

class VisibleObjectTest {

	@Test
	void constructorBindsControllerOwner() {
		TestVisibleObject object = new TestVisibleObject(1);

		assertSame(object, object.getController().getOwner());
	}

	private static final class TestVisibleObject extends VisibleObject {

		private TestVisibleObject(int objectId) {
			super(objectId, new TestVisibleObjectController(), null, null, null);
		}

		@Override
		public String getName() {
			return "test-" + getObjectId();
		}
	}

	private static final class TestVisibleObjectController extends VisibleObjectController<VisibleObject> {
	}
}
