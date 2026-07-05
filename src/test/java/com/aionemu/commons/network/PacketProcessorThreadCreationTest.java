package com.aionemu.commons.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PacketProcessorThreadCreationTest {

	@Test
	void processorThreadsAreCreatedThroughAThreadFactory() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/PacketProcessor.java"));

		assertTrue(source.contains("ThreadFactory"));
		assertFalse(source.contains("new Thread("));
	}

	@Test
	void checkerThreadKeepsRunningUntilInterrupted() throws Exception {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/commons/network/PacketProcessor.java"));

		assertTrue(source.contains("while (!Thread.currentThread().isInterrupted())"));
		assertTrue(source.contains("Thread.currentThread().interrupt();"));
		assertFalse(source.contains("// Ignore interruption"));
	}

	@Test
	void killedIdleProcessorThreadStopsWaitingForPackets() throws Exception {
		RecordingThreadFactory threadFactory = new RecordingThreadFactory();
		PacketProcessor<AConnection> processor = new PacketProcessor<>(1, 2, 1, 1, directExecutor(), threadFactory);
		try {
			Method newThread = PacketProcessor.class.getDeclaredMethod("newThread");
			newThread.setAccessible(true);
			assertTrue((Boolean) newThread.invoke(processor));

			Thread extraWorker = awaitThread(threadFactory, "PacketProcessor:1");
			awaitState(extraWorker, Thread.State.WAITING);

			Method killThread = PacketProcessor.class.getDeclaredMethod("killThread");
			killThread.setAccessible(true);
			killThread.invoke(processor);

			extraWorker.join(TimeUnit.SECONDS.toMillis(1));
			assertFalse(extraWorker.isAlive());
		} finally {
			threadFactory.interruptAll();
		}
	}

	private static Executor directExecutor() {
		return Runnable::run;
	}

	private static Thread awaitThread(RecordingThreadFactory threadFactory, String name) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
		while (System.nanoTime() < deadline) {
			for (Thread thread : threadFactory.threads()) {
				if (name.equals(thread.getName())) {
					return thread;
				}
			}
			Thread.sleep(10);
		}
		throw new AssertionError("Thread not created: " + name);
	}

	private static void awaitState(Thread thread, Thread.State state) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
		while (System.nanoTime() < deadline) {
			if (thread.getState() == state) {
				return;
			}
			Thread.sleep(10);
		}
		throw new AssertionError("Thread " + thread.getName() + " did not reach state " + state + ", current state: " + thread.getState());
	}

	private static final class RecordingThreadFactory implements ThreadFactory {
		private final List<Thread> threads = new ArrayList<>();

		@Override
		public synchronized Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);
			threads.add(thread);
			return thread;
		}

		synchronized List<Thread> threads() {
			return new ArrayList<>(threads);
		}

		synchronized void interruptAll() {
			for (Thread thread : threads) {
				thread.interrupt();
			}
		}
	}
}
