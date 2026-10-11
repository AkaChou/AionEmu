package com.aionemu.gameserver.world.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class RealGeoDataConcurrencyTest {

	@Test
	void collisionPrebuildUsesLifecycleBridgeWithoutNestedPools() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java"));

		assertTrue(source.contains("GameThreadPoolServices.threadPoolManager().submitLongRunning"));
		assertFalse(source.contains("ThreadPoolManager.getInstance().submitLongRunning"));
		assertFalse(source.contains("parallelStream()"));
	}

	@Test
	void worldMapLoadingUsesLifecyclePoolInsteadOfDedicatedExecutor() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java"));

		assertTrue(source.contains("GameThreadPoolServices.threadPoolManager().getForkingPool().invokeAll"));
		assertFalse(source.contains("Executors.newFixedThreadPool"));
		assertFalse(source.contains("ExecutorService executorService"));
	}

	@Test
	void worldMapLoadingRestoresInterruptedStatus() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java"));

		assertTrue(source.contains("Thread.currentThread().interrupt();"));
	}

	@Test
	void worldMapLoadingDoesNotMutateGeoMapRegistryFromWorkerThreads() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/RealGeoData.java"));
		// 锚定并发 worker 任务体（匿名 Callable 与 lambda 两种形态都兼容），断言其不直接改注册表。
		// Anchor the concurrent worker task body (anonymous Callable or lambda form) and assert it never
		// mutates the registry directly.
		int workerStart = source.indexOf("tasks.add(");
		int workerEnd = source.indexOf("});", workerStart);

		assertTrue(workerStart >= 0 && workerEnd > workerStart);
		assertFalse(source.substring(workerStart, workerEnd).contains("geoMaps.put"));
	}

	@Test
	void roundRobinPartitionCoversEveryMeshExactlyOnce() {
		List<List<String>> partitions = RealGeoData.partitionRoundRobin(
			List.of("a", "b", "c", "d", "e", "f", "g"), 3);

		assertEquals(3, partitions.size());
		assertEquals(List.of("a", "d", "g"), partitions.get(0));
		assertEquals(List.of("b", "e"), partitions.get(1));
		assertEquals(List.of("c", "f"), partitions.get(2));
		assertEquals(7, partitions.stream().mapToInt(List::size).sum());
		assertEquals(List.of("a", "b", "c", "d", "e", "f", "g"),
			partitions.stream().flatMap(List::stream).sorted().toList());
	}

	@Test
	void roundRobinPartitionRejectsNonPositiveCount() {
		assertThrows(IllegalArgumentException.class, () -> RealGeoData.partitionRoundRobin(List.of("a"), 0));
	}
}
