package com.aionemu.gameserver.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class RetailAiInstanceStateMigrationTest {
	private static final Path AI = Path.of("src/main/java/com/aionemu/gameserver/ai");
	private static final Pattern STATIC_INSTANCE_MAP = Pattern.compile(
		"(?m)^\\s*private\\s+static\\s+(?:final\\s+)?(?:Map|ConcurrentMap)<\\s*WorldMapInstance\\b");
	private static final List<String> ENGINES = List.of("RetailAreaEngine", "RetailConditionSpawnEngine",
		"RetailDynamicAreaEngine", "RetailGroupControlEngine", "RetailSensoryAreaEngine", "RetailWindstreamEngine");

	@Test
	void retailAiStateRemainsOwnedByEachWorldInstance() throws Exception {
		for (String engine : ENGINES) {
			String source = Files.readString(AI.resolve(engine + ".java"));
			assertFalse(STATIC_INSTANCE_MAP.matcher(source).find(), engine);
			assertTrue(source.contains("getRuntimeState()") || source.contains("getOrCreateTransientState("), engine);
		}
	}

	@Test
	void instanceDestructionClearsRetailAiTransientState() throws Exception {
		String service = Files.readString(
			Path.of("src/main/java/com/aionemu/gameserver/services/instance/InstanceService.java"));
		for (String engine : List.of("RetailConditionSpawnEngine", "RetailAreaEngine", "RetailSensoryAreaEngine",
			"RetailWindstreamEngine", "RetailDynamicAreaEngine")) {
			assertTrue(service.contains(engine + ".clear(instance)"), engine);
		}
		assertTrue(Files.readString(AI.resolve("RetailAreaEngine.java"))
			.contains("RetailGroupControlEngine.clear(instance)"));
	}
}
