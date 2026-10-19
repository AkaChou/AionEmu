package com.aionemu.commons.utils.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.testutil.LocalizedMessages;
import com.aionemu.testutil.LogCapture;
import com.aionemu.testutil.ServiceContextUnwind;
import com.google.common.util.concurrent.MoreExecutors;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.objenesis.ObjenesisStd;

/**
 * 使用无工作线程的执行器验证三类线程池管理器的关闭摘要、幂等和异常分级。
 * Uses executors without workers to verify shutdown summaries, idempotency and failure levels of all three managers.
 */
@Slf4j
class ThreadPoolShutdownLoggingTest {

	@AfterEach
	void unwindContext() {
		ServiceContextUnwind.unwind();
	}

	private static final List<Class<?>> TYPES = List.of(
		com.aionemu.gameserver.utils.ThreadPoolManager.class,
		com.aionemu.loginserver.utils.ThreadPoolManager.class,
		com.aionemu.commons.network.util.ThreadPoolManager.class);

	@Test
	void cleanShutdownProducesOneInfoSummaryEvenWhenRepeated() throws Exception {
		for (Class<?> type : TYPES) {
			try (Pools pools = new Pools(type); LocalizedMessages messages = new LocalizedMessages()) {
				var eventAppender = captureEvents(type);
				pools.shutdown();
				pools.shutdown();
				var events = events(eventAppender);
				assertEquals(1, events.stream().filter(event -> event.getLevel() == Level.INFO).count());
				assertEquals(0, events.stream().filter(event -> event.getLevel() == Level.WARN).count());
				assertEquals(1, pools.scheduled.shutdownCalls);
				assertEquals(1, pools.instant.shutdownCalls);
				assertEquals(pools.network ? 0 : 1, pools.longRunning.shutdownCalls);
			}
		}
	}

	@Test
	void failedShutdownKeepsCauseAndContinuesOtherPools() throws Exception {
		for (Class<?> type : TYPES) {
			try (Pools pools = new Pools(type); LocalizedMessages messages = new LocalizedMessages();
				LogCapture logs = new LogCapture(type)) {
				pools.scheduled.failShutdown = true;
				pools.shutdown();
				assertEquals(1, pools.instant.shutdownCalls);
				assertEquals(pools.network ? 0 : 1, pools.longRunning.shutdownCalls);
				assertNotNull(logs.events().stream().filter(event -> event.getLevel() == Level.ERROR)
					.findFirst().orElseThrow().getThrowableProxy());
				assertEquals(1, logs.events().stream().filter(event -> event.getLevel() == Level.WARN).count());
			}
		}
	}

	@Test
	void incompleteTerminationAndRemainingTasksAreWarnings() throws Exception {
		for (Class<?> type : TYPES) {
			try (Pools pools = new Pools(type); LocalizedMessages messages = new LocalizedMessages();
				LogCapture logs = new LogCapture(type)) {
				RecordingPool remaining = pools.network ? pools.instant : pools.longRunning;
				remaining.reportTerminated = false;
				remaining.getQueue().add(new QueuedTask());
				pools.shutdown();
				var result = logs.events().stream().filter(event -> event.getLevel() == Level.WARN)
					.findFirst().orElseThrow();
				assertTrue(result.getFormattedMessage().contains("1"));
				assertEquals(0, logs.events().stream().filter(event -> event.getLevel() == Level.INFO).count());
				assertTrue(pools.scheduled.awaitCalls >= (pools.network ? 1 : 2));
				assertTrue(remaining.awaitCalls >= (pools.network ? 1 : 2));
			}
		}
	}

	@Test
	void concurrentShutdownProducesOneResult() throws Exception {
		for (Class<?> type : TYPES) {
			try (Pools pools = new Pools(type); LocalizedMessages messages = new LocalizedMessages();
				LogCapture logs = new LogCapture(type);
				var executor = Executors.newFixedThreadPool(2)) {
				CountDownLatch start = new CountDownLatch(1);
				var first = executor.submit(() -> {
					start.await();
					pools.shutdown();
					return null;
				});
				var second = executor.submit(() -> {
					start.await();
					pools.shutdown();
					return null;
				});
				start.countDown();
				first.get(5, TimeUnit.SECONDS);
				second.get(5, TimeUnit.SECONDS);
				assertEquals(1, logs.events().stream().filter(event -> event.getLevel() == Level.INFO).count());
				assertEquals(1, pools.scheduled.shutdownCalls);
			}
		}
	}

	@Test
	void interruptedWaitRetainsWarningAndRestoresInterrupt() throws Exception {
		for (Class<?> type : TYPES) {
			try (Pools pools = new Pools(type); LocalizedMessages messages = new LocalizedMessages();
				LogCapture logs = new LogCapture(type)) {
				pools.scheduled.interruptWait = true;
				pools.shutdown();
				assertTrue(logs.events().stream().anyMatch(event -> event.getLevel() == Level.WARN));
				assertTrue(Thread.currentThread().isInterrupted());
			} finally {
				Thread.interrupted();
			}
		}
	}

	@Test
	void heartbeatHandoffKeepsStandardSlowTaskWarning() throws Exception {
		com.aionemu.gameserver.utils.ThreadPoolManager manager =
			new ObjenesisStd().newInstance(com.aionemu.gameserver.utils.ThreadPoolManager.class);
		RecordingPool heartbeatPool = new RecordingPool();
		Field field = com.aionemu.gameserver.utils.ThreadPoolManager.class.getDeclaredField("longRunningPool");
		field.setAccessible(true);
		ThreadPoolExecutor previous = (ThreadPoolExecutor) field.get(manager);
		field.set(manager, heartbeatPool);
		field = com.aionemu.gameserver.utils.ThreadPoolManager.class.getDeclaredField("shutdownStarted");
		field.setAccessible(true);
		field.set(manager, new AtomicBoolean());
		Runnable heartbeat = () -> {
		};

		try {
			manager.executeHeartbeat(heartbeat);

			assertEquals(1, heartbeatPool.submitted.size());
			assertEquals(heartbeat, heartbeatPool.submitted.getFirst());
		} finally {
			field.set(manager, previous);
		}
	}

	@Test
	void heartbeatIsNotSubmittedAfterShutdown() throws Exception {
		for (Class<?> type : List.of(com.aionemu.gameserver.utils.ThreadPoolManager.class,
			com.aionemu.loginserver.utils.ThreadPoolManager.class)) {
			Object manager = type.getConstructor().newInstance();
			RecordingPool heartbeatPool = new RecordingPool();
			Field field = type.getDeclaredField("longRunningPool");
			field.setAccessible(true);
			ThreadPoolExecutor previous = (ThreadPoolExecutor) field.get(manager);
			field.set(manager, heartbeatPool);
			try (LocalizedMessages messages = new LocalizedMessages()) {
				type.getMethod("shutdown").invoke(manager);
				type.getMethod("executeHeartbeat", Runnable.class).invoke(manager, (Runnable) () -> {
				});
				assertTrue(heartbeatPool.submitted.isEmpty());
			} finally {
				heartbeatPool.shutdownNow();
				field.set(manager, previous);
				previous.shutdownNow();
			}
		}
	}

	/** 绕过生产线程池构造，只注入无需启动的执行器。 / Bypasses production construction and injects idle executors. */
	@SuppressWarnings("unchecked")
	private static final class Pools implements AutoCloseable {
		private final Object manager;
		private final boolean network;
		private final RecordingPool scheduled = new RecordingPool();
		private final RecordingPool instant = new RecordingPool();
		private final RecordingPool longRunning = new RecordingPool();
		private final ForkJoinPool fork = new ForkJoinPool(1);

		private Pools(Class<?> type) throws Exception {
			manager = new ObjenesisStd().newInstance(type);
			network = type == com.aionemu.commons.network.util.ThreadPoolManager.class;
			if (network) {
				set("scheduledThreadPoolExecutor", scheduled);
				set("generalPacketsThreadPoolExecutor", instant);
				set("scheduledThreadPool", MoreExecutors.listeningDecorator(scheduled));
				set("generalPacketsThreadPool", MoreExecutors.listeningDecorator(instant));
			} else {
				set("scheduledPool", scheduled);
				set("instantPool", instant);
				set("longRunningPool", longRunning);
			}
			set("shutdownStarted", new AtomicBoolean());
			if (type == com.aionemu.gameserver.utils.ThreadPoolManager.class) {
				set("workStealingPool", fork);
			}
		}

		private void set(String name, Object value) throws Exception {
			Field field = manager.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.set(manager, value);
		}

		private void shutdown() throws Exception {
			manager.getClass().getMethod("shutdown").invoke(manager);
		}

		@Override
		public void close() {
			scheduled.shutdownNow();
			instant.shutdownNow();
			longRunning.shutdownNow();
			fork.shutdownNow();
		}
	}

	/** 捕获线程池专用事件，不依赖控制台 appender 的消息前缀过滤。 / Captures pool events independently of console prefix filtering. */
	private record CapturedEvents(Logger logger, ListAppender<ILoggingEvent> appender, Level previousLevel) {
	}

	private static CapturedEvents captureEvents(Class<?> type) {
		Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(type.getName());
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.setContext(logger.getLoggerContext());
		appender.start();
		Level previous = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(appender);
		return new CapturedEvents(logger, appender, previous);
	}

	private static List<ILoggingEvent> events(CapturedEvents capture) {
		capture.logger().detachAppender(capture.appender());
		capture.appender().stop();
		capture.logger().setLevel(capture.previousLevel());
		return List.copyOf(capture.appender().list);
	}

	/** 可入延迟队列的剩余任务，不调度工作线程。 / Residual delayed-queue task without scheduling a worker. */
	private static final class QueuedTask extends FutureTask<Void> implements RunnableScheduledFuture<Void> {
		private QueuedTask() {
			super(() -> {
			}, null);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(1, TimeUnit.DAYS);
		}

		@Override
		public int compareTo(Delayed other) {
			return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
		}

		@Override
		public boolean isPeriodic() {
			return false;
		}
	}

	/** 可控关闭失败及等待结果，无需睡眠。 / Controlled shutdown failures and wait results without sleeping. */
	private static final class RecordingPool extends ScheduledThreadPoolExecutor {
		private int shutdownCalls;
		private int awaitCalls;
		private final List<Runnable> submitted = new ArrayList<>();
		private boolean failShutdown;
		private boolean interruptWait;
		private boolean reportTerminated = true;

		private RecordingPool() {
			super(1);
		}

		@Override
		public void shutdown() {
			shutdownCalls++;
			if (failShutdown) {
				throw new IllegalStateException("executor shutdown failed");
			}
			if (reportTerminated) {
				super.shutdown();
			}
		}

		@Override
		public void execute(Runnable command) {
			submitted.add(command);
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			awaitCalls++;
			if (interruptWait) {
				throw new InterruptedException("shutdown wait interrupted");
			}
			return isTerminated();
		}
	}
}
