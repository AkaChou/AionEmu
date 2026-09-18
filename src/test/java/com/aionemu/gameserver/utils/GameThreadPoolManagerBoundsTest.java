package com.aionemu.gameserver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import com.aionemu.gameserver.configs.main.ThreadConfig;
import com.aionemu.testutil.ConfigSnapshot;

import org.junit.jupiter.api.Test;

class GameThreadPoolManagerBoundsTest {

	@Test
	void instantPoolUsesTheAlreadyCpuScaledConfiguration() {
		assertEquals(1, ThreadPoolManager.instantPoolSize(0));
		assertEquals(60, ThreadPoolManager.instantPoolSize(60));
	}

	@Test
	void longRunningPoolUsesFiniteThreadsAndQueueBackpressure() throws Exception {
		ThreadPoolManager manager = new ThreadPoolManager();
		try {
			ThreadPoolExecutor executor = threadPool(manager, "longRunningPool");

			assertTrue(executor.getMaximumPoolSize() < Integer.MAX_VALUE);
			assertFalse(executor.getQueue() instanceof SynchronousQueue);
		} finally {
			manager.shutdown();
		}
	}

	@Test
	void purgeTaskIsScheduledAsRunnableNotThreadWrapper() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/utils/ThreadPoolManager.java"));

		assertFalse(source.contains("new Thread(new Runnable()"));
		assertTrue(source.contains("scheduleAtFixedRate(new Runnable()"));
	}

	/**
	 * 注入的线程配置必须决定池大小，而不是构造时的静态门面。
	 * The injected thread configuration must decide the pool sizes, not the static facade used at construction time.
	 */
	@Test
	void injectedThreadConfigDecidesPoolSizes() throws Exception {
		ConfigSnapshot snapshot = ConfigSnapshot.of(ThreadConfig.class,
			"BASE_THREAD_POOL_SIZE", "EXTRA_THREAD_PER_CORE", "THREAD_POOL_SIZE");
		ThreadConfig config = new ThreadConfig();
		try {
			config.setBasepoolsize(7);
			config.setThreadpercore(3);

			ThreadPoolManager manager = new ThreadPoolManager(config);
			try {
				int processors = Runtime.getRuntime().availableProcessors();
				assertEquals((7 + 3) * processors, instantPool(manager).getCorePoolSize());
				assertEquals(3 * processors, scheduledPool(manager).getCorePoolSize());
			} finally {
				manager.shutdown();
			}
		} finally {
			snapshot.restore();
		}
	}

	private static ThreadPoolExecutor instantPool(ThreadPoolManager manager) throws Exception {
		return threadPool(manager, "instantPool");
	}

	private static ThreadPoolExecutor scheduledPool(ThreadPoolManager manager) throws Exception {
		return (ThreadPoolExecutor) threadPool(manager, "scheduledPool");
	}

	private static ThreadPoolExecutor threadPool(ThreadPoolManager manager, String fieldName) throws Exception {
		Field field = ThreadPoolManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (ThreadPoolExecutor) field.get(manager);
	}
}
