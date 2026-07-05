package com.aionemu.gameserver.world.geo.nav;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class NavHelperConcurrencyTest {

	@Test
	void destroyUsesLifecyclePoolInsteadOfRawThread() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/nav/NavHelper.java"));

		assertTrue(source.contains("GameThreadPoolServices.threadPoolManager().executeLongRunning"));
		assertFalse(source.contains("new Thread()"));
		assertFalse(source.contains("\"NavHelper GC\""));
	}

	@Test
	void destroyClearsConcurrentNodeMapWithoutKeySetLookupIteration() throws IOException {
		String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/world/geo/nav/NavHelper.java"));

		assertTrue(source.contains("ConcurrentHashMap"));
		assertFalse(source.contains("for (NavGeometry key : list.keySet())"));
		assertFalse(source.contains("list.get(key).parent = null"));
	}
}
