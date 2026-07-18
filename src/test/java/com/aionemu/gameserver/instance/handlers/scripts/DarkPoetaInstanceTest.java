package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DarkPoetaInstanceTest {
	private static final Path SCRIPTS = Path.of("src/main/java/com/aionemu/gameserver/instance/handlers/scripts");

	@Test
	void settlesOnceInsteadOfOncePerPlayer() throws Exception {
		String source = source("DarkPoetaInstance");

		assertFalse(source.contains("stopInstance(player);"));
		assertEquals(2, source.lines().filter(line -> line.contains("stopInstance();")).count());
	}

	@Test
	void globalInstanceSpawnsAreNotRepeatedForEveryPlayer() throws Exception {
		assertFalse(source("HamateIsleStoreroomInstance").contains("public void onEnterInstance"));
		assertFalse(source("CarpusIsleStoreroomInstance").contains("public void onEnterInstance"));
		assertSpawnGuarded("DraupnirCaveInstance", "spawn(237276");
		assertSpawnGuarded("DarkPoetaInstance", "spawn(npc1");
		assertSpawnGuarded("TheEternalBastionInstance", "instanceReward.addPoints(20000)");
	}

	private static void assertSpawnGuarded(String className, String spawnMarker) throws Exception {
		String source = source(className);
		int enter = source.indexOf("public void onEnterInstance");
		int guard = source.indexOf("if (spawnRace != null)", enter);
		int assignment = source.indexOf("spawnRace = player.getRace();", guard);
		int spawn = source.indexOf(spawnMarker, assignment);

		assertTrue(enter >= 0 && guard > enter && assignment > guard && spawn > assignment);
	}

	private static String source(String className) throws Exception {
		return Files.readString(SCRIPTS.resolve(className + ".java"));
	}
}
