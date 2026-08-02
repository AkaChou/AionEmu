package com.aionemu.gameserver.services;

import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestTimerPolicy;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.world.WorldPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestServiceManagedTimerTest {
	private static final int PLAYER_ONE = 700001;
	private static final int PLAYER_TWO = 700002;

	@AfterEach
	void cleanup() {
		QuestService.cleanupPlayerQuestTimers(PLAYER_ONE);
		QuestService.cleanupPlayerQuestTimers(PLAYER_TWO);
	}

	@Test
	void overwritePoliciesKeepFailAndReplaceDeterministically() throws Exception {
		Player player = player(PLAYER_ONE, 3);
		RecordingScheduler scheduler = new RecordingScheduler();
		QuestTimerPolicy replace = policy("policy", QuestTimerPolicy.OverwritePolicy.REPLACE);
		QuestTimerPolicy keep = policy("policy", QuestTimerPolicy.OverwritePolicy.KEEP_EXISTING);
		QuestTimerPolicy fail = policy("policy", QuestTimerPolicy.OverwritePolicy.FAIL_IF_RUNNING);

		assertEquals(QuestService.TimerStartOutcome.STARTED,
			start(player, 1001, replace, scheduler, ignored -> { }));
		RecordingFuture original = scheduler.futures.get(0);
		assertEquals(QuestService.TimerStartOutcome.KEPT_EXISTING,
			start(player, 1001, keep, scheduler, ignored -> { }));
		assertEquals(1, scheduler.tasks.size());
		assertFalse(original.cancelled);
		assertThrows(IllegalStateException.class,
			() -> start(player, 1001, fail, scheduler, ignored -> { }));

		assertEquals(QuestService.TimerStartOutcome.STARTED,
			start(player, 1001, replace, scheduler, ignored -> { }));
		assertTrue(original.cancelled);
		assertEquals(2, scheduler.tasks.size());
	}

	@Test
	void invisibleTimerCanBeCancelledByItsIdentity() throws Exception {
		Player player = player(PLAYER_ONE, 3);
		RecordingScheduler scheduler = new RecordingScheduler();
		QuestTimerPolicy policy = policy("hidden-cutscene", QuestTimerPolicy.OverwritePolicy.REPLACE);
		start(player, 1002, policy, scheduler, ignored -> { });

		assertTrue(QuestService.hasQuestTimers(PLAYER_ONE, 1002));
		assertTrue(QuestService.questTimerEnd(new QuestEnv(null, player, 1002, 0), policy.identity()));
		assertTrue(scheduler.futures.get(0).cancelled);
		assertFalse(QuestService.hasQuestTimers(PLAYER_ONE, 1002));
	}

	@Test
	void expiryDeliversAtMostOnceWithTheOwningQuestId() throws Exception {
		Player player = player(PLAYER_ONE, 3);
		RecordingScheduler scheduler = new RecordingScheduler();
		AtomicInteger deliveries = new AtomicInteger();
		AtomicInteger deliveredQuestId = new AtomicInteger();
		start(player, 24154, policy("movie", QuestTimerPolicy.OverwritePolicy.REPLACE), scheduler, questId -> {
			deliveredQuestId.set(questId);
			deliveries.incrementAndGet();
		});

		scheduler.tasks.get(0).run();
		scheduler.tasks.get(0).run();

		assertEquals(1, deliveries.get());
		assertEquals(24154, deliveredQuestId.get());
		assertFalse(QuestService.hasQuestTimers(PLAYER_ONE, 24154));
	}

	@Test
	void questPlayerAndInstanceCleanupCancelOnlyOwnedTimers() throws Exception {
		Player playerOne = player(PLAYER_ONE, 3);
		Player playerTwo = player(PLAYER_TWO, 4);
		RecordingScheduler scheduler = new RecordingScheduler();
		start(playerOne, 1001, policy("one", QuestTimerPolicy.OverwritePolicy.REPLACE), scheduler, ignored -> { });
		start(playerOne, 1002, policy("two", QuestTimerPolicy.OverwritePolicy.REPLACE), scheduler, ignored -> { });
		start(playerTwo, 1001, policy("three", QuestTimerPolicy.OverwritePolicy.REPLACE), scheduler, ignored -> { });

		QuestService.cleanupQuestTimers(PLAYER_ONE, 1001);
		assertTrue(scheduler.futures.get(0).cancelled);
		assertFalse(scheduler.futures.get(1).cancelled);
		assertFalse(scheduler.futures.get(2).cancelled);

		QuestService.cleanupPlayerQuestTimers(PLAYER_ONE);
		assertTrue(scheduler.futures.get(1).cancelled);
		assertFalse(scheduler.futures.get(2).cancelled);

		QuestService.cleanupInstanceQuestTimers(4);
		assertTrue(scheduler.futures.get(2).cancelled);
	}

	@Test
	void globalCleanupCancelsEveryManagedTimer() throws Exception {
		Player playerOne = player(PLAYER_ONE, 3);
		Player playerTwo = player(PLAYER_TWO, 4);
		RecordingScheduler scheduler = new RecordingScheduler();
		start(playerOne, 1001, policy("one", QuestTimerPolicy.OverwritePolicy.REPLACE), scheduler, ignored -> { });
		start(playerTwo, 1002, policy("two", QuestTimerPolicy.OverwritePolicy.REPLACE), scheduler, ignored -> { });

		QuestService.cleanupAllQuestTimers();

		assertTrue(scheduler.futures.get(0).cancelled);
		assertTrue(scheduler.futures.get(1).cancelled);
		assertFalse(QuestService.hasQuestTimers(PLAYER_ONE, 1001));
		assertFalse(QuestService.hasQuestTimers(PLAYER_TWO, 1002));
	}

	private static QuestService.TimerStartOutcome start(Player player, int questId, QuestTimerPolicy policy,
			RecordingScheduler scheduler, java.util.function.IntConsumer callback) {
		return QuestService.startManagedTimer(player, questId, 30, policy, false, callback, scheduler);
	}

	private static QuestTimerPolicy policy(String timerId, QuestTimerPolicy.OverwritePolicy overwritePolicy) {
		return QuestTimerPolicy.session(timerId, overwritePolicy);
	}

	private static Player player(int objectId, int instanceId) throws Exception {
		Player player = new ObjenesisStd().newInstance(Player.class);
		Field field = AionObject.class.getDeclaredField("objectId");
		field.setAccessible(true);
		field.set(player, objectId);
		player.setPosition(new WorldPosition(110010000) {
			@Override
			public int getInstanceId() {
				return instanceId;
			}
		});
		return player;
	}

	private static final class RecordingScheduler implements QuestService.QuestTimerScheduler {
		private final List<Runnable> tasks = new ArrayList<>();
		private final List<RecordingFuture> futures = new ArrayList<>();

		@Override
		public Future<?> schedule(Runnable task, long delayMillis) {
			assertEquals(30_000L, delayMillis);
			tasks.add(task);
			RecordingFuture future = new RecordingFuture();
			futures.add(future);
			return future;
		}
	}

	private static final class RecordingFuture implements Future<Object> {
		private boolean cancelled;

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return cancelled;
		}

		@Override
		public boolean isDone() {
			return cancelled;
		}

		@Override
		public Object get() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object get(long timeout, TimeUnit unit) {
			throw new UnsupportedOperationException();
		}
	}
}
