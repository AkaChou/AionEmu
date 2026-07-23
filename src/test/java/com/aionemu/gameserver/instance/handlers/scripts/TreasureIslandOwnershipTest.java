package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class TreasureIslandOwnershipTest {

	@Test
	void keepsHandlerUntilRetailConditionProducersAreImported() throws Exception {
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		String handler = Files.readString(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/TreasureIslandOfCourageInstance.java"));

		assertFalse(conditions.contains("<world id=\"301700000\""));
		assertTrue(handler.contains("Stage_\" + stage + \"_\" + side + \"_Condition_1"));
		assertTrue(handler.contains("idrun_treasure_despawn"));
	}
}
