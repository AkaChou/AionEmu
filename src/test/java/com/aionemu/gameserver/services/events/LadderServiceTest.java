package com.aionemu.gameserver.services.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.eventEngine.events.BattlegroundEvent;
import com.aionemu.gameserver.services.events.bg.Battleground;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

class LadderServiceTest {

	private final ObjenesisStd objenesis = new ObjenesisStd();

	@Test
	void getActiveBattlegroundUsesSnapshotWhenBattlegroundMapChangesDuringScan() throws ReflectiveOperationException {
		LadderService service = objenesis.newInstance(LadderService.class);
		Map<Integer, Battleground> battlegrounds = Collections.synchronizedMap(new LinkedHashMap<Integer, Battleground>());
		battlegrounds.put(1, new MutatingBattleground(() -> battlegrounds.remove(2)));
		battlegrounds.put(2, new TestBattleground());
		setField(service, "bgMap", battlegrounds);
		Player player = objenesis.newInstance(Player.class);

		assertDoesNotThrow(() -> service.getActiveBattleground(player));
	}

	@Test
	void getBattlegroundsReturnsReadOnlySnapshot() throws ReflectiveOperationException {
		LadderService service = objenesis.newInstance(LadderService.class);
		Map<Integer, Battleground> battlegrounds = Collections.synchronizedMap(new LinkedHashMap<Integer, Battleground>());
		battlegrounds.put(1, new TestBattleground());
		setField(service, "bgMap", battlegrounds);

		Map<Integer, Battleground> snapshot = service.getBattlegrounds();
		battlegrounds.clear();

		assertTrue(snapshot.containsKey(1));
		assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
	}

	@Test
	void handleNormalQueueUsesSnapshotWhenQueueChangesDuringScan() throws Throwable {
		LadderService service = objenesis.newInstance(LadderService.class);
		MutatingQueue queue = new MutatingQueue();
		queue.add(new TestAionObject(1));
		queue.add(new TestAionObject(2));
		queue.add(new TestAionObject(3));
		setField(service, "normalQueueList", queue);
		setField(service, "normalTeamBased", true);

		assertDoesNotThrow(() -> handleNormalQueue(service, new BattlegroundEvent()));
	}

	private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = LadderService.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	private static void handleNormalQueue(LadderService service, BattlegroundEvent event) throws Throwable {
		Method method = LadderService.class.getDeclaredMethod("HandleNormalQueue", BattlegroundEvent.class);
		method.setAccessible(true);
		try {
			method.invoke(service, event);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private static class TestAionObject extends AionObject {
		private TestAionObject(Integer objId) {
			super(objId);
		}

		@Override
		public String getName() {
			return "test";
		}
	}

	private static final class MutatingQueue extends ArrayList<AionObject> {
		@Override
		public Iterator<AionObject> iterator() {
			Iterator<AionObject> delegate = super.iterator();
			return new Iterator<AionObject>() {
				private boolean mutated;

				@Override
				public boolean hasNext() {
					return delegate.hasNext();
				}

				@Override
				public AionObject next() {
					AionObject next = delegate.next();
					if (!mutated) {
						mutated = true;
						MutatingQueue.this.remove(1);
					}
					return next;
				}
			};
		}
	}

	private static class TestBattleground extends Battleground {
		@Override
		public void createMatch(List<Integer> players) {
		}

		@Override
		public void startMatch() {
		}

		@Override
		public void onDie(Player player, Creature lastAttacker) {
		}

		@Override
		public void onLeave(Player player, boolean isLogout, boolean isAfk) {
		}

		@Override
		public int getSecondsLeft() {
			return 2;
		}

		@Override
		public Map<Integer, AionObject> getLeavers() {
			return Collections.emptyMap();
		}
	}

	private static final class MutatingBattleground extends TestBattleground {
		private final Runnable mutation;

		private MutatingBattleground(Runnable mutation) {
			this.mutation = mutation;
		}

		@Override
		public int getSecondsLeft() {
			mutation.run();
			return super.getSecondsLeft();
		}
	}
}
