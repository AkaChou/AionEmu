package com.aionemu.gameserver.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

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

	private static ThreadPoolExecutor threadPool(ThreadPoolManager manager, String fieldName) throws Exception {
		Field field = ThreadPoolManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (ThreadPoolExecutor) field.get(manager);
	}
}
