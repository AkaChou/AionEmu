package com.aionemu.gameserver.ai2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.ai.RetailPatternAI2;

/**
 * AI 死亡监听器列表的懒物化契约。
 * Lazily materialised death-listener list contract.
 */
class AbstractAIDeathListenerListTest {

	@Test
	void deathListenerListStaysOnTheSharedPlaceholderUntilTheFirstListener() throws ReflectiveOperationException {
		// 未注册监听器时两个 AI 共享同一占位符（即真的没有预制 COW 列表），首次写入才物化。
		// Untouched AIs share the placeholder (no COW list is pre-created) and only diverge after the first write.
		AbstractAI untouched = new RetailPatternAI2();
		AbstractAI other = new RetailPatternAI2();
		assertSame(deathListeners(untouched), deathListeners(other), "未注册监听器时应共享同一占位符");

		AbstractAI.AiDeathListener listener = obj -> {
		};
		assertDoesNotThrow(() -> untouched.removeAiDeathListener(listener));
		assertTrue(deathListeners(untouched).isEmpty(), "空占位符上的 remove/isEmpty 应是安全空操作");

		untouched.addAiDeathListener(listener);

		assertNotSame(deathListeners(untouched), deathListeners(other), "首次注册后应物化出独立列表");
		assertEquals(List.of(listener), deathListeners(untouched));
		assertTrue(deathListeners(other).isEmpty());
	}

	@Test
	void concurrentFirstListenerRegistrationsConvergeOnOneList() throws Exception {
		// 并发首次注册必须收敛到同一列表，否则先注册的监听器会落进被丢弃的列表而永远不会被回调。
		// Concurrent first registrations must converge on one list or the first listeners never get called back.
		AbstractAI ai = new RetailPatternAI2();
		int threads = 8;
		int perThread = 32;
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
						ai.addAiDeathListener(obj -> {
						});
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

		assertEquals(threads * perThread, deathListeners(ai).size());
	}

	@SuppressWarnings("unchecked")
	private static List<AbstractAI.AiDeathListener> deathListeners(AbstractAI target) throws ReflectiveOperationException {
		Field field = AbstractAI.class.getDeclaredField("aiDeathListeners");
		field.setAccessible(true);
		return (List<AbstractAI.AiDeathListener>) field.get(target);
	}
}
