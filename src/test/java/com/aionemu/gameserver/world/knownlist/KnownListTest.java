package com.aionemu.gameserver.world.knownlist;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import org.junit.jupiter.api.Test;

class KnownListTest {

	@Test
	void forgetObjectsToleratesRemovingKnownObjectDuringIteration() {
		TestVisibleObject owner = visibleObject(1);
		TestVisibleObject firstKnown = visibleObject(2);
		TestVisibleObject secondKnown = visibleObject(3);
		TestKnownList knownList = (TestKnownList) owner.getKnownList();

		knownList.addKnown(firstKnown);
		knownList.addKnown(secondKnown);
		((TestKnownList) firstKnown.getKnownList()).addKnown(owner);
		((TestKnownList) secondKnown.getKnownList()).addKnown(owner);

		assertDoesNotThrow(knownList::doUpdate);
		assertFalse(knownList.knowns(firstKnown));
		assertFalse(knownList.knowns(secondKnown));
	}

	private static TestVisibleObject visibleObject(int objectId) {
		TestVisibleObject object = new TestVisibleObject(objectId);
		object.setKnownlist(new TestKnownList(object));
		return object;
	}

	private static final class TestKnownList extends KnownList {

		private TestKnownList(VisibleObject owner) {
			super(owner);
		}

		private void addKnown(VisibleObject object) {
			add(object);
		}

		@Override
		protected void findVisibleObjects() {
		}

		@Override
		protected boolean checkObjectInRange(VisibleObject newObject) {
			return false;
		}

		@Override
		protected boolean checkReversedObjectInRange(VisibleObject newObject) {
			return false;
		}
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
