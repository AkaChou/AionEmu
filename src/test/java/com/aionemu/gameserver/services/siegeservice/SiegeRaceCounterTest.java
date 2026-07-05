package com.aionemu.gameserver.services.siegeservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.siege.SiegeRace;

class SiegeRaceCounterTest {

	@Test
	void counterSnapshotsTolerateConcurrentPlayerUpdates() throws Exception {
		TestableSiegeRaceCounter counter = new TestableSiegeRaceCounter();
		int updatesPerWriter = 5000;
		AtomicReference<Throwable> failure = new AtomicReference<>();
		AtomicBoolean writersDone = new AtomicBoolean();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(5);

		Future<?> reader = executor.submit(() -> {
			await(start);
			while (!writersDone.get() && failure.get() == null) {
				try {
					counter.snapshotDamage();
				} catch (Throwable t) {
					failure.compareAndSet(null, t);
				}
			}
		});

		Future<?>[] writers = new Future<?>[4];
		for (int writer = 0; writer < writers.length; writer++) {
			final int writerId = writer;
			writers[writer] = executor.submit(() -> {
				await(start);
				for (int i = 0; i < updatesPerWriter; i++) {
					counter.recordDamage(writerId * updatesPerWriter + i, 1);
					if ((i & 255) == 0) {
						Thread.yield();
					}
				}
			});
		}

		start.countDown();
		for (Future<?> writer : writers) {
			writer.get(10, TimeUnit.SECONDS);
		}
		writersDone.set(true);
		reader.get(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		assertNull(failure.get());
		assertEquals(4L * updatesPerWriter, counter.damageTotal());
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	private static class TestableSiegeRaceCounter extends SiegeRaceCounter {
		private final Map<Integer, AtomicLong> damage = new java.util.LinkedHashMap<>();

		private TestableSiegeRaceCounter() {
			super(SiegeRace.ELYOS);
		}

		private void recordDamage(int playerId, int damage) {
			addToCounter(playerId, damage, this.damage);
		}

		private Map<Integer, Long> snapshotDamage() {
			return getOrderedCounterMap(damage);
		}

		private long damageTotal() {
			return damage.values().stream().mapToLong(AtomicLong::get).sum();
		}
	}
}
