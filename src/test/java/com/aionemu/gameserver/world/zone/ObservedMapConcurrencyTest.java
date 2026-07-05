package com.aionemu.gameserver.world.zone;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ObservedMapConcurrencyTest {

	@Test
	void zoneObservedMapsAreNotBackedByLinkedHashMap() throws Exception {
		for (Path sourcePath : observedMapSources()) {
			String source = Files.readString(sourcePath);

			assertFalse(source.contains("observed = new LinkedHashMap"), sourcePath.toString());
		}
	}

	private static List<Path> observedMapSources() {
		return List.of(
			Path.of("src/main/java/com/aionemu/gameserver/ai/housing/GaleCycloneAI2.java"),
			Path.of("src/main/java/com/aionemu/gameserver/model/siege/SiegeShield.java"),
			Path.of("src/main/java/com/aionemu/gameserver/world/zone/handler/MaterialZoneHandler.java"),
			Path.of("src/main/java/com/aionemu/gameserver/world/zone/scripts/AbyssCore.java")
		);
	}
}
