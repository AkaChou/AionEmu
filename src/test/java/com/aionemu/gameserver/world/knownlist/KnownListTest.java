package com.aionemu.gameserver.world.knownlist;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.controllers.VisibleObjectController;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

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

	@Test
	void doOnAllNpcsWithOwnerUsesStableSnapshotWhenKnownObjectsChangeDuringVisit() {
		TestVisibleObject owner = visibleObject(1);
		TestKnownList knownList = (TestKnownList) owner.getKnownList();
		knownList.addKnown(npc(2));
		knownList.addKnown(npc(3));
		List<Integer> visited = new ArrayList<>();

		knownList.doOnAllNpcsWithOwner((npc, ignoredOwner) -> {
			visited.add(npc.getObjectId());
			if (visited.size() == 1) {
				knownList.addKnown(npc(99));
			}
		});

		assertEquals(List.of(2, 3), visited);
	}

	@Test
	void doOnAllNpcsUsesStableSnapshotWhenKnownObjectsChangeDuringVisit() {
		TestVisibleObject owner = visibleObject(1);
		TestKnownList knownList = (TestKnownList) owner.getKnownList();
		knownList.addKnown(npc(2));
		knownList.addKnown(npc(3));
		List<Integer> visited = new ArrayList<>();

		knownList.doOnAllNpcs(npc -> {
			visited.add(npc.getObjectId());
			if (visited.size() == 1) {
				knownList.addKnown(npc(99));
			}
		});

		assertEquals(List.of(2, 3), visited);
	}

	@Test
	void doOnAllPlayersUsesStableSnapshotWhenKnownPlayersChangeDuringVisit() {
		TestVisibleObject owner = visibleObject(1);
		TestKnownList knownList = (TestKnownList) owner.getKnownList();
		knownList.addKnown(player(2));
		knownList.addKnown(player(3));
		List<Integer> visited = new ArrayList<>();

		knownList.doOnAllPlayers(player -> {
			visited.add(player.getObjectId());
			if (visited.size() == 1) {
				knownList.addKnown(player(99));
			}
		});

		assertEquals(List.of(2, 3), visited);
	}

	@Test
	void getKnownPlayersReturnsSnapshotSafeForRemovalDuringIteration() {
		TestVisibleObject owner = visibleObject(1);
		TestKnownList knownList = (TestKnownList) owner.getKnownList();
		Player first = player(2);
		Player second = player(3);
		knownList.addKnown(first);
		knownList.addKnown(second);

		assertDoesNotThrow(() -> {
			for (Player player : knownList.getKnownPlayers().values()) {
				knownList.removeKnown(player);
			}
		});
		assertEquals(List.of(), new ArrayList<Player>(knownList.getKnownPlayers().values()));
	}

	@Test
	void doOnAllObjectsUsesStableSnapshotWhenKnownObjectsChangeDuringVisit() {
		TestVisibleObject owner = visibleObject(1);
		TestKnownList knownList = (TestKnownList) owner.getKnownList();
		knownList.addKnown(visibleObject(2));
		knownList.addKnown(visibleObject(3));
		List<Integer> visited = new ArrayList<>();

		knownList.doOnAllObjects(object -> {
			visited.add(object.getObjectId());
			if (visited.size() == 1) {
				knownList.addKnown(visibleObject(99));
			}
		});

		assertEquals(List.of(2, 3), visited);
	}

	private static TestVisibleObject visibleObject(int objectId) {
		TestVisibleObject object = new TestVisibleObject(objectId);
		object.setKnownlist(new TestKnownList(object));
		return object;
	}

	private static Npc npc(int objectId) {
		return object(Npc.class, objectId);
	}

	private static Player player(int objectId) {
		return object(Player.class, objectId);
	}

	private static <T extends AionObject> T object(Class<T> type, int objectId) {
		try {
			T object = new ObjenesisStd().newInstance(type);
			Field field = AionObject.class.getDeclaredField("objectId");
			field.setAccessible(true);
			field.set(object, objectId);
			return object;
		} catch (ReflectiveOperationException ex) {
			throw new AssertionError(ex);
		}
	}

	private static final class TestKnownList extends KnownList {

		private TestKnownList(VisibleObject owner) {
			super(owner);
		}

		private void addKnown(VisibleObject object) {
			add(object);
		}

		private void removeKnown(Player player) {
			knownObjects.remove(player.getObjectId());
			knownPlayers.remove(player.getObjectId());
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
