package com.aionemu.gameserver.world.geo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
