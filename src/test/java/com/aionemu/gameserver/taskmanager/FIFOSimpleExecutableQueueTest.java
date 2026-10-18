package com.aionemu.gameserver.taskmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.lifecycle.TestServiceProviders;
import com.aionemu.gameserver.utils.ThreadPoolManager;

class FIFOSimpleExecutableQueueTest {

	private static ThreadPoolManager threadPoolManager;

	@BeforeAll
	static void installThreadPoolProvider() {
		threadPoolManager = new ThreadPoolManager();
		TestServiceProviders.install(ThreadPoolManager.class, threadPoolManager);
	}

	@AfterAll
	static void clearThreadPoolProvider() {
		TestServiceProviders.clear(ThreadPoolManager.class);
		threadPoolManager.shutdown();
	}

	@Test
	void executesQueuedTasksInFifoOrder() throws InterruptedException {
		TestQueue queue = new TestQueue(3);

		queue.execute("first");
		queue.executeAll(List.of("second", "third"));

		assertTrue(queue.await());
		assertEquals(List.of("first", "second", "third"), queue.executedTasks);
	}

	private static final class TestQueue extends FIFOSimpleExecutableQueue<String> {

		private final CountDownLatch latch;
		private final List<String> executedTasks = Collections.synchronizedList(new ArrayList<>());

		private TestQueue(int expectedTasks) {
			latch = new CountDownLatch(expectedTasks);
		}

		private boolean await() throws InterruptedException {
			return latch.await(5, TimeUnit.SECONDS);
		}

		@Override
		protected void removeAndExecuteFirst() {
			executedTasks.add(removeFirst());
			latch.countDown();
		}
	}
}
