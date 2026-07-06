package com.aionemu.commons.network.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aionemu.commons.services.ServiceContext;
import com.aionemu.commons.utils.AionRuntimeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CommonsNetworkThreadPoolManagerBoundsTest {

	@AfterEach
	void clearRuntimeMode() {
		System.clearProperty(AionRuntimeMode.BOOT_EMBEDDED_PROPERTY);
	}

	@Test
	void packetPoolUsesFiniteThreadsAndQueueBackpressure() throws Exception {
		AionRuntimeMode.enableBootEmbeddedMode();
		ThreadPoolManager manager = new ThreadPoolManager();
		try {
			ThreadPoolExecutor executor = threadPool(manager, "generalPacketsThreadPoolExecutor");

			assertTrue(executor.getMaximumPoolSize() < Integer.MAX_VALUE);
			assertFalse(executor.getQueue() instanceof SynchronousQueue);
		} finally {
			manager.shutdown();
		}
	}

	@Test
	void shutdownRestoresInterruptedStatus() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/util/ThreadPoolManager.java"));

		assertTrue(source.contains("Thread.currentThread().interrupt();"));
	}

	@Test
	void scheduledTasksPreserveSubmitterServiceContext() throws Exception {
		ThreadPoolManager manager = new ThreadPoolManager();
		try {
			warmUpScheduledThread(manager);
			AtomicReference<String> observedContext = new AtomicReference<>();
			CountDownLatch ran = new CountDownLatch(1);

			try (ServiceContext.Scope ignored = ServiceContext.use("login")) {
				manager.schedule(() -> {
					observedContext.set(ServiceContext.current());
					ran.countDown();
				}, 1);
			}

			assertTrue(ran.await(5, TimeUnit.SECONDS));
			assertEquals("login", observedContext.get());
		} finally {
			manager.shutdown();
		}
	}

	@Test
	void fixedRateTasksPreserveSubmitterServiceContext() throws Exception {
		ThreadPoolManager manager = new ThreadPoolManager();
		try {
			warmUpScheduledThread(manager);
			AtomicReference<String> observedContext = new AtomicReference<>();
			CountDownLatch ran = new CountDownLatch(1);

			try (ServiceContext.Scope ignored = ServiceContext.use("login")) {
				manager.scheduleAtFixedRate(() -> {
					observedContext.compareAndSet(null, ServiceContext.current());
					ran.countDown();
				}, 1, 1000);
			}

			assertTrue(ran.await(5, TimeUnit.SECONDS));
			assertEquals("login", observedContext.get());
		} finally {
			manager.shutdown();
		}
	}

	private static void warmUpScheduledThread(ThreadPoolManager manager) throws InterruptedException {
		CountDownLatch warm = new CountDownLatch(1);
		try (ServiceContext.Scope ignored = ServiceContext.use("game")) {
			manager.schedule(warm::countDown, 1);
		}
		assertTrue(warm.await(5, TimeUnit.SECONDS));
	}

	private static ThreadPoolExecutor threadPool(ThreadPoolManager manager, String fieldName) throws Exception {
		Field field = ThreadPoolManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (ThreadPoolExecutor) field.get(manager);
	}
}
