package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SealedDanuarMysticariumMigrationTest {
	@Test
	void retailConditionsOwnMysticariumFlow() throws Exception {
		assertFalse(Files.exists(Path.of(
			"src/main/java/com/aionemu/gameserver/instance/handlers/scripts/SealedDanuarMysticariumInstance.java")));
		String conditions = Files.readString(Path.of(
			"src/main/resources/aion/definitions/compact/ai/condition-spawns.xml"));
		assertTrue(conditions.contains("<world id=\"300480000\""));
		assertTrue(conditions.contains("<variable name=\"crystal\"/>"));
		assertTrue(conditions.contains("<variable name=\"light_01\"/>"));
		assertTrue(conditions.contains("<variable name=\"dark_01\"/>"));
		assertTrue(conditions.contains("expression=\"crystal ==1\""));
	}
}
