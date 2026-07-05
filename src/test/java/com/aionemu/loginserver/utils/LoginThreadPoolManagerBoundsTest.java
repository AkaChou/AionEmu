package com.aionemu.loginserver.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.jupiter.api.Test;

class LoginThreadPoolManagerBoundsTest {

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

	private static ThreadPoolExecutor threadPool(ThreadPoolManager manager, String fieldName) throws Exception {
		Field field = ThreadPoolManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (ThreadPoolExecutor) field.get(manager);
	}
}
