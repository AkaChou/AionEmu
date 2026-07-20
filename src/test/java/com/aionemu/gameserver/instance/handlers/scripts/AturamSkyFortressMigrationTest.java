package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AturamSkyFortressMigrationTest {

	@Test
	void formalAndEventHandlersOnlyKeepFlyingRingCleanup() throws Exception {
		for (String relative : new String[] { "AturamSkyFortressInstance.java",
			"event/Event_AturamSkyFortressInstance.java" }) {
			String source = Files.readString(Path.of(
				"src/main/java/com/aionemu/gameserver/instance/handlers/scripts", relative));
			assertTrue(source.contains("ATURAM_SKY_FORTRESS_3"));
			assertTrue(source.contains("setDoorState(177, true)"));
			for (String legacy : new String[] { "onDropRegistered", "onDie(", "handleUseItemFinish",
				"GameThreadPoolServices", "Future<", "AbyssPointsService", "ItemService.addItem", "protected void sp" }) {
				assertFalse(source.contains(legacy), relative + ": " + legacy);
			}
		}
	}
}
