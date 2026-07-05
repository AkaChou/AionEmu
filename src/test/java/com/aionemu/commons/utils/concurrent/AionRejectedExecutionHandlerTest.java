package com.aionemu.commons.utils.concurrent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class AionRejectedExecutionHandlerTest {

	@Test
	void rejectedTaskRunsOnRejectingThreadEvenWhenCallerHasHighPriority() throws Exception {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
		AtomicReference<Thread> rejectingThread = new AtomicReference<>();
		AtomicReference<Thread> executingThread = new AtomicReference<>();
		CountDownLatch taskExecuted = new CountDownLatch(1);
		Thread caller = new Thread(() -> {
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 1);
			rejectingThread.set(Thread.currentThread());
			new AionRejectedExecutionHandler().rejectedExecution(() -> {
				executingThread.set(Thread.currentThread());
				taskExecuted.countDown();
			}, executor);
		}, "rejection-caller");

		try {
			caller.start();
			caller.join(1000);

			assertFalse(caller.isAlive());
			assertTrue(taskExecuted.await(1, TimeUnit.SECONDS));
			assertSame(rejectingThread.get(), executingThread.get());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void rejectionBackpressureDoesNotBuildSyntheticStackTrace() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/commons/utils/concurrent/AionRejectedExecutionHandler.java"));

		assertFalse(source.contains("new RejectedExecutionException()"));
	}
}
