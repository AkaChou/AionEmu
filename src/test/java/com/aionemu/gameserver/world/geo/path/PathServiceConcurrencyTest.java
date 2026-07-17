package com.aionemu.gameserver.world.geo.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.aionemu.gameserver.configs.main.GeoDataConfig;
import org.junit.jupiter.api.Test;

class PathServiceConcurrencyTest {

	@Test
	void boundsConcurrentAStarSearchesWithoutBlockingMoveThreads() throws Exception {
		PathService service = new PathService();
		Field field = PathService.class.getDeclaredField("pathfinders");
		field.setAccessible(true);
		ThreadPoolExecutor pathfinders = (ThreadPoolExecutor) field.get(service);

		assertTrue(pathfinders.getMaximumPoolSize() >= 1 && pathfinders.getMaximumPoolSize() <= 8);
		assertEquals(pathfinders.getCorePoolSize(), pathfinders.getMaximumPoolSize());
		assertTrue(pathfinders.getQueue() instanceof PriorityBlockingQueue);
		service.destroy();
		assertTrue(pathfinders.isShutdown());
	}

	@Test
	void ordersByPriorityThenSubmissionSequence() {
		assertTrue(PathService.comparePriority(0, 2, 1, 1) < 0);
		assertTrue(PathService.comparePriority(1, 1, 1, 2) < 0);
	}

	@Test
	void resolvesWorkerCountFromConfigOrCpu() {
		assertEquals(1, PathService.workerCount(0, 1));
		assertEquals(4, PathService.workerCount(0, 8));
		assertEquals(8, PathService.workerCount(0, 32));
		assertEquals(6, PathService.workerCount(6, 2));
	}

	@Test
	void resolvesQueueCapacityWithSensibleDefault() {
		assertEquals(256, PathService.queueCapacity(0));
		assertEquals(512, PathService.queueCapacity(512));
	}

	@Test
	void givesLocationRequestsEnoughTimeWithoutDelayingTargetRequests() {
		int oldTimeout = GeoDataConfig.GEO_PATH_TIMEOUT_MS;
		try {
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = 250;
			assertEquals(250, PathService.requestTimeout(0));
			assertEquals(1_000, PathService.requestTimeout(1));
		} finally {
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = oldTimeout;
		}
	}

	@Test
	void tracksObstacleVersionsPerInstance() {
		PathService service = new PathService();
		service.obstacleChanged(100, 1);
		assertEquals(1, service.obstacleVersion(100, 1));

		service.instanceDestroyed(100, 1);

		assertEquals(0, service.obstacleVersion(100, 1));
		assertEquals(0, service.obstacleVersion(100, 2));
	}

	@Test
	void returnsSlowRequestsWithoutBlockingTheCaller() throws Exception {
		int oldTimeout = GeoDataConfig.GEO_PATH_TIMEOUT_MS;
		GeoDataConfig.GEO_PATH_TIMEOUT_MS = 1000;
		try {
			PathService service = new PathService();
			long start = System.nanoTime();

			CompletableFuture<float[][]> result = service.executeAsync(1, () -> {
				TimeUnit.MILLISECONDS.sleep(200);
				return new float[0][];
			});

			assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 100);
			assertFalse(result.isDone());
			assertEquals(0, result.get(1, TimeUnit.SECONDS).length);
		} finally {
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = oldTimeout;
		}
	}

	@Test
	void reportsWorkerFailuresSeparatelyFromNoPath() {
		PathService service = new PathService();
		try {
			CompletableFuture<float[][]> result = service.executeAsync(1, () -> {
				throw new IllegalStateException("broken worker");
			});

			assertThrows(ExecutionException.class, () -> result.get(1, TimeUnit.SECONDS));
			assertEquals(1, service.metrics().failed());
		} finally {
			service.destroy();
		}
	}

	@Test
	void distinguishesDefinitiveSearchFailuresFromQueuePressure() {
		assertFalse(PathService.isDefinitivePathFailure(
				new PathService.IncompletePathSearchException(PathData.SearchStatus.NODE_LIMIT, 10)));
		assertTrue(PathService.isDefinitivePathFailure(
				new PathService.IncompletePathSearchException(PathData.SearchStatus.NO_PATH, 10)));
		assertFalse(PathService.isDefinitivePathFailure(new TimeoutException()));
		assertFalse(PathService.isDefinitivePathFailure(new RejectedExecutionException()));
	}

	@Test
	void classifiesPathResultsWithoutCollapsingTransientFailures() {
		assertEquals(PathService.PathResultStatus.FOUND, PathService.resultStatus(new float[0][], null));
		assertEquals(PathService.PathResultStatus.NO_PATH, PathService.resultStatus(null, null));
		assertEquals(PathService.PathResultStatus.NODE_LIMIT, PathService.resultStatus(null,
				new PathService.IncompletePathSearchException(PathData.SearchStatus.NODE_LIMIT, 10)));
		assertEquals(PathService.PathResultStatus.INTERRUPTED, PathService.resultStatus(null,
				new PathService.IncompletePathSearchException(PathData.SearchStatus.INTERRUPTED, 10)));
		assertEquals(PathService.PathResultStatus.TIMEOUT, PathService.resultStatus(null, new TimeoutException()));
		assertEquals(PathService.PathResultStatus.QUEUE_EXPIRED,
				PathService.resultStatus(null, new PathService.QueueExpiredException("queued")));
		assertEquals(PathService.PathResultStatus.REJECTED,
				PathService.resultStatus(null, new RejectedExecutionException()));
		assertEquals(PathService.PathResultStatus.CANCELLED,
				PathService.resultStatus(null, new CancellationException()));
		assertEquals(PathService.PathResultStatus.FAILED,
				PathService.resultStatus(null, new IllegalStateException()));
	}

	@Test
	void timeoutWhileQueuedRemainsTransient() throws Exception {
		int oldTimeout = GeoDataConfig.GEO_PATH_TIMEOUT_MS;
		PathService service = new PathService();
		CountDownLatch workersStarted = new CountDownLatch(workerCount(service));
		CountDownLatch releaseWorkers = new CountDownLatch(1);
		try {
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = 5_000;
			for (int i = 0; i < workerCount(service); i++) {
				service.executeAsync(1, () -> {
					workersStarted.countDown();
					try {
						releaseWorkers.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return null;
				});
			}
			assertTrue(workersStarted.await(1, TimeUnit.SECONDS));
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = 10;
			CompletableFuture<float[][]> queued = service.executeAsync(0, () -> new float[0][]);

			ExecutionException failure = assertThrows(ExecutionException.class,
					() -> queued.get(1, TimeUnit.SECONDS));
			assertTrue(failure.getCause() instanceof PathService.QueueExpiredException);
			assertFalse(PathService.isDefinitivePathFailure(failure.getCause()));
			assertEquals(1, service.metrics().queueExpired());
			assertEquals(0, service.metrics().timedOut());
		} finally {
			releaseWorkers.countDown();
			service.destroy();
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = oldTimeout;
		}
	}

	@Test
	void shutdownCompletesQueuedRequests() throws Exception {
		int oldTimeout = GeoDataConfig.GEO_PATH_TIMEOUT_MS;
		GeoDataConfig.GEO_PATH_TIMEOUT_MS = 5000;
		try {
			PathService service = new PathService();
			CountDownLatch workersStarted = new CountDownLatch(workerCount(service));
			for (int i = 0; i < workerCount(service); i++) {
				service.executeAsync(1, () -> {
					workersStarted.countDown();
					try {
						TimeUnit.SECONDS.sleep(5);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return null;
				});
			}
			assertTrue(workersStarted.await(1, TimeUnit.SECONDS));
			CompletableFuture<float[][]> queued = service.executeAsync(1, () -> new float[0][]);

			service.destroy();

			assertThrows(CancellationException.class, () -> queued.get(1, TimeUnit.SECONDS));
		} finally {
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = oldTimeout;
		}
	}

	@Test
	void timesOutAndRecordsSlowRequests() throws Exception {
		int oldTimeout = GeoDataConfig.GEO_PATH_TIMEOUT_MS;
		GeoDataConfig.GEO_PATH_TIMEOUT_MS = 10;
		try {
			PathService service = new PathService();
			float[][] result = service.execute(0, () -> {
				TimeUnit.SECONDS.sleep(1);
				return new float[0][];
			});

			assertEquals(null, result);
			assertEquals(1, service.metrics().submitted());
			assertEquals(1, service.metrics().timedOut());
		} finally {
			GeoDataConfig.GEO_PATH_TIMEOUT_MS = oldTimeout;
		}
	}

	private static int workerCount(PathService service) throws Exception {
		Field field = PathService.class.getDeclaredField("pathfinders");
		field.setAccessible(true);
		return ((ThreadPoolExecutor) field.get(service)).getMaximumPoolSize();
	}
}
