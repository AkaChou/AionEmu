package com.aionemu.gameserver.model.stats.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatAddFunction;

/**
 * 属性表与配套读写锁的懒物化契约。
 * Lazily materialised stats map and lock contract.
 */
class CreatureGameStatsTest {

	@Test
	void statsMapAndLockStayUnallocatedUntilTheFirstEffect() throws ReflectiveOperationException {
		TestGameStats untouched = new TestGameStats();
		TestGameStats other = new TestGameStats();

		assertSame(stats(untouched), stats(other), "未登记属性函数时应共享同一占位符");
		assertNull(lock(untouched), "未登记属性函数时不应持有读写锁");

		// 只读路径既不物化也不加锁：占位符等价于"没有任何属性函数"，直接返回基础值。
		// Read paths materialise nothing and take no lock: the placeholder means "no stat functions at all".
		assertEquals(100, untouched.getStat(StatEnum.POWER, 100).getCurrent());
		assertNull(untouched.getSetStatValue(StatEnum.POWER));
		assertNull(lock(untouched), "只读路径不应物化锁");
		assertSame(stats(untouched), stats(other));

		// 无函数的 endEffect 也不物化（等价于原来遍历空表），且必须照旧执行 onStatsChange（此处被测试替身屏蔽）。
		// endEffect without any function materialises nothing and still runs onStatsChange (no-op in the test double).
		untouched.endEffect(new StatOwner() {
		});
		assertNull(lock(untouched), "无函数的 endEffect 不应物化锁");
	}

	@Test
	void firstEffectMaterialisesLockAndMapAndStaysMaterialisedAfterRemoval() throws ReflectiveOperationException {
		TestGameStats stats = new TestGameStats();
		StatOwner owner = new StatOwner() {
		};

		stats.addEffectOnly(owner, List.of(new StatAddFunction(StatEnum.POWER, 50, false)));

		assertNotNull(lock(stats), "首次写入必须物化读写锁");
		assertFalse(stats(stats).isEmpty(), "首次写入必须物化属性表");
		assertEquals(150, stats.getStat(StatEnum.POWER, 100).getCurrent());

		stats.endEffect(owner);

		assertEquals(100, stats.getStat(StatEnum.POWER, 100).getCurrent(), "结束效果后修正应被移除");
		assertFalse(stats(stats).isEmpty(), "表一旦物化就不回退到占位符（其他线程可能仍持有它）");
	}

	@Test
	void concurrentFirstWritesConvergeOnOneMapAndOneLock() throws Exception {
		// 并发首次写入必须收敛到同一张表和同一把锁；否则先加入的属性函数会落进被丢弃的表而永久失效，
		// 或者写者用一把锁、读者用另一把锁而完全不互斥。
		// Concurrent first writes must converge on one map and one lock, otherwise early functions land in a
		// discarded map and writers/readers would end up using different locks.
		TestGameStats stats = new TestGameStats();
		int threads = 8;
		int perThread = 16;
		StatEnum[] names = StatEnum.values();
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
					for (int i = 0; i < perThread; i++) {
						StatEnum name = names[i % names.length];
						stats.addEffectOnly(new StatOwner() {
						}, List.of(new StatAddFunction(name, 1, true)));
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

		assertNotNull(lock(stats));
		int functions = 0;
		for (Set<IStatFunction> perStat : stats(stats).values()) {
			functions += perStat.size();
		}
		assertEquals(threads * perThread, functions, "并发首次写入的函数必须全部落在同一张表里");
	}

	@Test
	void readsRacingTheFirstWriteNeverSeeANullLock() throws Exception {
		// 关键不变式：写者先发布锁、再发布表，所以读到"非占位符表"的线程必然也能读到锁；顺序反了这里会 NPE。
		// The publisher writes the lock before the map, so a reader that sees a materialised map always sees the
		// lock too; with the order inverted this test would NPE inside readLock().
		TestGameStats stats = new TestGameStats();
		AtomicBoolean writerDone = new AtomicBoolean();
		ExecutorService pool = Executors.newFixedThreadPool(3);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		try {
			futures.add(pool.submit(() -> {
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
				for (int i = 0; i < 2_000; i++) {
					stats.addEffectOnly(new StatOwner() {
					}, List.of(new StatAddFunction(StatEnum.POWER, 1, true)));
				}
				writerDone.set(true);
				return null;
			}));
			for (int reader = 0; reader < 2; reader++) {
				futures.add(pool.submit(() -> {
					try {
						start.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new AssertionError(e);
					}
					while (!writerDone.get()) {
						stats.getStat(StatEnum.POWER, 100).getCurrent();
						stats.getItemStatBoost(StatEnum.POWER, new AdditionStat(StatEnum.POWER, 100, null));
						stats.getSetStatValue(StatEnum.POWER);
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

		assertNotNull(lock(stats));
		assertFalse(stats(stats).isEmpty());
	}

	@SuppressWarnings("unchecked")
	private static Map<StatEnum, TreeSet<IStatFunction>> stats(CreatureGameStats<?> target)
			throws ReflectiveOperationException {
		return (Map<StatEnum, TreeSet<IStatFunction>>) field(target, "stats");
	}

	private static ReentrantReadWriteLock lock(CreatureGameStats<?> target) throws ReflectiveOperationException {
		return (ReentrantReadWriteLock) field(target, "lock");
	}

	private static Object field(CreatureGameStats<?> target, String name) throws ReflectiveOperationException {
		Field field = CreatureGameStats.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}

	/**
	 * 测试用容器：owner 用 Objenesis 造的裸 {@link Npc}（{@code isPlayer()} 等基础判定可用），
	 * 并屏蔽 {@code onStatsChange} 的 owner 副作用——本用例只验证属性表与锁的懒物化。
	 * Test double whose owner is a constructor-less Npc and whose onStatsChange is a no-op.
	 */
	private static final class TestGameStats extends NpcGameStats {

		private TestGameStats() {
			super(new ObjenesisStd().newInstance(Npc.class));
		}

		@Override
		protected void onStatsChange() {
		}
	}
}
