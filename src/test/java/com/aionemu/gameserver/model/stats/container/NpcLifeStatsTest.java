package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.LOG;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;

class NpcLifeStatsTest {

	@Test
	void naturalNpcHealingUsesTheTargetHpPacketLayout() {
		assertEquals(TYPE.HP, NpcLifeStats.packetType(TYPE.NATURAL_HP));
		assertEquals(TYPE.DAMAGE, NpcLifeStats.packetType(TYPE.DAMAGE));
	}

	@Test
	void hpMpAndRestoreShareOneLockInstance() throws Exception {
		TestLifeStats stats = new TestLifeStats();

		// 结构护栏：HP/MP/恢复任务必须共用同一把锁（每个生物 3 把锁曾占全服 ≈18 MB）。
		// Structural guard: HP, MP and the restore task must share one lock instance.
		ReentrantLock lifeLock = (ReentrantLock) field(stats, "lifeLock");
		assertSame(lifeLock, field(stats, "hpLock"));
		assertSame(lifeLock, field(stats, "mpLock"));
		assertSame(lifeLock, stats.restoreLockInstance());
	}

	@Test
	void concurrentSubclassMutationsThroughTheSharedLockNeverLoseAnUpdate() throws Exception {
		TestLifeStats stats = new TestLifeStats();
		int threads = 8;
		int iterations = 2_000;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		try {
			for (int thread = 0; thread < threads; thread++) {
				futures.add(pool.submit(() -> {
					try {
						start.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new AssertionError(e);
					}
					for (int i = 0; i < iterations; i++) {
						stats.mutateUnderRestoreLock();
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		} finally {
			pool.shutdownNow();
		}

		// 单个实例上的临界区必须真的互斥：丢更新说明锁没生效或不再唯一。
		// The shared lock must really be mutual exclusion on one instance: a lost update means it is broken.
		assertEquals(threads * iterations, stats.mutated());
	}

	private static Object field(Object target, String name) throws Exception {
		Field field = CreatureLifeStats.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	private static final class TestLifeStats extends CreatureLifeStats<Creature> {

		private int mutated;

		private TestLifeStats() {
			super(null, 100, 100);
		}

		private void mutateUnderRestoreLock() {
			restoreLock.lock();
			try {
				mutated++;
			} finally {
				restoreLock.unlock();
			}
		}

		private int mutated() {
			return mutated;
		}

		private ReentrantLock restoreLockInstance() {
			return (ReentrantLock) restoreLock;
		}

		@Override
		protected void onIncreaseMp(TYPE type, int value, int skillId, LOG log) {
		}

		@Override
		protected void onReduceMp() {
		}

		@Override
		protected void onIncreaseHp(TYPE type, int value, int skillId, LOG log) {
		}

		@Override
		protected void onReduceHp() {
		}
	}
}
